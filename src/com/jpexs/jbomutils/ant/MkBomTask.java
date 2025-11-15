package com.jpexs.jbomutils.ant;

import com.jpexs.jbomutils.MkBom;
import com.jpexs.jbomutils.PrintNode;
import com.jpexs.jbomutils.Stat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    private static interface NodeBase {
        public String getPath();
        
        public void write(OutputStream os);
    }
    
    private static class Node implements NodeBase {
        String base;
        String system_path;
        String path;
        long uid;
        long gid;
        boolean recursive;
        int fileMode;

        public Node(String base, String system_path, String path, long uid, long gid, boolean recursive, int fileMode) {
            this.base = base;
            this.system_path = system_path;
            this.path = path;
            this.uid = uid;
            this.gid = gid;
            this.recursive = recursive;
            this.fileMode = fileMode;
        }                

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public void write(OutputStream os) {
            PrintNode.print_node(os, base, system_path, path, uid, gid, recursive, fileMode);
        }
    }
    
    private static class CustomNode implements NodeBase {
        String path;
        long uid;
        long gid;
        int mode;

        public CustomNode(String path, long uid, long gid, int mode) {
            this.path = path;
            this.uid = uid;
            this.gid = gid;
            this.mode = mode;
        }

        @Override
        public String getPath() {
            return path;
        }      

        @Override
        public void write(OutputStream os) {
            PrintNode.print_custom_node(os, path, uid, gid, mode);
        }                
    }
    
    @Override
    public void execute() {
        validate();
        System.out.println("MkBom: Creating BOM file to \"" + destFile + "\" ...");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Set<String> dirs = new HashSet<>();
        Set<String> files = new HashSet<>();
        Map<String, NodeBase> nodes = new TreeMap<>();
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
                } else if (!targetName.startsWith("./")) {
                    targetName = "./" + targetName;
                }
                if (!targetName.equals(".")) {
                    String path_parts[] = targetName.split("/", -1);
                    String pdir = "";
                    String dirname = targetName.substring(0, targetName.lastIndexOf("/"));
                    for (int p = 0; p < path_parts.length - 1/*Last is filename*/; p++) {
                        String pname = path_parts[p];
                        if (!pdir.isEmpty()) {
                            pdir += "/";
                        }
                        pdir += pname;
                        if (!nodes.containsKey(pdir)) {
                            /*if (verbose) {
                                System.out.println("MkBom: Adding parent dir \"" + pdir + "\" ...");
                            }*/
                            if (fullPath.isEmpty() && (pdir + "/").startsWith(prefix)) {
                                //PrintNode.print_node(output, fs.getDir(project).getAbsolutePath(), ((prefix.equals(pdir + "/")) ? prefix : pdir).replace("/", File.separator).substring(prefix.length()), pdir, uid, gid, false, fs.hasDirModeBeenSet() ? fs.getDirMode(project) : -1);
                                nodes.put(pdir, new Node(fs.getDir(project).getAbsolutePath(), ((prefix.equals(pdir + "/")) ? prefix : pdir).replace("/", File.separator).substring(prefix.length()), pdir, uid, gid, false, fs.hasDirModeBeenSet() ? fs.getDirMode(project) : -1));
                            } else {
                                //PrintNode.print_custom_node(output, pdir, uid, gid, Stat.S_IFDIR + (fs.hasDirModeBeenSet() ? fs.getDirMode(project) : 0777));
                                nodes.put(pdir, new CustomNode(pdir, uid, gid, Stat.S_IFDIR + (fs.hasDirModeBeenSet() ? fs.getDirMode(project) : 0777)));
                            }
                        }                            
                    }                    
                }
                
                if (files.contains(targetName)) {
                    continue;
                }
                files.add(targetName);
                //PrintNode.print_node(output, fs.getDir(project).getAbsolutePath(), fileName, targetName, uid, gid, false, fs.hasFileModeBeenSet() ? fs.getFileMode(project) : -1);
                nodes.put(targetName, new Node(fs.getDir(project).getAbsolutePath(), fileName, targetName, uid, gid, false, fs.hasFileModeBeenSet() ? fs.getFileMode(project) : -1));
            }

        }        
        
        for (String path : nodes.keySet()) {            
            if (verbose) {
                System.out.println("MkBom: Adding \"" + path + "\" ...");
            }
            nodes.get(path).write(output);
        }

        try {
            MkBom.write_bom(new ByteArrayInputStream(output.toByteArray()), destFile);
            System.out.println("MkBom: File created in \"" + destFile + "\"");
        } catch (IOException ex) {
            throw new BuildException("MkBom: Cannot write to \"" + destFile + "\": " + ex.getMessage(), ex);
        }

    }

}
