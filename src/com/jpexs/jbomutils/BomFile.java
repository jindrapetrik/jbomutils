package com.jpexs.jbomutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomFile implements WritableTo {

    long parent;    // Parent BOMPathInfo1->id 
    String name;

    public BomFile() {
    }

    public BomFile(BomInputStream bis) throws IOException {
        parent = bis.readUI32();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int v;
        while ((v = bis.read()) > 0) {
            baos.write(v);
        }
        name = new String(baos.toByteArray(), "UTF-8");
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.writeUI32(parent);
        bos.write(name.getBytes("UTF-8"));
        bos.write(0);
    }

}
