package com.jpexs.jbomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomTree implements WritableTo {

    public static final int size_of = 4 + 4 * Tools.sizeof_uint32_t + 1;

    byte[] tree = "tree".getBytes();    // Always "tree" 
    long version = 1;   // Always 1
    long child; // Index for BOMPaths
    long blockSize;   // Always 4096
    long pathCount;   // Total number of paths in all leaves combined
    /*byte*/
    int unknown3;

    public BomTree() {
    }

    public BomTree(BomInputStream bis) throws IOException {
        bis.read(tree);
        version = bis.readUI32();
        child = bis.readUI32();
        blockSize = bis.readUI32();
        pathCount = bis.readUI32();
        unknown3 = bis.read();
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.write(tree);
        bos.writeUI32(version);
        bos.writeUI32(child);
        bos.writeUI32(blockSize);
        bos.writeUI32(pathCount);
        bos.write(unknown3);
    }
}
