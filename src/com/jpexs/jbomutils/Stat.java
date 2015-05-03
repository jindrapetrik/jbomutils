package com.jpexs.jbomutils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Simulation of stat/lstat C function
 *
 * @author JPEXS
 */
public class Stat {

    public static final int S_IFMT = 00170000;
    public static final int S_IFSOCK = 0140000;
    public static final int S_IFLNK = 0120000;
    public static final int S_IFREG = 0100000;
    public static final int S_IFBLK = 0060000;
    public static final int S_IFDIR = 0040000;
    public static final int S_IFCHR = 0020000;
    public static final int S_IFIFO = 0010000;
    public static final int S_ISUID = 0004000;
    public static final int S_ISGID = 0002000;
    public static final int S_ISVTX = 0001000;

    public static boolean S_ISREG(int m) {
        return (m & S_IFMT) == S_IFREG;
    }

    public static boolean S_ISLNK(int m) {
        return (m & S_IFMT) == S_IFLNK;
    }

    public static boolean S_ISDIR(int m) {
        return (m & S_IFMT) == S_IFDIR;
    }

    public static boolean S_ISCHR(int m) {
        return (m & S_IFMT) == S_IFCHR;
    }

    public static boolean S_ISBLK(int m) {
        return (m & S_IFMT) == S_IFBLK;
    }

    public static boolean S_ISFIFO(int m) {
        return (m & S_IFMT) == S_IFIFO;
    }

    public static boolean S_ISSOCK(int m) {
        return (m & S_IFMT) == S_IFSOCK;
    }

    long st_dev;    /* inode's device */

    long st_ino;    /* inode's number */

    int st_mode;   /* inode protection mode */

    long st_nlink;  /* number of hard links */

    long st_uid;    /* user ID of the file's owner */

    long st_gid;    /* group ID of the file's group */

    long st_rdev;   /* device type */

    long st_atim;  /* time of last access */

    long st_mtim;  /* time of last data modification */

    long st_ctim;  /* time of last file status change */

    long st_size;   /* file size, in bytes */

    //int64_t

    long st_blocks; /* blocks allocated for file */

    long st_blksize;/* optimal blocksize for I/O */

    long st_flags;  /* user defined flags for file */

    long st_gen;    /* file generation number */


    public static int stat(String path, Stat s) {
        File f = new File(path);
        Path p = f.toPath();
        try {
            s.st_mode = 0;
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            if (attrs.isDirectory()) {
                s.st_mode |= S_IFDIR;
            }
            if (attrs.isRegularFile()) {
                s.st_mode |= S_IFREG;
            }
            if (attrs.isSymbolicLink()) {
                s.st_mode |= S_IFLNK;
            }
            s.st_atim = attrs.lastAccessTime().toMillis();
            s.st_mtim = attrs.lastModifiedTime().toMillis();
            s.st_ctim = attrs.creationTime().toMillis();
            s.st_size = attrs.size();
            s.st_mode |= (f.canRead() ? 0444 : 0) + (f.canWrite() ? 0222 : 0) + (f.canExecute() ? 0111 : 0);

        } catch (IOException ex) {
            return 1;
        }

        s.st_gid = 0;
        s.st_uid = 0;
        s.st_ino = 0;
        return 0;
    }

    public static int lstat(String path, Stat s) {
        int s_ret = stat(path, s);
        if (s_ret != 0) {
            return s_ret;
        }
        Path p = new File(path).toPath();

        try {
            s.st_gid = (Integer) Files.readAttributes(p, "unix:gid").get("gid");
        } catch (IOException ex) {
            return 1;
        }

        try {
            s.st_uid = (Integer) Files.readAttributes(p, "unix:uid").get("uid");
        } catch (IOException ex) {
            return 1;
        }

        try {
            s.st_mode = (Integer) Files.readAttributes(p, "unix:mode").get("mode");
        } catch (IOException ex) {
            return 1;
        }
        return 0;
    }
}
