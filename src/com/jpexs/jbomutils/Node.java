package com.jpexs.jbomutils;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author JPEXS
 */
public class Node {

    Map<String, Node> children = new TreeMap<>();
    TNodeType type = TNodeType.KNullNode;
    long mode = 0;
    long uid = 0;
    long gid = 0;
    long modtime = 0;
    long size = 0;
    long checksum = 0;
    //long linkNameLength = 0; //linkName length + 1
    String linkName;

}
