package com.jpexs.jbomutils;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author JPEXS
 */
public class Node {

    Map<String, Node> children = new HashMap<>();
    TNodeType type = TNodeType.KNullNode;
    long mode = 0;
    long uid = 0;
    long gid = 0;
    long size = 0;
    long checksum = 0;
    //long linkNameLength = 0; //linkName length + 1
    String linkName;

}
