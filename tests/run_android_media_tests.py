#!/usr/bin/env python3
"""Pure JVM Android metadata announcement tests; no SDK/device required."""
import os,shutil,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
if not (java/'javac').exists():java=Path(shutil.which('javac') or 'javac').resolve().parent
with tempfile.TemporaryDirectory(prefix='honda-android-media-') as out:
    sources=[root/'src/com/crewbeat/hondabox'/name for name in ['AndroidTrack.java','AndroidTrackChange.java']]
    subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',out,*map(str,sources),str(root/'tests/host/android-media/AndroidTrackHarness.java')],check=True)
    subprocess.run([str(java/'java'),'-cp',out,'com.crewbeat.hondabox.AndroidTrackHarness'],check=True)
