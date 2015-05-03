package com.jpexs.jbomutils;

import java.io.IOException;

/**
 *
 * @author JPEXS
 */
public interface WritableTo {

    public void writeTo(BomOutputStream bos) throws IOException;
}
