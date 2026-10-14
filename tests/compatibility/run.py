import argparse
import json
import os
import subprocess
import time
import signal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build' / 'compatibility'


def terminate(process):
    if os.name == 'nt':
        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
    else:
        os.killpg(process.pid, signal.SIGKILL)
    process.wait(timeout=15)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--profile', action='append')
    parser.add_argument('--skip-passed', action='store_true')
    parser.add_argument('--timeout', type=int, default=360)
    args = parser.parse_args()
    profiles = sorted((WORK / 'profiles').glob('*.json'))
    outcomes = []
    for path in profiles:
        profile = json.loads(path.read_text(encoding='utf-8'))
        name = profile['id']
        if args.profile and name not in args.profile:
            continue
        output = WORK / 'results' / name
        output.mkdir(parents=True, exist_ok=True)
        result = output / 'result.json'
        if args.skip_passed and result.exists() and json.loads(result.read_text(encoding='utf-8')).get('status') == 'passed':
            print(f'SKIP {name}: already passed', flush=True)
            continue
        result.unlink(missing_ok=True)
        log = output / 'launch.log'
        command = [str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')),
                   f"-Pplatform={profile['loader']}", '-Pacceptance', f'-PcompatibilityProfile={name}',
                   f":{profile['loader']}:runClient", '--console=plain', '--no-daemon']
        print(f'RUN {name} {profile["version"]}', flush=True)
        start = time.monotonic()
        with log.open('w', encoding='utf-8') as stream:
            process = subprocess.Popen(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, start_new_session=os.name != 'nt')
            code = None
            while process.poll() is None:
                if time.monotonic() - start >= args.timeout:
                    terminate(process)
                    code = -1
                    break
                if 'Incompatible mods found!' in log.read_text(encoding='utf-8', errors='replace'):
                    terminate(process)
                    code = 1
                    break
                time.sleep(1)
            code = process.returncode if code is None else code
        if result.exists():
            report = json.loads(result.read_text(encoding='utf-8'))
            status = report['status']
            if status == 'passed' and code != 0:
                status = report['status'] = 'failed'
                report['error'] = f'Fixture passed but the launcher exited with code {code}'
                result.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        else:
            status = 'startup-failed' if code != -1 else 'timeout'
            report = {'status': status, 'profile': profile, 'exitCode': code,
                      'error': 'Client did not reach the compatibility fixture. See launch.log.'}
            result.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        record = {'profile': name, 'status': status, 'exitCode': code, 'seconds': round(time.monotonic() - start, 1),
                  'enchantments': report.get('enchantmentCount'), 'checks': report.get('checks')}
        outcomes.append(record)
        print(json.dumps(record), flush=True)
        (WORK / 'run-summary.json').write_text(json.dumps(outcomes, indent=2) + '\n', encoding='utf-8')
    raise SystemExit(int(any(record['status'] != 'passed' for record in outcomes)))


if __name__ == '__main__':
    main()
