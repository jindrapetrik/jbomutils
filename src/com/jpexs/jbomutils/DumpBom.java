package com.jpexs.jbomutils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JPEXS
 */
public class DumpBom {

    public static void print_paths(BomPaths paths, byte[] buffer, List<BomPointer> block_table, int id) {

        try {
            System.out.println("");
            System.out.println("path id=" + id);
            System.out.println("paths.isLeaf = " + paths.isLeaf);
            System.out.println("paths.count = " + paths.count);
            System.out.println("paths.forward = " + paths.forward);
            System.out.println("paths.backward = " + paths.backward);
            for (int i = 0; i < paths.count; ++i) {
                BomPointer ptr = block_table.get((int) paths.indices.get(i).index1);

                BomFile file = new BomFile(Tools.getBIS(buffer, (int) ptr.address, (int) (buffer.length - ptr.address)));
                System.out.println("path.indices[" + i + "].index0 = " + paths.indices.get(i).index0);
                System.out.println("path.indices[" + i + "].index1.parent = " + file.parent);
                System.out.println("path.indices[" + i + "].index1.name = " + file.name);
            }

            if (paths.isLeaf == 0) {
                BomPointer child_ptr = block_table.get((int) paths.indices.get(0).index0);
                BomPaths child_paths = new BomPaths(Tools.getBIS(buffer, (int) child_ptr.address, (int) (buffer.length - child_ptr.address)));
                print_paths(child_paths, buffer, block_table, (int) paths.indices.get(0).index0);
            }

            if (paths.forward > 0) {
                BomPointer sibling_ptr = block_table.get((int) paths.forward);
                BomPaths sibling_paths = new BomPaths(Tools.getBIS(buffer, (int) sibling_ptr.address, (int) (buffer.length - sibling_ptr.address)));
                print_paths(sibling_paths, buffer, block_table, (int) paths.forward);
            }
        } catch (IOException ex) {

        }

    }

