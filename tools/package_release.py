#!/usr/bin/env python3
"""Verify and package a signed Honda Box release; never installs or publishes it."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from build import APP_VERSION, BT, JAVA, version

SHIZUKU_NAME = 'shizuku-v13.6.0.r1086.2650830c-release.apk'
SHIZUKU_CERT = '268b5590e868fb08bae7e0ac413564cd1ff88f5ccff74af9dbd0dc918e30db30'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(*args):
    return subprocess.check_output([str(a) for a in args], text=True, stderr=subprocess.STDOUT)


def verify_apk(path, expected_package, expected_version=None, expected_code=None):
    signature = run(JAVA / 'java', '-jar', BT / 'lib/apksigner.jar',
                    'verify', '--verbose', '--print-certs', path)
    signer = re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', signature)
    info = run(BT / 'aapt', 'dump', 'badging', path)
    identity = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", info)
    minimum = re.search(r"sdkVersion:'(\d+)'", info)
    if not signer or not identity or not minimum:
        raise ValueError('Incomplete APK verification: ' + path.name)
    package, code, app_version = identity.groups()
    if package != expected_package:
        raise ValueError('Unexpected package: ' + path.name + ' / ' + package)
    if expected_version is not None and app_version != expected_version:
        raise ValueError('Unexpected version: ' + path.name)
    if expected_code is not None and int(code) != expected_code:
        raise ValueError('Unexpected version code: ' + path.name)
    if "launchable-activity:" not in info:
        raise ValueError('Missing launcher entry: ' + path.name)
    return dict(file=path.name, package=package, versionName=app_version,
                versionCode=int(code), minSdk=int(minimum.group(1)),
                signatureVerified=True, signerSHA256=signer.group(1), sha256=digest(path))


def manifest_info(path):
    import xml.etree.ElementTree as ET
    tree = ET.parse(path).getroot()
    return tree.attrib['package'], int(tree.attrib['{http://schemas.android.com/apk/res/android}versionCode'])


def installer(files):
    return '''#!/usr/bin/env python3
"""Install this pack's Honda APKs on an explicitly selected ADB device."""
import argparse, hashlib, shutil, subprocess
from pathlib import Path

FILES = ''' + repr(files) + '''

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True, help='Device serial/IP:port from adb devices -l')
    parser.add_argument('--adb', default='adb', help='Current Android Platform Tools adb executable')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    adb = shutil.which(args.adb)
    if not adb:
        parser.error('adb not found; install Android Platform Tools or pass --adb')
    sums = {}
    for line in (root / 'SHA256SUMS.txt').read_text().splitlines():
        sha, name = line.split('  ', 1)
        sums[name] = sha
    # Validate every requested APK before any installation starts.
    for name in FILES:
        path = root / name
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != sums.get(name):
            raise SystemExit('Missing file or checksum mismatch: ' + name)
    subprocess.run([adb, '-s', args.serial, 'get-state'], check=True)
    for name in FILES:
        print('Installing ' + name, flush=True)
        subprocess.run([adb, '-s', args.serial, 'install', '--no-incremental', '-r', str(root / name)], check=True)
    print('Installed. Open Honda Box, grant overlay permission and configure DUDU autostart/Keep alive.')
    print('Shizuku is optional and was not installed. See INSTALL.en.md / INSTALL.ru.md.')

