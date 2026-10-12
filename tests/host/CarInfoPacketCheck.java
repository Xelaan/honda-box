import dev.alex.fyt.probe.CarInfoPacket;
import java.util.Arrays;
public final class CarInfoPacketCheck {
 public static void main(String[] args){
  if(CarInfoPacket.decode(null)!=null||CarInfoPacket.decode(new int[12])!=null||CarInfoPacket.decode(new int[14])!=null)throw new AssertionError("length validation");
  int[] actual=CarInfoPacket.decode(new int[]{0x80,0x34,0x12,0xCD,0xAB,0xFE,0x01,0x02,0x78,0x56,0xEF,0xCD,0xAB});
  if(!Arrays.equals(actual,new int[]{128,0x1234,0xABCD,254,1,2,0x5678,0xABCDEF}))throw new AssertionError("stock endian mapping");
  actual=CarInfoPacket.decode(new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1});
  if(!Arrays.equals(actual,new int[]{255,65535,65535,255,255,255,65535,16777215}))throw new AssertionError("signed bytes");
  if(!Arrays.equals(CarInfoPacket.decode(new int[13]),new int[8]))throw new AssertionError("valid zero frame");
  System.out.println("PASS: strict length, all8 fields/endian mapping, signed bytes, valid zero frame");
 }
}
