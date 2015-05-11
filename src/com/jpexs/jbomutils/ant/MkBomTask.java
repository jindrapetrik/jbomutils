package com.jpexs.jbomutils.ant;

import com.jpexs.jbomutils.MkBom;
import com.jpexs.jbomutils.PrintNode;
import com.jpexs.jbomutils.Stat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.TarFileSet;

/**
 *
 * @author JPEXS
 */
public class MkBomTask extends Task {

    private Project project;
    private boolean verbose = false;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private String destFile = null;

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    private List<TarFileSet> filesets = new ArrayList<>();

    public void addTarFileset(TarFileSet fileset) {
        filesets.add(fileset);
    }

    protected void validate() {
        if (filesets.isEmpty()) {
            throw new BuildException("fileset not set");
        }
        if (destFile == null) {
            throw new BuildException("destFile not set");
        }
    }

    private String[] getFileNames(FileSet fs) {
        DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());

        String[] directories = ds.getIncludedDirectories();
        String[] filesPerSe = ds.getIncludedFiles();

        String[] files = new String[directories.length + filesPerSe.length];

        System.arraycopy(directories, 0, files, 0, directories.length);
        System.arraycopy(filesPerSe, 0, files, directories.length, filesPerSe.length);

        return files;
    }

    @Override
    public void execute() {
        validate();
        System.out.println("MkBom: Creating BOM file to \"" + destFile + "\" ...");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Set<String> dirs = new HashSet<>();
        Set<String> files = new HashSet<>();
        for (TarFileSet fs : filesets) {

            String fullPath = fs.getFullpath(project);
            String prefix = fs.getPrefix(project);
            String fileNames[] = getFileNames(fs);

            long gid = fs.hasGroupIdBeenSet() ? fs.getGid() : Long.MAX_VALUE;
            long uid = fs.hasUserIdBeenSet() ? fs.getUid() : Long.MAX_VALUE;
            for (int i = 0; i < fileNames.length; i++) {
                String targetName;
                String fileName = fileNames[i];

                if (!fullPath.isEmpty()) {
                    targetName = fullPath;
                } else {
                    targetName = prefix + fileName;
                }
                targetName = targetName.replace('\\', '/');
                if (targetName.isEmpty()) {
                    targetName = ".";
                }
                if (!targetName.equals(".")) {
                    String path_parts[] = ("./" + targetName).split("/");
                    String pdir = "";
                    String dirname = targetName.contains("/") ? targetName.substring(0, targetName.lastIndexOf("/")) : "";
                    if (!dirs.contains(dirname)) {
                        for (int p = 0; p < path_parts.length - 1/*Last is filename*/; p++) {
                            String pname = path_parts[p];
                            if (!pdir.isEmpty()) {
                                pdir += "/";
                            }
                            pdir += pname;
                            if (!dirs.contains(pdir)) {
                                if (verbose) {
                                    System.out.println("MkBom: Adding parent dir \"" + pdir + "\" ...");
                                }
                                if (fullPath.isEmpty() && (pdir + "/").startsWith(prefix)) {
                                    PrintNode.print_node(output, fs.getDir(project).getAbsolutePath(), ((prefix.equals(pdir + "/")) ? prefix : pdir).replace("/", File.separator).substring(prefix.length()), pdir, uid, gid, false, fs.hasDirModeBeenSet() ? fs.getDirMode(project) : -1);
                                } else {
                                    PrintNode.print_custom_node(output, pdir, uid, gid, Stat.S_IFDIR + (fs.hasDirModeBeenSet() ? fs.getDirMode(project) : 0777));
                                }
                            }
                            dirs.add(pdir);
                            if (pdir.equals(".")) {
                                pdir = "";
                            }
                        }
                    }
                }
                if (verbose) {
                    System.out.println("MkBom: Adding \"" + targetName + "\" ...");
                }
                if (files.contains(targetName)) {
                    continue;
                }
                files.add(targetName);
                PrintNode.print_node(output, fs.getDir(project).getAbsolutePath(), fileName, targetName, uid, gid, false, fs.hasFileModeBeenSet() ? fs.getFileMode(project) : -1);
            }

        }

        try {
            MkBom.write_bom(new ByteArrayInputStream(output.toByteArray()), destFile);
            System.out.println("MkBom: File created in \"" + destFile + "\"");
        } catch (IOException ex) {
            throw new BuildException("MkBom: Cannot write to \"" + destFile + "\"", ex);
        }

    }

}
