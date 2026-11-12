#!/usr/bin/env python3
"""Portable (no dependencies) checks for the WFF v1 resource-only project."""
import sys, re, xml.etree.ElementTree as ET
from pathlib import Path
root = Path(__file__).resolve().parents[1]
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
application=manifest.find('application')
assert application is not None and application.get(ns+'hasCode')=='false', 'WFF must have no code'
assert application.find('property').get(ns+'value')=='1', 'WFF v1 required for Wear OS 4'
face=ET.parse(root/'app/src/main/res/raw/watchface.xml').getroot()
assert face.tag=='WatchFace' and face.attrib.get('width')=='450'
assert len(face.findall('.//HourHand'))==1
assert len(face.findall('.//MinuteHand'))==1
assert len(face.findall('.//SecondHand'))==1
assert 'STEP_COUNT' in ET.tostring(face,encoding='unicode')
assert 'HEART_RATE' in ET.tostring(face,encoding='unicode')
assert 'BATTERY_PERCENT' in ET.tostring(face,encoding='unicode')
resources=set(re.findall(r'resource="([a-z0-9_]+)"',ET.tostring(face,encoding='unicode')))
for r in resources:
    assert (root/f'app/src/main/res/drawable/{r}.png').exists(), f'Missing {r}.png'
assert (root/'app/src/main/res/drawable/preview.png').exists()
assert (root/'app/src/main/res/drawable/ambient.png').exists()
for h in face.findall('.//HourHand') + face.findall('.//MinuteHand') + face.findall('.//SecondHand'):
    x,y,w,he=map(int,(h.get('x'),h.get('y'),h.get('width'),h.get('height')))
    px=x+w*float(h.get('pivotX'))
    py=y+he*float(h.get('pivotY'))
    assert abs(px-225)<1, f'{h.tag} x pivot {px}'
    assert abs(py-(325 if h.tag=='SecondHand' else 225))<1, f'{h.tag} y pivot {py}'
print('PASS: WFF XML parsed, manifest WFF1 valid, sources and assets present, pivots aligned.')
