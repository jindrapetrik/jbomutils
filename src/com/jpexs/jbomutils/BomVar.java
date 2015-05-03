package com.jpexs.jbomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomVar implements WritableTo {

    long index;
    String name;

    public BomVar() {
    }

    public BomVar(BomInputStream bis) throws IOException {
        index = bis.readUI32();
        int len = bis.read();
        byte d[] = new byte[len];
        bis.read(d);
        name = new String(d, "UTF-8");
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.writeUI32(index);
        byte d[] = name.getBytes();
        bos.write(d.length);
        bos.write(d);
    }

    @Override
    public String toString() {
        return name;
    }

}
