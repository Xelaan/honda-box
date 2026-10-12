#!/usr/bin/env python3
"""Offline Android build with an installed SDK/JDK and a local signing key."""
from pathlib import Path
import os, subprocess, zipfile, hashlib, shutil, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SDK = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or (Path.home()/'Library/Android/sdk'))
BT = SDK/'build-tools'/os.environ.get('ANDROID_BUILD_TOOLS', '36.0.0')
ANDROID = SDK/'platforms'/('android-'+os.environ.get('ANDROID_COMPILE_SDK','33'))/'android.jar'
def java_bin():
    if os.environ.get('JAVA_HOME'): return Path(os.environ['JAVA_HOME'])/'bin'
    bundled=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
    if (bundled/'javac').exists(): return bundled
    javac=shutil.which('javac')
    if javac: return Path(javac).resolve().parent
    raise RuntimeError('Set JAVA_HOME to an installed JDK (17 or newer recommended).')
JAVA=java_bin()
BUILD=Path(os.environ.get('HONDA_BUILD_DIR',str(ROOT/'build')))
OUTPUT=Path(os.environ.get('HONDA_OUTPUT_DIR',str(ROOT/'dist')))
ANDROID_NS='{http://schemas.android.com/apk/res/android}'
def version(manifest): return ET.parse(manifest).getroot().get(ANDROID_NS+'versionName')
APP_VERSION=version(ROOT/'AndroidManifest.xml')
APP_NAME='Honda-Box-'+APP_VERSION
# Retain the existing owner's key for compatible local updates; fresh clones use a new dev key.
legacy_key=ROOT/'probe-development.p12'
SIGNING_KEY=Path(os.environ.get('HONDA_KEYSTORE',str(legacy_key if legacy_key.exists() else ROOT/'.local/signing/development.p12')))
os.environ.setdefault('HONDA_KEYSTORE_PASSWORD','local-development')

def run(args): subprocess.run([str(a) for a in args],check=True)
def executable(name): return JAVA/(name+'.exe' if os.name=='nt' else name)
def android_tool(name): return BT/(name+'.exe' if os.name=='nt' else name)
def check_toolchain():
    required=[ANDROID,android_tool('aapt'),android_tool('zipalign'),BT/'lib/d8.jar',BT/'lib/apksigner.jar',executable('javac'),executable('java'),executable('keytool')]
    missing=[str(p) for p in required if not p.exists()]
    if missing: raise RuntimeError('Missing build tools; see README.md:\n'+'\n'.join(missing))

def build(manifest,source,resource,name,key,extra_sources=(),extra_classpath=None,assets=None,dependencies=()):
    check_toolchain();out=BUILD/name
    for folder in ['generated','classes','dex']:
        shutil.rmtree(out/folder,ignore_errors=True);(out/folder).mkdir(parents=True,exist_ok=True)
    args=[android_tool('aapt'),'package','-f','-M',manifest,'-I',ANDROID,'-J',out/'generated','-F',out/'resources.apk']
    if resource: args+=['-S',resource]
    if assets: args+=['-A',assets]
    run(args)
    sources=sorted(Path(source).rglob('*.java'))+sorted((out/'generated').rglob('*.java'))+list(extra_sources)
    classpath=os.pathsep.join([str(ANDROID)]+([str(extra_classpath)] if extra_classpath else [])+[str(p) for p in dependencies])
    run([executable('javac'),'-encoding','UTF-8','-source','8','-target','8','-classpath',classpath,'-d',out/'classes',*sources])
    dex_args=[executable('java'),'-cp',BT/'lib/d8.jar','com.android.tools.r8.D8','--lib',ANDROID,'--min-api','23','--output',out/'dex']
    # D8 needs the same application interfaces as javac when desugaring test lambdas.
    for i,entry in enumerate(extra_classpath.split(os.pathsep) if extra_classpath else []):
        cp=Path(entry)
        if cp.is_dir():
            archive=out/('classpath-'+str(i)+'.jar')
            with zipfile.ZipFile(archive,'w') as jar:
                for cls in sorted(cp.rglob('*.class')): jar.write(cls,cls.relative_to(cp).as_posix())
            cp=archive
        dex_args+=['--classpath',cp]
    run([*dex_args,*sorted((out/'classes').rglob('*.class')),*dependencies])
    with zipfile.ZipFile(out/'resources.apk') as src,zipfile.ZipFile(out/'unsigned.apk','w',compression=zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist(): dst.writestr(item,src.read(item.filename))
        for dex in sorted((out/'dex').glob('*.dex')): dst.write(dex,dex.name)
    run([android_tool('zipalign'),'-f','4',out/'unsigned.apk',out/'aligned.apk'])
    key=Path(key)
    if not key.exists():
        key.parent.mkdir(parents=True,exist_ok=True)
        run([executable('keytool'),'-genkeypair','-keystore',key,'-storepass:env','HONDA_KEYSTORE_PASSWORD','-keypass:env','HONDA_KEYSTORE_PASSWORD','-alias','probe','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=Honda Box local development'])
        key.chmod(0o600)
    OUTPUT.mkdir(parents=True,exist_ok=True);apk=OUTPUT/(name+'.apk')
    run([executable('java'),'-jar',BT/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','env:HONDA_KEYSTORE_PASSWORD','--out',apk,out/'aligned.apk'])
    run([executable('java'),'-jar',BT/'lib/apksigner.jar','verify',apk])
    (OUTPUT/(name+'.sha256')).write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'  '+apk.name+'\n')
    print('APK:',apk,flush=True);return apk

def build_app():
    return build(ROOT/'AndroidManifest.xml',ROOT/'src',ROOT/'res',APP_NAME,SIGNING_KEY,dependencies=sorted((ROOT/'third_party/shizuku').glob('*.jar')),assets=ROOT/'assets')
if __name__=='__main__': build_app()
