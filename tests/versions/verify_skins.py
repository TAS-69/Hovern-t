import json
import sys
from pathlib import Path
from PIL import Image


def verify(directory, skin, fixture):
    geometry = fixture['geometry']
    background = (164, 194, 214) if skin == 'auto' else (216, 193, 145)
    results = []
    for filename in ['expanded.png', 'scrolled.png', 'reexpanded.png']:
        with Image.open(directory / filename) as source:
            image = source.convert('RGB')
        sx = image.width / geometry['width']
        sy = image.height / geometry['height']
        left, top = geometry['left'], geometry['top']
        height = geometry['merchantHeight']

        def fraction(box):
            x1, y1, x2, y2 = box
            pixels = image.crop((round(x1 * sx), round(y1 * sy), round(x2 * sx), round(y2 * sy)))
            return sum(pixel == background for pixel in pixels.getdata()) / (pixels.width * pixels.height)

        sidebar = fraction((left - geometry['sidebar'] + 4, top + 4, left - 4, top + height - 4))
        merchant = fraction((left + 105, top + 4, left + geometry['merchantWidth'] - 5, top + height - 4))
        seam = fraction((left + 1, top + 8, left + 2, top + height - 8))
        assert sidebar > 0.60, f'{filename}: sidebar did not adopt the resource pack ({sidebar:.3f})'
        assert merchant > 0.20, f'{filename}: merchant did not adopt the resource pack ({merchant:.3f})'
        assert seam > 0.98, f'{filename}: join contains a mismatched vertical border ({seam:.3f})'
        results.append({'screenshot': filename, 'sidebarBackground': round(sidebar, 4),
                        'merchantBackground': round(merchant, 4), 'seamBackground': round(seam, 4)})
    return {'status': 'passed', 'checks': 9, 'screenshots': results}


if __name__ == '__main__':
    directory = Path(sys.argv[1])
    report = json.loads((directory / 'client.json').read_text(encoding='utf-8'))
    print(json.dumps(verify(directory, report['skin'], report['fixture']), indent=2))
