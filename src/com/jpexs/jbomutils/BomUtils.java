package com.jpexs.jbomutils;

import java.util.Arrays;

/**
 * BOMUtils - Open source tools to create bill-of-materials files used in Mac OS
 * X installers.
 *
 * Based on https://github.com/hogliux/bomutils
 *
 * @author JPEXS
 */
public class BomUtils {

    public static void usage() {
        MkBom.usage();
        Ls4MkBom.usage();
        DumpBom.usage();
        LsBom.short_usage();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        switch (args[0]) {
            case "mkbom":
                MkBom.main(Arrays.copyOfRange(args, 1, args.length - 1));
                break;
            case "ls4mkbom":
                Ls4MkBom.main(Arrays.copyOfRange(args, 1, args.length - 1));
                break;
            case "dumpbom":
                DumpBom.main(Arrays.copyOfRange(args, 1, args.length - 1));
                break;
            case "lsbom":
                LsBom.main(Arrays.copyOfRange(args, 1, args.length - 1));
                break;
            default:
                usage();
                System.exit(1);
        }
    }
}
