/*
 * MediaServlet.java
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

package org.docma.webapp;

import org.docma.app.*;
import org.docma.coreapi.*;
import org.docma.plugin.ckeditor.CKEditUtils;
import org.docma.util.*;

import java.util.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author MP
 */
public class MediaServlet extends HttpServlet
{

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException 
    {
        String extrapath = request.getPathInfo();
        int startpos = (extrapath.charAt(0) == '/') ? 1 : 0;
        int endpos = extrapath.indexOf('/', startpos);
        String sessId = extrapath.substring(startpos, endpos);

        ServletContext context = getServletConfig().getServletContext();
        DocmaWebApplication docmaApp = GUIUtil.getDocmaWebApplication(context);
        DocmaWebSession docmaWebSess = docmaApp.getWebSessionContext(sessId);
        DocmaSession docmaSess = docmaWebSess.getDocmaSession();  // docmaApp.getOpenSession(sessId);

        startpos = endpos + 1;
        endpos = extrapath.indexOf('/', startpos);
        String storeId = extrapath.substring(startpos, endpos);

        startpos = endpos + 1;
        endpos = extrapath.indexOf('/', startpos);
        String ver_str = extrapath.substring(startpos, endpos);
        DocVersionId verId;
        try {
            verId = docmaSess.createVersionId(ver_str);
        } catch (DocException dex) {
            throw new ServletException("Invalid Version Id: " + ver_str);
        }

        startpos = endpos + 1;
        endpos = extrapath.indexOf('/', startpos);
        String langCode = extrapath.substring(startpos, endpos);
        String origLangCode = docmaSess.getOriginalLanguage().getCode();

        boolean same_sess = storeId.equals(docmaSess.getStoreId()) &&
                            verId.equals(docmaSess.getVersionId()) &&
                            langCode.equalsIgnoreCase(docmaSess.getLanguageCode());
        if (! same_sess) {
            // Create temporary session to serve content
            docmaSess = docmaSess.createNewSession();
        }


        try {
            if (! same_sess) {
                // open store of temporary session
                docmaSess.openDocStore(storeId, verId);
                if (! langCode.equalsIgnoreCase(origLangCode)) {
                    docmaSess.enterTranslationMode(langCode);
                }
            }
            startpos = endpos + 1;
            endpos = extrapath.indexOf('/', startpos);
            if (endpos < 0) return;  // no valid request
            String mediaType = extrapath.substring(startpos, endpos);
            if (mediaType.equals("image")) {
                String imageAlias = extrapath.substring(endpos + 1);
                
                String[] applics = null;  // unfiltered
                MainWindow mainWin = docmaWebSess.getMainWindow();
                if (mainWin != null) {
                    DocmaPublicationConfig pub_conf = mainWin.getPreviewPubConfig();
                    DocmaOutputConfig out_conf = mainWin.getPreviewHTMLConfig();  // or getPreviewOutputConfig()
                    applics = DocmaAppUtil.getFilterApplics(pub_conf, out_conf);
                }

                serveImage(response, docmaSess, imageAlias, applics);
            } else
            if (mediaType.equals("imagelist")) {
                serveImageList(response, docmaSess, extrapath.substring(endpos + 1));
            } else
            if (mediaType.equals("linklist")) {
                serveLinkList(response, docmaSess, extrapath.substring(endpos + 1));
            } else
            if (mediaType.equals("videolist")) {
                serveVideoList(response, docmaSess, extrapath.substring(endpos + 1));
            } else
            if (mediaType.equals("filelist")) {
                serveFileList(response, docmaSess, extrapath.substring(endpos + 1));
            } else
            if (mediaType.equals("stylelist")) {
                serveStyleList(response, docmaSess, extrapath.substring(endpos + 1));
            } else
            if (mediaType.equals("css")) {
                MainWindow mainWin = docmaWebSess.getMainWindow();
                DocmaOutputConfig out_conf = mainWin.getPreviewHTMLConfig();
                out_conf.setShowIndexTerms(mainWin.isShowIndexTermsChecked());
                serveContentCSS(request, response, docmaSess, out_conf);
            } else
            if (mediaType.equals("file")) {
                int p1 = endpos + 1;
                int p2 = extrapath.indexOf('/', p1);
                if (p2 < 0) p2 = extrapath.length();
                String node_alias = extrapath.substring(p1, p2);
                String node_id = docmaSess.getNodeIdByAlias(node_alias);
                if (node_id != null) {
                    serveFile(context, response, docmaSess, node_id, false);
                }
            } else
            if (mediaType.equals("download")) {
                int p1 = endpos + 1;
                int p2 = extrapath.indexOf('/', p1);
                if (p2 < 0) p2 = extrapath.length();
                String nodeid = extrapath.substring(p1, p2);
                serveFile(context, response, docmaSess, nodeid, true);
            } else
            if (mediaType.equals("template")) {
                String template_id = extrapath.substring(endpos + 1);
                serveTemplate(response, docmaSess, template_id);
            } else
            if (mediaType.equals("templatelist")) {
                serveTemplateList(response, docmaSess);
            } else
            if (mediaType.equals("publicationlog")) {
                startpos = endpos + 1;
                endpos = extrapath.indexOf('/', startpos);
                if (endpos < 0) {
                    endpos = extrapath.length();
                }
                String publication_id = extrapath.substring(startpos, endpos);
                servePublicationLog(response, docmaSess, publication_id);
            } else {
                throw new ServletException("Unknown media type: " + mediaType);
            }
        } finally {
            // Close session if it is a temporary session
            if (! same_sess) {
                docmaSess.closeSession();
            }
        }
    }

