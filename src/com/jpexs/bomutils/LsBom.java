package com.jpexs.bomutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author JPEXS
 */
public class LsBom {

    public static final int LIST_FILES = 1 + 0,
            LIST_DIRS = 1 + 1,
            LIST_LINKS = 1 + 2,
            LIST_BDEVS = 1 + 3,
            LIST_CDEVS = 1 + 4,
            LIST_ALL = 0x1f;

    static int debug = 0;
    static byte[] data;
    static List<BomPointer> indexHeader;

    public static long lookup(int i) {
        BomPointer index = indexHeader.get(i);
        int addr = (int) index.address;

        /*  DEBUG(2, "@ index=0x" + hex + ntohl(i)
         + " addr=0x" + hex + setw(4) + setfill('0') + addr
         + " len=" + dec + ntohl(index.length));*/
        return addr;
    }

    public static void short_usage() {
        System.out.println("Usage: lsbom [-h] [-s] [-f] [-d] [-l] [-b] [-c] [-m] [-x]\n"
                + "\t" + "[-p parameters] bom ...");
    }

    public static void usage_error(String msg) {
        System.out.println(msg);
        short_usage();
        System.exit(1);
    }

    public static void usage() {
        short_usage();
        System.out.print("\n"
                + "\t-h              print full usage\n"
                + "\t-s              print pathnames only\n"
                + "\t-f              list files\n"
                + "\t-d              list directories\n"
                + "\t-l              list symbolic links\n"
                + "\t-b              list block devices\n"
                + "\t-c              list character devices\n"
                + "\t-m              print modified times\n"
                + "\t-x              suppress modes for directories and symlinks\n"
                + "\t-p parameters   print only some of the results.  EACH OPTION CAN "
                + "ONLY BE USED ONCE\n"
                + "\t\tParameters:\n"
                + "\t\t\tf    file name\n"
                + "\t\t\tF    file name with quotes (i.e. \"/usr/bin/lsbom\")\n"
                + "\t\t\tm    file mode (permissions)\n"
                + "\t\t\tM    symbolic file mode\n"
                + "\t\t\tg    group id\n"
                + "\t\t\tG    group name\n"
                + "\t\t\tu    user id\n"
                + "\t\t\tU    user name\n"
                + "\t\t\tt    mod time\n"
                + "\t\t\tT    formatted mod time\n"
                + "\t\t\ts    file size\n"
                + "\t\t\tS    formatted size\n"
                + "\t\t\tc    32-bit checksum\n"
                + "\t\t\t/    user id/group id\n"
                + "\t\t\t?    user name/group name\n"
                + "\t\t\tl    link name\n"
                + "\t\t\tL    quoted link name\n"
                + "\t\t\t0    device type\n"
                + "\t\t\t1    device major\n"
                + "\t\t\t2    device minor\n");
    }

