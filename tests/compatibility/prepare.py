import argparse
import hashlib
import json
import time
import tomllib
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build' / 'compatibility'
API = 'https://api.modrinth.com/v2'
HEADERS = {'User-Agent': 'TAS-69/Hovern-t/1.0 compatibility verification'}
SELECTION = {
    'vein-mining': ['fabric', 'neoforge'],
    'enchanted-arsenal': ['neoforge'],
    'tacz-bullet-proof-enchant-add-on': ['neoforge'],
    'allurement': ['neoforge'],
    'majruszs-enchantments': ['fabric', 'neoforge'],
    'enchantedshulkers': ['fabric'],
    'enchancement': ['fabric'],
    'zenith-renewed': ['fabric'],
    'enchantments-plus': ['fabric'],
    'soulbound-enchantment': ['fabric', 'neoforge'],
    'apotheosis': ['neoforge'],
    'ensorcellation': ['neoforge'],
    'extra-enchantments-and-curses': ['fabric', 'neoforge'],
}


def request(url):
    if not url.startswith('https://'):
        url = API + url
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=90) as response:
                return response.read()
        except Exception:
            if attempt == 3:
                raise
            time.sleep(2 + attempt)


def metadata(path):
    cache = WORK / 'metadata' / (hashlib.sha256(path.encode()).hexdigest() + '.json')
    if not cache.exists():
        cache.parent.mkdir(parents=True, exist_ok=True)
        cache.write_bytes(request(path))
    return json.loads(cache.read_text(encoding='utf-8'))


def select(project, loader):
    query = urllib.parse.urlencode({'game_versions': json.dumps(['1.20.1'])})
    versions = metadata(f'/project/{project}/version?{query}')
    accepted = ['fabric'] if loader == 'fabric' else ['neoforge', 'forge']
    matching = [v for v in versions if any(x in v['loaders'] for x in accepted)]
    if not matching:
        raise ValueError(f'No 1.20.1 {loader} release for {project}')
    releases = [v for v in matching if v['version_type'] == 'release']
    return sorted(releases or matching, key=lambda v: v['date_published'], reverse=True)[0]


def download(version):
    file = next((f for f in version['files'] if f['primary']), version['files'][0])
    path = WORK / 'downloads' / version['id'] / file['filename']
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_bytes(request(file['url']))
    actual = hashlib.sha512(path.read_bytes()).hexdigest()
    assert actual == file['hashes']['sha512'], f'Hash mismatch: {path}'
    with zipfile.ZipFile(path) as jar:
        if 'fabric.mod.json' in jar.namelist():
            mod = json.loads(jar.read('fabric.mod.json'))
            ids = [mod['id']]
            requirements = mod.get('depends', {})
            nested = extract_nested(jar, path.parent / 'nested')
        else:
            mod = tomllib.loads(jar.read('META-INF/mods.toml').decode())
            ids = [m['modId'] for m in mod.get('mods', [])]
            requirements = mod.get('dependencies', {})
            nested = []
    return {'path': path.relative_to(ROOT).as_posix(), 'filename': file['filename'], 'url': file['url'],
            'sha512': actual, 'modIds': ids, 'requirements': requirements, 'nestedPaths': nested}


def extract_nested(jar, directory):
    data = json.loads(jar.read('fabric.mod.json'))
    result = []
    for entry in data.get('jars', []):
        filename = entry['file']
        path = directory / Path(filename).name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(jar.read(filename))
        result.append(path.relative_to(ROOT).as_posix())
        with zipfile.ZipFile(path) as child:
            if 'fabric.mod.json' in child.namelist():
                result.extend(extract_nested(child, directory / path.stem))
    return result


