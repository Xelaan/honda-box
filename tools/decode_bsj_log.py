#!/usr/bin/env python3
"""Read-only FYT/BSJ JSON -> chronological Russian Markdown and machine-readable messages."""
import argparse
from collections import Counter
from datetime import datetime
import json
from pathlib import Path
from zoneinfo import ZoneInfo

NAMES = {1:'Кнопка',2:'Климат',3:'Источник / громкость',4:'Радио',5:'Медиа',6:'CD-чейнджер',7:'Текст TITLE',9:'Меню',14:'Поля автомобиля',15:'Уведомление FYT',16:'Поля FYT 133/134',17:'Двери / CAN-поля',127:'Идентификатор адаптера'}
LENGTHS = {1:{2},2:{6},3:{2},4:{5},5:{8},6:{3},9:{49},14:{13},15:{2},16:{1,2},17:{1,2},127:{16}}
FIELDS = {0:'альбом CD',1:'название трека CD',3:'исполнитель CD',4:'поле 4 (обычно папка USB)',5:'поле 5 (обычно имя файла USB)',6:'поле 6 (обычно альбом USB)',7:'поле 7 (обычно трек USB)',8:'поле 8 (обычно исполнитель USB)'}
BANDS = {0:'FM',1:'FM1',2:'FM2',3:'AM',4:'AM1',5:'AM2'}

def hx(a): return ' '.join(f'{n:02X}' for n in a)
def bcd(n):
    result=0; place=1
    while n:
        digit=n&15
        if digit>9:return None
        result+=digit*place;place*=10;n>>=4
    return result

def display(a):
    # Preserve unrecognized encoding, rather than fabricate Russian letters.
    raw=bytes(a)
    try:
        if any(n>=128 for n in a) and 183 not in a:
            return raw.decode('utf-8').replace('\0',' ').strip()
    except UnicodeDecodeError:pass
    return ''.join('□' if n==183 else chr(n) if 32<=n<127 else ' ' if n==0 else f'\\x{n:02X}' for n in a).strip()

