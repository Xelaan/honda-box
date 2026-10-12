#!/usr/bin/env python3
from build import ROOT, SIGNING_KEY, build, version

def build_launchers():
    for name in ['Radio','CD','USB','Box']:
        folder=ROOT/'launchers'/name
        stem='Honda-Box-Launcher' if name=='Box' else 'Honda-'+name+'-Launcher'
        build(folder/'AndroidManifest.xml',folder/'src',folder/'res',stem+'-'+version(folder/'AndroidManifest.xml'),SIGNING_KEY)
if __name__=='__main__': build_launchers()
