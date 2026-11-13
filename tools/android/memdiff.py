#!/usr/bin/env python3
"""Compare guest memory dumps (files/dumps/*.bin from "@dump", game_state_jni.cpp) to find state variables.

    memdiff.py --menu m1.bin m2.bin ... --fight f1.bin f2.bin ...

Lists 32-bit big-endian words that are 0 in every menu dump and a guest pointer (0x40000000-0xF0000000) in every
fight dump ("pointer to the battle object"), then words that hold one value in all menus and another in all fights
("flags").
"""
import argparse
import struct


def load(path):
    data = open(path, 'rb').read()
    assert data[:4] == b'RB2D', path
    out, o = {}, 4
    while o < len(data):
        addr, size = struct.unpack_from('<II', data, o)
        out[addr] = data[o + 8:o + 8 + size]
        o += 8 + size
    return out


def words(dump):
    for base, blob in dump.items():
        n = len(blob) // 4
        vals = struct.unpack('>%dI' % n, blob[:n * 4])
        for i, v in enumerate(vals):
            yield base + i * 4, v


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--menu', nargs='+', required=True)
    ap.add_argument('--fight', nargs='+', required=True)
    ap.add_argument('--limit', type=int, default=60)
    a = ap.parse_args()
    menus = [dict(words(load(p))) for p in a.menu]
    fights = [dict(words(load(p))) for p in a.fight]
    ptr, flags = [], []
    for addr in sorted(menus[0]):
        mv = [m.get(addr) for m in menus]
        fv = [f.get(addr) for f in fights]
        if None in mv or None in fv:
            continue
        if all(v == 0 for v in mv) and all(0x40000000 <= v < 0xF0000000 for v in fv):
            ptr.append((addr, fv))
        elif len(set(mv)) == 1 and len(set(fv)) == 1 and mv[0] != fv[0]:
            flags.append((addr, mv[0], fv[0]))
    print(f'pointers (0 in menus, pointer in fights): {len(ptr)}')
    for addr, fv in ptr[:a.limit]:
        print(f'  {addr:08X}  ' + ' '.join(f'{v:08X}' for v in fv))
    print(f'flags (one value in menus, another in fights): {len(flags)}')
    for addr, m, f in flags[:a.limit]:
        print(f'  {addr:08X}  menu {m:08X}  fight {f:08X}')


if __name__ == '__main__':
    main()