    private void serveFile(ServletContext context,
                           HttpServletResponse response,
                           DocmaSession docmaSess, 
                           String nodeid,
                           boolean download)
    throws IOException
    {
        DocmaNode node = docmaSess.getNodeById(nodeid);

        String cont_type = download ? "application/octet-stream" : node.getContentType();
        if ((cont_type == null) || (cont_type.length() == 0)) {
            // Try to determine content type from file extension as configured by the servlet container.
            String ext = node.getFileExtension();
            if ((ext != null) && (ext.length() > 0)) {
                cont_type = context.getMimeType("filename." + ext);
            }
        }
        if ((cont_type == null) || (cont_type.length() == 0)) {
            cont_type = "application/octet-stream";  // set default mime type
        }
        response.setContentType(cont_type);
        int len = (int) node.getContentLength();
        if (len >= 0) response.setContentLength(len);

        // disableBrowserCache(response);

        OutputStream streamout = response.getOutputStream();
        InputStream streamin = node.getContentStream();
        try {
            DocmaUtil.copyStream(streamin, streamout);
        } finally {
            streamin.close();
        }
        // streamout.write(node.getContent());
    }

    private void serveImage(HttpServletResponse response,
                            DocmaSession docmaSess, 
                            String imageAlias, 
                            String[] applics)
    throws IOException
    {
        // org.docma.util.Log.info("imageAlias: '" + imageAlias + "'");

        // DocmaNode node = docmaSess.getNodeByAlias(imageAlias);
        DocmaNode node = docmaSess.getApplicableNodeByLinkAlias(imageAlias, applics);
        if (node == null) {
            if (DocmaConstants.DEBUG) {
                org.docma.util.Log.info("MediaServlet.serveImage(): Could not find image '" + imageAlias + "'.");
            }
            return;
        }
        if (! node.isContent()) {
            org.docma.util.Log.info("MediaServlet.serveImage(): Node with alias '" + imageAlias + "' is no content node.");
            return;
        }

        response.setContentType(node.getContentType());
        int len = (int) node.getContentLength();
        if (len >= 0) response.setContentLength(len);

        disableBrowserCache(response);

        OutputStream streamout = response.getOutputStream();
        try {
            streamout.write(node.getContent());
        } finally {
            streamout.close();
        }
    }


//    private void serveImageList(HttpServletResponse response,
//                                DocmaSession docmaSess)
//    throws IOException
//    {
//        response.setContentType("text/javascript");
//        // response.setCharacterEncoding("UTF-8");
//        disableBrowserCache(response);
//
//        // List resultList = new ArrayList(1000);
//        // DocmaAppUtil.getImageNodesRecursive(docmaSess.getDocumentRoot(), resultList);
//        // DocmaAppUtil.getImageNodesRecursive(docmaSess.getMediaRoot(), resultList);
//        // DocmaAppUtil.getImageNodesRecursive(docmaSess.getSystemRoot(), resultList);
//        // Collections.sort(resultList, new DocmaAppUtil.DocmaNodeAliasComparator());
//        String[] img_aliases = docmaSess.getImageAliases();
//        if (img_aliases == null) { img_aliases = new String[0]; }
//
//        final String TAB = "..............."; // "            ";
//        PrintWriter out = response.getWriter();
//        out.println("var tinyMCEImageList = new Array(");
//        boolean notfirst = false;
//        for (int i=0; i < img_aliases.length; i++) {    // resultList.size()
//            String alias = img_aliases[i];
//            DocmaNode img_node = docmaSess.getNodeByAlias(alias);  // (DocmaNode) resultList.get(i);
//            if (img_node == null) continue;  // should not occur
//            // String alias = img_node.getAlias();
//            // if (alias == null) continue;  // skip images without alias (should not occur)
//            String txt = img_node.getTitle();
//            if ((txt == null) || (txt.length() == 0)) {
//                txt = alias;
//            } else {
//                String space = "..." +
//                  ((alias.length() < TAB.length()) ? TAB.substring(alias.length()) : "");
//                txt = txt.replace('"', '_').replace('\\', '_');
//                txt = alias + " " + space + txt;
//            }
//            if (notfirst) out.println(",");
//            else notfirst = true;
//            out.print("[\"" + txt + "\", \"image/" + alias + "\"]");
//        }
//        out.println(");");
//    }


