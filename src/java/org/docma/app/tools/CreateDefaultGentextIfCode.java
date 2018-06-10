/*
 * CreateDefaultGentextIfCode.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.app.tools;

import java.io.*;

/**
 *
 * @author MP
 */
public class CreateDefaultGentextIfCode 
{
    public static void main(String[] args) throws Exception
    {
        File in_folder = new File("C:\\TEMP\\gentext_files\\out");
        File fig_file = new File("C:\\TEMP\\default_figure_titles.txt");
        File table_file = new File("C:\\TEMP\\default_table_titles.txt");
        
        FileWriter fw_fig = new FileWriter(fig_file);
        FileWriter fw_table = new FileWriter(table_file);
        for (File fin : in_folder.listFiles()) {
            String fn = fin.getName();
            final String PREFIX = "gentext_default_";
            final String EXT = ".properties";
            if (! (fn.startsWith(PREFIX) && fn.endsWith(EXT))) {
                continue;
            }
            String langCode = fn.substring(PREFIX.length(), fn.length() - EXT.length());
            
            System.out.println("Reading " + fn);
            BufferedReader in = new BufferedReader(new FileReader(fin));
            String line;
            while ((line = in.readLine()) != null) {
                final String FIG_START = "# title|figure=";
                final String TABLE_START = "# title|table=";
                Writer out = null;
                String val = null;
                if (line.startsWith(FIG_START)) {
                    out = fw_fig;
                    val = line.substring(FIG_START.length());
                } else if (line.startsWith(TABLE_START)) {
                    out = fw_table;
                    val = line.substring(TABLE_START.length());
                }
                if (val != null) {
                    out.append("else if (lang.equals(\"").append(langCode)
                       .append("\")) pattern = \"").append(val).append("\";\n");
                }
            }
            in.close();
        }
        fw_fig.close();
        fw_table.close();
        System.out.println("Finished.");
    }
}
