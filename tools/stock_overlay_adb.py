#!/usr/bin/env python3
"""Package-scoped ADB overlay trial. No firmware edits or service/package disabling."""
import argparse, json, os, re, subprocess
from pathlib import Path

PACKAGE = 'com.syu.canbus'
OP = 'SYSTEM_ALERT_WINDOW'
MODES = {'allow', 'ignore', 'deny', 'default', 'foreground'}

def run(adb, serial, *args):
    return subprocess.check_output([adb, '-s', serial, *args], text=True, stderr=subprocess.STDOUT).strip()

def mode_of(report):
    for line in report.splitlines():
        m = re.match(r'\s*SYSTEM_ALERT_WINDOW:\s*(allow|ignore|deny|default|foreground)\b', line)
        if m: return m.group(1)
    if 'No operations' in report: return 'default'
    raise RuntimeError('Cannot identify the previous package mode; no change made.\n'+report)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('action', choices=['status','hide','restore'])
    p.add_argument('--serial', required=True, help='Explicit adb device serial; use adb devices -l')
    p.add_argument('--adb', default=os.environ.get('ADB', 'adb'))
    p.add_argument('--state', type=Path, help='Local JSON backup; required for hide/restore')
    a=p.parse_args()
    if a.action!='status' and not a.state: p.error('--state is required for hide/restore')
    if not run(a.adb,a.serial,'shell','pm','path',PACKAGE).startswith('package:'):
        raise RuntimeError(PACKAGE+' is not installed on this device.')
    report=run(a.adb,a.serial,'shell','appops','get',PACKAGE,OP)
    if a.action=='status':
        print(report)
        print('FYT volume-bar property: '+run(a.adb,a.serial,'shell','getprop','ro.syu.enableVolBar'))
        print('This property controls the separate FYT volume bar, not Honda Car Vol/menu.')
        return
    if a.action=='hide':
        previous=mode_of(report)
        if a.state.exists():
            raise RuntimeError('Backup already exists. Restore it first or use a different backup path.')
        backup=dict(serial=a.serial,package=PACKAGE,op=OP,previous_mode=previous,previous_report=report)
        with a.state.open('x',encoding='utf-8') as f: json.dump(backup,f,ensure_ascii=False,indent=2)
        run(a.adb,a.serial,'shell','appops','set',PACKAGE,OP,'ignore')
        current=run(a.adb,a.serial,'shell','appops','get',PACKAGE,OP)
        if mode_of(current)!='ignore': raise RuntimeError('Mode did not become ignore. Backup retained for restoration.\n'+current)
        print('Overlay permission trial applied; original mode saved to '+str(a.state))
        print('Now test OEM Car Vol/menu and Honda Box on the head unit. All CANBUS overlays may be affected.')
        print('Firmware-specific system windows may remain visible; no app was stopped or disabled.')
    else:
        backup=json.loads(a.state.read_text(encoding='utf-8'))
        if backup.get('serial')!=a.serial or backup.get('package')!=PACKAGE or backup.get('op')!=OP or backup.get('previous_mode') not in MODES:
            raise RuntimeError('Backup does not match this device/package/operation.')
        target=backup['previous_mode']
        run(a.adb,a.serial,'shell','appops','set',PACKAGE,OP,target)
        current=run(a.adb,a.serial,'shell','appops','get',PACKAGE,OP)
        if mode_of(current)!=target: raise RuntimeError('Restore could not be verified; backup retained.\n'+current)
        print('Original overlay mode restored: '+target)

if __name__=='__main__':
    try: main()
    except (RuntimeError,subprocess.CalledProcessError) as e: raise SystemExit(str(e))