if __name__ == '__main__':
    main()
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apks', type=Path, default=ROOT / 'dist')
    parser.add_argument('--output', type=Path, default=ROOT / 'dist/release')
    parser.add_argument('--shizuku', type=Path, required=True,
                        help='Unmodified official Shizuku 13.6.0 APK')
    parser.add_argument('--expected-signer', help='Required SHA-256 Honda signing certificate')
    parser.add_argument('--validation', type=Path, help='Optional public synthetic validation summary')
    args = parser.parse_args()
    if run('git', '-C', ROOT, 'status', '--porcelain').strip():
        raise ValueError('Commit source/documentation changes before packaging so sourceCommit identifies the exact release tree')
    args.output.mkdir(parents=True, exist_ok=True)
    name = 'Honda-Box-' + APP_VERSION + '-Setup-Pack'
    with tempfile.TemporaryDirectory(prefix=name + '-', dir=args.output) as temp:
        stage = Path(temp)
        entries = [('Honda-Box-' + APP_VERSION + '.apk', ROOT / 'AndroidManifest.xml')]
        for mode in ['Box', 'Radio', 'CD', 'USB']:
            manifest = ROOT / 'launchers' / mode / 'AndroidManifest.xml'
            stem = 'Honda-Box-Launcher' if mode == 'Box' else 'Honda-' + mode + '-Launcher'
            entries.append((stem + '-' + version(manifest) + '.apk', manifest))
        rows = []
        for filename, manifest in entries:
            shutil.copyfile(args.apks / filename, stage / filename)
            package, code = manifest_info(manifest)
            rows.append(verify_apk(stage / filename, package, version(manifest), code))
        certificates = {row['signerSHA256'] for row in rows}
        if len(certificates) != 1:
            raise ValueError('Honda APK certificates do not match')
        if args.expected_signer and certificates != {args.expected_signer.lower()}:
            raise ValueError('Honda release signing certificate differs from the expected owner key')
        for row in rows:
            if row['minSdk'] > 24:
                raise ValueError('APK exceeds the documented Android 7 minimum: ' + row['file'])
        shutil.copyfile(args.shizuku, stage / SHIZUKU_NAME)
        shizuku = verify_apk(stage / SHIZUKU_NAME, 'moe.shizuku.privileged.api', '13.6.0.r1086.2650830c')
        if shizuku['signerSHA256'] != SHIZUKU_CERT:
            raise ValueError('Shizuku certificate is not the known official release certificate')
        rows.append(shizuku)
        for source, target in [('docs/install.ru.md', 'INSTALL.ru.md'),
                               ('docs/install.en.md', 'INSTALL.en.md'),
                               ('docs/background-operation.md', 'background-operation.md'),
                               ('docs/releases/v' + APP_VERSION + '.md', 'RELEASE-NOTES.md'),
                               ('LICENSE', 'LICENSE'), ('NOTICE.md', 'NOTICE.md'),
                               ('third_party/shizuku/Shizuku-app-LICENSE.txt', 'Shizuku-LICENSE.txt'),
                               ('tools/stock_overlay_adb.py', 'stock_overlay_adb.py')]:
            shutil.copyfile(ROOT / source, stage / target)
        (stage / 'INSTALL-ADB.py').write_text(installer([entry[0] for entry in entries]), encoding='utf-8')
        (stage / 'READ-ME-FIRST.txt').write_text(
            'Honda Box ' + APP_VERSION + '\n\n'
            'Русский: откройте INSTALL.ru.md. English: open INSTALL.en.md.\n'
            'Install the main Honda-Box APK first, then the Honda Box / Radio / CD / USB launchers.\n'
            'USB and iPod share one launcher. Shizuku is optional.\n'
            'New com.crewbeat.hondabox IDs install separately from old dev.alex.* apps; preferences do not transfer.\n'
            'No firmware, test APKs, vehicle logs or signing keys are included.\n', encoding='utf-8')
        (stage / 'SHIZUKU-SOURCE.txt').write_text(
            'Unmodified official Shizuku 13.6.0 r1086 / Apache-2.0.\n'
            'Release: https://github.com/RikkaApps/Shizuku/releases/tag/v13.6.0\n'
            'Download: https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/' + SHIZUKU_NAME + '\n'
            'Source: https://github.com/RikkaApps/Shizuku/tree/v13.6.0\n'
            'Setup: https://shizuku.rikka.app/guide/setup/\n'
            'APK SHA-256: ' + shizuku['sha256'] + '\n'
            'Certificate SHA-256: ' + SHIZUKU_CERT + '\n'
            'Not modified or re-signed by Honda Box.\n', encoding='utf-8')
        commit = run('git', '-C', ROOT, 'rev-parse', 'HEAD').strip()
        report = dict(version=APP_VERSION, sourceCommit=commit, apks=rows,
                      physicalDeviceValidated=False,
                      note='File/signature verification and synthetic emulator validation are separate from head-unit command support.')
        if args.validation:
            report['syntheticValidation'] = json.loads(args.validation.read_text())
        (stage / 'BUILD-VERIFICATION.json').write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        paths = sorted(stage.iterdir())
        forbidden = [p.name for p in paths if p.suffix in {'.p12', '.jks', '.keystore', '.idsig'} or p.name.startswith('TEST-')]
        if forbidden or len(list(stage.glob('*.apk'))) != 6:
            raise ValueError('Unexpected package contents: ' + repr(forbidden))
        (stage / 'SHA256SUMS.txt').write_text(''.join(digest(p) + '  ' + p.name + '\n' for p in paths), encoding='utf-8')
        archive = args.output / (name + '.zip')
        with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED) as out:
            for path in sorted(stage.iterdir()):
                out.write(path, path.name)
        with zipfile.ZipFile(archive) as packed:
            if packed.testzip() is not None:
                raise ValueError('Archive CRC failed')
            for line in packed.read('SHA256SUMS.txt').decode().splitlines():
                sha, filename = line.split('  ', 1)
                if hashlib.sha256(packed.read(filename)).hexdigest() != sha:
                    raise ValueError('Archive checksum failed: ' + filename)
            if set(packed.namelist()) != {p.name for p in stage.iterdir()}:
                raise ValueError('Archive file list differs')
        (args.output / (archive.name + '.sha256')).write_text(digest(archive) + '  ' + archive.name + '\n', encoding='utf-8')
        for filename in ['INSTALL.ru.md', 'INSTALL.en.md', 'BUILD-VERIFICATION.json', 'SHA256SUMS.txt']:
            shutil.copyfile(stage / filename, args.output / filename)
        print('PASS: six signed APKs, five matching Honda certificates, ZIP CRC and every file checksum')
        print('Setup Pack:', archive)
        print('Source commit:', commit)


if __name__ == '__main__':
    main()
