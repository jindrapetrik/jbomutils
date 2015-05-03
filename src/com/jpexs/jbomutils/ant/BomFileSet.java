package com.jpexs.jbomutils.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ArchiveScanner;
import org.apache.tools.ant.types.TarFileSet;

/**
 *
 * @author JPEXS
 */
public class BomFileSet extends TarFileSet {

    private boolean uidSet = false;
    private boolean gidSet = false;
    private boolean dirModeSet = false;
    private boolean fileModeSet = false;

    @Override
    public void setDirMode(String octalString) {
        super.setDirMode(octalString);
        dirModeSet = true;
    }

    @Override
    public void setFileMode(String octalString) {
        super.setFileMode(octalString);
        fileModeSet = true;
    }

    @Override
    public int getFileMode(Project p) {
        return fileModeSet ? super.getFileMode(p) : -1;
    }

    @Override
    public int getDirMode(Project p) {
        return dirModeSet ? super.getDirMode(p) : -1;
    }

    @Override
    public void setGid(int gid) {
        super.setGid(gid);
        gidSet = true;
    }

    @Override
    public void setUid(int uid) {
        super.setUid(uid);
        uidSet = true;
    }

    @Override
    public int getGid() {
        return gidSet ? super.getGid() : -1;
    }

    @Override
    public int getUid() {
        return uidSet ? super.getUid() : -1;
    }

}
