/*
 * FormatterUtil.java
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

package org.docma.app;

import java.io.*;
import java.util.*;
import org.docma.coreapi.DocVersionId;
import org.docma.coreapi.ExportLog;
import org.docma.util.DocmaUtil;
import org.docma.util.ZipUtil;

/**
 *
 * @author MP
 */
public class FormatterUtil
{
    public static void writeCustomOutputFiles(File outDir, 
                                              DocmaOutputConfig outConf,
                                              DocmaSession docmaSess, 
                                              DocmaExportContext exportCtx)
    {
        String files_str = outConf.getHtmlCustomFiles();
        if ((files_str == null) || files_str.trim().equals("")) {
            return;
        }
        ExportLog expLog = exportCtx.getExportLog();
        String[] filter_applics = outConf.getEffectiveFilterApplics();
        String[] lines_arr = files_str.split("[\\n\\r]");  // split lines
        for (String line : lines_arr) {
            line = line.trim();
            String file_expression = line;
            File fileDir = outDir;
            if (line.startsWith("[")) {
                int p = line.indexOf(']');
                if (p < 0) {
                    logError(expLog, "Invalid custom output file configuration. Closing ] is missing: " + line);
                    continue;
                }
                String path =  line.substring(1, p).trim();
                file_expression = line.substring(p + 1).trim();
                fileDir = getCustomHTMLOutputDir(path, outDir, expLog);
                if (fileDir == null) {  // error has been written to log
                    continue;
                }
            }
            
            if (file_expression.equals("")) {
                // Nothing to do. For example, if just an output directory shall
                // be created (see getCustomHTMLOutputDir() above), but no file 
                // shall be written, then file_expression can be empty. 
                continue;
            }
            boolean ends_with_slash = file_expression.endsWith("/");
            String alias = null;
            DocmaNode nd;
            if (file_expression.startsWith("/")) {
                nd = DocmaAppUtil.getNodeByPath(docmaSess, file_expression);
            } else {
                // Get node by alias name
                alias = file_expression;
                if (ends_with_slash) {
                    alias = alias.substring(0, alias.length() - 1);
                }
                nd = docmaSess.getApplicableNodeByLinkAlias(alias, filter_applics);
            }
            if (nd == null) {
                logError(expLog, "Custom output file not found or not applicable: " + file_expression);
                continue;
            }
            boolean is_file = nd.isFileContent() || nd.isImageContent();
            if (is_file) {
                String ext = nd.getFileExtension();
                boolean is_zip = is_file && (ext != null) && ext.equalsIgnoreCase("zip");
                if (is_zip && ends_with_slash) {
                    extractZipNodeToFilesystem(nd, fileDir, expLog);
                } else {
                    String fn; 
                    if (alias == null) {  // file was located via path expression
                        fn = nd.getDefaultFileName();
                    } else { 
                        // If file was located via alias name, use alias name as filename.
                        // This allows using same filename for different node-variants.
                        fn = ((ext == null) || ext.equals("")) ? alias : alias + "." + ext;
                    }
                    File fout = new File(fileDir, fn);
                    writeNodeToFile(nd, fout, expLog);
                }
            } else {
                if (! nd.isFolder()) {
                    logError(expLog, "Invalid node type. Custom output file has to be a file or folder: " + file_expression);
                    continue;
                }
                ApplicEvaluator evaluator = null;
                if (filter_applics != null) {
                    evaluator = new ApplicEvaluator();
                    evaluator.setApplicability(filter_applics);
                }
                writeFolderToFilesystemRecursive(nd, fileDir, evaluator, expLog);
            }
        }
    }
    
