#!/usr/bin/env python3
"""Contact sheet of the in-game preview captures (scene `previews`, 1920x1080): one row per variant.
usage: python3 -I dev/preview/sheets.py [captures dir] [out.png]"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

MOD = Path(__file__).resolve().parents[2]
W = Path(sys.argv[1]) if len(sys.argv) > 1 else MOD.parents[1] / "tools/ModTest/.work/BeltLantern/26.3-fabric-1920-1080/out/previews"
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else MOD / "dev/preview/out/previews-sheet.png"
BOX = (1419, 141, 1817, 441)
rows = {}
for f in sorted(W.glob("*.png")):
    name, moment = f.stem.rsplit("-", 1)
    rows.setdefault(name, []).append((moment, f))
order = [n for n in ("hip-left", "hip-right", "clink", "clink-quiet", "clink-off", "light", "light-off", "day-soft", "day-full",
                     "smooth", "smooth-off", "smooth-rate") if n in rows]
cols = max(len(v) for v in rows.values())
w, h = BOX[2] - BOX[0], BOX[3] - BOX[1]
sheet = Image.new("RGB", (cols * (w + 6) + 6, len(order) * (h + 22) + 6), (255, 0, 255))
d = ImageDraw.Draw(sheet)
for r, name in enumerate(order):
    y = 6 + r * (h + 22)
    d.rectangle((0, y - 6, sheet.width, y + 13), fill=(20, 20, 22))
    d.text((8, y - 2), name, fill=(255, 255, 0))
    for c, (moment, f) in enumerate(rows[name]):
        x = 6 + c * (w + 6)
        sheet.paste(Image.open(f).convert("RGB").crop(BOX), (x, y + 14))
        d.text((x + w - 40, y + 18), moment, fill=(160, 200, 255))
OUT.parent.mkdir(parents=True, exist_ok=True)
sheet.save(OUT)
print(OUT, sheet.size)
