package com.jpexs.jbomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomInfoEntry implements WritableTo {

    public static final int size_of = 4 * Tools.sizeof_uint32_t;

    long unknown0;  // Always zero (?) 
    long unknown1;  // Always zero (?) 
    long unknown2;   // Some obscure value, zero for empty boms (?) 
    long unknown3;  // Always zero (?) 

    public BomInfoEntry() {
    }

    public BomInfoEntry(BomInputStream bis) throws IOException {
        unknown0 = bis.readUI32();
        unknown1 = bis.readUI32();
        unknown2 = bis.readUI32();
        unknown3 = bis.readUI32();
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.writeUI32(unknown0);
        bos.writeUI32(unknown1);
        bos.writeUI32(unknown2);
        bos.writeUI32(unknown3);
    }
}
