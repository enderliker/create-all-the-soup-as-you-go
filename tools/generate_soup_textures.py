"""Generate distinct, seamless animated pixel textures for every soup fluid.

Uses only the Python standard library so the source artwork can be regenerated.
Run from the project root with: python tools/generate_soup_textures.py
"""

from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path


OUT = Path("src/main/resources/assets/allthesoupasyougo/textures/block")

# Broth, reflection, shadow, and small ingredients. Coordinates repeat every 16 px.
SOUPS = {
    "bone_broth": ("#AFA57F", "#D9D0AC", "#858064", [
        ("bone", 4, 4, "#EEE6C9"), ("bone", 11, 11, "#DED4B5"),
        ("herb", 12, 3, "#6B894C"),
    ]),
    "onion_soup": ("#AC7942", "#D4A86C", "#805636", [
        ("ring", 4, 4, "#E9D5A1"), ("ring", 11, 11, "#D7BF88"),
        ("herb", 12, 3, "#789155"),
    ]),
    "mushroom_stew": ("#C9886B", "#DFA285", "#A96B53", [
        ("mushroom", 3, 4, "#E9C7A3"), ("mushroom", 12, 11, "#E9C7A3"),
        ("chunk", 10, 3, "#A65345"),
    ]),
    "beetroot_soup": ("#A92736", "#D24748", "#791C31", [
        ("chunk", 4, 4, "#EE7762"), ("chunk", 12, 10, "#D84E51"),
        ("herb", 10, 3, "#6E9C56"),
    ]),
    "rabbit_stew": ("#C47A48", "#E9A766", "#95583D", [
        ("chunk", 4, 4, "#F3BE73"), ("chunk", 11, 10, "#93523D"),
        ("herb", 12, 3, "#77985A"),
    ]),
    "suspicious_stew": ("#B19565", "#DBC18A", "#8F744C", [
        ("flower", 4, 4, "#A468B9"), ("flower", 11, 11, "#DCB557"),
        ("herb", 12, 3, "#6CA858"),
    ]),
    "baked_cod_stew": ("#D2AD87", "#EED6AD", "#AA8667", [
        ("fish", 4, 4, "#F4E9D0"), ("fish", 11, 11, "#EEE1C4"),
        ("herb", 12, 3, "#72A06B"),
    ]),
    "beef_stew": ("#985337", "#BB7650", "#6C392F", [
        ("chunk", 4, 4, "#6B322B"), ("chunk", 11, 10, "#D58C52"),
        ("herb", 12, 3, "#799358"),
    ]),
    "chicken_soup": ("#D9AA62", "#F3CC85", "#AD8148", [
        ("chunk", 4, 4, "#F7E0B2"), ("chunk", 11, 11, "#EEE0BA"),
        ("herb", 11, 3, "#77A66A"),
    ]),
    "fish_stew": ("#B66B50", "#DA9472", "#8C4C3C", [
        ("fish", 4, 4, "#F0DCC4"), ("fish", 11, 11, "#F0DCC4"),
        ("chunk", 11, 3, "#C74338"),
    ]),
    "noodle_soup": ("#D8AF69", "#F0CE89", "#B28A53", [
        ("noodle", 3, 4, "#F9E1AD"), ("noodle", 10, 10, "#F9E1AD"),
        ("herb", 11, 3, "#7F9F5F"),
    ]),
    "pumpkin_soup": ("#D88231", "#F6AB4B", "#AE5F2B", [
        ("swirl", 4, 4, "#FFD080"), ("swirl", 11, 10, "#FFD080"),
        ("herb", 11, 3, "#6F9C51"),
    ]),
    "vegetable_soup": ("#8F9255", "#B6AA69", "#676C40", [
        ("chunk", 4, 4, "#E69A4F"), ("chunk", 11, 10, "#6FAA59"),
        ("herb", 11, 3, "#4F8547"),
    ]),
}

SHAPES = {
    "bone": ((-1, -1), (1, -1), (0, 0), (0, 1), (-1, 2), (1, 2)),
    "ring": ((-1, 0), (0, -1), (1, 0), (1, 1), (0, 2), (-1, 1)),
    "chunk": ((0, 0), (1, 0), (0, 1), (1, 1)),
    "mushroom": ((-1, 0), (0, 0), (1, 0), (0, 1)),
    "herb": ((0, 0), (1, -1), (1, 1)),
    "flower": ((0, -1), (-1, 0), (0, 0), (1, 0), (0, 1)),
    "fish": ((-1, 0), (0, 0), (1, 0), (0, 1), (2, -1)),
    "noodle": ((-1, -1), (0, -1), (0, 0), (0, 1), (1, 1)),
    "swirl": ((-1, 0), (0, 0), (1, 0), (1, 1), (0, 2)),
}


def rgb(value: str) -> tuple[int, int, int]:
    return tuple(bytes.fromhex(value.removeprefix("#")))


def tile_pixel(name: str, x: int, y: int, frame: int) -> bytes:
    broth, light, dark, ingredients = SOUPS[name]
    # The broad wavy highlights stay subtle, so toppings remain readable.
    wave = (x * 3 + y * 2 + frame * 2 + (x * y) % 5) % 17
    color = light if wave in (0, 1, 2) else dark if wave == 9 else broth
    for shape, center_x, center_y, ingredient_color in ingredients:
        for dx, dy in SHAPES[shape]:
            if (center_x + dx) % 16 == x and (center_y + dy) % 16 == y:
                color = ingredient_color
    return bytes((*rgb(color), 235))


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(
        ">I", zlib.crc32(kind + payload) & 0xFFFFFFFF
    )


def write_png(path: Path, width: int, frame_size: int, flowing: bool) -> None:
    height = frame_size * 8
    rows = bytearray()
    for y in range(height):
        frame, local_y = divmod(y, frame_size)
        rows.append(0)
        for x in range(width):
            offset = frame if flowing else 0
            tile_x = (x + offset) % 16
            tile_y = (local_y + offset // 2) % 16
            rows.extend(tile_pixel(path.stem.rsplit("_", 1)[0], tile_x, tile_y, frame))
    header = struct.pack(">2I5B", width, height, 8, 6, 0, 0, 0)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", header)
        + png_chunk(b"IDAT", zlib.compress(bytes(rows), 9))
        + png_chunk(b"IEND", b"")
    )


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name in SOUPS:
        for kind, size in (("still", 16), ("flow", 32)):
            image = OUT / f"{name}_{kind}.png"
            write_png(image, size, size, kind == "flow")
            metadata = {"animation": {"frametime": 4}}
            image.with_suffix(".png.mcmeta").write_text(json.dumps(metadata, indent=2) + "\n")
    print(f"Generated distinct animated textures for {len(SOUPS)} soups")


if __name__ == "__main__":
    main()
