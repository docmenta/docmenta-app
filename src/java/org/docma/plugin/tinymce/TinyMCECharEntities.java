/*
 * TinyMCECharEntities.java
 * 
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.plugin.tinymce;

import org.docma.app.*;
import org.docma.coreapi.*;
import org.docma.plugin.CharEntity;
import java.io.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class TinyMCECharEntities
{
    private static final String CHAR_MAP_PATH = "themes" + File.separator +
                                                "advanced" + File.separator +
                                                "js" + File.separator +
                                                "charmap.js";

    private final File charMapFile;
    private CharEntity[] charMap = null;
    private String beforeCharMap = null;
    private String afterCharMap = null;

    private String entitiesConfig = null;  // JavaScript configuration string for TinyMCE char entities 


    public TinyMCECharEntities(File tinyMCEDir)
    {
        charMapFile = new File(tinyMCEDir, CHAR_MAP_PATH);
    }


    public CharEntity[] getCharEntities()
    {
        if (charMap == null) {
            try {
                readCharMap(charMapFile, true);
            } catch(Exception ex) {
                throw new DocRuntimeException(ex);
            }
        }
        return charMap;
    }

    public String getCharEntitiesConfigString()
    {
        if (entitiesConfig == null) {
            CharEntity[] entities = getCharEntities();

            // Create entities configuration for tinyMCE editor
            StringBuilder buf = new StringBuilder(4000);
            for (int i=0; i < entities.length; i++) {
                if (i > 0) {
                    buf.append(",");
                }
                // Append numeric value of entity
                String numeric = entities[i].getNumeric();
                int start_pos = numeric.startsWith("&#") ? 2 : 0;
                int end_pos = numeric.endsWith(";") ? numeric.length() - 1 : numeric.length();
                buf.append(numeric.substring(start_pos, end_pos));

                buf.append(",");

                // Append entity name
                String symbolic = entities[i].getSymbolic();
                start_pos = symbolic.startsWith("&") ? 1 : 0;
                end_pos = symbolic.endsWith(";") ? symbolic.length() - 1 : symbolic.length();
                buf.append(symbolic.substring(start_pos, end_pos));
            }
            entitiesConfig = buf.toString();
        }
        return entitiesConfig;
    }


    public void setCharEntities(CharEntity[] entities)
    {
        entitiesConfig = null;  // clear cached config string
        
//        if (entities instanceof DocmaCharEntity[]) {
//            charMap = (DocmaCharEntity[]) entities;
//        } else {
//            DocmaCharEntity[] arr = new DocmaCharEntity[entities.length];
//            for (int i=0; i < entities.length; i++) {
//                CharEntity e = entities[i];
//                arr[i] = new DocmaCharEntity(e.getSymbolic(), e.getNumeric(), e.isSelectable(), e.getDescription());
//            }
//            charMap = arr;
//        }
        charMap = entities;
        try {
            updateCharMap(charMapFile);
        } catch(Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }


    private void updateCharMap(File charMapFile) throws Exception
    {
        readCharMap(charMapFile, false);  // set beforeCharMap and afterCharMap

        FileWriter fout = new FileWriter(charMapFile);
        PrintWriter out = new PrintWriter(fout);
        out.print(beforeCharMap);
        for (int i=0; i < charMap.length; i++) {
            CharEntity ent = charMap[i];
            if (i > 0) out.println(",");
            out.print("  ['");
            out.print(ent.getSymbolic());
            out.print("', '");
            out.print(ent.getNumeric());
            out.print("', " + ent.isSelectable() + ", '");
            out.print(ent.getDescription());
            out.print("']");
        }
        out.println();
        out.print(afterCharMap);
        out.close();
        fout.close();
    }


    private void readCharMap(File charMapFile, boolean load) throws Exception
    {
        FileReader fin = new FileReader(charMapFile);
        BufferedReader in = new BufferedReader(fin);

        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();

        List<DocmaCharEntity> charList = null;
        try {
            // find start position
            String line;
            while ((line = in.readLine()) != null) {
                before.append(line).append("\n");
                if (line.trim().equalsIgnoreCase("var charmap = [")) break;
            }
            if (line == null) {
                throw new DocRuntimeException("Start of character map not found.");
            }

            if (load) {
                charList = new ArrayList<DocmaCharEntity>(500);
            }

            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[")) {
                    if (charList != null) {
                        charList.add(readCharEntityLine(line));
                    }
                } else
                if (line.equalsIgnoreCase("];")) break;
            }
            if (line == null) {
                throw new DocRuntimeException("End of character map not found.");
            }

            after.append(line).append("\n");
            while ((line = in.readLine()) != null) {
                after.append(line).append("\n");
            }
        } finally {
            in.close();
            fin.close();
        }

        beforeCharMap = before.toString();
        afterCharMap = after.toString();

        if (charList != null) {  // load == true
            charMap = new DocmaCharEntity[charList.size()];
            charMap = (DocmaCharEntity[]) charList.toArray(charMap);
        }
    }


    private DocmaCharEntity readCharEntityLine(String line)
    {
        DocmaCharEntity ent = new DocmaCharEntity();

        int p1 = line.indexOf('\'') + 1;
        int p2 = line.indexOf('\'', p1);
        ent.setSymbolic(line.substring(p1, p2));

        int p3 = line.indexOf('\'', p2 + 1) + 1;
        int p4 = line.indexOf('\'', p3);
        ent.setNumeric(line.substring(p3, p4));

        int p5 = line.indexOf(',', p4 + 1) + 1;
        int p6 = line.indexOf(',', p5);
        ent.setSelectable(line.substring(p5, p6).trim().equalsIgnoreCase("true"));

        int p7 = line.indexOf('\'', p6 + 1) + 1;
        int p8 = line.indexOf('\'', p7);
        ent.setDescription(line.substring(p7, p8));

        return ent;
    }

}
