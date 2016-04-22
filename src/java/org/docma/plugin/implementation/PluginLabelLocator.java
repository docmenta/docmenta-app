/*
 * PluginLabelLocator.java
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
package org.docma.plugin.implementation;

import java.io.*;
import java.util.Locale;
import org.zkoss.util.resource.LabelLocator2;

import org.docma.util.Log;
import org.docma.util.DocmaUtil;

/**
 *
 * @author MP
 */
public class PluginLabelLocator implements LabelLocator2
{
    private File pluginDir;
    private String filename_pattern;
    
    public PluginLabelLocator(File pluginDir, String filename_pattern)
    {
        this.pluginDir = pluginDir;
        this.filename_pattern = filename_pattern;
    }

    public InputStream locate(Locale locale) 
    {
        String loc_str = (locale == null) ? "" : ("_" + locale.toString());
        String fn = filename_pattern.replace("?", loc_str);
        File locale_file = new File(pluginDir, fn);
        if (locale_file.exists()) {
            // Load file into memory to be able to immediately close the file
            int buf_size = (int) locale_file.length() + 100;
            ByteArrayOutputStream bufout = new ByteArrayOutputStream(buf_size);
            try {
                FileInputStream fin = new FileInputStream(locale_file);
                try {
                    DocmaUtil.copyStream(fin, bufout);
                } finally {
                    try { fin.close(); } catch (Exception cex) {}
                }
                return new ByteArrayInputStream(bufout.toByteArray());
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.error("Could not load labels of plugin: " + locale_file + ". " + ex.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String getCharset() 
    {
        return "UTF-8";
    }
    
}
