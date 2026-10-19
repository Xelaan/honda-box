import unittest
from decode_bsj_log import decode,raw_packets,display

def packet(t,p):
    body=[t,len(p),*p];return [46,*body,(~sum(body))&255]
def chunk(a,seq,at=100):return {'hex':' '.join(f'{n:02X}' for n in a),'sequence':seq,'atMs':at,'session':2}
class DecoderTests(unittest.TestCase):
    def test_split_and_unframed(self):
        a=packet(3,[0x83,17]);b=packet(127,list(b'XC-GBJR-0722V704!'))
        frames,orphans=raw_packets([chunk(a[:3],1),chunk(a[3:]+[255]+b,2,200)])
        self.assertEqual([f['type'] for f in frames],[3,127]);self.assertEqual(orphans[0]['bytes'],[255]);self.assertEqual(frames[0]['timestampMs'],200)
    def test_invalid_checksum_not_lost(self):
        a=packet(3,[129,7]);a[-1]^=1
        frames,orphans=raw_packets([chunk(a+packet(6,[50,34,34]),1)])
        self.assertEqual(len(frames),1);self.assertEqual(sum(len(o['bytes']) for o in orphans),len(a))
    def test_sessions_not_joined(self):
        a=packet(3,[129,7]);c=chunk(a[3:],1);c['session']=3
        frames,orphans=raw_packets([chunk(a[:3],1),c]);self.assertFalse(frames);self.assertEqual(sum(len(o['bytes']) for o in orphans),len(a))
    def test_unknown_and_malformed(self):
        self.assertEqual(decode(12,[0]*17)[2],'unknown');self.assertEqual(decode(5,[0])[2],'unknown');self.assertEqual(decode(7,[255,65])[2],'unknown')
    def test_source_volume(self):
        t,u,k=decode(3,[0x83,40]);self.assertIn('Источник 4',t);self.assertIn('40/40',t);self.assertEqual(k,'known');self.assertIn('аудио включено',t);self.assertFalse(u)
        self.assertIn('аудио выключено',decode(3,[0x03,0])[0]);self.assertTrue(decode(3,[0x33,0])[1])
    def test_climate_auto_without_numeric_fan(self):
        text,unknown,category=decode(2,[0x88,0x10,0x20,0x2A,0,0])
        self.assertIn('AUTO (числовая скорость не передана)',text)
        self.assertIn('вкл. (AUTO)',text)
        self.assertEqual(category,'known')
        self.assertIn('Слева 18 °C',decode(2,[0x88,0x10,20,20,0,0])[0])
        for payload in ([0x80,0,0,0,0,0],[0x88,0x10,0,0,0,0],[0,0x10,0,0,0,0]):
            text,_,_=decode(2,payload)
            self.assertNotIn('числовая скорость не передана',text)
            self.assertIn('поправкой Honda: выкл.',text)

    def test_menu_null_separator(self):
        row=list(b'CLOCK\0 12H')+[0]*6;p=[32,*row,*([0]*32)];self.assertIn('CLOCK  12H',decode(9,p)[0])
    def test_menu_sel_is_known(self):
        text,unknown,category=decode(9,[0x20]+[0]*49)
        self.assertEqual(category,'unknown') # Length validation still takes priority.
        text,unknown,category=decode(9,[0x20]+[0]*48)
        self.assertIn('SEL',text);self.assertIn('доступен для выбора',text);self.assertFalse(unknown);self.assertEqual(category,'known')
        for header in (0,0x21,0x40):
            text,unknown,category=decode(9,[header]+[0]*48)
            self.assertEqual(category,'unknown');self.assertTrue(unknown)
    def test_lost_letters_not_fabricated(self):
        self.assertEqual(display([183,183,32,65]),'□□ A');self.assertIn('не восстанавливается',str(decode(7,[7,183,183,0])[1]))
if __name__=='__main__':unittest.main()
