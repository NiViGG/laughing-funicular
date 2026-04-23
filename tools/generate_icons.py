#!/usr/bin/env python3
"""Generate launcher icons from SVG template."""
import os
try:
    import cairosvg
except ImportError:
    raise SystemExit("pip install cairosvg")

SVG = """<svg xmlns='http://www.w3.org/2000/svg' width='{s}' height='{s}' viewBox='0 0 {s} {s}'>
  <defs><radialGradient id='g' cx='50%' cy='50%' r='50%'>
    <stop offset='0%' stop-color='#00ff88' stop-opacity='0.3'/>
    <stop offset='100%' stop-color='#080c12'/>
  </radialGradient></defs>
  <rect width='{s}' height='{s}' rx='{r}' fill='#080c12'/>
  <rect width='{s}' height='{s}' rx='{r}' fill='url(#g)'/>
  <circle cx='{c}' cy='{c}' r='{cr}' fill='none' stroke='#00ff88' stroke-width='{sw}' opacity='0.4'/>
  <text x='{c}' y='{c}' text-anchor='middle' dominant-baseline='central'
        font-family='monospace' font-size='{fs}' fill='#00ff88'>&#x2726;</text>
</svg>"""

SIZES = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96,
    "xxhdpi": 144, "xxxhdpi": 192,
}
ROOT = "app/src/main/res"

for dpi, s in SIZES.items():
    out_dir = f"{ROOT}/mipmap-{dpi}"
    os.makedirs(out_dir, exist_ok=True)
    svg = SVG.format(s=s, r=s//4, c=s//2, cr=int(s*0.375), sw=max(1, s//24), fs=int(s*0.45))
    cairosvg.svg2png(bytestring=svg.encode(), write_to=f"{out_dir}/ic_launcher.png", output_width=s, output_height=s)
    print(f"✓ {out_dir}/ic_launcher.png ({s}x{s})")
