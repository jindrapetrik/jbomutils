package com.jpexs.jbomutils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author JPEXS
 */
public class BomInputStream extends DataInputStream {

    public BomInputStream(InputStream is) {
        super(is);
    }

    public long readUI32() throws IOException {
        return (long) readInt();
    }

    public int readUI16() throws IOException {
        return readShort() & 0xffff;
    }

}
