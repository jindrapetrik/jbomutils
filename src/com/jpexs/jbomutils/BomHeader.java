package com.jpexs.jbomutils;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomHeader implements WritableTo {

    public static final int size_of = 8 + 6 * Tools.sizeof_uint32_t;

    byte[] magic = "BOMStore".getBytes();
    long version = 1;   // Always 1
    long numberOfBlocks; // Number of non-null entries in BOMBlockTable 
    long indexOffset;   // Offset to index table 
    long indexLength; // Length of index table, indexOffset + indexLength = file_length 
    long varsOffset;
    long varsLength;

    public BomHeader() {
    }

    public BomHeader(BomInputStream bis) throws IOException {
        bis.readFully(magic);
        version = bis.readUI32();
        numberOfBlocks = bis.readUI32();
        indexOffset = bis.readUI32();
        indexLength = bis.readUI32();
        varsOffset = bis.readUI32();
        varsLength = bis.readUI32();
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.write(magic);
        bos.writeUI32(version);
        bos.writeUI32(numberOfBlocks);
        bos.writeUI32(indexOffset);
        bos.writeUI32(indexLength);
        bos.writeUI32(varsOffset);
        bos.writeUI32(varsLength);
        bos.write(new byte[512 - size_of]);
    }
}
