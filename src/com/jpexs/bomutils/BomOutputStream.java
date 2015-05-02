package com.jpexs.bomutils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author JPEXS
 */
public class BomOutputStream extends DataOutputStream {

    public BomOutputStream(OutputStream out) {
        super(out);
    }

    public void writeUI32(long v) throws IOException {
        writeInt((int) v);
    }

    public void writeUI16(long v) throws IOException {
        writeShort((int) v);
    }

    public void write(WritableTo w) throws IOException {
        w.writeTo(this);
    }

}