    public static void error(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    public static void main(String[] args) {
        boolean suppressDirSimModes = false;
        boolean suppressDevSize = false;
        boolean pathsOnly = false;
        int listType = 0;
        String params = "";
        int optind = 0;
        loopi:
        for (int i = 0; i < args.length; i++) {

            switch (args[i]) {
                case "-h":
                    usage();
                    System.exit(0);
                case "-s":
                    pathsOnly = true;
                    break;
                case "-f":
                    listType |= LIST_FILES;
                    break;
                case "-d":
                    listType |= LIST_DIRS;
                    break;
                case "-l":
                    listType |= LIST_LINKS;
                    break;
                case "-b":
                    listType |= LIST_BDEVS;
                    break;
                case "-c":
                    listType |= LIST_CDEVS;
                    break;
                case "-m":
                    params += "T";
                    break;
                case "-x":
                    suppressDirSimModes = true;
                    break;
                case "-a":
                    usage_error("--arch not supported");
                    break;
                case "-p":
                    if (15 < args[i + 1].length()) {
                        usage_error("Too many parameters");
                    }
                    params = args[i + 1];
                    i++;
                    break;
                /*case "-D":
                 if (optarg) {
                 debug = atoi(optarg);
                 } else {
                 debug++;
                 }
                 break;*/
                case "-:":
                case "-?":
                    short_usage();
                    System.exit(1);
                default:
                    break loopi;
            }
            optind = i + 1;
        }

        if (optind == args.length) {
            usage();
            System.exit(1);
        }
        if (listType == 0) {
            listType = LIST_ALL;
        }
        if (params.isEmpty()) {
            params = "fm/scl0";
            suppressDevSize = true;
        }

        for (int i = optind; i < args.length; i++) {
            //ifstream f(argv[i], ios::in | ios::binary
            //);
            try (InputStream f = new FileInputStream(args[i])) {

                long length = new File(args[i]).length();
                // on unix we need an extra check as fstreams will happily open directories
                /*if ((int) length == -1) {
                 System.err.println("Unable to open file: \"" + argv[i] + "\"" );
                 return 1;
                 }*/

                // Allocate space
                data = new byte[(int) length];

                // Read data
                f.read(data);

            } catch (FileNotFoundException ex) {
                System.err.println("Unable to open file: \"" + args[i] + "\"");
                System.exit(1);
            } catch (IOException ex) {
                System.err.println("Failed to read BOM file");
                System.exit(1);
            }

            try {
                BomInputStream bis = Tools.getBIS(data);
                BomHeader header = new BomHeader(bis);

                if (!new String(header.magic).equals("BOMStore")) {
                    System.err.println("Not a BOM file: " + args[i]);
                    System.exit(1);
                }
                bis = Tools.getBIS(data, (int) header.indexOffset);
                int table_len = (int) bis.readUI32();
                indexHeader = new ArrayList<>();//BOMBlockTable *)(data + ntohl(header.indexOffset)
                for (int j = 0; j < table_len; j++) {
                    indexHeader.add(new BomPointer(bis));
                }
                // Process vars
                //BOMVars *vars = (BOMVars *)(data + ntohl(header.varsOffset));

                bis = Tools.getBIS(data, (int) header.varsOffset);
                int var_count = (int) bis.readUI32();
                List<BomVar> vars = new ArrayList<>();
                for (int j = 0; j < var_count; j++) {
                    vars.add(new BomVar(bis));
                }

                for (BomVar var : vars) {

                    BomTree tree = new BomTree(Tools.getBIS(data, (int) lookup((int) var.index)));
                    String name = var.name;

                    if (name.equals("Paths")) {
                        BomPaths paths = new BomPaths(Tools.getBIS(data, (int) lookup((int) tree.child)));

                        Map<Long, String> filenames = new HashMap<>();
                        Map<Long, Long> parents = new HashMap<>();

                        while (paths.isLeaf == 0) {
                            paths = new BomPaths(Tools.getBIS(data, (int) lookup((int) paths.indices.get(0).index0)));
                        }

                        while (paths != null) {
                            for (int j = 0; j < paths.count; j++) {
                                long index0 = paths.indices.get(j).index0;
                                long index1 = paths.indices.get(j).index1;

                                BomFile file = new BomFile(Tools.getBIS(data, (int) lookup((int) index1)));
                                BomPathInfo1 info1 = new BomPathInfo1(Tools.getBIS(data, (int) lookup((int) index0)));
                                //long length2;
                                BomPathInfo2 info2 = new BomPathInfo2(Tools.getBIS(data, (int) lookup((int) info1.index)));

                                // Compute full name
                                String filename = file.name;
                                filenames.put(info1.id, filename);
                                if (file.parent > 0) {
                                    parents.put(info1.id, file.parent);
                                }
                                Long it = parents.get(info1.id);
                                while (it != null) {
                                    filename = filenames.get(it) + "/" + filename;
                                    it = parents.get(it);
                                }

                                switch (info2.type) {
                                    case TYPE.FILE:
                                        if ((LIST_FILES & listType) == 0) {
                                            continue;
                                        }
                                        break;
                                    case TYPE.DIR:
                                        if ((LIST_DIRS & listType) == 0) {
                                            continue;
                                        }
                                        break;
                                    case TYPE.LINK:
                                        if ((LIST_LINKS & listType) == 0) {
                                            continue;
                                        }
                                        break;
                                    case TYPE.DEV: {
                                        int mode = info2.mode;
                                        boolean isBlock = (mode & 0x4000) > 0;
                                        if (isBlock && (LIST_BDEVS & listType) == 0) {
                                            continue;
                                        }
                                        if (!isBlock && (LIST_CDEVS & listType) == 0) {
                                            continue;
                                        }
                                        break;
                                    }
                                }

                                if (pathsOnly) {
                                    System.out.print(filename + '\n');
                                } else {
                                    // Print requested parameters
                                    boolean printed = true;
                                    for (int k = 0; k < params.length(); k++) {
                                        if (k > 0 && printed) {
                                            System.out.print('\t');
                                        }
                                        printed = true;

                                        switch (params.charAt(k)) {
                                            case 'f':
                                                System.out.print(filename);
                                                continue;
                                            case 'F':
                                                System.out.print('"' + filename + '"');
                                                continue;
                                            case 'g':
                                                System.out.print(info2.group);
                                                continue;
                                            case 'G':
                                                error("Group name not yet supported");
                                                break;
                                            case 'u':
                                                System.out.print(info2.user);
                                                continue;
                                            case 'U':
                                                error("User name not yet supported");
                                                break;
                                            case '/':
                                                System.out.print("" + info2.user + '/' + info2.group);
                                                continue;
                                            case '?':
                                                error("User/group name not yet supported");
                                                break;

                                            default:
                                                if (!suppressDirSimModes || (info2.type != TYPE.DIR && info2.type != TYPE.LINK)) {
                                                    switch (params.charAt(k)) {
                                                        case 'm':
                                                            System.out.print(Integer.toOctalString(info2.mode));
                                                            continue;
                                                        case 'M':
                                                            error("Symbolic mode not yet supported");
                                                            break;
                                                    }
                                                    if (info2.type == TYPE.FILE || info2.type == TYPE.LINK) {
                                                        switch (params.charAt(j)) {
                                                            case 't':
                                                                System.out.print(info2.modtime);
                                                                continue;
                                                            case 'T':
                                                                error("Formated mod time not yet supported");
                                                                break;
                                                            case 'c':
                                                                System.out.print(info2.checksum_devType);
                                                                continue;
                                                        }
                                                    }
                                                    if (info2.type != TYPE.DIR && (!suppressDevSize || info2.type != TYPE.DEV)) {
                                                        switch (params.charAt(j)) {
                                                            case 's':
                                                                System.out.print(info2.size);
                                                                continue;
                                                            case 'S':
                                                                error("Formated size not yet supported");
                                                                break;
                                                        }
                                                    }

                                                    if (info2.type == TYPE.LINK) {
                                                        switch (params.charAt(j)) {
                                                            case 'l':
                                                                System.out.print(info2.linkName);
                                                                continue;
                                                            case 'L':
                                                                System.out.print('"' + info2.linkName + '"');
                                                                continue;
                                                        }
                                                    }
                                                    if (info2.type == TYPE.DEV) {
                                                        long devType = info2.checksum_devType;

                                                        switch (params.charAt(j)) {
                                                            case '0':
                                                                System.out.print(devType);
                                                                continue;
                                                            case '1':
                                                                System.out.print(devType >> 24);
                                                                continue;
                                                            case '2':
                                                                System.out.print(devType & 0xff);
                                                                continue;
                                                        }
                                                    }
                                                }
                                                printed = false;
                                        }
                                    }
                                    System.out.print("\n");
                                }
                            }
                            if (paths.forward == 0) {
                                paths = null;
                            } else {
                                paths = new BomPaths(Tools.getBIS(data, (int) paths.forward));
                            }
                        }
                    }
                }

            } catch (IOException ex) {
                System.exit(1);
            }
        }

    }

}
