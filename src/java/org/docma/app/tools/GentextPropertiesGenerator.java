/*
 * GentextPropertiesGenerator.java
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

package org.docma.app.tools;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 *
 * @author MP
 */
public class GentextPropertiesGenerator
{
    public static final String CONTEXT_SEPARATOR = "|";

    private static DocumentBuilderFactory domFactory = null;
    private static DocumentBuilder domBuilder = null;

    public static void main(String[] args) throws Exception
    {
        initXML();
        File xml_in_folder = new File("C:\\TEMP\\gentext_files");
        File out_folder = new File(xml_in_folder, "out");
        if (! out_folder.exists()) out_folder.mkdir();
        File[] xml_files = xml_in_folder.listFiles();
        for (int i=0; i < xml_files.length; i++) {
            File xml_file = xml_files[i];
            try {
                createPropertiesFromXML(xml_file, out_folder);
            } catch (Exception ex) {
                System.out.println("Error reading " + xml_file.getName() + ": " + ex.getMessage());
            }
        }
    }

    /* ----------------  Private methods  --------------------- */

    private static void createPropertiesFromXML(File xml_file, File out_folder) throws Exception
    {
        if (! xml_file.getName().endsWith(".xml")) return;

        Document doc = domBuilder.parse(xml_file);

        Element root = doc.getDocumentElement();
        if (! root.getTagName().equals("l:l10n")) return;

        String lang_code = root.getAttribute("language");
        String lang_name = root.getAttribute("english-language-name");

        String out_filename = "gentext_default_" + lang_code + ".properties";
        System.out.println("Generating properties file " + out_filename);

        Properties props = new Properties();

        NodeList nodelist = root.getElementsByTagName("l:gentext");
        for (int i=0; i < nodelist.getLength(); i++) {
            Element gen = (Element) nodelist.item(i);
            String gen_key = gen.getAttribute("key");
            String gen_text = gen.getAttribute("text");
            props.put(gen_key, gen_text);
        }

        nodelist = root.getElementsByTagName("l:context");
        for (int i=0; i < nodelist.getLength(); i++) {
            Element context = (Element) nodelist.item(i);
            String ctx_name = context.getAttribute("name");
            NodeList childlist = context.getElementsByTagName("l:template");
            for (int k=0; k < childlist.getLength(); k++) {
                Element templ = (Element) childlist.item(k);
                String templ_name = templ.getAttribute("name");
                String templ_text = templ.getAttribute("text");
                props.put(ctx_name + CONTEXT_SEPARATOR + templ_name, templ_text);
            }
        }
        
        // Add Docmenta specific properties
        props.put("xref" + CONTEXT_SEPARATOR + "label-title-separator", ": ");

        // Write properties file
        File out_file = new File(out_folder, out_filename);
        FileOutputStream fout = new FileOutputStream(out_file);
        props.store(fout, null);
        fout.close();

        // Replace properties file by commented out properties file
        reworkProperties(out_file, lang_code, lang_name);
    }

