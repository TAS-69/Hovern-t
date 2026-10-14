import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build' / 'compatibility'
WIKI = Path(sys.argv[1]).resolve()
profiles = [json.loads(path.read_text(encoding='utf-8')) for path in sorted((WORK / 'profiles').glob('*.json'))]
results = {}
for profile in profiles:
    path = WORK / 'results' / profile['id'] / 'result.json'
    if path.exists():
        results[profile['id']] = json.loads(path.read_text(encoding='utf-8'))
passed = [profile for profile in profiles if results.get(profile['id'], {}).get('status') == 'passed']
projects = {profile['project'] for profile in passed}
ids = {case['id'] for profile in passed for case in results[profile['id']]['cases']}
checks = sum(results[profile['id']]['checks'] for profile in passed)
book_count = sum(results[profile['id']]['bookCount'] for profile in passed)
lines = [
    '# Compatibility verification', '',
    '**Verified on 14 October 2026: Minecraft 1.20.1, Fabric Loader 0.19.5, and legacy NeoForge 47.1.106.**', '',
    f'{len(projects)} enchantment mods passed in {len(passed)} of {len(profiles)} mod/loader profiles, covering **{len(ids)} distinct enchantment identifiers**, **{book_count} book offers**, and **{checks:,} assertions**. Identifiers shared by Fabric and NeoForge count once in the distinct total.', '',
    '## Selection', '',
    "I prioritized enchantment-focused projects from Modrinth's `enchant` search, ordered by downloads, with available 1.20.1 releases. The leading eligible selection covers Vein Mining, Enchanted Arsenal, Allurement, Majrusz's Enchantments, Enchanted Shulkers, Enchancement, Bullet Proof Enchant, Zenith Renewed, Enchantments Plus, and Soulbound. Apotheosis, Ensorcellation, Fabric Enchantments, and Extra Enchantments and Curses extend that coverage.", '',
    'This is a popularity-informed selection, not a universal ranking across hosting sites. Download counts differ by platform and change over time. The [locked manifest](https://github.com/TAS-69/Hovern-t/blob/main/tests/compatibility/lock.json) preserves the selected versions, download sources, dependency files, and hashes.', '',
    'Description helpers, table/transfer utilities, cosmetic book mods, and general content mods were excluded from the core selection. MC Dungeons Enchanting was checked and excluded after confirming that it registers no enchantments; its [project description](https://modrinth.com/mod/mc-dungeons-enchanting) explains that it changes how existing enchantments are obtained.', '',
    'Supplementary project sources: [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis), [Ensorcellation](https://www.curseforge.com/minecraft/mc-mods/ensorcellation), and [Fabric Enchantments 1.0.0](https://www.curseforge.com/minecraft/mc-mods/fabric-enchantments/files/4765940).', '',
    '## Results', '',
    '| Mod | Loader | Version | Registered enchantments | Book offers | Assertions | Result |',
    '| --- | --- | --- | ---: | ---: | ---: | --- |',
]
for profile in profiles:
    result = results.get(profile['id'], {})
    url = 'https://www.curseforge.com/minecraft/mc-mods/fabric-enchantments' if profile['project'] == 'fabric-enchantments' else 'https://modrinth.com/mod/' + profile['project']
    status = result.get('status', 'not run')
    label = 'PASS' if status == 'passed' else status.upper()
    lines.append(f"| [{profile['title']}]({url}) | {profile['loader']} | {profile['version']} | {result.get('enchantmentCount', '—')} | {result.get('bookCount', '—')} | {result.get('checks', '—')} | {label} |")
lines += [
    '', '## What was verified', '',
    'Each profile launches a real Minecraft client with the released third-party mod JARs and required dependencies. The fixture enumerates the profile\'s registered modded enchantments and places books containing them in controlled offers on the actual vanilla merchant screen.', '',
    '- Every registered modded enchantment, including any supplied by required dependencies: level 1, its maximum level, and an above-maximum level, where distinct.',
    '- Names and levels from the actual registered enchantment, including curse and custom colors.',
    '- Book data saved to NBT and loaded again before display.',
    '- Multiple enchantments on one book.',
    '- Every offer reaching the sidebar drawing code while the trade list scrolls.',
    '- Collapse and expansion through real button clicks.',
    '- Client screenshots of the first and last pages and both toggle states.', '',
    'Some mods normalize requested levels. Enchancement\'s default single-level mode is one example. The evidence records both the requested and stored levels, and the expected label follows the book\'s actual stored value.', '',
    'The development loader remaps published mod binaries for the client environment. Nested Fabric libraries are made available individually; fallback modules that alias an already-present library are omitted. Original JARs are also staged in the fixture\'s `libs` directory for libraries that discover their classes by scanning files. Third-party mod behavior is not patched for these checks.', '',
    '## Other verification', '',
    'The 1.20.1 vanilla fixture passed on both loaders, including scrolling, toggle hitboxes, the centered connected frame, and the automatic and dedicated resource-pack texture paths. The shared data suite has 19 checks for enchantment reading, missing registry data, multiple enchantments, live item changes, text motion, and layout.', '',
    'Both 26.3 ports compile. Their component-reader suite has 8 checks. **26.3 has not yet completed in-game client or third-party enchantment-mod verification.** Intermediate Minecraft versions do not have builds yet.', '',
    'Installable JARs are checked separately for metadata, the supplied icon, resource-loading dependencies, runtime mappings, and absence of test fixtures.', '',
    '## Scope', '',
    'These checks verify real mod registration and real client rendering using controlled merchant offers. They do not test naturally generated villager trade pools, enchantment effects in gameplay, multiplayer transactions, every combination of mods, or screens that replace the vanilla merchant screen.', '',
    'Registered enchantments using Minecraft\'s ordinary book data are read automatically. Items using a private enchantment system outside that data format need a dedicated integration. Translation availability comes from the enchantment mod.', '',
    'A startup failure or timeout does not count as a pass. A successful launcher exit without the fixture\'s explicit pass result does not count either.', '',
    '## Reproduce', '',
    'See [Development](Development) for the locked-profile commands. The [machine-readable evidence](https://github.com/TAS-69/Hovern-t/blob/main/tests/compatibility/evidence.json) contains every tested identifier, enchantment class, requested/stored level, display name, book data, and assertion totals.', '',
    '## Screenshots', '',
    'First and last pages from each successful profile are available below. The complete local run also saves collapsed and reexpanded states.', '',
]
images = WIKI / 'images'
images.mkdir(parents=True, exist_ok=True)
for profile in passed:
    lines.extend([f"### {profile['title']} ({profile['loader']})", ''])
    for name in ['first-page.png', 'last-page.png']:
        source = WORK / 'results' / profile['id'] / name
        if source.exists():
            destination = images / (profile['id'] + '-' + name)
            shutil.copyfile(source, destination)
            lines.extend([f"![{profile['title']} {name}](https://raw.githubusercontent.com/wiki/TAS-69/Hovern-t/images/{destination.name})", ''])
(WIKI / 'Compatibility-Verification.md').write_text('\n'.join(lines), encoding='utf-8')
print(f'Wrote verification report: {len(passed)}/{len(profiles)} profiles passed')
