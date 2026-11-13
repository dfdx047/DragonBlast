# Dragon Blast icon: SVG sources (docs/art) + Android adaptive icon PNGs (rsvg-convert).
import pathlib, subprocess, sys, math
repo = pathlib.Path(sys.argv[1])
art = repo / "docs/art"; art.mkdir(parents=True, exist_ok=True)
res = repo / "android/app/src/main/res"

def star(cx, cy, r, rot=-90):
    pts = []
    for i in range(10):
        rr = r if i % 2 == 0 else r * 0.42
        a = math.radians(rot + i * 36)
        pts.append(f"{cx + rr*math.cos(a):.2f},{cy + rr*math.sin(a):.2f}")
    return " ".join(pts)

DEFS = """
  <defs>
    <radialGradient id="bg" cx="50%" cy="45%" r="70%">
      <stop offset="0" stop-color="#202a38"/><stop offset="0.55" stop-color="#11161e"/><stop offset="1" stop-color="#06080b"/>
    </radialGradient>
    <radialGradient id="orb" cx="40%" cy="34%" r="70%">
      <stop offset="0" stop-color="#ffd9a3"/><stop offset="0.28" stop-color="#e9a558"/>
      <stop offset="0.68" stop-color="#a95a21"/><stop offset="1" stop-color="#3e1a07"/>
    </radialGradient>
    <radialGradient id="rim" cx="50%" cy="50%" r="50%">
      <stop offset="0.80" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity="0.45"/>
    </radialGradient>
    <radialGradient id="hl" cx="50%" cy="50%" r="50%">
      <stop offset="0" stop-color="#fff" stop-opacity="0.85"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="glow" cx="50%" cy="50%" r="50%">
      <stop offset="0" stop-color="#f4c58c" stop-opacity="0.55"/><stop offset="0.5" stop-color="#d9934c" stop-opacity="0.18"/><stop offset="1" stop-color="#d9934c" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="cool" x1="0" y1="1" x2="1" y2="0">
      <stop offset="0" stop-color="#9cc3ea" stop-opacity="0"/><stop offset="0.7" stop-color="#b9d6f2" stop-opacity="0.85"/><stop offset="1" stop-color="#ffffff"/>
    </linearGradient>
    <linearGradient id="warm" x1="1" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#e7a45c" stop-opacity="0"/><stop offset="0.7" stop-color="#f0bd7f" stop-opacity="0.85"/><stop offset="1" stop-color="#ffffff"/>
    </linearGradient>
    <filter id="blur2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2"/></filter>
    <filter id="blur1" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="0.8"/></filter>
    <filter id="blur4" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="4"/></filter>
    <clipPath id="orbclip"><circle cx="54" cy="54" r="19"/></clipPath>
  </defs>"""

CRACK = "M54 35 L51.5 43 L56 47.5 L52.5 53 L57.5 58 L54.5 64 L56.5 73"
CRACK2 = "M52.5 53 L46 55.5 L42 61"
CRACK3 = "M56 47.5 L63 45 L67.5 41"

