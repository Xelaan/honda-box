import dev.alex.fyt.probe.MediaText;
public final class MediaTextHarness {
 static int checks;
 static void expect(String label,String actual,String expected){checks++;if(!actual.equals(expected))throw new AssertionError(label+": "+actual+" != "+expected);}
 public static void main(String[] args){
  MediaText m=new MediaText();expect("River truncation",m.accept("River (feat. Ed >"),"River (feat. Ed…");
  expect("new iPod song replaces incomplete title",m.accept("Believe"),"Believe");
  m.accept("River (feat. Ed >");expect("new partial song replaces old partial",m.accept("Believe>"),"Believe…");
  expect("repeat partial no duplicate",m.accept("Believe>"),"Believe…");
  expect("complete incoming value replaces prefix",m.accept("Believe in You"),"Believe in You");
  m.clear();m.accept("Babe I'm Gonna>");expect("overlapping incoming value stays independent",m.accept("Gonna Leave You"),"Gonna Leave You");
  m.clear();m.accept("It's a Beautifu>");expect("matching prefix never reconstructs a title",m.accept("Beautiful Day"),"Beautiful Day");
  m.clear();m.accept("004 Magic! - Ru>");expect("numbered next song replaces old",m.accept("005 Natan feat.>"),"005 Natan feat.…");
  m.clear();m.accept("Different song>");expect("short incidental overlap not joined",m.accept("song title"),"song title");
  m.clear();m.accept("□□ □□□□□>");expect("lost letters are not overlap evidence",m.accept("□□□□□ next"),"□□□□□ next");
  m.clear();m.accept("Don't Believe");expect("complete suffix title is a new value",m.accept("Believe"),"Believe");
  expect("empty clears",m.accept(""),"");
  System.out.println("PASS: "+checks+" media title regressions");
 }
}
