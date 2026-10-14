import json
import sys
import tomllib
import zipfile
from pathlib import Path


paths = sorted(Path(sys.argv[1]).glob('*.jar'))
assert paths, 'No installable JARs found'
for path in paths:
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        assert not any('Smoke' in name or 'Compatibility' in name for name in names), f'Test fixture shipped in {path}'
        assert jar.read('assets/hovernt/icon.png') == Path('hovernt_logo.png').read_bytes(), f'Incorrect icon in {path}'
        mixins = json.loads(jar.read('hovernt.mixins.json'))
        assert mixins['client'] == ['MerchantScreenMixin'], f'Test mixin shipped in {path}'
        assert 'dev/tas69/hovernt/BookNames.class' in names
        assert 'dev/tas69/hovernt/SidebarSkin.class' in names
        json.loads(jar.read('pack.mcmeta'))
        assert mixins['compatibilityLevel'] == ('JAVA_17' if '-1.20.1-' in path.name else 'JAVA_25')
        if '-1.20.1-' in path.name:
            refmap = json.loads(jar.read('hovernt.refmap.json'))
            assert refmap.get('mappings'), f'Missing runtime mappings in {path}'
        if '-fabric-' in path.name:
            metadata = json.loads(jar.read('fabric.mod.json'))
            assert metadata['name'] == "Hovern't"
            assert metadata['environment'] == 'client'
            assert metadata['icon'] == 'assets/hovernt/icon.png'
            assert len(metadata['jars']) == 2, f'Missing embedded resource loader in {path}'
        else:
            metadata = 'META-INF/mods.toml' if '-1.20.1-' in path.name else 'META-INF/neoforge.mods.toml'
            assert metadata in names
            mod = tomllib.loads(jar.read(metadata).decode())['mods'][0]
            assert mod['displayName'] == "Hovern't"
            assert mod['logoFile'] == 'assets/hovernt/icon.png'
        print(f'Package verified: {path.name}')
