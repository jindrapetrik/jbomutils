package com.jpexs.bomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public interface WritableTo {

    public void writeTo(BomOutputStream bos) throws IOException;
}
