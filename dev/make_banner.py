#!/usr/bin/env python3
"""Compose the Traveler's Lantern description banner (1536x512, 3:1) from dev/icon/banner_frames/ (pack-icon-animation skill).

Art: dev/icon/build_scene.js rendered with TL.camera(0.34) -> banner_frames/ (1600px, cropped 1:1, no resampling).
The key colours (shadow, light pool) are mapped by make_icon.split and drawn under the outline.
Background, title and tagline: dev/icon/sprites/banner_*.png (draw_sprites.lua), scaled up nearest-neighbour.
Outputs dev/icon/out/banner.png (STILL_FRAME, stepped transparent corners) and banner-animated.gif (exact palette, slot 255
transparent: the corners plus every pixel unchanged since the previous frame). Copy the GIF to docs/banner.gif for GitHub."""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_icon  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
SPRITES = ICON / "sprites"
OUT = ICON / "out"
W, H = 1536, 512
FPS = make_icon.FPS
STILL_FRAME = 19
ART_CENTRE = (1160, 262)      # centre of the loop's solid-art box on the banner
OUTLINE = 13
LAYERS = (("banner_title_top", 6, (72, 92)), ("banner_title", 10, (70, 186)), ("banner_tagline", 6, (74, 352)))
GIF_LIMIT = 5 * 1024 * 1024
GRID = 8
CORNER = (4, 2, 1, 1)


def sprite(name: str, scale: int) -> Image.Image:
    im = Image.open(SPRITES / f"{name}.png").convert("RGBA")
    return im.resize((im.width * scale, im.height * scale), Image.NEAREST)


def corner_mask() -> np.ndarray:
    gw, gh = W // GRID, H // GRID
    m = np.ones((gh, gw), bool)
    for row, cut in enumerate(CORNER):
        m[row, :cut] = m[row, gw - cut:] = False
        m[gh - 1 - row, :cut] = m[gh - 1 - row, gw - cut:] = False
    return np.repeat(np.repeat(m, GRID, 0), GRID, 1)


def main() -> None:
    files = sorted((ICON / "banner_frames").glob("frame_*.png"))
    if not files:
        sys.exit("no frames in dev/icon/banner_frames - render them from Blockbench first (TL.camera(0.34))")
    parts = [make_icon.split(Image.open(f).convert("RGBA")) for f in files]
    boxes = [s.getchannel("A").getbbox() for s, _ in parts]
    x0, y0 = min(b[0] for b in boxes), min(b[1] for b in boxes)
    x1, y1 = max(b[2] for b in boxes), max(b[3] for b in boxes)
    dx, dy = ART_CENTRE[0] - (x0 + x1) // 2, ART_CENTRE[1] - (y0 + y1) // 2
    print(f"art box {x1 - x0}x{y1 - y0}, offset ({dx}, {dy})")

    bg = sprite("banner_bg", 8)
    text = [(sprite(n, s), p) for n, s, p in LAYERS]
    frames = []
    for solid, under in parts:
        sol = Image.new("RGBA", (W, H)); sol.alpha_composite(solid.crop((-dx, -dy, W - dx, H - dy)))
        und = Image.new("RGBA", (W, H)); und.alpha_composite(under.crop((-dx, -dy, W - dx, H - dy)))
        out = Image.alpha_composite(bg, und)
        ink = Image.new("RGBA", (W, H), make_icon.INK + (0,))
        ink.putalpha(sol.getchannel("A").filter(ImageFilter.MaxFilter(OUTLINE)))
        out = Image.alpha_composite(Image.alpha_composite(out, ink), sol)
        for im, pos in text:
            out.alpha_composite(im, pos)
        frames.append(out.convert("RGB"))

    mask = corner_mask()
    OUT.mkdir(parents=True, exist_ok=True)
    still = np.array(frames[STILL_FRAME].convert("RGBA"))
    still[~mask, 3] = 0
    Image.fromarray(still).save(OUT / "banner.png", optimize=True)

    stack = np.stack([np.array(f) for f in frames]).astype(np.uint32)
    code = (stack[..., 0] << 16) | (stack[..., 1] << 8) | stack[..., 2]
    colours = np.unique(code)
    if len(colours) > 255:
        sys.exit(f"{len(colours)} colours - more than a GIF palette holds next to the transparent slot")
    idx = np.searchsorted(colours, code).astype(np.uint8)
    pal = [v for c in colours for v in (int(c >> 16) & 255, int(c >> 8) & 255, int(c) & 255)]
    pal += [0, 0, 0] * (255 - len(colours)) + [255, 0, 255]
    gif = []
    for i in range(len(frames)):
        q = idx[i].copy()
        if i:
            q[idx[i] == idx[i - 1]] = 255
        q[~mask] = 255
        p = Image.fromarray(q, "P")
        p.putpalette(pal)
        gif.append(p)
    out = OUT / "banner-animated.gif"
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=1000 // FPS, loop=0, optimize=True, disposal=1,
                transparency=255)

    back, i = Image.open(out), 0
    for n in range(back.n_frames):
        back.seek(n)
        got = np.array(back.convert("RGBA"))
        for _ in range(round(back.info["duration"] * FPS / 1000)):
            want = np.array(frames[i])
            if not (np.array_equal(got[mask][:, :3], want[mask]) and (got[..., 3][mask] == 255).all()
                    and (got[..., 3][~mask] == 0).all()):
                sys.exit(f"banner GIF frame {i} decodes differently")
            i += 1
    if i != len(frames):
        sys.exit(f"banner GIF: {i} frames decoded, {len(frames)} expected")
    size = out.stat().st_size
    print(f"banner.png (frame {STILL_FRAME}); {len(frames)} frames, {len(colours)} colours -> {out.relative_to(ROOT)} "
          f"{size / 1024:.0f} KiB ({'OK' if size <= GIF_LIMIT else 'OVER'} 5 MiB), every frame verified")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