    private static void reworkProperties(File prop_file, String lang_code, String lang_name)
    throws Exception
    {
        System.out.print("Commenting out file " + prop_file.getName() + "...");
        StringWriter header = new StringWriter();
        writeHeader(header, lang_name);

        // Create temporary file for writing commented out lines
        File folder = prop_file.getParentFile();
        File temp_file = new File(folder, "temp.file");
        FileOutputStream fout = new FileOutputStream(temp_file);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fout, "ISO-8859-1"));

        // Write header comment to temporary file
        out.write(header.toString());

        // Read lines of properties file into sorted map. Skip leading comment lines.
        SortedMap in_lines = new TreeMap(new IgnoreCaseComparator());
        FileInputStream fin = new FileInputStream(prop_file);
        BufferedReader in = new BufferedReader(new InputStreamReader(fin, "ISO-8859-1"));
        String line;
        boolean skip_comment = true;
        while ((line = in.readLine()) != null) {
            if (line.equals("")) continue;
            if (skip_comment && line.startsWith("#")) continue;

            skip_comment = false;
            int p = line.indexOf('=');
            if (p < 0) {
                System.out.print("Warning: property line does not contain = character.");
                continue;
            }
            String key = line.substring(0, p);
            in_lines.put(key, line);
        }
        in.close();
        fin.close();

        // Write commented out properties to temporary file:

        // Write general gentext elements
        Iterator it = in_lines.keySet().iterator();
        while(it.hasNext()) {
            String key = (String) it.next();
            if (! key.contains(CONTEXT_SEPARATOR)) {
                line = (String) in_lines.get(key);
                // out.append("# ");
                out.append(line).append("\n");
            }
        }

        addDefaultDocmaProperties(out, lang_code);

        // Write context gentext elements
        it = in_lines.keySet().iterator();
        String prev_context = "";
        while(it.hasNext()) {
            String key = (String) it.next();
            int p = key.indexOf(CONTEXT_SEPARATOR);
            if (p >= 0) {
                String context = key.substring(0, p);
                if (! context.equals(prev_context)) {
                    String hl = "#### Elements used in context of " + context + " ####";
                    out.write("\n");
                    for (int i=0; i < hl.length(); i++) out.write('#');
                    out.append("\n").append(hl).append("\n");
                    for (int i=0; i < hl.length(); i++) out.write('#');
                    out.write("\n\n");
                }
                line = (String) in_lines.get(key);
                out.append("# ").append(line).append("\n");
                prev_context = context;
            }
        }

        out.close();
        fout.close();

        // Replace properties file by temporary file
        prop_file.delete();
        temp_file.renameTo(prop_file);
        System.out.println("finished.");
    }

    private static void addDefaultDocmaProperties(BufferedWriter out, String lang_code)
    throws Exception
    {
        out.append("\n");
        out.append("####################################\n");
        out.append("#### Elements used for Web Help ####\n");
        out.append("####################################\n");
        out.append("\n");
        if (lang_code.equals("de")) {
            out.append("WebHelpContents=Inhalt\n");
            out.append("Search=Suche\n");
            out.append("Enter_a_term_and_click=Wort eingeben und\\ \n");
            out.append("Go=Los\n");
            out.append("to_perform_a_search=\\ klicken um Suche zu starten.\n");
            out.append("txt_filesfound=Ergebnis\n");
            out.append("txt_enter_at_least_1_char=Sie müssen mindestens ein Zeichen eingeben.\n");
            out.append("txt_browser_not_supported=Ihr Browser wird nicht unterstützt. Die Benutzung von Mozilla Firefox wird empfohlen.\n");
            out.append("txt_please_wait=Bitte warten. Suche läuft...\n");
            out.append("txt_results_for=Ergebnis für:\\ \n");
            out.append("HighlightButton=Hervorhebung von Suchtreffern ein-/ausschalten\n");
        } else {
            out.append("WebHelpContents=Contents\n");
            out.append("Search=Search\n");
            out.append("Enter_a_term_and_click=Enter a term and click \n");
            out.append("Go=Go\n");
            out.append("to_perform_a_search=\\ to perform a search.\n");
            out.append("txt_filesfound=Results\n");
            out.append("txt_enter_at_least_1_char=You must enter at least one character.\n");
            out.append("txt_browser_not_supported=Your browser is not supported. Use of Mozilla Firefox is recommended.\n");
            out.append("txt_please_wait=Please wait. Search in progress...\n");
            out.append("txt_results_for=Results for:\\ \n");
            out.append("HighlightButton=Toggle search result highlighting\n");
        }
        out.append("\n");
        out.append("###################################################\n");
        out.append("#### Elements for referenced publication links ####\n");
        out.append("###################################################\n");
        out.append("\n");
        out.append("# external-pub-ref='%t' [%r]\n");
    }

    private static void writeHeader(Writer out, String langname) throws Exception
    {
        final int BOX_WIDTH = 80;

        String[] lines = new String[] {
          "Default gentext properties for '" + langname + "' language.",
          "Uncomment properties if you want to overwrite the system default.",
          "To uncomment a line remove the # character at the start of the line.",
          "This file has to be encoded in ISO-8859-1 character encoding.",
          "Characters that cannot be directly represented in this encoding can",
          "be written using Unicode escapes in the format \\uXXXX for the",
          "appropriate hexadecimal value XXXX. The ASCII characters \\, tab,",
          "form feed, newline and carriage return are written as \\\\, \\t, \\f,",
          "\\n and \\r, respectively. The characters #, !, =, and : should be",
          "written with a preceding backslash to ensure proper loading."
        };

        for (int i=0; i < BOX_WIDTH; i++) out.write('#');
        out.write("\n");
        for (int i=0; i < lines.length; i++) {
            String line = lines[i];
            int space_cnt = BOX_WIDTH - (line.length() + 6);
            out.append("## ").append(line);
            for (int k=0; k < space_cnt; k++) out.write(' ');
            out.write(" ##\n");
        }
        for (int i=0; i < BOX_WIDTH; i++) out.write('#');
        out.write("\n\n");
    }

    private static void initXML() throws Exception
    {
        if (domFactory == null) {
            domFactory = DocumentBuilderFactory.newInstance();
            domBuilder = domFactory.newDocumentBuilder();
        }
    }

    static class IgnoreCaseComparator implements Comparator
    {

        public int compare(Object arg0, Object arg1)
        {
            String s0 = (String) arg0;
            String s1 = (String) arg1;
            int c = s0.compareToIgnoreCase(s1);
            if ((c == 0) && !s0.equals(s1)) {
                c = s0.compareTo(s1);
            }
            return c;
        }

    }
}