def decode(t,p):
    unknown=[];category='known'
    if t==0x11:return 'Пакет дверей TYPE11: исключён из поддержки по просьбе владельца; payload: '+hx(p),[],'known'
    if t not in NAMES:return 'Тип не установлен; payload: '+hx(p),['Весь пакет не расшифрован.'],'unknown'
    if t in LENGTHS and len(p) not in LENGTHS[t] or t==7 and len(p)<2:
        return 'Неожиданная длина для '+NAMES[t],['Payload не интерпретировался: '+hx(p)],'unknown'
    if t==1:
        text=f'Код кнопки {p[0]}, фаза {p[1]} (1 — нажатие, 0 — отпускание).'
        unknown.append('Соответствие этого кода физической кнопке не задано в скрипте.')
    elif t==2:
        temp=lambda n:'LOW' if n==1 else 'HIGH' if n==255 else 'не задана' if n==0 else f'{n/2:g} °C'
        bits=lambda n:'вкл.' if n else 'выкл.'
        fan=p[1]&15
        airflow=[name for mask,name in [(128,'стекло'),(64,'лицо'),(32,'ноги')] if p[1]&mask]
        text=f'Слева {temp(p[2])}; справа {temp(p[3])}; вентилятор {fan}; A/C {bits(p[0]&64)}; AUTO {bits(p[0]&8)}; направление: '+(', '.join(airflow) or 'ни одного бита')+f'. Raw power={p[0]>>7}; питание по вентилятору: '+('вкл.' if 1<=fan<=7 else 'выкл.' if fan==0 else 'не установлено')+'.'
        if p[0]&0x37:unknown.append(f'b0: неописанные биты = {p[0]&0x37:02X}.')
        if p[1]&16:unknown.append('b1: неописанный бит 4 установлен.')
        if p[4:]!=[0,0]:unknown.append('b4/b5: назначение не установлено, '+hx(p[4:]))
    elif t==3:
        source=(p[0]&15)+1
        name={1:'радио',2:'CD-код (не доказательство физического диска)',3:'AUX по текущей конфигурации Honda',4:'USB/iPod'}.get(source,'неизвестный источник')
        text=f'Источник {source}: {name}; громкость {p[1]&127}/40; сигнал штатного меню={1 if p[0]&64 else 0}; штатное аудио '+('включено' if p[0]&128 else 'выключено')+' (bit7, подтверждено владельцем для профиля6291908).'
        if p[0]&0x30:unknown.append(f'b0: биты4/5 не определены = {p[0]&0x30:02X}; штатный парсер их игнорирует.')
        if p[1]&128:unknown.append('b1: старший бит установлен, смысл не определён.')
    elif t==4:
        band=p[0];index=(p[1]<<8)|p[2]
        freq=f'{(8750+(index-1)*5)/100:.2f} МГц' if band<=2 and index else f'{531+(index-1)*9} кГц' if band in (3,4,5) and index else 'не определена'
        statuses={1:'Normal',2:'TUNE+',3:'TUNE−',4:'SEEK+',5:'SEEK−',6:'SCAN',7:'Auto Search'}
        text=f'{BANDS.get(band,"банк "+str(band))}: {freq}; ячейка {p[3]}; ST '+('ON' if p[4]&128 else 'Off')+f'; статус {statuses.get(p[4]&15,str(p[4]&15))}; индекс частоты {index}.'
        if p[4]&0x70:unknown.append(f'b4: неописанные биты = {p[4]&0x70:02X}.')
    elif t==5:
        track=bcd((p[4]<<8)|p[5]);minute=bcd(p[6]);second=bcd(p[7]);work=p[1]&15
        time=f'{minute:02d}:{second:02d}' if minute is not None and second is not None and second<60 else 'некорректный BCD'
        if not any(p):text='Пустой экранный пакет 05. Сам по себе не доказывает отключение устройства.'
        elif p==[0,0,1,0,0,0,0,0]:text='Пустой экранный пакет 05 с b2=1. Нет номера/времени; не доказывает iPod.'
        else:text=f'Диск/поле b0={p[0]}; трек {track if track is not None else "некорректный BCD"}; время {time}; состояние {work}'+(' (NO DATA)' if work==9 else ' (нет устройства)' if work==10 else '')+f'; режим воспроизведения {p[1]>>4}; семейство отображения b3={p[3]} (1 — CD-подобное, 2 — USB-подобное; iPod по этому полю не определяется).'
        unknown.append(f'b2={p[2]:02X}: служебное поле, точный смысл не установлен.')
    elif t==6:
        slots=[n for b in p for n in (b>>4,b&15)]
        text='Поля слотов DATA[106..111]: '+', '.join(map(str,slots))+'.'
        unknown.append('Значения 2/3/4 видны в логах, но физический смысл состояний не доказан. Это не флаг iPod.')
    elif t==7:
        field=p[0];raw=p[1:];end=raw.index(0) if 0 in raw else len(raw);text=display(raw[:end])
        text=f'TITLE subtype {field}: {FIELDS.get(field,"неизвестное поле")}; текст «{text or "пусто"}».'
        if field not in FIELDS:category='unknown';unknown.append('Подтип текста не установлен.')
        if field>=4:unknown.append('Названия полей 4..8 выведены по USB-логам; на iPod subtype не гарантирует тот же смысл.')
        if 183 in raw:unknown.append('Байт B7 уже заменяет потерянные буквы; исходная кириллица здесь не восстанавливается.')
        if any(raw[end:]):unknown.append('Ненулевые байты после первого NUL: '+hx(raw[end:]))
        if raw[:end] and raw[end-1]==62:unknown.append('«>» — обрезанный текст. Следующий пакет не обязательно продолжение этого трека.')
    elif t==9:
        rows=[display(p[1+i*16:17+i*16]) for i in range(3)]
        indicator=' (SEL: пункт доступен для выбора нажатием)' if p[0]==0x20 else ''
        text=f'Меню, header={p[0]:02X}{indicator}; строки: '+' / '.join('«'+r+'»' for r in rows)+'.'
        if p[0]!=0x20:
            category='unknown';unknown.append(f'Header {p[0]:02X}: значение не установлено; известен только 20 = индикатор SEL, по уточнению владельца для Honda/BSJ V704.')
    elif t==14:
        vals=[p[0],p[1]|p[2]<<8,p[3]|p[4]<<8,p[5],p[6],p[7],p[8]|p[9]<<8,p[10]|p[11]<<8|p[12]<<16]
        text='DATA[140..147] по штатному парсеру: '+', '.join(map(str,vals))+'.'
        unknown.append('Названия физических величин и единицы не установлены; RPM/скорость/руль не утверждаются.')
    elif t==15:text=f'Штатное уведомление FYT: код {p[0]}, значение {p[1]}.';unknown.append('Значение уведомления не установлено.')
    elif t==16:
        text=f'DATA[133]={p[0]>>5}, DATA[134]={p[0]&31}.';unknown.append('Смысл этих полей не установлен.')
        if len(p)>1:unknown.append('Второй байт штатный парсер не использует: '+hx(p[1:]))
    elif t==127:text='Идентификатор адаптера: «'+display(p)+'».'
    return text,unknown,category

