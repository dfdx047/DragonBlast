"""Xenos shader containers of Raging Blast 2 (USA Rev 1), for XenosRecomp (Renut).

    python3 -I tools/renut/extract_rb2_shaders.py <game dir> <out dir> [--jobs N]

<game dir> is the extracted disc (default.xex, data/X360/*.afs). Each unique container is written once as
vs_/ps_<sha256 prefix>.bin, like Dragon-Burst's extract_bl_shaders.py. The output is game data: never commit or
distribute it. Standard library only; the output does not depend on --jobs.

Disc files needed: default.xex, data/X360/st_pack_boot_pt_x360.afs and data/X360/st_pack_battle_map_pt_x360.afs.
Other .afs packs found next to them are scanned too; on USA Rev 1 the character, progress and others packs hold no
containers (checked 2026-11-13), and the sound pack is never read.

Where the shaders are:
- data/X360/st_pack_*_pt_x360.afs: AFS archives (u32 LE count, then u32 LE offset/size pairs). Every entry except
  the sound pack is STPZ (16 bytes) + 0DCS, or a bare 0DCS: Spike's chunked compression. 0DCS header: magic, u32 LE
  decompressed size, u32 LE compressed size, u32 LE chunk size; then chunks, each with a 16-byte header (magic,
  u32 LE decompressed size, u32 LE size including the header). 0LCS chunks are a byte LZ (token t: even -> t>>1
  literal bytes; odd -> copy t>>1 bytes from distance next byte), 0RCS are stored. The game also knows 0HCS / HLCS
  (Huffman, sub_823BC060) but this disc does not use them; the script stops if it meets one.
  The decompressed data is STPK packs; the shaders sit inside .spr files (db_character_shader_x360.spr in the boot
  pack, map_XX*.spr in the map pack, character .spr files), found here by scanning the decompressed bytes.
- default.xex: the engine's own shaders in .rdata. XEX2 with normal encryption (AES-128-CBC, session key decrypted
  with the retail key) and basic compression (blocks of data then zeros). AES is implemented below (decrypt only).
"""
import concurrent.futures
import hashlib
import os
import struct
import sys

# ---------------------------------------------------------------------------------------------------------------
# AES-128 decryption (FIPS-197, equivalent inverse cipher with T-tables).


def _aes_tables():
    sbox = [0] * 256
    p = q = 1
    while True:  # walk the multiplicative group: p = 3^i, q = p^-1
        p = p ^ ((p << 1) & 0xFF) ^ (0x1B if p & 0x80 else 0)
        q ^= q << 1
        q ^= q << 2
        q ^= q << 4
        q &= 0xFF
        if q & 0x80:
            q ^= 0x09
        rot = lambda v, n: ((v << n) | (v >> (8 - n))) & 0xFF
        sbox[p] = q ^ rot(q, 1) ^ rot(q, 2) ^ rot(q, 3) ^ rot(q, 4) ^ 0x63
        if p == 1:
            break
    sbox[0] = 0x63
    inv = [0] * 256
    for i, v in enumerate(sbox):
        inv[v] = i

    def mul(a, b):
        r = 0
        while b:
            if b & 1:
                r ^= a
            a = ((a << 1) ^ (0x1B if a & 0x80 else 0)) & 0xFF
            b >>= 1
        return r

    td0 = [(mul(inv[x], 14) << 24) | (mul(inv[x], 9) << 16) | (mul(inv[x], 13) << 8) | mul(inv[x], 11)
           for x in range(256)]
    ror = lambda t, n: [((v >> n) | (v << (32 - n))) & 0xFFFFFFFF for v in t]
    return sbox, inv, td0, ror(td0, 8), ror(td0, 16), ror(td0, 24)