    public static void print_tree(BomTree tree, byte[] buffer, List<BomPointer> block_table) {

        try {
            System.out.println("tree.tree = " + new String(tree.tree));
            System.out.println("tree.version = " + tree.version);
            System.out.println("tree.child = " + tree.child);
            System.out.println("tree.blockSize = " + tree.blockSize);
            System.out.println("tree.pathCount = " + tree.pathCount);
            System.out.println("tree.unknown3 = " + (int) tree.unknown3);
            BomPointer child_ptr = block_table.get((int) tree.child);
            BomPaths paths = new BomPaths(Tools.getBIS(buffer, (int) child_ptr.address, (int) (buffer.length - child_ptr.address)));
            print_paths(paths, buffer, block_table, (int) tree.child);
        } catch (IOException ex) {
            Logger.getLogger(DumpBom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void usage() {
        System.out.println("Usage: java -jar bomutils.jar dumpbom bomfile");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
            System.exit(1);
        }

        byte[] buffer = null;
        long file_length = 0;
        {
            File f = new File(args[0]);
            try (DataInputStream bom_file = new DataInputStream(new FileInputStream(f))) {
                file_length = f.length();
                buffer = new byte[(int) file_length];
                bom_file.readFully(buffer);
            } catch (IOException ex) {
                System.err.println("Unable to read bomfile");
                System.exit(1);
            }
        }

        System.out.println(args[0]);
        System.out.println("file_length = " + file_length);

        try {
            System.out.println("Header:");
            System.out.println("-----------------------------------------------------");
            BomHeader header = new BomHeader(Tools.getBIS(buffer, 0, BomHeader.size_of));

            List<BomPointer> block_table = new ArrayList<>();

            BomInputStream bis = Tools.getBIS(buffer, (int) header.indexOffset, (int) (buffer.length - header.indexOffset));
            int blk_count = (int) bis.readUI32();
            for (int i = 0; i < blk_count; i++) {
                block_table.add(new BomPointer(bis));
            }
            int numberOfNonNullEntries = 0;
            for (int i = 0; i < block_table.size(); ++i) {
                if (block_table.get(i).address != 0) {
                    numberOfNonNullEntries++;
                }
            }

            {
                System.out.println("magic = \"" + new String(header.magic) + "\"");
                System.out.println("version = " + header.version);
                System.out.println("numberOfBlocks = " + header.numberOfBlocks);
                System.out.println("indexOffset = " + header.indexOffset);
                System.out.println("indexLength = " + header.indexLength);
                System.out.println("varsOffset = " + header.varsOffset);
                System.out.println("varsLength = " + header.varsLength);
                System.out.println("(calculated number of blocks = " + numberOfNonNullEntries + ")");
            }
            System.out.println();
            System.out.println("Index Table:");
            System.out.println("-----------------------------------------------------");
            System.out.println("numberOfBlockTableEntries = " + block_table.size());

            int free_list_pos = (int) (header.indexOffset + Tools.sizeof_uint32_t + (block_table.size() * BomPointer.sizeof));
            bis = Tools.getBIS(buffer, free_list_pos, buffer.length - free_list_pos);
            List<BomPointer> free_list = new ArrayList<>();
            int numberOfFreeListPointers = (int) bis.readUI32();
            System.out.println();
            System.out.println("Free List:");
            System.out.println("-----------------------------------------------------");
            System.out.println("numberOfFreeListPointers = " + numberOfFreeListPointers);
            for (int i = 0; i < numberOfFreeListPointers; ++i) {
                free_list.add(new BomPointer(bis));
            }
            System.out.println();
            System.out.println("Variables:");
            System.out.println("-----------------------------------------------------");

            int var_count;
            List<BomVar> vars = new ArrayList<>();
            {
                bis = Tools.getBIS(buffer, (int) header.varsOffset, (int) header.varsLength);
                var_count = (int) bis.readUI32();
                int total_length = 0;
                total_length += Tools.sizeof_uint32_t;
                for (int i = 0; i < var_count; ++i) {
                    BomVar var = new BomVar(bis);
                    vars.add(var);
                    total_length += Tools.sizeof_uint32_t;
                    total_length += var.name.getBytes().length + 1;
                }
                System.out.println("vars.count = " + vars.size());
                System.out.println("( calculated length = " + total_length + ")");

                for (int i = 0; i < vars.size(); i++) {
                    if (i != 0) {
                        System.out.print(",");
                    }
                    System.out.print("\"" + vars.get(i).name + "\"");
                }
                System.out.println();
            }
            for (int i = 0; i < var_count; ++i) {
                BomVar var = vars.get(i);
                String name = var.name;
                BomPointer ptr = block_table.get((int) var.index);
                System.out.println();
                System.out.println("\"" + name + "\" (file offset: 0x" + Long.toHexString(ptr.address) + " length: " + ptr.length + " )");
                System.out.println("-----------------------------------------------------");
                if (name.equals("Paths") || name.equals("HLIndex") || name.equals("Size64")) {
                    BomTree tree = new BomTree(Tools.getBIS(buffer, (int) ptr.address, (int) BomTree.size_of));
                    print_tree(tree, buffer, block_table);
                } else if (name.equals("BomInfo")) {
                    BomInfo info = new BomInfo(Tools.getBIS(buffer, (int) ptr.address));
                    System.out.println("info.version = " + info.version);
                    System.out.println("info.numberOfPaths = " + info.numberOfPaths);
                    System.out.println("info.numberOfInfoEntries = " + info.entries.size());
                    for (int j = 0; j < info.entries.size(); ++j) {
                        System.out.println("info.entries[" + j + "].unknown0 = " + info.entries.get(j).unknown0);
                        System.out.println("info.entries[" + j + "].unknown1 = " + info.entries.get(j).unknown1);
                        System.out.println("info.entries[" + j + "].unknown2 = " + info.entries.get(j).unknown2);
                        System.out.println("info.entries[" + j + "].unknown3 = " + info.entries.get(j).unknown3);
                    }
                } else if (name.equals("VIndex")) {
                    BomVIndex vindex = new BomVIndex(Tools.getBIS(buffer, (int) ptr.address));
                    System.out.println("vindex.unknown0 = " + vindex.unknown0);
                    System.out.println("vindex.indexToVTree = " + vindex.indexToVTree);
                    System.out.println("vindex.unknown2 = " + vindex.unknown2);
                    System.out.println("vindex.unknown3 = " + (int) vindex.unknown3);
                    System.out.println();
                    BomPointer v_ptr = block_table.get((int) vindex.indexToVTree);
                    BomTree tree = new BomTree(Tools.getBIS(buffer, (int) v_ptr.address));
                    print_tree(tree, buffer, block_table);
                } else {
                    int j = 0;
                    bis = Tools.getBIS(buffer, (int) ptr.address);
                    for (; j < ptr.length / Tools.sizeof_uint32_t; ++j) {
                        System.out.println("0x" + String.format("%08X", bis.readUI32()));
                    }
                    j *= Tools.sizeof_uint32_t;
                    for (; j < ptr.length; ++j) {
                        System.out.println("0x" + String.format("%02X", (int) buffer[(int) (ptr.address + j)]));
                    }
                }
            }
        } catch (IOException ex) {
            //should never happen
        }
        System.exit(0);

    }
}
