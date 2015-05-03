package com.jpexs.jbomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomVIndex implements WritableTo {

    public static final int size_of = Tools.sizeof_uint32_t * 3 + 1;

    long unknown0;      // Always 1
    long indexToVTree;
    long unknown2;      // Always 0
    /*byte*/
    int unknown3;       // Always 0 

    public BomVIndex() {
    }

    public BomVIndex(BomInputStream bis) throws IOException {
        unknown0 = bis.readUI32();
        indexToVTree = bis.readUI32();
        unknown2 = bis.readUI32();
        unknown3 = bis.read();
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.writeUI32(unknown0);
        bos.writeUI32(indexToVTree);
        bos.writeUI32(unknown2);
        bos.write(unknown3);
    }
}