    private void serveImageList(HttpServletResponse response,
                                DocmaSession docmaSess, 
                                String listPath)
    throws IOException
    {
        boolean isJSON = listPath.equals("json") || listPath.startsWith("json/");

        response.setContentType(isJSON ? "application/json" : "text/javascript");
        response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        List<NodeInfo> infos = docmaSess.listImageInfos();
        if (infos == null) { 
            infos = new ArrayList<NodeInfo>(); 
        }
        Collections.sort(infos, new DocmaAppUtil.NodeInfoAliasComparator());

        PrintWriter out = response.getWriter();
        if (isJSON) {
            // JSON response; Used by CK editor
            out.println("[");
        } else {
            // This is the response as required by TinyMCE editor
            out.println("var tinyMCEImageList = new Array(");
        }
        boolean showAlias = !isJSON;  // show alias in title (for TinyMCE editor)
        writeFileListLines(out, infos, "image/", showAlias);
        if (isJSON) {
            out.println("]");
        } else {
            out.println(");");
        }
    }

    private void serveFileList(HttpServletResponse response,
                                DocmaSession docmaSess, 
                                String listPath)
    throws IOException
    {
        boolean isJSON = listPath.equals("json") || listPath.startsWith("json/");
        if (! isJSON) {
            return;   // Currently, only JSON response is supported
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        List<NodeInfo> infos = docmaSess.listFileInfos();
        if (infos == null) { 
            infos = new ArrayList<NodeInfo>(); 
        }
        Collections.sort(infos, new DocmaAppUtil.NodeInfoAliasComparator());

        PrintWriter out = response.getWriter();
        out.println("[");
        writeFileListLines(out, infos, "file/", false);
        out.println("]");
    }
    
    private void writeFileListLines(PrintWriter out, List<NodeInfo> infos, String urlPrefix, boolean showAlias)
    throws IOException
    {
        final String TAB = "..............."; // "            ";
        boolean notfirst = false;
        for (NodeInfo inf : infos) {
            String alias = inf.getAlias();
            if (alias == null) {
                continue;  // skip images/files without alias
            }
            String txt = inf.getTitle();
            if ((txt == null) || (txt.length() == 0)) {
                txt = showAlias ? alias : "";
            } else {
                txt = txt.replace('"', '_').replace('\\', '_');
                if (showAlias) {
                    String space = "..." +
                      ((alias.length() < TAB.length()) ? TAB.substring(alias.length()) : "");
                    txt = alias + " " + space + txt;
                }
            }
            if (notfirst) out.println(",");
            else notfirst = true;
            out.print("[\"" + txt + "\", \"" + urlPrefix + alias + "\"]");
        }
    }

    private void serveVideoList(HttpServletResponse response,
                                DocmaSession docmaSess, 
                                String listPath)
    throws IOException
    {
        boolean isJSON = listPath.equals("json") || listPath.startsWith("json/");
        
        response.setContentType(isJSON ? "application/json" : "text/javascript");
        response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);
        
        List<String> resultList = new ArrayList<String>(1000);
        getVideoListRecursive(docmaSess.getRoot(), resultList);
        Collections.sort(resultList);

        PrintWriter out = response.getWriter();
        if (isJSON) {
            // JSON response; Used by CK editor
            out.println("[");
        } else {
            // This is the response as required by TinyMCE editor
            out.println("var tinyMCEMediaList = new Array(");
        }
        boolean notfirst = false;
        for (String line : resultList) {
            if (notfirst) out.println(",");
            else notfirst = true;
            out.print(line);
        }
        if (isJSON) {
            out.println("]");
        } else {
            out.println(");");
        }
    }

