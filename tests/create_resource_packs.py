import json
import struct
import sys
import zlib
from pathlib import Path


def png(path, width, height, pixels):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

    raw = b''.join(b'\x00' + bytes(pixels[y * width * 4:(y + 1) * width * 4]) for y in range(height))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))


def texture(width, height, palette, sidebar=False):
    pixels = bytearray(width * height * 4)

    def rect(x, y, w, h, color):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                offset = (yy * width + xx) * 4
                pixels[offset:offset + 4] = bytes((*color, 255))

    bg, light, dark, inset = palette
    frame_width = width if sidebar else 276
    rect(0, 0, frame_width, 166, dark)
    rect(1, 1, frame_width - 2, 164, light)
    rect(3, 3, frame_width - 6, 160, bg)
    rect(3, 163, frame_width - 4, 2, dark)
    if sidebar:
        rect(frame_width - 4, 0, 4, 166, bg)
        rect(frame_width - 4, 1, 4, 2, light)
        rect(frame_width - 4, 163, 4, 2, dark)
        for y in range(18, 157, 20):
            rect(4, y + 18, frame_width - 8, 1, inset)
    else:
        rect(4, 17, 97, 140, inset)
        for y in [83, 101, 119, 141]:
            for x in range(107, 269, 18):
                rect(x, y, 18, 18, dark)
                rect(x + 1, y + 1, 17, 17, light)
                rect(x + 1, y + 1, 16, 16, inset)
        for x, y, size in [(135, 36, 18), (161, 36, 18), (215, 33, 26)]:
            rect(x, y, size, size, dark)
            rect(x + 1, y + 1, size - 1, size - 1, light)
            rect(x + 1, y + 1, size - 2, size - 2, inset)
    return pixels


def create(destination, minecraft='1.20.1'):
    target = json.loads((Path(__file__).resolve().parents[1] / 'versions.json').read_text(encoding='utf-8'))[minecraft]
    pack_format = target['packFormat']
    merchant_texture = 'villager2.png' if minecraft == '1.20.1' else 'villager.png'
    for name, palette, custom in [
        ('hovernt-test-auto', ((164, 194, 214), (227, 243, 252), (55, 79, 99), (115, 146, 165)), False),
        ('hovernt-test-custom', ((216, 193, 145), (255, 237, 194), (100, 76, 37), (174, 151, 106)), True),
    ]:
        pack = destination / name
        pack.mkdir(parents=True, exist_ok=True)
        metadata = {'description': "Hovern't resource-pack acceptance fixture"}
        if isinstance(pack_format, list):
            metadata.update(min_format=pack_format, max_format=pack_format)
        else:
            metadata['pack_format'] = pack_format
        (pack / 'pack.mcmeta').write_text(json.dumps({'pack': metadata}))
        png(pack / f'assets/minecraft/textures/gui/container/{merchant_texture}', 512, 256, texture(512, 256, palette))
        if custom:
            png(pack / 'assets/hovernt/textures/gui/sidebar.png', 194, 166, texture(194, 166, palette, True))
    print(f'Two resource-pack fixtures written to {destination}')


if __name__ == '__main__':
    create(Path(sys.argv[1]), sys.argv[2] if len(sys.argv) > 2 else '1.20.1')