def foreground(mono=False):
    if mono:
        return f"""<g fill="#fff">
  <path d="M10 82 Q 34 66 46 57" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.8"/>
  <path d="M98 26 Q 74 42 62 51" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.8"/>
  <g transform="translate(54 54) scale(1.2) translate(-54 -54)">
  <circle cx="54" cy="54" r="19"/>
  <polygon points="{star(49,50,4.4)}" fill="#000"/><polygon points="{star(59.5,58,4.4)}" fill="#000"/>
  <path d="{CRACK}" stroke="#000" stroke-width="1.6" fill="none" stroke-linejoin="round"/>
  </g>
</g>"""
    return f"""
  <!-- the clash behind the orb: steel from the lower left, ember from the upper right -->
  <circle cx="54" cy="54" r="40" fill="url(#glow)"/>
  <path d="M8 86 Q 30 70 44 59" stroke="url(#cool)" stroke-width="7" fill="none" stroke-linecap="round" filter="url(#blur2)" opacity="0.8"/>
  <path d="M8 86 Q 30 70 44 59" stroke="url(#cool)" stroke-width="2.2" fill="none" stroke-linecap="round"/>
  <path d="M100 22 Q 78 38 64 49" stroke="url(#warm)" stroke-width="7" fill="none" stroke-linecap="round" filter="url(#blur2)" opacity="0.8"/>
  <path d="M100 22 Q 78 38 64 49" stroke="url(#warm)" stroke-width="2.2" fill="none" stroke-linecap="round"/>
  <!-- the orb, scaled up around the centre -->
  <g transform="translate(54 54) scale(1.2) translate(-54 -54)">
  <circle cx="55" cy="57" r="19.5" fill="#000" opacity="0.45" filter="url(#blur2)"/>
  <circle cx="54" cy="54" r="19" fill="url(#orb)"/>
  <g clip-path="url(#orbclip)">
    <polygon points="{star(49,50,4.4)}" fill="#8f2a1c" opacity="0.92"/>
    <polygon points="{star(59.5,58,4.4)}" fill="#8f2a1c" opacity="0.92"/>
    <!-- light breaking through the cracks -->
    <path d="{CRACK}" stroke="#ffcf8f" stroke-width="3.2" fill="none" stroke-linejoin="round" filter="url(#blur1)" opacity="0.9"/>
    <path d="{CRACK2}" stroke="#ffcf8f" stroke-width="2.4" fill="none" stroke-linejoin="round" filter="url(#blur1)" opacity="0.8"/>
    <path d="{CRACK3}" stroke="#ffcf8f" stroke-width="2.4" fill="none" stroke-linejoin="round" filter="url(#blur1)" opacity="0.8"/>
    <path d="{CRACK}" stroke="#fff6e6" stroke-width="0.9" fill="none" stroke-linejoin="round"/>
    <path d="{CRACK2}" stroke="#fff6e6" stroke-width="0.7" fill="none" stroke-linejoin="round"/>
    <path d="{CRACK3}" stroke="#fff6e6" stroke-width="0.7" fill="none" stroke-linejoin="round"/>
    <circle cx="54" cy="54" r="19" fill="url(#rim)"/>
    <ellipse cx="47" cy="44" rx="7" ry="4.2" fill="url(#hl)" transform="rotate(-30 47 44)"/>
  </g>
  <circle cx="54" cy="54" r="19" fill="none" stroke="#ffe2b8" stroke-opacity="0.35" stroke-width="0.5"/>
  </g>
  <!-- chips flying off -->
  <polygon points="77,64 80,62.8 80.6,65.8 78,66.8" fill="#c9823f"/>
  <polygon points="29,44 31.4,42.6 32.1,45.2 29.7,46.1" fill="#b77236"/>
  <polygon points="72,74 74,73.4 74.2,75.2 72.5,75.8" fill="#a96a32"/>
  <circle cx="81" cy="57" r="0.8" fill="#ffe3bf"/><circle cx="27" cy="52" r="0.7" fill="#d8e8f8"/><circle cx="68" cy="79" r="0.6" fill="#ffe3bf"/>
"""

def svg(body, bg=True, size=108, extra=""):
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">{DEFS}
  {'<rect width="108" height="108" fill="url(#bg)"/>' if bg else ''}{extra}
{body}
</svg>"""

files = {
    "icon-foreground.svg": svg(foreground(), bg=False),
    "icon-background.svg": svg("", bg=True),
    "icon-monochrome.svg": svg(foreground(mono=True), bg=False),
    "icon-full.svg": svg(foreground(), bg=True),
}
for n, t in files.items(): (art / n).write_text(t)

def render(svgname, px, out):
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), str(art / svgname), "-o", str(out)], check=True)

dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for d, k in dens.items():
    render("icon-foreground.svg", int(108*k), res / f"drawable-{d}/ic_launcher_foreground.png")
    render("icon-background.svg", int(108*k), res / f"drawable-{d}/ic_launcher_background.png")
    render("icon-monochrome.svg", int(108*k), res / f"drawable-{d}/ic_launcher_monochrome.png")
    # legacy icons: the central 72/108 of the full art, rounded square and circle
    px = int(48*k)
    tmp = repo / "build/tmp"; tmp.mkdir(parents=True, exist_ok=True)
    big = tmp / f"full_{d}.png"
    subprocess.run(["rsvg-convert", "-w", str(int(px*108/72)), "-h", str(int(px*108/72)), str(art / "icon-full.svg"), "-o", str(big)], check=True)
    crop = int(px*108/72); off = (crop - px)//2
    for name, mask in (("ic_launcher.png", f"roundrectangle 0,0,{px-1},{px-1},{px*0.22},{px*0.22}"), ("ic_launcher_round.png", f"circle {px/2},{px/2} {px/2},0")):
        o = res / f"mipmap-{d}/{name}"; o.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["magick", str(big), "-crop", f"{px}x{px}+{off}+{off}", "+repage",
                        "(", "-size", f"{px}x{px}", "xc:none", "-fill", "white", "-draw", mask, ")",
                        "-compose", "DstIn", "-composite", str(o)], check=True)
# play-store size preview
render("icon-full.svg", 512, art / "icon-512.png")
print("ok")
# in-app logo (Compose cannot draw adaptive icons): the full art cropped to the visible part, rounded
big = repo / "build/tmp/logo_big.png"
subprocess.run(["rsvg-convert", "-w", "384", "-h", "384", str(art / "icon-full.svg"), "-o", str(big)], check=True)
o = res / "drawable-nodpi/app_logo.png"; o.parent.mkdir(parents=True, exist_ok=True)
subprocess.run(["magick", str(big), "-crop", "256x256+64+64", "+repage",
                "(", "-size", "256x256", "xc:none", "-fill", "white", "-draw", "roundrectangle 0,0,255,255,56,56", ")",
                "-compose", "DstIn", "-composite", str(o)], check=True)
