import datetime
import hashlib
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build' / 'versions'
TARGETS = json.loads((ROOT / 'versions.json').read_text(encoding='utf-8'))
VERSION = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('version='))
SKIN_TARGETS = ['1.20.5', '1.21.2', '1.21.6', '26.3']


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    evidence = {'date': datetime.date.today().isoformat(), 'modVersion': VERSION,
                'fabricLoader': '0.19.5', 'targets': [], 'skins': [], 'baselineRuntimeClasses': {}}
    for minecraft, target in TARGETS.items():
        for loader in ['fabric', 'neoforge']:
            directory = WORK / 'results' / f'{loader}-{minecraft}'
            build = json.loads((directory / 'build.json').read_text(encoding='utf-8'))
            client = json.loads((directory / 'client.json').read_text(encoding='utf-8'))
            assert build['status'] == client['status'] == 'passed', directory.name
            assert build['modVersion'] == VERSION, directory.name
            filename = f'hovernt-{loader}-{minecraft}-{VERSION}.jar'
            screenshots = {name: digest(directory / name) for name in ['expanded.png', 'scrolled.png', 'collapsed.png', 'reexpanded.png']}
            evidence['targets'].append({'minecraft': minecraft, 'loader': loader, 'java': target['java'],
                'neoForge': target['neoForge'] if loader == 'neoforge' else None,
                'build': 'passed', 'client': 'passed', 'assertions': client['fixture']['checks'],
                'offers': client['fixture']['offers'], 'screenshots': screenshots,
                'jar': filename, 'sha256': digest(ROOT / 'dist' / VERSION / filename)})
    for minecraft in SKIN_TARGETS:
        for loader in ['fabric', 'neoforge']:
            for skin in ['auto', 'custom']:
                directory = WORK / 'results' / f'{loader}-{minecraft}-{skin}'
                client = json.loads((directory / 'client.json').read_text(encoding='utf-8'))
                assert client['status'] == client['skinVerification']['status'] == 'passed', directory.name
                evidence['skins'].append({'minecraft': minecraft, 'loader': loader, 'skin': skin,
                    'client': 'passed', 'assertions': client['fixture']['checks'],
                    'pixels': client['skinVerification'],
                    'screenshots': {name: digest(directory / name) for name in ['expanded.png', 'scrolled.png', 'collapsed.png', 'reexpanded.png']}})
    for loader in ['fabric', 'neoforge']:
        baseline = ROOT / 'build' / 'releases' / '1.1.0' / f'hovernt-{loader}-1.20.1-1.1.0.jar'
        baseline_sums = dict(line.split(maxsplit=1)[::-1] for line in (baseline.parent / 'SHA256SUMS.txt').read_text().splitlines())
        assert digest(baseline) == baseline_sums[baseline.name], f'Published baseline checksum mismatch: {baseline.name}'
        current = ROOT / 'dist' / VERSION / f'hovernt-{loader}-1.20.1-{VERSION}.jar'
        with zipfile.ZipFile(baseline) as old, zipfile.ZipFile(current) as new:
            old_classes = {name: old.read(name) for name in old.namelist() if name.endswith('.class')}
            new_classes = {name: new.read(name) for name in new.namelist() if name.endswith('.class')}
            assert old_classes == new_classes, f'1.20.1 {loader} runtime changed; repeat third-party mod verification'
            evidence['baselineRuntimeClasses'][loader] = {'version': '1.1.0', 'identical': True, 'classes': len(new_classes)}
    evidence['totals'] = {'targets': len(evidence['targets']),
        'clientAssertions': sum(target['assertions'] for target in evidence['targets']),
        'skinProfiles': len(evidence['skins']), 'skinClientAssertions': sum(skin['assertions'] for skin in evidence['skins']),
        'skinPixelAssertions': sum(skin['pixels']['checks'] for skin in evidence['skins'])}
    path = ROOT / 'tests' / 'versions' / 'evidence.json'
    path.write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(evidence['totals'], indent=2))


if __name__ == '__main__':
    main()
