package com.jpexs.bomutils;

/**
 *
 * @author JPEXS
 */
public class Ls4MkBom {

    public static void usage() {
        System.out.println("Usage: java -jar bomutils.jar ls4mkbom [-u uid] [-g gid] path");
        System.out.println("\t-u\tForce user ID to the specified value");
        System.out.println("\t-g\tForce group ID to the specified value");
    }

    public static void main(String[] args) {
        long uid = Long.MAX_VALUE;
        long gid = Long.MAX_VALUE;
        int i = 0;
        for (; i < args.length; i++) {
            switch (args[i]) {
                case "-u":
                    uid = Long.parseLong(args[i + 1]);
                    i++;
                    break;
                case "-g":
                    gid = Long.parseLong(args[i + 1]);
                    i++;
                    break;
                case "-h":
                    usage();
                    System.exit(0);
                case "-:":
                case "-?":
                    usage();
                    System.exit(1);
            }
        }

        if (i == args.length) {
            usage();
            System.exit(1);
        }

        PrintNode.print_node(System.out, args[i], uid, gid);
        System.exit(0);
    }
}
