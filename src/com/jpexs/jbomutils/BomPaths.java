package com.jpexs.jbomutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JPEXS
 */
public class BomPaths implements WritableTo {

    long isLeaf; //uint16_t            
    long count; //uint16_t 
    long forward;
    long backward;
    List<BomPathIndices> indices = new ArrayList<>();

    public BomPaths() {
    }

    public BomPaths(BomInputStream bis) throws IOException {
        isLeaf = bis.readUI16();
        count = bis.readUI16();
        forward = bis.readUI32();
        backward = bis.readUI32();
        indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(new BomPathIndices(bis));
        }
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.writeUI16(isLeaf);
        bos.writeUI16(count);
        bos.writeUI32(forward);
        bos.writeUI32(backward);
        for (BomPathIndices i : indices) {
            bos.write(i);
        }
    }
}
