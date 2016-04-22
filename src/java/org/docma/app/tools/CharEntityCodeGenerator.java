/*
 * CharEntityCodeGenerator.java
 */
package org.docma.app.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.docma.app.DocmaCharEntity;
import org.docma.coreapi.DocRuntimeException;

/**
 *
 * @author MP
 */
public class CharEntityCodeGenerator 
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        File f = new File("C:\\Arbeit\\docmenta_work\\docmenta_ext_components\\tinymce\\tinymce_3_5_11_original\\jscripts\\tiny_mce\\themes\\advanced\\js\\charmap.js");
        if (! f.exists()) {
            System.out.println("File not found.");
            return;
        }
        DocmaCharEntity[] entities = readCharMap(f);
        System.out.println("Number of entities: " + entities.length);
        for (DocmaCharEntity ent : entities) {
            String desc = ent.getDescription();
            if (desc.contains("\"")) {
                System.out.println("Quote in description!");
            }
            System.out.println("new DocmaCharEntity(\"" + ent.getSymbolic() + 
                                               "\", \"" + ent.getNumeric() + 
                                               "\", " + ent.isSelectable() + 
                                               ", \"" + desc.replace('"', ' ') + "\"),");
        }
    }

    private static DocmaCharEntity[] readCharMap(File charMapFile) throws Exception
    {
        FileReader fin = new FileReader(charMapFile);
        BufferedReader in = new BufferedReader(fin);

        List charList = null;
        try {
            // find start position
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("var charmap = [")) break;
            }
            if (line == null) {
                throw new DocRuntimeException("Start of character map not found.");
            }

            charList = new ArrayList(500);

            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[")) {
                    charList.add(readCharEntityLine(line));
                } else
                if (line.equalsIgnoreCase("];")) break;
            }
            if (line == null) {
                throw new DocRuntimeException("End of character map not found.");
            }

        } finally {
            in.close();
            fin.close();
        }

        DocmaCharEntity[] charMap = new DocmaCharEntity[charList.size()];
        charMap = (DocmaCharEntity[]) charList.toArray(charMap);
        return charMap;
    }

    private static DocmaCharEntity readCharEntityLine(String line)
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
