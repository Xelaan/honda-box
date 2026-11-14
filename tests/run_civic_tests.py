#!/usr/bin/env python3
"""Compile Android sources/resources and replay Civic numeric data on the host JVM.
Requires the Android SDK and JDK used by build.py; creates no APK and installs nothing.
"""
import importlib.util, os, subprocess, tempfile
from pathlib import Path
root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("honda_build", root / "build.py")
build = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build)
suites = ["civic/CivicClimateHarness", "clock/ClockHarness", "ipod-inference/IpodMarkerStateHarness",
          "diagnostics/DiagnosticRecordingHarness", "cd-volume/CdVolumeHarness", "radio-volume/RadioVolumeHarness"]
with tempfile.TemporaryDirectory(prefix="honda-civic-tests-") as directory:
    out = Path(directory)
    generated, classes = out / "generated", out / "classes"
    generated.mkdir(); classes.mkdir()
    subprocess.run([str(build.android_tool("aapt")), "package", "-f", "-M", str(root / "AndroidManifest.xml"),
                    "-I", str(build.ANDROID), "-J", str(generated), "-S", str(root / "res"), "-A", str(root / "assets")], check=True)
    cp = os.pathsep.join([str(build.ANDROID)] + [str(p) for p in (root / "third_party/shizuku").glob("*.jar")])
    sources = list((root / "src").rglob("*.java")) + list(generated.rglob("*.java"))
    sources += [root / "tests/host" / (suite + ".java") for suite in suites]
    subprocess.run([str(build.executable("javac")), "-encoding", "UTF-8", "-source", "8", "-target", "8",
                    "-classpath", cp, "-d", str(classes)] + list(map(str, sources)), check=True)
    for suite in suites:
        args = [str(root / "tests/host/civic/climate-replay.csv")] if suite.startswith("civic/") else []
        subprocess.run([str(build.executable("java")), "-cp", str(classes) + os.pathsep + cp,
                        "com.crewbeat.hondabox." + suite.split("/")[-1]] + args, check=True)