def raw_packets(chunks):
    """Join split callbacks; retain bytes outside valid packets, including buffer edges."""
    packets=[];orphans=[];buf=[];times=[];session=None;prevseq=-1
    def scan(final=False):
        nonlocal buf,times
        while buf:
            if buf[0]==46 and len(buf)>=3:
                size=buf[2]+4
                if len(buf)<size and not final:break
                if len(buf)>=size and sum(buf[1:size])&255==255:
                    packets.append({'timestampMs':times[size-1],'session':session,'type':buf[1],'length':buf[2],'payload':buf[3:size-1],'fullHex':hx(buf[:size]),'origin':'rawBsj'});del buf[:size];del times[:size];continue
            elif len(buf)<3 and buf[0]==46 and not final:break
            if orphans and orphans[-1]['session']==session and orphans[-1]['timestampMs']==times[0]:orphans[-1]['bytes'].append(buf[0])
            else:orphans.append({'timestampMs':times[0],'session':session,'bytes':[buf[0]]})
            del buf[0];del times[0]
    for c in chunks:
        seq=c.get('sequence',prevseq+1)
        if session!=c.get('session') or seq<=prevseq:scan(True);session=c.get('session');prevseq=-1
        a=[int(n,16) for n in c.get('hex','').split()];buf.extend(a);times.extend([c['atMs']]*len(a));prevseq=seq;scan()
    scan(True);return packets,orphans

