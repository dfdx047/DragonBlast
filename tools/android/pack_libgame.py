#!/usr/bin/env python3
"""libgame.so -> <out>/<name>.zdat (encrypted) + <name>.json. Called by tools/android/build_native.sh.

The recompiled game (libgame.so: the translated default.xex, the hooks, the Renut and its translated shaders) is game
data: it never ships in the clear. The key (AES-256-GCM) is the SHA-256 of the default.xex PE image
(tools/xex-image, the same android/native/src/loader/xex_image.cpp the launcher runs): only someone with the disc gets
it. The APK carries the .zdat and the json with the SHA-256 of the key (to say "unsupported version" before trying),
never the key.

.zdat format (read by GameLib.kt):
  24-byte header: "ZDT1", chunk size (u32 LE), .so size (u64 LE), nonce prefix (8 bytes);
  then every chunk i of the .so, encrypted (AES-256-GCM, nonce = prefix + i as u32 BE, AAD = the header) with its
  16-byte tag at the end. 1 MiB chunks: the phone decrypts without holding the whole .so in memory.

Usage: pack_libgame.py <libgame.so> <llvm-strip> <default.xex> <out dir> [name, default libgame]
The name is libgame_v80 for the Armv8.0 set of the universal APK.
"""
import hashlib
import json
import os
import struct
import subprocess
import sys
import tempfile

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

CHUNK = 1 << 20


def main():
    so, strip, xex, out = sys.argv[1:5]
    name = sys.argv[5] if len(sys.argv) > 5 else 'libgame'
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    subprocess.run(['bash', os.path.join(root, 'tools/xex-image/build.sh')], check=True)
    tmpdir = os.path.join(root, 'build/tmp')
    os.makedirs(tmpdir, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=tmpdir) as t:
        pe_path = os.path.join(t, 'pe.bin')
        info = subprocess.run([os.path.join(root, 'build/host/xex-image'), xex, pe_path], check=True,
                              capture_output=True, text=True).stdout.split()
        title_id = info[0]
        with open(pe_path, 'rb') as f:
            key = hashlib.sha256(f.read()).digest()
        stripped = os.path.join(t, 'libgame.so')
        subprocess.run([strip, '--strip-unneeded', so, '-o', stripped], check=True)
        with open(stripped, 'rb') as f:
            plain = f.read()
    with open(xex, 'rb') as f:
        xex_sha256 = hashlib.sha256(f.read()).hexdigest()

    prefix = os.urandom(8)
    header = b'ZDT1' + struct.pack('<IQ', CHUNK, len(plain)) + prefix
    aes = AESGCM(key)
    os.makedirs(out, exist_ok=True)
    tmp = os.path.join(out, name + '.zdat.part')
    with open(tmp, 'wb') as f:
        f.write(header)
        for i in range(0, len(plain), CHUNK):
            f.write(aes.encrypt(prefix + struct.pack('>I', i // CHUNK), plain[i:i + CHUNK], header))
    os.replace(tmp, os.path.join(out, name + '.zdat'))
    meta = {
        'format': 1,
        'title_id': title_id,
        'xex_sha256': xex_sha256,
        'key_sha256': hashlib.sha256(key).hexdigest(),
        # Identifies this libgame.so (the phone decrypts again when the APK brings another one).
        'id': hashlib.sha256(header).hexdigest()[:16],
        'size': len(plain),
    }
    with open(os.path.join(out, name + '.json'), 'w') as f:
        json.dump(meta, f, indent=1)
    print(f'{name}.zdat: {len(plain) / 1e6:.1f} MB, title {title_id}, id {meta["id"]}')


if __name__ == '__main__':
    main()
