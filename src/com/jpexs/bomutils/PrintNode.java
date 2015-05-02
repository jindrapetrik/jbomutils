package com.jpexs.bomutils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 *
 * @author JPEXS
 */
public class PrintNode {
    /* on unix system_path = path; on windows system_path is the windows native path format of path */

    public static void print_node(OutputStream output, final String base, final String system_path, final String path,
            long uid, long gid) {
        try {
            Stat s = new Stat();
            String fullpath = base;

            int stat_ret = 0;
            final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
            if (WINDOWS) {
                if (system_path.length() != 0) {
                    fullpath += "\\" + system_path;
                }
                stat_ret = Stat.stat(fullpath, s);
            } else {
                fullpath += "/" + system_path;
                stat_ret = Stat.lstat(fullpath, s);
            }
            if (stat_ret != 0) {
                System.err.println("Unable to find path: " + fullpath);
                System.exit(1);
            }
            output.write((path + "\t" + Integer.toOctalString(s.st_mode) + "\t").getBytes());
            output.write(("" + (uid == Long.MAX_VALUE ? s.st_uid : uid) + "/" + (gid == Long.MAX_VALUE ? s.st_gid : gid)).getBytes());
            if (Stat.S_ISREG(s.st_mode)) {
                output.write(("\t" + s.st_size + "\t" + Crc32.calc(new File(fullpath))).getBytes());
            }

            if (!WINDOWS) {
                if (Stat.S_ISLNK(s.st_mode)) {
                    String buffer = Files.readSymbolicLink(new File(fullpath).toPath()).toFile().getAbsolutePath();
                    output.write(("\t" + s.st_size + "\t" + Crc32.calc(buffer) + "\t" + buffer).getBytes());
                }
            }
            output.write(System.lineSeparator().getBytes());

            if (Stat.S_ISDIR(s.st_mode)) {
                File d = new File(fullpath);
                File files[] = d.listFiles();
                for (File f : files) {
                    String new_path = path;
                    new_path += "/" + f.getName();

                    String new_system_path = "";
                    if (WINDOWS) {
                        new_system_path = system_path;
                        new_system_path += /*"\\"*/ "/" + (f.getName()); //FIXME!!!
                    } else {
                        new_system_path = new_path;
                    }
                    print_node(output, base, new_system_path, new_path, uid, gid);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error during writing");
            System.exit(1);
        }
    }

    public static void print_node(OutputStream output, String directory, long uid, long gid) {
        if (directory.isEmpty()) {
            System.err.println("Invalid path");
            System.exit(1);
        }
        if (directory.charAt(directory.length() - 1) == '/') {
            directory = directory.substring(0, directory.length() - 1);
        }
        Stat s = new Stat();
        if (Stat.stat(directory, s) != 0) {
            System.err.println("Unable to find path: " + directory);
            System.exit(1);
        }
        if (Stat.S_ISDIR(s.st_mode) == false) {
            System.err.println("Argument must be a directory");
            System.exit(1);
        }
        print_node(output, directory, "", ".", uid, gid);
    }
}
