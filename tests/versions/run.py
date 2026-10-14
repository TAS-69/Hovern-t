import argparse
import contextlib
import importlib.util
import json
import os
import signal
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build' / 'versions'
TARGETS = json.loads((ROOT / 'versions.json').read_text(encoding='utf-8'))
MOD_VERSION = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('version='))


@contextlib.contextmanager
def loader_lock(loader):
    WORK.mkdir(parents=True, exist_ok=True)
    with (WORK / (loader + '.lock')).open('a+b') as lock:
        lock.write(b'\0')
        lock.flush()
        if os.name == 'nt':
            import msvcrt
            while True:
                try:
                    lock.seek(0)
                    msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
                    break
                except OSError:
                    time.sleep(1)
        else:
            import fcntl
            fcntl.flock(lock, fcntl.LOCK_EX)
        try:
            yield
        finally:
            if os.name == 'nt':
                lock.seek(0)
                msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(lock, fcntl.LOCK_UN)


def terminate(process):
    if os.name == 'nt':
        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
    else:
        os.killpg(process.pid, signal.SIGKILL)
    process.wait(timeout=20)


def run(minecraft, loader, phase, timeout, skip, skin):
    suffix = '' if skin == 'native' else '-' + skin
    name = f'{loader}-{minecraft}{suffix}'
    result_dir = WORK / 'results' / name
    result_dir.mkdir(parents=True, exist_ok=True)
    result_path = result_dir / (phase + '.json')
    if skip and result_path.exists():
        previous = json.loads(result_path.read_text(encoding='utf-8'))
        if previous.get('status') == 'passed' and (phase != 'build' or previous.get('modVersion') == MOD_VERSION):
            print(f'SKIP {phase} {name}', flush=True)
            return True
    log_path = result_dir / (phase + '.log')
    command = [str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')),
               f'-Pmc={minecraft}', f'-Pplatform={loader}', '--console=plain', '--no-daemon']
    if phase == 'build':
        command.append('collectJars')
    else:
        command.extend(['-PversionAcceptance', f':{loader}:runClient'])
        (result_dir / 'client-result.json').unlink(missing_ok=True)
        run_dir = ROOT / 'build' / 'runs' / minecraft / (loader + suffix)
        run_dir.mkdir(parents=True, exist_ok=True)
        options_path = run_dir / 'options.txt'
        options = options_path.read_text(encoding='utf-8').splitlines() if options_path.exists() else []
        options = [line for line in options if not line.startswith(('pauseOnLostFocus:', 'maxFps:', 'inactivityFpsLimit:'))]
        options.extend(['pauseOnLostFocus:false', 'maxFps:60', 'inactivityFpsLimit:"minimized"'])
        if skin != 'native':
            spec = importlib.util.spec_from_file_location('resource_packs', ROOT / 'tests' / 'create_resource_packs.py')
            resource_packs = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(resource_packs)
            resource_packs.create(run_dir / 'resourcepacks', minecraft)
            command.append(f'-PversionSkin={skin}')
            options = [line for line in options if not line.startswith(('resourcePacks:', 'incompatibleResourcePacks:'))]
            options.extend([f'resourcePacks:["vanilla","file/hovernt-test-{skin}"]', 'incompatibleResourcePacks:[]'])
        options_path.write_text('\n'.join(options) + '\n', encoding='utf-8')
    print(f'RUN {phase} {name}', flush=True)
    started = time.monotonic()
    with log_path.open('w', encoding='utf-8') as log:
        process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, start_new_session=os.name != 'nt')
        try:
            code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            terminate(process)
            code = -1
    record = {'minecraft': minecraft, 'loader': loader, 'phase': phase,
              'modVersion': MOD_VERSION, 'skin': skin, 'status': 'passed' if code == 0 else ('timeout' if code == -1 else 'failed'),
              'exitCode': code, 'seconds': round(time.monotonic() - started, 1)}
    if phase == 'client':
        fixture = result_dir / 'client-result.json'
        if fixture.exists():
            record['fixture'] = json.loads(fixture.read_text(encoding='utf-8'))
            if record['fixture']['status'] != 'passed':
                record['status'] = 'failed'
        else:
            record['status'] = 'startup-failed' if code != -1 else 'timeout'
        if skin != 'native' and record['status'] == 'passed':
            try:
                spec = importlib.util.spec_from_file_location('verify_skins', ROOT / 'tests' / 'versions' / 'verify_skins.py')
                verifier = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(verifier)
                record['skinVerification'] = verifier.verify(result_dir, skin, record['fixture'])
            except Exception as error:
                record['status'] = 'failed'
                record['skinVerification'] = {'status': 'failed', 'error': str(error)}
    result_path.write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(record), flush=True)
    return record['status'] == 'passed'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mc', action='append')
    parser.add_argument('--loader', action='append', choices=['fabric', 'neoforge'])
    parser.add_argument('--phase', choices=['build', 'client'], default='build')
    parser.add_argument('--jobs', type=int, default=1)
    parser.add_argument('--timeout', type=int, default=900)
    parser.add_argument('--skip-passed', action='store_true')
    parser.add_argument('--skin', choices=['native', 'auto', 'custom'], default='native')
    args = parser.parse_args()
    if args.mc and (unknown := sorted(set(args.mc) - TARGETS.keys())):
        parser.error('Unsupported Minecraft target: ' + ', '.join(unknown))
    if args.skin != 'native' and args.phase != 'client':
        parser.error('--skin requires --phase client')
    if args.jobs != 1:
        parser.error('Use one job per loader; Gradle shares launch state between Minecraft targets')
    targets = [(mc, loader) for mc in TARGETS if not args.mc or mc in args.mc
               for loader in args.loader or ['fabric', 'neoforge']]
    with contextlib.ExitStack() as stack:
        for loader in sorted({loader for _, loader in targets}):
            stack.enter_context(loader_lock(loader))
        passed = [run(mc, loader, args.phase, args.timeout, args.skip_passed, args.skin) for mc, loader in targets]
    raise SystemExit(int(not all(passed)))


if __name__ == '__main__':
    main()