def resolve(project, loader, locked=None, found=None):
    found = {} if found is None else found
    details = metadata(f'/project/{project}')
    if details['id'] in found:
        return found
    version = metadata(f'/version/{locked}') if locked else select(details['id'], loader)
    record = {'project': details['slug'], 'projectId': details['id'], 'title': details['title'],
              'downloads': details['downloads'], 'versionId': version['id'], 'version': version['version_number'],
              'published': version['date_published'], 'source': details.get('source_url'), **download(version)}
    found[details['id']] = record
    for dependency in version['dependencies']:
        if dependency['dependency_type'] != 'required':
            continue
        dependency_id = dependency.get('project_id')
        dependency_version = dependency.get('version_id')
        if not dependency_id and dependency_version:
            dependency_id = metadata(f'/version/{dependency_version}')['project_id']
        if dependency_id:
            resolve(dependency_id, loader, dependency_version, found)
    return found


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--project', action='append')
    parser.add_argument('--locked', action='store_true')
    args = parser.parse_args()
    if args.locked:
        lock = json.loads((ROOT / 'tests' / 'compatibility' / 'lock.json').read_text(encoding='utf-8'))
        for profile in lock['profiles']:
            if args.project and profile['project'] not in args.project:
                continue
            mods = []
            for digest in profile['modHashes']:
                record = dict(lock['mods'][digest])
                path = ROOT / record['path']
                path.parent.mkdir(parents=True, exist_ok=True)
                if not path.exists():
                    path.write_bytes(request(record['url']))
                assert hashlib.sha512(path.read_bytes()).hexdigest() == digest, f'Hash mismatch: {path}'
                with zipfile.ZipFile(path) as jar:
                    record['nestedPaths'] = extract_nested(jar, path.parent / 'nested') if 'fabric.mod.json' in jar.namelist() else []
                mods.append(record)
            output = dict(profile)
            output.pop('modHashes')
            output['mods'] = mods
            destination = WORK / 'profiles' / (profile['id'] + '.json')
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text(json.dumps(output, indent=2) + '\n', encoding='utf-8')
            print(f"Prepared locked {profile['id']}", flush=True)
        return
    profiles = []
    for slug, loaders in SELECTION.items():
        if args.project and slug not in args.project:
            continue
        for loader in loaders:
            try:
                mods = resolve(slug, loader)
                if loader == 'fabric' and any('fabric' in mod['requirements'] or 'fabric-api' in mod['requirements'] for mod in mods.values()):
                    resolve('fabric-api', loader, found=mods)
                root = next(m for m in mods.values() if m['project'] == slug)
                profile = {'id': f'{loader}-{slug}', 'minecraft': '1.20.1', 'loader': loader,
                           'project': slug, 'title': root['title'], 'version': root['version'],
                           'mods': list(mods.values())}
                file = WORK / 'profiles' / (profile['id'] + '.json')
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text(json.dumps(profile, indent=2) + '\n')
                profiles.append(profile)
                print(f"Prepared {profile['id']} {profile['version']}: {len(mods)} mod files", flush=True)
            except Exception as error:
                print(f'PREPARATION FAILED {loader}-{slug}: {error}', flush=True)
    if not args.project or 'fabric-enchantments' in args.project:
        path = WORK / 'downloads' / 'curseforge-4765940' / 'fabric-enchantments-1.0.0.jar'
        url = 'https://mediafilez.forgecdn.net/files/4765/940/fabric-enchantments-1.0.0.jar'
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists():
            path.write_bytes(request(url))
        digest = hashlib.sha512(path.read_bytes()).hexdigest()
        assert digest == '6c5288ebbedb1a74c951c2297a2dbe4b3cbc11f5aec6992a4c0e3fc87e443fafb1e374aef6118bdc6d60ae9701525d64d05a304c3d4bfcf330bb182670475288'
        with zipfile.ZipFile(path) as jar:
            mod = json.loads(jar.read('fabric.mod.json'))
            nested = extract_nested(jar, path.parent / 'nested')
        record = {'project': 'fabric-enchantments', 'projectId': 'curseforge-498465',
                  'title': 'Fabric Enchantments', 'versionId': 'curseforge-4765940', 'version': '1.0.0',
                  'source': 'https://www.curseforge.com/minecraft/mc-mods/fabric-enchantments/files/4765940',
                  'path': path.relative_to(ROOT).as_posix(), 'filename': path.name, 'url': url,
                  'sha512': digest, 'modIds': [mod['id']], 'requirements': mod.get('depends', {}), 'nestedPaths': nested}
        dependencies = resolve('fabric-api', 'fabric')
        profile = {'id': 'fabric-fabric-enchantments', 'minecraft': '1.20.1', 'loader': 'fabric',
                   'project': record['project'], 'title': record['title'], 'version': record['version'],
                   'mods': [record, *dependencies.values()]}
        (WORK / 'profiles' / (profile['id'] + '.json')).write_text(json.dumps(profile, indent=2) + '\n', encoding='utf-8')
        print('Prepared fabric-fabric-enchantments 1.0.0', flush=True)
    profiles = [json.loads(path.read_text(encoding='utf-8')) for path in sorted((WORK / 'profiles').glob('*.json'))]
    (WORK / 'prepared.json').write_text(json.dumps(profiles, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
