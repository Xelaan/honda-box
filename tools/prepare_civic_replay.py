#!/usr/bin/env python3
"""Prepare local, numeric-only timestamped replay assets from a Honda Box capture."""
import json,re,sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
data=json.loads(Path(sys.argv[1]).read_text())
frames=iter(data['parsedBsjFrames']);events=[]
for line in data['log']:
    match=re.search(r'event (\d+) (\{.*\})',line)
    if match:
        code=int(match[1]);value=json.loads(match[2])
        if code<1000 and value['ints'] is not None:
            events.append({'at':value['receivedAtMs'],'code':code,'ints':value['ints']})
    elif 'RAW 1019' in line:
        frame=next(frames)
        if frame['profile']!=15729093 or frame['type']!=0x28 or not frame['checksumValid']:
            raise ValueError('Expected the captured LOW Civic climate-only log')
        events.append({'at':frame['timestampMs'],'code':1019,'ints':[int(x,16) for x in line.split(' · ')[-1].split()]})
if not events:raise ValueError('No events to replay')
start=data['capture'].get('startedAtMs',events[0]['at'])
for event in events:event['at']-=start
output=root/'tests/runner/assets/civic-climate-realtime.json'
output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(json.dumps({'durationMs':events[-1]['at'],'events':events}))
print(output, len(events), 'events')