    private void getVideoListRecursive(DocmaNode parentNode, List<String> resultList)
    {
        int cnt = parentNode.getChildCount();
        for (int i=0; i < cnt; i++) {
            DocmaNode nd = parentNode.getChild(i);
            if (nd.isFileContent()) {
                String alias = nd.getLinkAlias();
                if (alias != null) {
                    String ext = nd.getFileExtension();
                    if (VideoUtil.isSupportedVideoExtension(ext)) {
                        resultList.add(jsVideoLine(nd.getDefaultFileName(), alias));
                    }
                }
            } else
            if (nd.isChildable()) {
                getVideoListRecursive(nd, resultList);
            }
        }
    }

    private String jsVideoLine(String filepath, String link_alias)
    {
        filepath = filepath.replace('"', ' ').replace('\\', ' ');
        return "[\"" + filepath + "\", \"file/" + link_alias + "\"]";
    }

    private void serveLinkList(HttpServletResponse response,
                               DocmaSession docmaSess, 
                               String listPath)
    throws IOException
    {
        boolean isJSON = listPath.equals("json") || listPath.startsWith("json/");
        
        response.setContentType(isJSON ? "application/json" : "text/javascript");
        response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        List<String> resultList = new ArrayList<String>(1000);
        getLinkJsTocRecursive(docmaSess.getDocumentRoot(), "", resultList);

        PrintWriter out = response.getWriter();
        if (isJSON) {
            out.println("[");
        } else {
            out.println("var tinyMCELinkList = new Array(");
        }
        boolean notfirst = false;
        for (String tocline : resultList) {
            if (notfirst) out.println(",");
            else notfirst = true;
            out.print(tocline);
        }
        if (isJSON) {
            out.println("]");
        } else {
            out.println(");");
        }
    }

    private void getLinkJsTocRecursive(DocmaNode parentNode, String title_prefix, List<String> resultList)
    {
        final String INDENT = "..";
        int cnt = parentNode.getChildCount();
        int sect_cnt = 0;
        for (int i=0; i < cnt; i++) {
            DocmaNode nd = parentNode.getChild(i);
            String alias = nd.getAlias();
            String title = nd.getTitle();
            boolean is_sect = nd.isSection();
            boolean is_cont = nd.isContent();
            if (is_cont) {
                if (alias != null) {
                    String pref = (title_prefix.length() == 0) ? "" : title_prefix + INDENT;
                    resultList.add(jsTocLine(alias, pref, title));
                }
                DocmaAnchor[] anchors = nd.getContentAnchors();
                if (anchors != null) {
                    String pref = title_prefix + INDENT;
                    for (DocmaAnchor anc : anchors) {
                        resultList.add(jsTocLine(anc.getAlias(), pref, anc.getTitle()));
                    }
                }
            } else
            if (is_sect) {
                String prefix = title_prefix + (++sect_cnt);
                if (alias != null) {
                    resultList.add(jsTocLine(alias, prefix + " ", title));
                }
                getLinkJsTocRecursive(nd, prefix + ".", resultList);
            }
        }
    }
    
    private String jsTocLine(String alias, String indent, String title)
    {
        // final String TAB = ". . . . . . . . ";
        String link_alias;
        String display_alias;
        if (alias == null) {  // section without alias
            display_alias = "";
            link_alias = "";
        } else {
            display_alias = "[#" + alias + "] ";
            link_alias = DocmaAppUtil.getLinkAlias(alias);
        }
        if (title == null) {
            title = "";
        }
        String txt = indent + " " + display_alias + title.replace('"', ' ').replace('\\', ' ');
        // if (txt.length() > 42) {
        //     txt = txt.substring(0, 42);
        // }
        return "[\"" + txt + "\", \"#" + link_alias + "\"]";
    }


