package com.jpexs.jbomutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JPEXS
 */
public class BomInfo implements WritableTo {

    long version;
    long numberOfPaths;
    //long numberOfInfoEntries;
    List<BomInfoEntry> entries = new ArrayList<>();

    public BomInfo() {
    }

    public BomInfo(BomInputStream bis) throws IOException {
        version = bis.readUI32();
        numberOfPaths = bis.readUI32();
        int numberOfInfoEntries = (int) bis.readUI32();
        for (int i = 0; i < numberOfInfoEntries; i++) {
            entries.add(new BomInfoEntry(bis));
        }
    }

    @Override
    public void writeTo(BomOutputStream bos) throws IOException {
        bos.writeUI32(version);
        bos.writeUI32(numberOfPaths);
        bos.writeUI32(entries.size());
        for (BomInfoEntry b : entries) {
            b.writeTo(bos);
        }
    }
}
