#!/usr/bin/env python3
"""Compile test-only APKs. This command does not install or run them."""
import os
from build import ROOT, BUILD, APP_NAME, SIGNING_KEY, build, build_app
if not (BUILD/APP_NAME/'classes').exists(): build_app()
fixture_key=ROOT/'fixture-development.p12'
if not fixture_key.exists(): fixture_key=ROOT/'.local/signing/fixture.p12'
build(ROOT/'tests/fixture/AndroidManifest.xml',ROOT/'tests/fixture/src',None,'TEST-ONLY-fyt-fixture',fixture_key)
assets=ROOT/'tests/runner/assets'
build(ROOT/'tests/runner/AndroidManifest.xml',ROOT/'tests/runner/src',None,'TEST-ONLY-probe-tests',SIGNING_KEY,extra_classpath=os.pathsep.join([str(BUILD/APP_NAME/'classes')]+[str(p) for p in sorted((ROOT/'third_party/shizuku').glob('*.jar'))]),assets=assets if assets.exists() else None)
