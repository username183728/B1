package com.example.aidetest

import java.io.ByteArrayOutputStream
import java.util.Locale

internal class SimpleTextWatcher(val fn:(String)->Unit): android.text.TextWatcher {
    override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){}
    override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){fn(s?.toString()?:"")}
    override fun afterTextChanged(s:android.text.Editable?){}
}

internal object Base32 {
    private const val ALPH="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    fun encode(data:ByteArray):String {
        var buffer=0; var bits=0; val out=StringBuilder()
        for(b in data) {
            buffer=(buffer shl 8) or (b.toInt() and 255); bits+=8
            while(bits>=5){ bits-=5; out.append(ALPH[(buffer shr bits) and 31]) }
        }
        if(bits>0) out.append(ALPH[(buffer shl (5-bits)) and 31])
        return out.toString()
    }
    fun decode(s:String):ByteArray {
        var buffer=0; var bits=0; val out=ByteArrayOutputStream()
        for(ch in s.uppercase(Locale.getDefault()).replace("=","").filter { !it.isWhitespace() }) {
            val v=ALPH.indexOf(ch); require(v>=0)
            buffer=(buffer shl 5) or v; bits+=5
            if(bits>=8){bits-=8; out.write((buffer shr bits) and 255)}
        }
        return out.toByteArray()
    }
}
