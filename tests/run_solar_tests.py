#!/usr/bin/env python3
from pathlib import Path
import subprocess,os,shutil
root=Path(__file__).resolve().parents[1]
if os.environ.get('JAVA_HOME'): java=Path(os.environ['JAVA_HOME'])/'bin'
elif Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/javac').exists(): java=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
else: java=Path(shutil.which('javac') or 'javac').resolve().parent
out=root/'build/solar-tests';out.mkdir(parents=True,exist_ok=True)
subprocess.run([str(java/'javac'),'-d',str(out),str(root/'src/dev/alex/fyt/probe/SolarTimes.java'),str(root/'tests/SolarTests.java')],check=True)
result=subprocess.run([str(java/'java'),'-cp',str(out),'SolarTests'],check=True,text=True,capture_output=True)
print(result.stdout,end='')
