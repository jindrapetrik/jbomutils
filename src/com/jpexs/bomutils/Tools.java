package com.jpexs.bomutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author JPEXS
 */
public class Tools {

    public static final int sizeof_uint32_t = 4;
    public static final int sizeof_uint16_t = 2;

    /**
     * Simulation of C function getline
     *
     * @param is stream
     * @param delim delimiter
     * @return
     * @throws IOException
     */
    public static String getline(InputStream is, int delim) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int v;
        int len = 0;
        while (true) {
            v = is.read();
            if (v == -1 && len == 0) {
                return null;
            }
            if (v == -1 || v == delim) {
                break;
            }
            len++;
            baos.write(v);
        }
        return new String(baos.toByteArray());
    }

    public static String getline(InputStream is) throws IOException {
        return getline(is, '\n');
    }

    public static BomInputStream getBIS(byte data[]) {
        return getBIS(data, 0, data.length);
    }

    public static BomInputStream getBIS(byte data[], int offset) {
        return new BomInputStream(new ByteArrayInputStream(data, offset, data.length - offset));
    }

    public static BomInputStream getBIS(byte data[], int offset, int len) {
        return new BomInputStream(new ByteArrayInputStream(data, offset, len));
    }

    public static byte[] getBytes(WritableTo w) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BomOutputStream bos = new BomOutputStream(baos);
            bos.write(w);
            return baos.toByteArray();
        } catch (IOException ex) {
            //ignore
        }
        return new byte[0];
    }

}
