package com.jpexs.jbomutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author JPEXS
 */
public class MkBom {

    private static int dec_octal_to_int(int dec_rep_octal) {
        int retval = 0;
        for (int n = 1; dec_rep_octal > 0; n *= 8) {
            int digit = dec_rep_octal - ((dec_rep_octal / 10) * 10);
            if (digit > 7) {
                throw new RuntimeException("argument not in dec oct rep");
            }
            retval += digit * n;
            dec_rep_octal /= 10;
        }
        return retval;
    }

    private static class Pair<A, B> {

        public A first;
        public B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

    }
   
    public static void write_bom(InputStream is, String outputPath) throws IOException {
        Node root = new Node();
        int num;
        root.type = TNodeType.KRootNode;
        {
            Map<String, Node> all_nodes = new LinkedHashMap<>();
            String line;
            while ((line = Tools.getline(is)) != null) {
                Node n = new Node();
                InputStream ss = new ByteArrayInputStream(line.getBytes());

                String name = Tools.getline(ss, '\t');
                if (name == null) {
                    throw new RuntimeException("Syntax error in lsbom input");
                }
                String[] elements;
                String rest = Tools.getline(ss);
                rest = rest.replaceFirst("/", " ");
                elements = rest.split("\\s");
                n.mode = dec_octal_to_int(Integer.parseInt(elements[0]));
                n.uid = Integer.parseInt(elements[1]);
                n.gid = Integer.parseInt(elements[2]);
                n.modtime = Long.parseLong(elements[3]);
                n.size = 0;
                n.checksum = 0;
                //n.linkNameLength = 0;              
                if ((n.mode & 0xF000) == 0x4000) {
                    n.type = TNodeType.KDirectoryNode;
                    n.linkName = "";
                } else if ((n.mode & 0xF000) == 0x8000) {
                    n.type = TNodeType.KFileNode;
                    n.size = Integer.parseInt(elements[4]);
                    n.checksum = Long.parseUnsignedLong(elements[5]);
                    n.linkName = "";
                } else if ((n.mode & 0xF000) == 0xA000) {
                    n.type = TNodeType.KSymbolicLinkNode;
                    n.size = Integer.parseInt(elements[4]);
                    n.checksum = Long.parseUnsignedLong(elements[5]);
                    //n.linkNameLength = elements[5].length() + 1;
                    n.linkName = elements[6];
                } else {
                    throw new RuntimeException("Node type not supported");
                }
                all_nodes.put(name, n);
            }
            for (String name : all_nodes.keySet()) {
                List<String> path_elements = new ArrayList<>();
                ByteArrayInputStream ss = new ByteArrayInputStream(name.getBytes());
                String element;
                while ((element = Tools.getline(ss, '/')) != null) {
                    path_elements.add(element);
                }
                Node parent = root;
                String full_path = "";
                for (String jt : path_elements) {
                    full_path += jt;
                    if (!parent.children.containsKey(jt)) {
                        if (!all_nodes.containsKey(full_path)) {
                            throw new RuntimeException("Parent directory of file/folder \"" + full_path + "\" does not appear in list");
                        }
                        parent.children.put(jt, all_nodes.get(full_path));
                    }
                    parent = parent.children.get(jt);
                    full_path += "/";
                }
            }
            num = all_nodes.size();
        }
                
        BomStorage bom = new BomStorage();
        {
            int bom_info_size = (Tools.sizeof_uint32_t * 3) + (((num != 0) ? 1 : 0) * BomInfoEntry.size_of);
            BomInfo info = new BomInfo();
            //malloc(bom_info_size);
            //memset(info, 0, bom_info_size);
            info.version = 1;
            info.numberOfPaths = (num + 1);
            if ((num != 0) && (info.entries.isEmpty())) {
                info.entries.add(new BomInfoEntry());
            }
            if (num != 0) {
                // info.entries.get(0).unknown2 = 57826303 /* ???? */
                info.entries.get(0).unknown2 = 0; /* ???? */

            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BomOutputStream bos = new BomOutputStream(baos);
            info.writeTo(bos);
            bom.addVar("BomInfo", baos.toByteArray(), bom_info_size);
        }

        {
            BomTree tree = new BomTree();
            tree.version = 1;
            tree.blockSize = 4096;
            tree.pathCount = num;
            tree.unknown3 = 0; /* ?? */

            int num_paths = (int) Math.ceil(num / 256.0);
            int path_size = (Tools.sizeof_uint16_t * 2) + (Tools.sizeof_uint32_t * 2)
                    + (num_paths * BomPathIndices.size_of);
            BomPaths root_paths = new BomPaths();
            //malloc(path_size);
            root_paths.isLeaf = 0;
            root_paths.count = num_paths;
            root_paths.forward = 0;
            root_paths.backward = 0;
            root_paths.indices = new ArrayList<>();

            Stack<Pair<Long, Node>> stack = new Stack<>();

            stack.push(new Pair<>(0L, root));
            int j = 0;
            int k = 0;
            int current_path = 0;
            int current_path_size = 0;
            int last_file_info = 0;
            int last_paths_id = 0;
            BomPaths paths = null;
            while (!stack.isEmpty()) {
                Pair<Long, Node> p = stack.pop();
                Node arg = p.second;
                long parent = p.first;                
                for (String it : arg.children.keySet()) {
                    Node node = arg.children.get(it);
                    String s = it;
                    if (k == 0) {
                        int new_paths_id = 0;
                        if (paths != null) {
                            new_paths_id = bom.addBlock(Tools.getBytes(paths), current_path_size);
                            root_paths.indices.add(new BomPathIndices());
                            //root_paths.indices[current_path] = new BOMPathIndices();
                            root_paths.indices.get(current_path).index0 = new_paths_id;
                            if (last_paths_id != 0) {
                                BomPaths prev_paths = new BomPaths(new BomInputStream(new ByteArrayInputStream(bom.getBlock(last_paths_id))));
                                prev_paths.forward = new_paths_id;
                            }
                            root_paths.indices.get(current_path).index1 = last_file_info;
                            paths = null;
                            current_path++;
                        }
                        int next_num = 256 < (num - j) ? 256 : (num - j);
                        current_path_size = (Tools.sizeof_uint16_t * 2) + (Tools.sizeof_uint32_t * 2)
                                + (next_num * BomPathIndices.size_of);
                        //paths = (BOMPaths *)
                        //malloc(current_path_size);
                        paths = new BomPaths();
                        paths.isLeaf = 1;
                        paths.count = next_num;
                        paths.forward = 0;
                        paths.backward = new_paths_id;
                        last_paths_id = new_paths_id;
                    }

                    int bom_path_info2_size = BomPathInfo2.size_of + node.linkName.getBytes().length;
                    BomPathInfo2 info2 = new BomPathInfo2();
                    if (node.type == TNodeType.KDirectoryNode) {
                        info2.type = TYPE.DIR;
                    } else if (node.type == TNodeType.KFileNode) {
                        info2.type = TYPE.FILE;
                    } else {
                        info2.type = TYPE.LINK;
                    }
                    info2.unknown0 = 1;
                    info2.architecture = 3; /* ?? */

                    info2.mode = (int) node.mode;
                    info2.user = node.uid;
                    info2.group = node.gid;
                    info2.modtime = node.modtime;
                    info2.size = node.size;
                    info2.unknown1 = 1;
                    info2.checksum_devType = node.checksum;
                    //info2.linkNameLength = node.linkNameLength;
                    info2.linkName = node.linkName;

                    BomPathInfo1 info1 = new BomPathInfo1();
                    info1.id = j + 1;
                    info1.index = bom.addBlock(Tools.getBytes(info2), bom_path_info2_size);
                    paths.indices.add(new BomPathIndices());
                    paths.indices.get(k).index0 = bom.addBlock(Tools.getBytes(info1), BomPathInfo1.size_of);

                    //free((void *) info2);
                    int bom_file_size = Tools.sizeof_uint32_t + 1 + s.getBytes().length;
                    BomFile f = new BomFile();
                    //malloc(bom_file_size);
                    f.parent = parent;
                    f.name = s;
                    paths.indices.get(k).index1 = last_file_info = (bom.addBlock(Tools.getBytes(f), bom_file_size));
                    //free((void *) f );

                    //stack.push_back(std::pair < uint32_t, const Node * > (j + 1, &node ) );
                    stack.add(0, new Pair<>((Long) (long) (j + 1), node));
                    j++;
                    k = (k + 1) % 256;                                        
                }
            }

            if (num_paths > 1) {
                root_paths.indices.get(current_path).index0 = (new BomPaths(Tools.getBIS(bom.getBlock(last_paths_id)))).forward = bom.addBlock(Tools.getBytes(paths), current_path_size);
                root_paths.indices.get(current_path).index1 = last_file_info;
                tree.child = bom.addBlock(Tools.getBytes(root_paths), path_size);
            } else {
                tree.child = bom.addBlock(Tools.getBytes(paths), current_path_size);
            }

            bom.addVar("Paths", Tools.getBytes(tree), BomTree.size_of);
        }

        {
            int path_size = (Tools.sizeof_uint16_t * 2) + (Tools.sizeof_uint32_t * 2);
            BomPaths empty_path = new BomPaths(); //(BOMPaths*)malloc( path_size );
            empty_path.isLeaf = 1;
            empty_path.count = 0;
            empty_path.forward = 0;
            empty_path.backward = 0;

            BomTree tree = new BomTree();
            tree.tree = "tree".getBytes();
            tree.version = 1;
            tree.blockSize = 4096;
            tree.pathCount = 0;
            tree.unknown3 = 0;

            tree.child = (bom.addBlock(Tools.getBytes(empty_path), path_size));
            bom.addVar("HLIndex", Tools.getBytes(tree), BomTree.size_of);

            BomVIndex vindex = new BomVIndex();
            vindex.unknown0 = (1);
            tree.child = (bom.addBlock(Tools.getBytes(empty_path), path_size));
            tree.blockSize = (128);
            vindex.indexToVTree = (bom.addBlock(Tools.getBytes(tree), BomTree.size_of));
            vindex.unknown2 = (0);
            vindex.unknown3 = 0;
            bom.addVar("VIndex", Tools.getBytes(vindex), BomVIndex.size_of);

            tree.blockSize = (4096);
            tree.child = (bom.addBlock(Tools.getBytes(empty_path), path_size));
            bom.addVar("Size64", Tools.getBytes(tree), BomTree.size_of);
        }
        bom.write(new FileOutputStream(outputPath));
    }

    public static void usage() {
        System.out.println("Usage: java -jar bomutils.jar mkbom [-i] [-u uid] [-g gid] source target-bom-file");
        System.out.println("\t-i\tTreat source as a file in the format generated by ls4mkbom and lsbom");
        System.out.println("\t-u\tForce user ID to the specified value (incompatible with -i)");
        System.out.println("\t-g\tForce group ID to the specified value (incompatible with -i)");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        long uid = Long.MAX_VALUE;
        long gid = Long.MAX_VALUE;
        boolean isFileListSource = false;
        int i;
        loopi:
        for (i = 0; i < args.length; i++) {

            switch (args[i]) {
                case "-i":
                    isFileListSource = true;
                    break;
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
                default:
                    break loopi;
            }
        }

        if (i + 2 > args.length) {
            usage();
            System.exit(1);
        }

        String source = args[i];
        String target_bom = args[i + 1];

        if (isFileListSource) {

            FileInputStream file_list = null;
            try {
                file_list = new FileInputStream(source);
            } catch (IOException ex) {
                System.err.println("Unable to open file list: " + source);
                System.exit(1);
            }
            if ((uid != Long.MAX_VALUE) || (gid != Long.MAX_VALUE)) {
                System.err.println("The -u and -g options cannot be used with -i");
                System.exit(1);
            }
            try {
                write_bom(file_list, target_bom);
            } catch (IOException ex) {
                System.exit(1);
            }
        } else {
            String buffer;
            {
                ByteArrayOutputStream ss = new ByteArrayOutputStream();
                PrintNode.print_node(ss, source, uid, gid);
                buffer = new String(ss.toByteArray());
            }
            ByteArrayInputStream file_list = new ByteArrayInputStream(buffer.getBytes());

            try {
                write_bom(file_list, target_bom);
            } catch (IOException ex) {
                System.exit(1);
            }
        }
        System.exit(0);
    }

}
