package com.crewbeat.hondabox;

import java.nio.*;
import java.nio.charset.*;

/** Decode bytes that actually arrived; BSJ's B7 replacement glyph cannot recover the original letter. */
public final class BsjText {
    public static final class Result {
        public final String text,encoding;public final boolean lostCharacters;
        Result(String text,String encoding,boolean lost){this.text=text;this.encoding=encoding;lostCharacters=lost;}
    }
    public static Result decode(int[] payload,int from){
        int until=payload.length;while(until>from&&(payload[until-1]&255)==0)until--;
        if(from>=until)return new Result("","empty",false);
        byte[] bytes=new byte[until-from];boolean placeholders=false,otherHigh=false;
        for(int i=0;i<bytes.length;i++){int n=payload[from+i]&255;bytes[i]=(byte)n;if(n==0xb7)placeholders=true;else if(n>=128)otherHigh=true;}
        String value,encoding;boolean lost=false;
        try{value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();encoding="UTF-8";}
        catch(CharacterCodingException e){
            if(placeholders&&!otherHigh){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append((b&255)==0xb7?'□':(char)(b&255));value=s.toString();encoding="BSJ replacement glyph B7";lost=true;}
            else{value=Charset.forName("windows-1251").decode(ByteBuffer.wrap(bytes)).toString();encoding="windows-1251 fallback; unconfirmed on device";}
        }
        value=value.replaceAll("[\\p{Cntrl}]"," ").trim();return new Result(value,encoding,lost);
    }
}