class AES128Decrypt:
    S, SI, TD0, TD1, TD2, TD3 = _aes_tables()

    def __init__(self, key):
        S = self.S
        w = list(struct.unpack('>4I', key))
        rcon = 1
        for i in range(4, 44):
            t = w[i - 1]
            if i % 4 == 0:
                t = ((S[(t >> 16) & 0xFF] << 24) | (S[(t >> 8) & 0xFF] << 16) | (S[t & 0xFF] << 8) | S[t >> 24])
                t ^= rcon << 24
                rcon = ((rcon << 1) ^ (0x1B if rcon & 0x80 else 0)) & 0xFF
            w.append(w[i - 4] ^ t)
        rk = [w[r * 4:r * 4 + 4] for r in range(11)]
        # Decryption keys: reversed, InvMixColumns on the inner ones (TDx[S[b]] = InvMixColumns column of b).
        imc = lambda v: (self.TD0[S[v >> 24]] ^ self.TD1[S[(v >> 16) & 0xFF]] ^ self.TD2[S[(v >> 8) & 0xFF]]
                         ^ self.TD3[S[v & 0xFF]])
        self.dk = [rk[10]] + [[imc(v) for v in rk[r]] for r in range(9, 0, -1)] + [rk[0]]

    def decrypt_cbc(self, data, iv=bytes(16)):
        T0, T1, T2, T3, SI, dk = self.TD0, self.TD1, self.TD2, self.TD3, self.SI, self.dk
        k0, kl = dk[0], dk[10]
        words = struct.unpack(f'>{len(data) // 4}I', data[:len(data) // 16 * 16])
        out = []
        p0, p1, p2, p3 = struct.unpack('>4I', iv)
        for i in range(0, len(words), 4):
            c0, c1, c2, c3 = words[i:i + 4]
            s0, s1, s2, s3 = c0 ^ k0[0], c1 ^ k0[1], c2 ^ k0[2], c3 ^ k0[3]
            for r in range(1, 10):
                k = dk[r]
                s0, s1, s2, s3 = (
                    T0[s0 >> 24] ^ T1[(s3 >> 16) & 255] ^ T2[(s2 >> 8) & 255] ^ T3[s1 & 255] ^ k[0],
                    T0[s1 >> 24] ^ T1[(s0 >> 16) & 255] ^ T2[(s3 >> 8) & 255] ^ T3[s2 & 255] ^ k[1],
                    T0[s2 >> 24] ^ T1[(s1 >> 16) & 255] ^ T2[(s0 >> 8) & 255] ^ T3[s3 & 255] ^ k[2],
                    T0[s3 >> 24] ^ T1[(s2 >> 16) & 255] ^ T2[(s1 >> 8) & 255] ^ T3[s0 & 255] ^ k[3])
            out += (
                ((SI[s0 >> 24] << 24) | (SI[(s3 >> 16) & 255] << 16) | (SI[(s2 >> 8) & 255] << 8) | SI[s1 & 255])
                ^ kl[0] ^ p0,
                ((SI[s1 >> 24] << 24) | (SI[(s0 >> 16) & 255] << 16) | (SI[(s3 >> 8) & 255] << 8) | SI[s2 & 255])
                ^ kl[1] ^ p1,
                ((SI[s2 >> 24] << 24) | (SI[(s1 >> 16) & 255] << 16) | (SI[(s0 >> 8) & 255] << 8) | SI[s3 & 255])
                ^ kl[2] ^ p2,
                ((SI[s3 >> 24] << 24) | (SI[(s2 >> 16) & 255] << 16) | (SI[(s1 >> 8) & 255] << 8) | SI[s0 & 255])
                ^ kl[3] ^ p3)
            p0, p1, p2, p3 = c0, c1, c2, c3
        return struct.pack(f'>{len(out)}I', *out)


# ---------------------------------------------------------------------------------------------------------------
# default.xex -> PE image.

XEX_RETAIL_KEY = bytes.fromhex('20B185A59D28FDC340583FBB0896BF91')


def xex_image(path):
    """The loaded PE image of a retail XEX2 (encryption none/normal, basic compression or none)."""
    x = open(path, 'rb').read()
    if x[:4] != b'XEX2':
        sys.exit(f'extract_rb2_shaders: {path}: not a XEX2')
    data_at, security = struct.unpack('>I', x[8:12])[0], struct.unpack('>I', x[16:20])[0]
    fmt = None
    for i in range(struct.unpack('>I', x[20:24])[0]):
        key, value = struct.unpack('>II', x[24 + i * 8:32 + i * 8])
        if key == 0x3FF:  # file format info
            fmt = value
    info_size, encryption, compression = struct.unpack('>IHH', x[fmt:fmt + 8])
    if encryption not in (0, 1) or compression not in (0, 1):
        sys.exit(f'extract_rb2_shaders: {path}: encryption {encryption} compression {compression} not handled')
    body = x[data_at:]
    if encryption == 1:
        session = AES128Decrypt(XEX_RETAIL_KEY).decrypt_cbc(x[security + 0x150:security + 0x160])  # ECB = CBC, IV 0
        body = AES128Decrypt(session).decrypt_cbc(body)
    if compression == 1:
        image, p = bytearray(), 0
        for i in range((info_size - 8) // 8):
            size, zeros = struct.unpack('>II', x[fmt + 8 + i * 8:fmt + 16 + i * 8])
            image += body[p:p + size] + bytes(zeros)
            p += size
    else:
        image = body
    if image[:2] != b'MZ':
        sys.exit(f'extract_rb2_shaders: {path} did not decrypt to a PE image')
    return bytes(image)


# ---------------------------------------------------------------------------------------------------------------
# AFS + STPZ / 0DCS.


def afs_entries(path):
    with open(path, 'rb') as f:
        head = f.read(8)
        if head[:4] != b'AFS\0':
            sys.exit(f'extract_rb2_shaders: {path}: not an AFS archive')
        count = struct.unpack('<I', head[4:8])[0]
        table = f.read(count * 8)
    return [struct.unpack_from('<II', table, i * 8) for i in range(count)]


def lcs(src, p, end, out):
    """0LCS payload src[p:end] appended to out (bytearray)."""
    while p < end:
        t = src[p]
        n = t >> 1
        if t & 1:
            d = src[p + 1]
            p += 2
            if d == 0:
                raise ValueError('0LCS: zero distance')
            start = len(out) - d
            if d >= n:
                out += out[start:start + n]
            else:  # overlapping copy repeats the last d bytes
                out += (out[start:] * (n // d + 1))[:n]
        else:
            out += src[p + 1:p + 1 + n]
            p += 1 + n


def dcs(raw):
    """STPZ / 0DCS -> decompressed bytes."""
    p = 16 if raw[:4] == b'STPZ' else 0
    if raw[p:p + 4] != b'0DCS':
        raise ValueError(f'expected 0DCS, got {raw[p:p + 4]!r}')
    size, csize, _ = struct.unpack('<III', raw[p + 4:p + 16])
    q, end, out = p + 16, p + csize, bytearray()
    while len(out) < size:
        if q + 16 > end:
            raise ValueError('0DCS: ran out of chunks')
        magic = raw[q:q + 4]
        dsize, chunk = struct.unpack('<II', raw[q + 4:q + 12])
        before = len(out)
        if magic == b'0LCS':
            lcs(raw, q + 16, q + chunk, out)
        elif magic == b'0RCS':
            out += raw[q + 16:q + 16 + dsize]
        else:
            raise ValueError(f'0DCS: chunk {magic!r} not handled')
        if len(out) - before != dsize:
            raise ValueError(f'0DCS: chunk {magic!r} gave {len(out) - before} bytes of {dsize}')
        q += chunk
    return bytes(out)


# ---------------------------------------------------------------------------------------------------------------
# Containers.


def containers(data):
    """Xenos shader containers (ShaderContainer of XenosRecomp shader.h), with XenosRecomp's own acceptance test:
    flags 0x102A1100 (pixel) / 0x102A1101 (vertex), length virtualSize + physicalSize, field1C = field20 = 0."""
    found, p = [], 0
    while True:
        p = data.find(b'\x10\x2a\x11', p)
        if p < 0:
            return found
        if p % 4 == 0 and data[p + 3] in (0, 1) and p + 0x24 <= len(data):
            vsize, psize = struct.unpack('>II', data[p + 4:p + 12])
            f1c, f20 = struct.unpack('>II', data[p + 0x1C:p + 0x24])
            if 0x24 <= vsize + psize <= len(data) - p and f1c == 0 and f20 == 0:
                found.append(data[p:p + vsize + psize])
                p += vsize + psize
                continue
        p += 1


def scan_entry(job):
    path, index, offset, size = job
    with open(path, 'rb') as f:
        f.seek(offset)
        raw = f.read(size)
    if raw[:4] not in (b'STPZ', b'0DCS'):
        return index, None
    try:
        data = dcs(raw)
    except ValueError as e:
        raise SystemExit(f'extract_rb2_shaders: {path} entry {index}: {e}')
    return index, containers(data) if b'\x10\x2a\x11' in data else []


REQUIRED_PACKS = ('st_pack_boot_pt_x360.afs', 'st_pack_battle_map_pt_x360.afs')


def main():
    args = sys.argv[1:]
    jobs = os.cpu_count() or 1
    if '--jobs' in args:
        i = args.index('--jobs')
        jobs = max(1, int(args[i + 1]))
        del args[i:i + 2]
    if len(args) != 2:
        sys.exit(__doc__)
    game, out = args
    pack_dir = os.path.join(game, 'data', 'X360')
    if not os.path.isdir(pack_dir):
        sys.exit(f'extract_rb2_shaders: {pack_dir} missing (extract the disc first)')
    packs = sorted(n for n in os.listdir(pack_dir) if n.endswith('.afs') and 'sound' not in n)
    for need in REQUIRED_PACKS:
        if need not in packs:
            sys.exit(f'extract_rb2_shaders: {pack_dir}/{need} missing (it holds shaders)')

    found, report = [], []
    with concurrent.futures.ProcessPoolExecutor(jobs) as pool:
        for name in packs:
            path = os.path.join(pack_dir, name)
            work = [(path, i, o, s) for i, (o, s) in enumerate(afs_entries(path))]
            # Results come back in entry order, so the output is the same for any job count.
            n, skipped = 0, 0
            for index, conts in pool.map(scan_entry, work, chunksize=4):
                if conts is None:
                    skipped += 1
                    continue
                found += conts
                n += len(conts)
            report.append(f'{name}: {len(work)} entries ({skipped} not STPZ), {n} containers')
    from_xex = containers(xex_image(os.path.join(game, 'default.xex')))
    report.append(f'default.xex: {len(from_xex)} containers')
    found += from_xex

    os.makedirs(out, exist_ok=True)
    seen, kinds = set(), {'vs': 0, 'ps': 0}
    for c in found:
        digest = hashlib.sha256(c).hexdigest()
        if digest in seen:
            continue
        seen.add(digest)
        stage = 'vs' if c[3] & 1 else 'ps'
        kinds[stage] += 1
        with open(os.path.join(out, f'{stage}_{digest[:16]}.bin'), 'wb') as w:
            w.write(c)
    for line in report:
        print('  ' + line)
    print(f"extract_rb2_shaders: {len(found)} containers, {kinds['vs']} VS + {kinds['ps']} PS unique -> {out}")


if __name__ == '__main__':
    main()
