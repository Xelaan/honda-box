#!/usr/bin/env python3
"""Run the Android-free AUX source/playback policy tests."""
from pathlib import Path
import os,shutil,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
java=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
if not (java/'javac').exists():java=Path(shutil.which('javac') or 'javac').resolve().parent
with tempfile.TemporaryDirectory(prefix='honda-aux-tests-') as temp:
    subprocess.run([str(java/'javac'),'-d',temp,str(root/'src/com/crewbeat/hondabox/AuxPlaybackPolicy.java'),str(root/'tests/AuxPlaybackTests.java')],check=True)
    subprocess.run([str(java/'java'),'-cp',temp,'AuxPlaybackTests'],check=True)
