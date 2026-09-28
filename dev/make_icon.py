#!/usr/bin/env python3
"""Compose the Traveler's Lantern icon from the Blockbench renders in dev/icon/frames/ (pack-icon-animation skill).

Light and shadow are rendered in key colours (see dev/icon/draw_sprites.lua): the golem's shadow and the ground light pool.
They are mapped to their real colours here and drawn under the dark outline, which hugs only the solid art.

Outputs (dev/icon/out/, since ./gradlew dist may clear dist/):
  icon-animated.gif  Modrinth icon, 240 px, <= 256 KiB, exact palette
  icon-512.png       still (STILL_FRAME)
  src/main/resources/assets/travelers_lantern/icon.png   the mod's own icon (128 px still)
  dev/icon/contact.png                                    frame sheet for review
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
OUT = ICON / "out"
MOD_ICON = ROOT / "src/main/resources/assets/travelers_lantern/icon.png"
S = 512
FPS = 20
STILL_FRAME = 19
GIF_SIZE = 240
GIF_LIMIT = 256 * 1024
OUTLINE = 17
INK = (11, 15, 32)
NAVY = (0x1A, 0x21, 0x40)

# key colour -> real colour (all under the outline)
UNDER = {
    (255, 0, 255): (0x12, 0x17, 0x2E),        # golem shadow
    (0, 255, 255): (0x2C, 0x2E, 0x4C),        # pool outer
    (0, 255, 192): (0x4E, 0x42, 0x56),        # pool mid
    (0, 255, 128): (0x80, 0x5C, 0x52),        # pool inner
}

CROP = None     # fixed (x0, y0, side) in render pixels, or None to derive from the frames
CROP_MARGIN = 1.08
CROP_PCT = 0.95


def split(im: Image.Image) -> tuple:
    """(solid art RGBA, under-layer RGBA) with the key colours mapped."""
    a = np.array(im.convert("RGBA"))
    under = np.zeros_like(a)
    for key, col in UNDER.items():
        m = (a[..., 3] > 0) & (a[..., 0] == key[0]) & (a[..., 1] == key[1]) & (a[..., 2] == key[2])
        under[m] = col + (255,)
        a[m] = 0
    return Image.fromarray(a), Image.fromarray(under)


def crop_box(raws: list) -> tuple:
    if CROP:
        x0, y0, side = CROP
        return (x0, y0, x0 + side, y0 + side)
    boxes = [split(im)[0].getchannel("A").getbbox() for im in raws]
    boxes = [b for b in boxes if b]
    # robust box: the pirouette's outflung arms may leave the frame for a moment
    lo = lambda v: sorted(v)[int(len(v) * (1 - CROP_PCT))]
    hi = lambda v: sorted(v)[int(len(v) * CROP_PCT) - 1]
    x0, y0 = lo([b[0] for b in boxes]), lo([b[1] for b in boxes])
    x1, y1 = hi([b[2] for b in boxes]), hi([b[3] for b in boxes])
    side = int(max(x1 - x0, y1 - y0) * CROP_MARGIN)
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    return (cx - side // 2, cy - side // 2, cx - side // 2 + side, cy - side // 2 + side)


def compose(im: Image.Image, box: tuple, size: int = S, bg: Image.Image = None) -> Image.Image:
    solid, under = (x.crop(box).resize((size, size), Image.NEAREST) for x in split(im))
    base = bg.copy() if bg is not None else Image.new("RGBA", (size, size), NAVY + (255,))
    base = Image.alpha_composite(base, under)
    ink = Image.new("RGBA", (size, size), INK + (0,))
    ink.putalpha(solid.getchannel("A").filter(ImageFilter.MaxFilter(OUTLINE if size == S else OUTLINE * size // S | 1)))
    return Image.alpha_composite(Image.alpha_composite(base, ink), solid)


def write_gif(frames: list, out: Path) -> None:
    """Exact palette (every colour kept); slot 255 is transparent and marks every pixel unchanged since the previous frame
    (disposal 1 keeps what is underneath), and Pillow's optimize then crops each frame to the box that still changes."""
    stack = np.stack([np.array(f.convert("RGB")) for f in frames]).astype(np.uint32)
    code = (stack[..., 0] << 16) | (stack[..., 1] << 8) | stack[..., 2]
    colours = np.unique(code)
    if len(colours) > 255:
        sys.exit(f"{len(colours)} colours - more than a GIF palette holds next to the transparent slot")
    idx = np.searchsorted(colours, code).astype(np.uint8)
    pal = []
    for c in colours:
        pal += [int(c >> 16) & 255, int(c >> 8) & 255, int(c) & 255]
    pal += [0, 0, 0] * (255 - len(colours)) + [255, 0, 255]
    ims = []
    for i in range(len(frames)):
        q = idx[i].copy()
        if i:
            q[idx[i] == idx[i - 1]] = 255
        p = Image.fromarray(q, "P")
        p.putpalette(pal)
        ims.append(p)
    ims[0].save(out, save_all=True, append_images=ims[1:], duration=1000 // FPS, loop=0, optimize=True, disposal=1,
                transparency=255)
    # Pillow merges identical consecutive frames into one longer frame: expand by duration when checking
    back, i = Image.open(out), 0
    for n in range(back.n_frames):
        back.seek(n)
        got = np.array(back.convert("RGB"))
        for _ in range(round(back.info["duration"] * FPS / 1000)):
            want = np.array(frames[i].convert("RGB"))
            if not np.array_equal(got, want):
                sys.exit(f"{out.name}: frame {i} decodes differently ({(got != want).any(-1).sum()} px)")
            i += 1
    if i != len(frames):
        sys.exit(f"{out.name}: {i} frames decoded, {len(frames)} expected")
    print(f"{out.name}: {len(colours)} colours, {i} frames verified")


def main() -> None:
    files = sorted((ICON / "frames").glob("frame_*.png"))
    if not files:
        sys.exit("no frames in dev/icon/frames - render them from Blockbench first")
    raws = [Image.open(f).convert("RGBA") for f in files]
    box = crop_box(raws)
    print("crop", box, "side", box[2] - box[0])
    frames = [compose(im, box) for im in raws]

    OUT.mkdir(parents=True, exist_ok=True)
    frames[STILL_FRAME].convert("RGB").save(OUT / "icon-512.png", optimize=True)
    MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
    frames[STILL_FRAME].convert("RGB").resize((128, 128), Image.NEAREST).save(MOD_ICON, optimize=True)

    small = [f.convert("RGB").resize((GIF_SIZE, GIF_SIZE), Image.NEAREST) for f in frames]
    out = OUT / "icon-animated.gif"
    write_gif(small, out)

    cols = 10
    rows = (len(frames) + cols - 1) // cols
    contact = Image.new("RGB", (cols * 128, rows * 128))
    for i, f in enumerate(frames):
        contact.paste(f.convert("RGB").resize((128, 128), Image.LANCZOS), ((i % cols) * 128, (i // cols) * 128))
    contact.save(ICON / "contact.png")

    size = out.stat().st_size
    print(f"{len(frames)} frames @ {FPS} fps -> {out.relative_to(ROOT)} {size / 1024:.0f} KiB "
          f"({'OK' if size <= GIF_LIMIT else 'OVER'} Modrinth 256 KiB limit); still = frame {STILL_FRAME}")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
