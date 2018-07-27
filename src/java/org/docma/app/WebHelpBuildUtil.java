/*
 * WebHelpBuildUtil.java
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
import java.util.Properties;
import org.docma.coreapi.ExportLog;
import org.docma.plugin.CharEntity;
import org.docma.util.DocmaUtil;
import org.docma.util.ZipUtil;

/**
 *
 * @author MP
 */
public class WebHelpBuildUtil
{

    public static void buildWebHelp2(File inputDir,
                                     File outDir,
                                     File docbookXSLDir,
                                     DocmaOutputConfig out_config,
                                     ExportLog export_log)
    {
        writeEncodingProps(outDir, out_config.getHtmlOutputEncoding(), 
                           out_config.getCharEntities(), export_log);
        build(null, inputDir, outDir, docbookXSLDir, out_config, export_log);
    }
    
    public static void build(File docbookfile,
                             File inputDir,
                             File outDir,
                             File docbookXSLDir,
                             DocmaOutputConfig out_config,
                             ExportLog export_log)
    {
        boolean is_webhelp_new = (docbookfile == null);
        
        String javahome = System.getProperty("java.home");
        if (DocmaConstants.DEBUG) System.out.println("Java Home: " + javahome);
        export_log.infoMsg("Using Java Home: " + javahome);

        File javabin = new File(javahome, "bin");
        String javacommand = new File(javabin, "java").getPath();

        File webRootDir = docbookXSLDir.getParentFile();
        File webhelpDir = new File(docbookXSLDir, "webhelp");
        File antHomeDir = new File(webhelpDir, "ant_home");
        File antLauncher = new File(antHomeDir, "lib" + File.separator + "ant-launcher.jar");
        if (! antLauncher.exists()) {
            export_log.warningMsg("Could not find ant-launcher: " + antLauncher.getAbsolutePath());
        }
        File xercesLib = new File(webhelpDir, "xerces" + File.separator + "xercesImpl.jar");
        if (! xercesLib.exists()) {
            export_log.warningMsg("Could not find xerces library: " + xercesLib.getAbsolutePath());
        }
        File saxonLib = null; 
        if (! is_webhelp_new) {
            saxonLib = new File(webhelpDir, "saxon" + File.separator + "saxon.jar");
            if (! saxonLib.exists()) {
                export_log.warningMsg("Could not find saxon library: " + saxonLib.getAbsolutePath());
            }
        }
        File webhelpBuildFile = new File(webhelpDir, is_webhelp_new ? "build_v2.xml" : "build.xml");
        if (! webhelpBuildFile.exists()) {
            export_log.warningMsg("Could not find file: " + webhelpBuildFile.getAbsolutePath());
        }

        try {
            writeCustomBuildFile(webRootDir, webhelpBuildFile, docbookfile, inputDir, outDir, saxonLib, out_config.getLanguageCode());
        } catch(Exception ex) {
            ex.printStackTrace();
            export_log.errorMsg("Could not write custom build file: " + ex.getMessage());
            return;
        }

        String[] cmd = new String[] {
            javacommand,
            "-Dant.home=" + antHomeDir.getAbsolutePath(),
            "-cp",
            antLauncher.getAbsolutePath() + File.pathSeparator + xercesLib.getAbsolutePath(),
            "org.apache.tools.ant.launch.Launcher",
            "webhelp"
        };

        Runtime rt = Runtime.getRuntime();
        try {
            Process proc = rt.exec(cmd, new String[] {}, inputDir);  // set inputDir as working directory
            InputStream in_std = proc.getInputStream();
            InputStream in_err = proc.getErrorStream();
            OutputStream out = proc.getOutputStream();
            out.close();
            String msg_std = readStringFromInputStream(in_std).trim();
            String msg_err = readStringFromInputStream(in_err).trim();
            in_std.close();
            in_err.close();
            int exitcode = proc.waitFor();
            if (msg_std.length() > 0) export_log.infoMsg(msg_std);
            if (msg_err.length() > 0) export_log.errorMsg(msg_err);
            if (exitcode != 0) {
                export_log.warningMsg("Web Help formatting task finished with exit code: " + exitcode);
            } else {
                export_log.infoMsg("Web Help formatting task finished successfully.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            export_log.errorMsg(ex.getMessage());
        }
    }


    public static void writeTemplateFiles(DocmaNode conf_folder,
                                          DocmaOutputConfig out_conf,
                                          File outDir,
                                          ExportLog export_log)
    {
        File zip_extract_dir = null;
        try {
            String webconf_foldername = out_conf.getHtmlWebhelpConfigFolder();
            if ((webconf_foldername == null) || webconf_foldername.trim().equals("")) {
                webconf_foldername = "webhelp";
            }
            DocmaNode web_folder = conf_folder.getChildByFilename(webconf_foldername);
            if (web_folder == null) {
                export_log.warningMsg("Could not find Web Help configuration folder '" +
                                      webconf_foldername + "'. Using defaults.");
                return;
            }
            
            boolean is_zip_conf = false;
            File[] extract_files = null;
            if (web_folder.isFileContent()) {
                String fext = web_folder.getFileExtension();
                if ((fext != null) && fext.equalsIgnoreCase("zip")) {
                    zip_extract_dir = extractZipConf(web_folder, outDir);
                    if (zip_extract_dir == null) {
                        export_log.warningMsg("Could not extract Web Help configuration '" +
                                              webconf_foldername + "'. Using defaults.");
                        return;
                    }
                    extract_files = zip_extract_dir.listFiles();
                    is_zip_conf = true;
                }
            }
            
            int cnt = is_zip_conf ? extract_files.length : web_folder.getChildCount();
            for (int i=0; i < cnt; i++) {
                DocmaNode nd = null;
                File file = null;
                String fName;
                final String s = File.separator;
                
                if (is_zip_conf) {
                    file = extract_files[i];
                    if (file.isDirectory()) continue;
                    fName = file.getName();
                } else {
                    nd = web_folder.getChild(i);
                    if (! nd.isContent()) continue;
                    fName = nd.getDefaultFileName();
                }

                String fn = fName.toLowerCase();
                boolean is_gif = fn.endsWith(".gif");
                boolean is_png = fn.endsWith(".png");
                String path = null;
                if (fn.startsWith("favicon") && fn.endsWith(".ico")) {
                    path = "favicon.ico";
                } else
                if (is_gif && fn.startsWith("file")) {
                    path = "common" + s + "jquery" + s +"treeview" + s + "images" + s + "file.gif";
                } else
                if (is_gif && fn.startsWith("treeview-icons")) {
                    path = "common" + s + "jquery" + s +"treeview" + s + "images" + s + "treeview-default.gif";
                } else
                if (is_gif && fn.startsWith("treeview-line")) {
                    path = "common" + s + "jquery" + s +"treeview" + s + "images" + s + "treeview-default-line.gif";
                } else
                // if (is_gif && fn.startsWith("header-bg")) {
                //     path = "common" + s + "images" + s + "header-bg.gif";
                // } else
                // if (is_png && fn.startsWith("showHideTreeIcons")) {
                //     path = "common" + s + "images" + s + "showHideTreeIcons.png";
                // } else
                if (is_gif && fn.startsWith("highlight")) {
                    path = "common" + s + "images" + s + "highlight-blue.gif";
                } else
                if (is_png && fn.startsWith("logo1")) {
                    path = "common" + s + "images" + s + "logo1.png";
                } else
                if (is_png && fn.startsWith("logo2")) {
                    path = "common" + s + "images" + s + "logo2.png";
                } else
                if (is_png && fn.startsWith("prev")) {
                    path = "common" + s + "images" + s + "prev.png";
                } else
                if (is_png && fn.startsWith("next")) {
                    path = "common" + s + "images" + s + "next.png";
                } else
                if (is_png && fn.startsWith("up")) {
                    path = "common" + s + "images" + s + "up.png";
                } else
                if (is_png && fn.startsWith("home")) {
                    path = "common" + s + "images" + s + "home.png";
                } else
                if (is_png && fn.startsWith("toc")) {
                    path = "common" + s + "images" + s + "toc.png";
                } else
                if (is_png && fn.startsWith("tab-bg")) {
                    path = "common" + s + "jquery" + s +"theme-redmond" + s +
                           "images" + s + "ui-bg_gloss-wave_55_5c9ccc_500x100.png";
                } else
                if (fn.startsWith("positioning") && fn.endsWith(".css")) {
                    path = "common" + s + "css" + s + "positioning.css";
                } else 
                if (fn.startsWith("webhelp_config") && fn.endsWith(".js")) {
                    String mainjs_path = "common" + s + "main.js";
                    String js_code = is_zip_conf ? DocmaUtil.readFileToString(file) 
                                                 : nd.getContentString();
                    updateMainJS(js_code, new File(outDir, mainjs_path), export_log);
                    continue;
                } else {
                    String f_ext;
                    if (is_zip_conf) {
                        int pos = fName.lastIndexOf('.');
                        f_ext = (pos < 0) ? null : fName.substring(pos + 1);
                    } else {
                        f_ext = nd.getFileExtension();
                    }
                    if ((f_ext != null) && ImageUtil.isSupportedImageExtension(f_ext)) {
                        path = "common" + s + "images" + s + fName;
                    }
                }

                // Write node content to the file at outDir/path
                if (path != null) {
                    File destPath = new File(outDir, path);
                    if (is_zip_conf) {
                        moveFile(file, destPath, export_log);
                    } else {
                        writeNodeContentToFile(nd, destPath, export_log);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            export_log.errorMsg("Error writing Web Help template files: " + ex.getMessage());
        } finally {
            if (zip_extract_dir != null) {
                if (! DocmaUtil.recursiveFileDelete(zip_extract_dir)) {
                    export_log.warningMsg("Could not remove temporary directory: " + zip_extract_dir.getAbsolutePath());
                }
            }
        }
    }

    private static void updateMainJS(String js_code, File mainjs_file, ExportLog export_log) 
    {
        if (js_code == null) return;
        if (! mainjs_file.exists()) {
            export_log.errorMsg("File main.js not found!");
            return;
        }
        try {
            InputStream fin = new FileInputStream(mainjs_file);
            String cont = DocmaUtil.readStreamToString(fin, "UTF-8");
            fin.close();
            
            final String SEARCH_PATTERN = "/*__USER_WEBHELP_JS__*/";
            int pos = cont.indexOf(SEARCH_PATTERN);
            if (pos >= 0) {
                cont = cont.substring(0, pos) + js_code + cont.substring(pos + SEARCH_PATTERN.length());
                DocmaUtil.writeStringToFile(cont, mainjs_file, "UTF-8");
            } else {
                export_log.warningMsg("Could not find replace pattern in file main.js.");
            }
        } catch (Exception ex) {
            export_log.errorMsg("Could not update file main.js: " + ex.getMessage());
        }
    }

    private static void writeNodeContentToFile(DocmaNode nd, File fout, ExportLog export_log)
    {
        InputStream in = null;
        try {
            in = nd.getContentStream();
            DocmaUtil.writeStreamToFile(in, fout);
        } catch (Exception ex) {
            ex.printStackTrace();
            export_log.errorMsg("Error writing file " + fout.getAbsolutePath());
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ex2) {}
        }
    }
    
    private static void moveFile(File source, File dest, ExportLog export_log)
    {
        if (dest.exists()) {
            dest.delete();
        }
        boolean okay = source.renameTo(dest);
        if (! okay) {
            // if moving the file did not work, try copy
            okay = DocmaUtil.fileCopy(source, dest, true);
        }
        if (! okay) {
            export_log.errorMsg("Could not write configuration file " + source.getName() + 
                                " to location " + dest.getAbsolutePath());
        }
    }

    private static void writeCustomBuildFile(File webRootDir,
                                             File webhelpBuildFile,
                                             File docbookfile,
                                             File inputDir,
                                             File outDir,
                                             File saxonLib,
                                             String languageCode)
                                             throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<project>");
        if (docbookfile != null) {  // WebHelp v1
            writeProperty(sb, "input-xml", docbookfile.getAbsolutePath());
            writeProperty(sb, "stylesheet-path", "custom_layer_webhelp.xsl");
            writeProperty(sb, "xslt-processor-classpath", saxonLib.getAbsolutePath());
        }
        writeProperty(sb, "web-root-dir", webRootDir.getAbsolutePath());
        writeProperty(sb, "output-dir", outDir.getAbsolutePath());
        writeProperty(sb, "validate-against-dtd", "false");
        writeProperty(sb, "webhelp.indexer.language", languageCode.toLowerCase());
        sb.append("<import file=\"").append(webhelpBuildFile.getAbsolutePath()).append("\"/>");
        sb.append("</project>");
        File customBuildFile = new File(inputDir, "build.xml");
        DocmaUtil.writeStringToFile(sb.toString(), customBuildFile, "UTF-8");
    }

    private static void writeProperty(StringBuilder sb, String name, String value)
    {
        sb.append("<property name=\"").append(name).append("\" value=\"").append(value).append("\"/>");
    }

    private static String readStringFromInputStream(InputStream in) throws Exception
    {
        BufferedReader rd = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
    
    private static File extractZipConf(DocmaNode zip_node, File outDir) 
    {
        File extractDir = new File(outDir, "common" + File.separator + "extract" + System.currentTimeMillis());
        extractDir.mkdirs();
        if (! extractDir.exists()) return null;  // check existence
        
        InputStream stream = zip_node.getContentStream();
        try {
            ZipUtil.extractZipStream(stream, extractDir);
            return extractDir;
        } catch (Exception ex) {
            DocmaUtil.recursiveFileDelete(extractDir);
            return null;
        } finally {
            try { stream.close(); } catch (Exception ex2) { ex2.printStackTrace(); }
        }
    }

    private static void writeEncodingProps(File outDir, 
                                           String encoding, 
                                           CharEntity[] entities, 
                                           ExportLog export_log)
    {
        File propFile = new File(outDir, "docma_encoding.props");
        Properties props = new Properties();
        if ((encoding == null) || encoding.equals("")) {
            encoding = "UTF-8";
        }
        props.setProperty("file_encoding", encoding);
        if (entities != null) {
            for (CharEntity ent : entities) {
                String sym = ent.getSymbolic();
                int num = ent.getNumericValue();
                if ((sym != null) && (num > 0)) {
                    int startpos = sym.startsWith("&") ? 1 : 0;
                    int endpos = sym.endsWith(";") ? sym.length() - 1 : sym.length();
                    sym = sym.substring(startpos, endpos);
                    props.setProperty("symbol." + sym, Integer.toString(num));
                }
            }
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(propFile);
            props.store(fout, "Encoding Properties");
        } catch (Exception ex) {
            String msg = "Failed to write encoding file:" + propFile;
            if (export_log != null) {
                export_log.errorMsg(msg);
            } else {
                System.out.println(msg);
            }
        } finally {
            if (fout != null) try {  fout.close(); } catch (Exception ex2) {};
        }
    }
}
