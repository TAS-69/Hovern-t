import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build' / 'compatibility'
DESTINATION = ROOT / 'tests' / 'compatibility'
profiles = []
mods = {}
reports = []
for path in sorted((WORK / 'profiles').glob('*.json')):
    profile = json.loads(path.read_text(encoding='utf-8'))
    record = {k: v for k, v in profile.items() if k != 'mods'}
    record['modHashes'] = []
    for mod in profile['mods']:
        digest = mod['sha512']
        mods[digest] = {k: v for k, v in mod.items() if k != 'nestedPaths'}
        record['modHashes'].append(digest)
    profiles.append(record)
    result = WORK / 'results' / profile['id'] / 'result.json'
    if result.exists():
        report = json.loads(result.read_text(encoding='utf-8'))
        for case in report.get('cases', []):
            if 'storedLevel' not in case:
                case['storedLevel'] = int(re.search(r'lvl:(\d+)s', case['bookNbt'])[1])
        if report['status'] == 'passed':
            assert {case['id'] for case in report['cases']} == set(report['allModdedRegistryIds']), f'Incomplete registry coverage: {profile["id"]}'
            assert report['enchantmentCount'] > 0 and report['checks'] > 0
        report['profile'] = profile['id']
        reports.append(report)
lock = {'minecraft': '1.20.1', 'fabricLoader': '0.19.5', 'neoForge': '47.1.106', 'profiles': profiles, 'mods': mods}
evidence = {'date': '2026-10-14', 'minecraft': '1.20.1', 'results': reports}
(DESTINATION / 'lock.json').write_text(json.dumps(lock, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
(DESTINATION / 'evidence.json').write_text(json.dumps(evidence, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
print(f'Exported {len(profiles)} profiles, {len(mods)} unique files, {len(reports)} results')