    private void serveStyleList(HttpServletResponse response,
                                DocmaSession docmaSess, 
                                String listPath)
    throws IOException
    {
        boolean isJSON = listPath.equals("json") || listPath.startsWith("json/");
        if (! isJSON) {
            return;   // Currently, only JSON response is supported
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        PrintWriter out = response.getWriter();
        
        // Return styles as required by CK editor
        CKEditUtils.writeStyleList_JSON(out, docmaSess.getPluginStoreConnection());
    }


    private void serveTemplate(HttpServletResponse response,
                               DocmaSession docmaSess,
                               String templateId)
    throws IOException
    {
        // org.docma.util.Log.info("templateId: '" + templateId + "'");

        DocmaNode node = docmaSess.getNodeById(templateId);

        response.setContentType(node.getContentType());
        int len = (int) node.getContentLength();
        if (len >= 0) response.setContentLength(len);

        disableBrowserCache(response);

        OutputStream streamout = response.getOutputStream();
        try {
            streamout.write(node.getContent());
        } finally {
            streamout.close();
        }
    }


    private void serveTemplateList(HttpServletResponse response,
                                   DocmaSession docmaSess)
    throws IOException
    {
        response.setContentType("text/javascript");
        response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        DocmaNode templates_folder = docmaSess.getTemplatesFolder();

        PrintWriter out = response.getWriter();
        out.println("var tinyMCETemplateList = [");
        boolean notfirst = false;
        for (int i=0; i < templates_folder.getChildCount(); i++) {
            DocmaNode tmpl_node = (DocmaNode) templates_folder.getChild(i);
            if (! tmpl_node.isHTMLContent()) continue;  // skip nodes other than html content
            String tmpl_id = tmpl_node.getId();
            // String alias = tmpl_node.getAlias();
            // if (alias == null) continue;  // skip templates without alias
            String txt = tmpl_node.getTitle();
            if ((txt == null) || (txt.length() == 0)) {
                continue;  // skip templates without title
            } else {
                txt = txt.replace('"', '_').replace('\\', '_');
            }
            if (notfirst) out.println(",");
            else notfirst = true;
            out.print("[\"" + txt + "\", \"template/" + tmpl_id + "\", \"" + txt + "\"]");
        }
        out.println("];");
    }


    private void serveContentCSS(HttpServletRequest request, HttpServletResponse response,
                                 DocmaSession docmaSess, DocmaOutputConfig outputConfig)
    throws IOException
    {
        response.setContentType("text/css");
        // response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        String mode = request.getParameter("mode");
        boolean isEditMode = (mode != null) && mode.equals("edit");
        boolean isExport = false;
        DocmaAppUtil.writeContentCSS(docmaSess, outputConfig, isExport, isEditMode, response.getWriter());
    }


    private void servePublicationLog(HttpServletResponse response,
                                     DocmaSession docmaSess, String publicationId)
    throws IOException
    {
        response.setContentType("text/html");
        // response.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);

        PrintWriter out = response.getWriter();
        out.println("<html><head><style type=\"text/css\">");
        out.println("body, pre { font-family:Arial,sans-serif; }");
        out.println(".log_msg { margin-top:10pt; }");
        out.println(".msg_head_error   { font-weight:bold; color:red; }");
        out.println(".msg_head_warning { font-weight:bold; color:blue; }");
        out.println(".msg_head_info    { font-weight:bold; color:black; }");
        out.println(".msg_content { margin-top:0px; margin-left:15pt; padding:3pt; background-color:#F0F0F0; }");
        out.println("</style></head>");
        out.println("<body><h2>Publication export log</h2>");
        out.println("<b>");
        out.println("Store: " + docmaSess.getStoreId());
        out.println("<br>Version: " + docmaSess.getVersionId());

        DocmaPublication pub = docmaSess.getPublication(publicationId);
        out.println("<br>Publication configuration: " + pub.getPublicationConfigId());
        out.println("<br>Output configuration: " + pub.getOutputConfigId());
        String pub_lang = pub.getLanguage();
        String sess_lang = docmaSess.getLanguageCode();
        out.println("<br>Publication language: " + pub_lang);
        out.println("</b>");
        if (! sess_lang.equalsIgnoreCase(pub_lang)) {
        out.println(" <b style=\"color:red;\"> Internal error: Publication language" +
                    " does not match session language (" + sess_lang + ").</b><br>");
        }

        DocmaExportLog pub_log = pub.getExportLog();
        if (pub_log == null) {
            out.println("<p><b>Log does not exist!</b></p>");
        } else {
            out.println(pub_log.toHTMLString());
        }
        out.println("</body></html>");
    }


    private void disableBrowserCache(HttpServletResponse response)
    {
        response.setHeader("expires", "0");
        response.setHeader("cache-control", "no-cache");
        response.setHeader("pragma", "no-cache");
    }


    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    public String getServletInfo() {
        return "MediaServlet";
    }// </editor-fold>

}