    private static void writeNodeToFile(DocmaNode nd, File fout, ExportLog expLog)
    {
        InputStream in = null;
        try {
            in = nd.getContentStream();
            DocmaUtil.writeStreamToFile(in, fout);
        } catch (Exception ex) {
            ex.printStackTrace();
            logError(expLog, "Failed to write custom output file: " + fout.getAbsolutePath());
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ex2) { ex2.printStackTrace(); }
        }
    }
    
    private static void writeFolderToFilesystemRecursive(DocmaNode folderNode, 
                                                         File outDir, 
                                                         ApplicEvaluator evaluator,
                                                         ExportLog expLog)
    {
        int child_cnt = folderNode.getChildCount();
        for (int i = 0; i < child_cnt; i++) {
            DocmaNode nd = folderNode.getChild(i);
            if (evaluator != null) {
                String appl = nd.getApplicability();
                if ((appl != null) && !appl.trim().equals("")) {
                    try {
                        if (! evaluator.evaluate(appl)) {
                            continue;  // Node is not applicable -> skip
                        }
                    } catch (Exception ex) {
                        logWarning(expLog, "Failed to evaluate applicability expression '" + 
                                   appl + "' of node '" + nd.getDefaultFileName() + "': " + ex.getMessage());
                    }
                }
            }
            if (nd.isFolder() || nd.isSection()) {
                File subDir = new File(outDir, nd.getDefaultFileName());
                if (! subDir.mkdir()) {
                    logError(expLog, "Could not create custom output folder: " + subDir.getAbsolutePath());
                }
                writeFolderToFilesystemRecursive(nd, subDir, evaluator, expLog);  // recursive call
            } else {
                if (nd.isFileContent() || nd.isImageContent()) {
                    File fout = new File(outDir, nd.getDefaultFileName());
                    writeNodeToFile(nd, fout, expLog);
                } else {
                    logInfo(expLog, "Skipping node in custom output folder (invalid node type): " + nd.getDefaultFileName());
                }
            }
        }
    }
    
    private static void extractZipNodeToFilesystem(DocmaNode zip_node, File extractDir, ExportLog expLog) 
    {
        InputStream stream = zip_node.getContentStream();
        try {
            ZipUtil.extractZipStream(stream, extractDir);
        } catch (Exception ex) {
            ex.printStackTrace();
            logError(expLog, "Failed to extract custom zip file '" + zip_node.getDefaultFileName() + "': " + ex.getMessage());
        } finally {
            try { stream.close(); } catch (Exception ex2) { ex2.printStackTrace(); }
        }
    }
    
    private static File getCustomHTMLOutputDir(String path, File rootDir, ExportLog expLog)
    {
        if (path.equals("")) {
            return rootDir;
        } else {
            if (File.separatorChar != '/') {
                path = path.replace('/', File.separatorChar);
            }
            if (path.startsWith(File.separator)) {
                path = path.substring(1);
            }
            if (new File(path).isAbsolute()) {
                logError(expLog, "Invalid custom output file configuration. Absolute path is not allowed: " + path);
                return null;  // error -> skip this line
            }
            File customDir = new File(rootDir, path);
            if (! customDir.exists()) {
                if (! customDir.mkdirs()) {
                    logWarning(expLog, "Failed to create output directory: " + customDir.getAbsolutePath());
                }
            }
            return customDir;
        }
    }

    private static void logError(ExportLog expLog, String msg)
    {
        if (expLog != null) {
            expLog.errorMsg(msg);
        }
    }

    private static void logWarning(ExportLog expLog, String msg)
    {
        if (expLog != null) {
            expLog.warningMsg(msg);
        }
    }

    private static void logInfo(ExportLog expLog, String msg)
    {
        if (expLog != null) {
            expLog.infoMsg(msg);
        }
    }

    public static void createHTMLCoverPage(File outDir, String cover_img_url, ExportLog export_log)
    {
        File outfile = new File(outDir, "cover.html");
        if (outfile.exists()) {
            export_log.infoMsg("The file cover.html already exists. Skipping automatic cover page generation.");
            return;
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(outfile);
            BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(fout, "ISO-8859-1"));
            buf.write("<html><head>\n");
            buf.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">");
            buf.write("<title>Cover</title>\n");
            buf.write("<style type=\"text/css\"> img { max-width: 100%; }</style></head>\n");
            buf.write("<body><div id=\"cover-image\"><img src=\"");
            buf.write(cover_img_url);
            buf.write("\" alt=\"Cover\" ></div></body></html>");
            buf.close();
        } catch (Exception ex) {
            export_log.errorMsg("Creation of HTML cover page failed: " + ex.getMessage());
        } finally {
            try { if (fout != null) fout.close(); } catch (Exception ex2) {}
        }
    }

    public static void createHTMLRedirectPage(File out_file, String redirect_url, ExportLog export_log)
    {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(out_file);
            BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(fout, "ISO-8859-1"));
            buf.write("<html><head>\n");
            buf.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">");
            buf.write("<meta http-equiv=\"Refresh\" content=\"0; URL="); 
            buf.write(redirect_url);
            buf.write("\"/>");
            buf.write("<meta http-equiv=\"expires\" content=\"0\"/>");
            buf.write("<meta http-equiv=\"cache-control\" content=\"no-cache\"/>");
            buf.write("<meta http-equiv=\"pragma\" content=\"no-cache\"/>");
            buf.write("</head>\n");
            buf.write("<body>");
            buf.write("If not automatically redirected, click here: <a href=\"");
            buf.write(redirect_url);
            buf.write("\">");
            buf.write(redirect_url);
            buf.write("</a></body></html>");
            buf.close();
        } catch (Exception ex) {
            export_log.errorMsg("Creation of redirect page '" + out_file + "' failed: " + ex.getMessage());
        } finally {
            try { if (fout != null) fout.close(); } catch (Exception ex2) {}
        }
    }

    public static String formatPersonNameList(String[] names, DocmaExportContext export_ctx)
    {
        // Note: This implementation is equivalent to the person.name.list 
        // template in the DocBook stylesheet (see file common/common.xsl)
        
        if ((names == null) || (names.length == 0)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int count=1; count <= names.length; count++) {
            sb.append(names[count - 1]);
            String sep = null;
            if ((count == 1) && (names.length == 2)) {  // separator of two names
                sep = export_ctx.getGenTextProperty("authorgroup|sep2");
            } else 
            if ((names.length > 2) && (count + 1 == names.length)) {  // last separator
                sep = export_ctx.getGenTextProperty("authorgroup|seplast");
            } else
            if (count < names.length) {  // separator but not the last
                sep = export_ctx.getGenTextProperty("authorgroup|sep");
            } else {
                continue;  // no separator if last name of list has been added
            }
            if ((sep == null) || (sep.equals(""))) {
                sep = ", ";
            }
            sb.append(sep);
        }
        return sb.toString();
    }
    
    public static String formatTitlePageAuthors(String[] authors)
    {
        StringBuilder sb = new StringBuilder(); 
        for (int i=0; i < authors.length; i++) {
            sb.append("<span class=\"author\">").append(authors[i]).append("</span>");
            if (i < authors.length - 1) {
                sb.append("<br/>");
            }
        }
        return sb.toString();
    }
    
    public static String formatTitlePageCredits(String[] credits)
    {
        StringBuilder sb = new StringBuilder(); 
        for (int i=0; i < credits.length; i++) {
            sb.append(credits[i]);
            if (i < credits.length - 1) {
                sb.append("<br/>");
            }
        }
        return sb.toString();
    }
    
    public static String formatTitlePageAuthorList(String[] authors, DocmaExportContext export_ctx)
    {
        return formatPersonNameList(authors, export_ctx);
    }

    public static String replacePublicationConfigPlaceholders(DocmaSession docmaSess, String value)
    {
        if (value == null) {
            return null;
        }
        GregorianCalendar cal = null;
        int start_pos = 0;
        while (start_pos < value.length()) {
            int pos = value.indexOf('%', start_pos);
            if ((pos < 0) || (pos + 1 == value.length())) {
                break;
            }
            if ((pos > 0) && (value.charAt(pos - 1) == '\\')) {
                value = value.substring(0, pos - 1) + value.substring(pos);
                start_pos = pos;
                continue;
            }
            start_pos = pos + 1;
            
            char ch = value.charAt(pos + 1);
            String replace_val = null;
            if (ch == 'd') {
                cal = initCal(cal);
                replace_val = format2DigitNum(cal.get(Calendar.DAY_OF_MONTH));
            } else
            if (ch == 'H') {
                cal = initCal(cal);
                replace_val = format2DigitNum(cal.get(Calendar.HOUR_OF_DAY));
            } else
            if (ch == 'm') {
                cal = initCal(cal);
                replace_val = format2DigitNum(cal.get(Calendar.MONTH) + 1);
            } else
            if (ch == 'M') {
                cal = initCal(cal);
                replace_val = format2DigitNum(cal.get(Calendar.MINUTE));
            } else
            if (ch == 'S') {
                cal = initCal(cal);
                replace_val = format2DigitNum(cal.get(Calendar.SECOND));
            } else
            if (ch == 'Y') {
                cal = initCal(cal);
                replace_val = format2DigitNum(cal.get(Calendar.YEAR));
            } else
            if (ch == 'v') {
                DocVersionId vid = docmaSess.getVersionId();
                if (vid != null) {
                    replace_val = vid.toString();
                }
            }
            if (replace_val != null) {
                value = value.substring(0, pos) + replace_val + value.substring(pos + 2);
                start_pos = pos + replace_val.length();
            }
        }
        return value;
    }
    
    private static GregorianCalendar initCal(GregorianCalendar cal)
    {
        if (cal == null) {
            cal = new GregorianCalendar();
        }
        return cal;
    }
    
    private static String format2DigitNum(int num) 
    {
        String s = Integer.toString(num);
        if (s.length() == 1) {
            return "0" + s;
        } else {
            return s;
        }
    }

}
