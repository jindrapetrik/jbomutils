package com.jpexs.bomutils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author JPEXS
 */
public class BomStorage {

    private long size_of_header;
    private BomHeader header;

    private long size_of_vars;
    private long num_vars;
    private List<BomVar> vars;

    private long size_of_block_table;
    private long num_block_entries;
    private List<BomPointer> block_table;

    private long size_of_free_list;
    private long num_free_list_entries;
    private List<BomPointer> free_list;

    private long entry_size;

    byte[] entries;

    public BomStorage() {
        size_of_header = 512;
        header = new BomHeader();
        num_block_entries = 1;
        size_of_block_table = Tools.sizeof_uint32_t + num_block_entries * BomPointer.sizeof;
        block_table = new ArrayList<>();
        size_of_free_list = Tools.sizeof_uint32_t + 2 * BomPointer.sizeof;
        free_list = new ArrayList<>();
        num_free_list_entries = 0;
        num_vars = 0;
        size_of_vars = Tools.sizeof_uint32_t;
        vars = new ArrayList<>();

        entry_size = 0;
        //entries = null
        header = new BomHeader();
        header.numberOfBlocks = 0;
        header.indexOffset = size_of_header + size_of_vars + entry_size;
        header.indexLength = size_of_block_table + size_of_free_list;
        header.varsOffset = size_of_header;
        header.varsLength = size_of_vars;
        //block_table.numberOfBlockTablePointers = num_block_entries;
        block_table.add(new BomPointer());
        block_table.get(0).address = 0;
        block_table.get(0).length = 0;
        vars = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            free_list.add(new BomPointer());
            free_list.get(i).address = 0;
            free_list.get(i).length = 0;
        }

    }

    public byte[] getBlock(int id) {
        return Arrays.copyOfRange(entries, (int) block_table.get(id).address, (int) block_table.get(id).address + (int) block_table.get(id).length);
    }

    public int getBlockAddr(int id) {
        return (int) block_table.get(id).address;
    }

    public int getBlockLength(int id) {
        return (int) block_table.get(id).length;
    }

    public int addBlock(byte[] data, int length) {
        if (entries == null) {
            entries = new byte[length];
        } else {
            entries = Arrays.copyOf(entries, length + (int) entry_size);
        }
        for (int i = 0; i < length; i++) {
            entries[(int) entry_size + i] = data[i];
        }
        size_of_block_table = Tools.sizeof_uint32_t + ((num_block_entries + 1) * BomPointer.sizeof);
        //block_table = new BOMBlockTable//(BOMBlockTable *)realloc( block_table, size_of_block_table );
        block_table.add(new BomPointer());
        block_table.get((int) num_block_entries).address = entry_size;   // This will be converted to the right value later on.
        block_table.get((int) num_block_entries).length = length;
        num_block_entries++;
        entry_size += length;
        //block_table.numberOfBlockTablePointers = num_block_entries;
        /* update header */
        header.numberOfBlocks = header.numberOfBlocks + 1;
        header.indexLength = size_of_block_table + size_of_free_list;
        return (int) num_block_entries - 1;
    }

    public void addVar(String name, byte[] data, int length) {
        int new_size = Tools.sizeof_uint32_t + 1 + name.length();

        BomVar var = new BomVar();
        size_of_vars += new_size;
        var.index = addBlock(data, length);
        var.name = name;
        vars.add(var);
        /* update header */
        header.indexOffset = size_of_header + size_of_vars + entry_size;
        header.varsLength = size_of_vars;
    }

    public void write(OutputStream bom_file) throws IOException {
        BomOutputStream bos = new BomOutputStream(bom_file);

        bos.write(header);//size_of_header        
        bos.writeUI32(vars.size());
        for (BomVar v : vars) {
            bos.write(v);
        }
        if (entries != null) {
            bos.write(entries, 0, (int) entry_size);
        }
        @SuppressWarnings("unchecked")
        List<BomPointer> temp = (ArrayList<BomPointer>) ((ArrayList<BomPointer>) block_table).clone();
        for (int i = 0; i < temp.size(); ++i) {
            if (temp.get(i).length != 0) {
                temp.get(i).address = temp.get(i).address + size_of_header + size_of_vars;
            }
        }
        bos.writeUI32(temp.size());
        for (BomPointer t : temp) {
            bos.write(t);
        }
        bos.writeUI32(free_list.size() - 2);
        for (BomPointer f : free_list) {
            bos.write(f);
        }
    }
}
