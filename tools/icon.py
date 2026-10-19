"""Draws docs/logo.png (README, Stillcraft) from the launcher icon's stones in res/drawable/ic_launcher_foreground.xml.

python tools/icon.py
"""
import re
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SIZE = 512
K = 4  # drawn large, then scaled down for smooth edges


def main():
    xml = (ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml").read_text(encoding="utf-8")
    big = SIZE * K
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, big - 1, big - 1), radius=big * 0.22, fill="#0F1220")
    # The 108-unit adaptive canvas, its 66-unit safe zone filling most of the logo.
    scale = big / 66 * 0.86
    off = big / 2 - 54 * scale
    for color, path in re.findall(r'fillColor="(#[0-9A-Fa-f]+)" android:pathData="([^"]+)"', xml):
        nums = [float(n) for n in re.findall(r"-?\d+\.?\d*", path)]
        pts = [(off + nums[i] * scale, off + nums[i + 1] * scale) for i in range(0, len(nums), 2)]
        d.polygon(pts, fill=color)
    im.resize((SIZE, SIZE), Image.LANCZOS).save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
