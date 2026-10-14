import json
import re
import struct
import sys
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGETS = json.loads((ROOT / 'versions.json').read_text(encoding='utf-8'))
paths = sorted(Path(sys.argv[1]).glob('*.jar'))
assert paths, 'No installable JARs found'
for path in paths:
    match = re.fullmatch(r'hovernt-(fabric|neoforge)-(.+)-(\d+\.\d+\.\d+)\.jar', path.name)
    assert match, f'Unrecognized package name: {path.name}'
    loader, minecraft, version = match.groups()
    target = TARGETS[minecraft]
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        assert not any('Smoke' in name or 'Compatibility' in name or 'Acceptance' in name or '/versions/' in name or '/compat/' in name for name in names), f'Test fixture shipped in {path}'
        assert jar.read('assets/hovernt/icon.png') == (ROOT / 'hovernt_logo.png').read_bytes(), f'Incorrect icon in {path}'
        mixins = json.loads(jar.read('hovernt.mixins.json'))
        assert mixins['client'] == ['MerchantScreenMixin'], f'Test mixin shipped in {path}'
        assert mixins['compatibilityLevel'] == f"JAVA_{target['java']}"
        for cls in ['BookNames', 'SidebarSkin', 'TradeLabels', 'SidebarLayout']:
            content = jar.read(f'dev/tas69/hovernt/{cls}.class')
            assert struct.unpack('>H', content[6:8])[0] == 44 + target['java'], f'Wrong Java target in {path}'
        pack = json.loads(jar.read('pack.mcmeta'))['pack']
        if isinstance(target['packFormat'], list):
            assert pack['min_format'] == pack['max_format'] == target['packFormat']
        else:
            assert pack['pack_format'] == target['packFormat']
        refmap_required = (loader == 'fabric' and not minecraft.startswith('26.')) or (loader == 'neoforge' and minecraft == '1.20.1')
        if refmap_required:
            assert mixins['refmap'] == 'hovernt.refmap.json'
            refmap = json.loads(jar.read('hovernt.refmap.json'))
            assert refmap.get('mappings'), f'Missing runtime mappings in {path}'
        else:
            assert 'refmap' not in mixins
        if loader == 'fabric':
            metadata = json.loads(jar.read('fabric.mod.json'))
            assert metadata['name'] == "Hovern't" and metadata['version'] == version
            assert metadata['environment'] == 'client'
            assert metadata['authors'] == ['TAS-69'], f'Incorrect public credit in {path}'
            assert metadata['icon'] == 'assets/hovernt/icon.png'
            assert metadata['depends']['minecraft'] == minecraft
            assert len(metadata['jars']) == 2 + int('resourceLoaderExtra' in target), f'Missing embedded resource loader in {path}'
        else:
            early = minecraft in ['1.20.1', '1.20.2', '1.20.3', '1.20.4']
            filename = 'META-INF/mods.toml' if early else 'META-INF/neoforge.mods.toml'
            metadata = tomllib.loads(jar.read(filename).decode())
            mod = metadata['mods'][0]
            assert mod['displayName'] == "Hovern't" and mod['version'] == version
            assert mod['authors'] == 'TAS-69', f'Incorrect public credit in {path}'
            assert mod['logoFile'] == 'assets/hovernt/icon.png'
            dependency = next(d for d in metadata['dependencies']['hovernt'] if d['modId'] == 'minecraft')
            assert dependency['versionRange'] == f'[{minecraft}]'
        print(f'Package verified: {path.name}')
