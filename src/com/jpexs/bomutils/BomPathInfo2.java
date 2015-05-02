package com.jpexs.bomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public class BomPathInfo2 implements WritableTo {

    public static final int size_of = 1 + 1 + 2 * Tools.sizeof_uint16_t + 4 * Tools.sizeof_uint32_t + 1 + Tools.sizeof_uint32_t + Tools.sizeof_uint32_t;

    //uint8_t
    int type;           // See type enums above
    //uint8_t 
    int unknown0;       // = 1 (?)
    //uint16_t 
    int architecture;  // Not sure exactly what this means
    //uint16_t 
    int mode;
    long user;
    long group;
    long modtime;
    long size;
    //uint8_t
    int unknown1;       // = 1 (?)
    long checksum_devType; //union
    //long linkNameLength;
    //char linkName[];
    String linkName = "";

    public BomPathInfo2() {
    }

    public BomPathInfo2(BomInputStream bis) throws IOException {
        type = bis.read();
        unknown0 = bis.read();
        architecture = bis.readUI16();
        mode = bis.readUI16();
        user = bis.readUI32();
        group = bis.readUI32();
        modtime = bis.readUI32();
        size = bis.readUI32();
        unknown1 = bis.read();
        checksum_devType = bis.readUI32();
        int linkNameLength = (int) bis.readUI32();
        byte data[] = new byte[linkNameLength];
        bis.readFully(data);
        linkName = new String(data, "UTF-8");
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.write(type);
        bos.write(unknown0);
        bos.writeUI16(architecture);
        bos.writeUI16(mode);
        bos.writeUI32(user);
        bos.writeUI32(group);
        bos.writeUI32(modtime);
        bos.writeUI32(size);
        bos.write(unknown1);
        bos.writeUI32(checksum_devType);
        byte[] data = linkName.getBytes("UTF-8");
        bos.writeUI32(data.length);
        bos.write(data);
    }
}