def convert(path,out,tz):
    d=json.loads(path.read_text());parsed=d.get('parsedBsjFrames',[]);raw,orphans=raw_packets(d.get('rawBsj',{}).get('chunks',[]))
    frames=[dict(f,origin='parsedBsjFrames') for f in parsed]
    seen=Counter((f.get('session'),f['timestampMs'],f['fullHex']) for f in parsed)
    extra=0
    for f in raw:
        k=(f.get('session'),f['timestampMs'],f['fullHex'])
        if seen[k]:seen[k]-=1
        else:frames.append(f);extra+=1
    events=[]
    for f in frames:
        text,unhandled,category=decode(f['type'],f['payload'])
        events.append({'atMs':f['timestampMs'],'kind':category,'family':NAMES.get(f['type'],'НЕИЗВЕСТНЫЙ ПАКЕТ'),'text':text,'unhandled':unhandled,'hex':f['fullHex'],'origin':f['origin'],'type':f['type'],'session':f.get('session')})
    for o in orphans:
        explanation='Не образуют валидный BSJ-пакет: нет подтверждённой структуры заголовок/длина/checksum. Назначение не установлено.'
        if o['bytes']==[255] and any(f['type']==127 and 0<=f['timestampMs']-o['timestampMs']<=1000 for f in raw):
            explanation+=' Одиночный FF непосредственно перед идентификатором адаптера. Возможный служебный маркер, но это гипотеза.'
        chunks=d.get('rawBsj',{}).get('chunks',[])
        if chunks and o['timestampMs']==chunks[0]['atMs'] and d.get('rawBsj',{}).get('droppedChunks',0):
            explanation+=' Здесь возможен обрыв на начале вытесненного RAW-буфера.'
        events.append({'atMs':o['timestampMs'],'kind':'orphan','family':'БАЙТЫ ВНЕ ПАКЕТОВ','text':explanation,'unhandled':[],'hex':hx(o['bytes']),'session':o['session']})
    for e in d.get('audioRoutes',{}).get('events',[]):events.append({'atMs':e['atMs'],'kind':'route','family':'FYT '+e.get('name','module'),'text':f"Модуль {e['module']}, code {e['code']}: ints={e.get('ints')}, strings={e.get('strings')}. Отдельное пространство кодов, не пакет CAN.",'unhandled':[]})
    for name in ('markers','experimentMarkers','commands'):
        for e in d.get(name,[]):
            if isinstance(e,dict) and ('atMs' in e or 'timestampMs' in e):events.append({'atMs':e.get('atMs',e.get('timestampMs')),'kind':'annotation','family':name,'text':str(e.get('action',e.get('label',e.get('name',e)))),'unhandled':[]})
    events.sort(key=lambda e:e['atMs'])
    stats={'input':str(path),'app':d.get('app'),'parsedFrames':len(parsed),'rawFrames':len(raw),'extraRawFrames':extra,'orphanBytes':sum(len(o['bytes']) for o in orphans),'rawDroppedChunks':d.get('rawBsj',{}).get('droppedChunks',0),'analyzerDroppedFrames':d.get('bsjAnalyzer',{}).get('droppedFrames',0),'unknownPackets':sum(e['kind']=='unknown' for e in events),'packetsWithUnexplainedFields':sum(bool(e['unhandled']) and e['kind']=='known' for e in events),'messages':len(events)}
    lines=[f'# Расшифровка {path.name}','',f'Часовой пояс: {tz.key}. Приложение: {d.get("app","—")}.','',f'Кадров анализатора: {len(parsed)}; кадров из RAW: {len(raw)}; дополнительно найдено: {extra}; байтов вне пакетов: {stats["orphanBytes"]}.',f'Вытеснено из истории: {stats["analyzerDroppedFrames"]} кадров и {stats["rawDroppedChunks"]} RAW-фрагментов. Отсутствие в вытесненной части не доказывает отсутствие сигнала.','','Все сохранённые события показаны, повторы не удалены. HEX сохранён для каждого пакета. Частично нерасшифрованные поля отмечены отдельно; это не обязательно неизвестный тип пакета. Последние значения из lastValues не выдаются за новые события. RAW1019 — поток MCU/BSJ, а не доказанные исходные CAN-кадры Honda.','']
    for e in events:
        time=datetime.fromtimestamp(e['atMs']/1000,tz).strftime('%H:%M:%S.%f')[:-3]
        lines.extend([f'### {time} · {e["family"]}'+(' · НЕ РАСШИФРОВАНО' if e['kind'] in ('unknown','orphan') else ''),'',e['text'],''])
        for u in e['unhandled']:lines.append('> **Не установлено / не обработано:** '+u)
        if e.get('hex'):lines.extend(['',f'`{e["hex"]}`'])
        lines.append('')
    out.mkdir(parents=True,exist_ok=True);stem=out/path.stem
    stem.with_suffix('.md').write_text('\n'.join(lines));stem.with_suffix('.messages.json').write_text(json.dumps({'statistics':stats,'messages':events},ensure_ascii=False,indent=2)+'\n')
    return stats

def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('logs',nargs='+',type=Path);ap.add_argument('--output-dir',type=Path,default=Path.cwd()/'decoded-logs');ap.add_argument('--timezone',default='Asia/Barnaul');args=ap.parse_args()
    results=[convert(p,args.output_dir,ZoneInfo(args.timezone)) for p in args.logs]
    (args.output_dir/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
    for s in results:print(Path(s['input']).name+f": {s['messages']} сообщений, неизвестных пакетов {s['unknownPackets']}, необъяснённых байтов {s['orphanBytes']}")
if __name__=='__main__':main()
