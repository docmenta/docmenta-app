/*
 * TemplateReader.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.app;

import java.io.*;
import java.util.*;

/**
 * A helper class used by {@link WebFormatter}.
 *
 * @author MP
 */
public class TemplateReader 
{
    private final File tmplFile;
    private final String charSet;
    private List fragments = null;

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) throws Exception
//    {
//        String tmpl_path = "C:\\sonst\\docma\\webhelp_v2.tmpl";
//        
//        long startTime = System.currentTimeMillis();
//        TemplateReader tr = new TemplateReader(new File(tmpl_path), "UTF-8");
//        
//        final Properties gen_props = new Properties();
//        gen_props.load(new FileInputStream(new File("C:\\sonst\\docma\\gentext_default_de.properties")));
//        GentextRetriever gen = new GentextRetriever() {
//
//            @Override
//            public String getGenText(String key) throws Exception
//            {
//                return gen_props.getProperty(key);
//            }
//        };
//    
//        Map<String, String> placeholders = new HashMap<String, String>();
//        placeholders.put("encoding", "UTF-8");
//        placeholders.put("custom_head_tags", "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=8\" />\n" +
//                         "<meta http-equiv=\"expires\" content=\"43200\" />");
//        placeholders.put("app_shortname", "MyApp");
//        placeholders.put("app_version", "1.0");
//        placeholders.put("common_url", "common");
//        placeholders.put("header_title1", "1. My First Chapter");
//        placeholders.put("header_title2", "1.1 My Sub-chapter");
//        
//        StringBuilder sb = new StringBuilder();
//        tr.writeTemplate(sb, placeholders, gen);
//        long now = System.currentTimeMillis();
//
//        System.out.println(sb.toString());
//        System.out.println();
//        
//        System.out.println("Time (millis): " + (now - startTime));
//    }

    public TemplateReader(File file, String encoding) throws IOException
    {
        this.tmplFile = file;
        this.charSet = (encoding == null) ? "UTF-8" : encoding;
        parseTemplate();
    }

    public void writeTemplate(Appendable out, 
                              Map<String, String> placeholderMap, 
                              GentextRetriever genRetriever) throws Exception
    {
        for (Object obj : fragments) {
            if (obj instanceof Placeholder) {
                Placeholder ph = (Placeholder) obj;
                out.append(ph.getValue(placeholderMap, genRetriever));
            } else {
                out.append(obj.toString());
            }
        }
    }
    
    private void parseTemplate() throws IOException
    {
        fragments = new ArrayList();
        String tmpl = readFileToString(tmplFile, charSet);
        
        int tmpl_len = tmpl.length();
        final String START_MARKER = "###";
        final String END_MARKER = "###";
        int copy_pos = 0;
        int pos = 0;
        while (pos < tmpl_len) {
            pos = tmpl.indexOf(START_MARKER, pos);
            if (pos < 0) {  // no more placeholders exist
                break;
            }
            // Found possible start of placeholder
            int inner_start = pos + START_MARKER.length();
            int inner_end = tmpl.indexOf(END_MARKER, inner_start);
            if (inner_end < 0) {  // no more placeholders exist
                break;
            }
            String expr = tmpl.substring(inner_start, inner_end);
            try {
                Placeholder ph = new Placeholder(expr);
                fragments.add(tmpl.substring(copy_pos, pos));
                fragments.add(ph);
                // continue search after placeholder
                pos = inner_end + END_MARKER.length();
                copy_pos = pos;
            } catch (Exception ex) {  // no valid placeholder at pos
                // continue search after pos
                pos++;
            }
        }
        if (copy_pos < tmpl_len) {
            fragments.add(tmpl.substring(copy_pos));
        }
    }
    
    private static String readStreamToString(InputStream in, String encoding) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(in, encoding);
        StringBuilder outbuf = new StringBuilder();
        char[] buf = new char[16 * 1024];
        int cnt;
        while ((cnt = reader.read(buf)) >= 0) {
            outbuf.append(buf, 0, cnt);
        }
        return outbuf.toString();
    }

    private static String readFileToString(File f, String encoding) throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        String s = readStreamToString(fin, encoding);
        try { fin.close(); } catch (Exception ex) {}
        return s;
    }

    static class Placeholder
    {
        private String alternative = null;
        private String key = null;
        private boolean lower = false;
        private boolean upper = false;
        private boolean gentext = false;
        private boolean noDoubleQuotes = false;

        public Placeholder(String expr) 
        {
            int p = expr.lastIndexOf("||");
            if (p >= 0) {
                alternative = expr.substring(p + 2);
                expr = expr.substring(0, p).trim();
            } else {
                expr = expr.trim();
            }
            final String F_GENTEXT = "gentext(";
            final String F_LOWER = "lower(";
            final String F_UPPER = "upper(";
            final String F_NO_DQUOTES = "noDoubleQuotes(";
            
            // Note: noDoubleQuotes() has to be the most outer function
            // Inside of noDoubleQuotes() the functions lower(), upper() and
            // gentext() can be used.
            if (expr.startsWith(F_NO_DQUOTES) && expr.endsWith(")")) {
                noDoubleQuotes = true;
                expr = expr.substring(F_NO_DQUOTES.length(), expr.length() - 1).trim();
            }
            
            // Functions lower() and upper(). The lower() and upper() functions
            // can only have one parameter, which has to be a placeholder name 
            // or the gentext() function.
            if (expr.startsWith(F_LOWER) && expr.endsWith(")")) {
                lower = true;
                expr = expr.substring(F_LOWER.length(), expr.length() - 1).trim();
            } else if (expr.startsWith(F_UPPER) && expr.endsWith(")")) {
                upper = true;
                expr = expr.substring(F_UPPER.length(), expr.length() - 1).trim();
            }
            
            // Note: If gentext() is used in combination with other functions, 
            // then it has to be the most inner function. The gentext function
            // can only have one parameter which has to be a string constant.
            if (expr.startsWith(F_GENTEXT) && expr.endsWith(")")) {
                gentext = true;
                expr = expr.substring(F_GENTEXT.length(), expr.length() - 1).trim();
            }
            
            key = expr;
        }

        public String getValue(Map<String, String> placeholderMap, 
                               GentextRetriever genRetriever) throws Exception
        {
            String res = gentext ? genRetriever.getGenText(key) : 
                                   placeholderMap.get(key);
            if (res == null) {
                res = "";
            } else {
                if (lower) {
                    res = res.toLowerCase();
                } else if (upper) {
                    res = res.toUpperCase();
                }
                
                if (noDoubleQuotes) {
                    res = res.replace('"', '\'');
                }
            }
            
            if (res.equals("") && (alternative != null)) {
                res = alternative;
            }
            return res;
        }
    }
}
