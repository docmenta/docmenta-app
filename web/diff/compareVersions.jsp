<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.util.*,java.io.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*"
%><%!
    private void cleanDiffDir(File diff_dir)
    {
        File[] farr = diff_dir.listFiles();
        long now = System.currentTimeMillis();
        for (int i=0; i < farr.length; i++) {
            File f = farr[i];
            String fn = f.getName();
            if (fn.startsWith("diff-") && fn.endsWith(".html")) {
                int p1 = fn.lastIndexOf('_');
                int p2 = fn.lastIndexOf('.');
                try {
                    long stamp = Long.parseLong(fn.substring(p1 + 1, p2));
                    if ((now - stamp) > (7*24*60*60*1000)) {
                        f.delete();  // Delete file if older than 1 week
                    }
                } catch(Exception ex) {}
            }
        }
    }

    private File createTempFile(File diff_dir, String userid)
    {
        long stamp = System.currentTimeMillis();
        File f = null;
        for (int i=0; i < 20; i++) {
            f = new File(diff_dir, "diff-" + userid + "_" + stamp + ".html");
            if (f.exists()) stamp++;
            else break;
        }
        if (f == null) {
            throw new DocRuntimeException("Could not generate temporary filename.");
        }
        return f;
    }

    private void writeToFile(File f, StringBuilder buf) throws IOException
    {
        FileOutputStream fout = new FileOutputStream(f);
        Writer wout = new OutputStreamWriter(fout, "UTF-8");
        wout.append(buf);
        wout.close();
        fout.close();
    }

%><%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String storeId = request.getParameter("store");
    String new_value = request.getParameter("new");
    String old_value = request.getParameter("old");
    String lang = request.getParameter("lang");
    if ((lang != null) && lang.equals("")) lang = null;
    String img_para = request.getParameter("images");
    boolean showImages = (img_para == null) || !img_para.equalsIgnoreCase("false");

    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    MainWindow mainwin = docmaWebSess.getMainWindow();

    DocmaPublicationConfig pub_conf = mainwin.getPreviewPubConfig();
    DocmaOutputConfig out_conf = mainwin.getPreviewOutputConfig();
    String userId = docmaSess.getUserId();
    String temp_path = docmaSess.getApplicationProperty(DocmaConstants.PROP_TEMP_PATH);

    DocmaSession tempSess = docmaSess.createNewSession();
    try {
        String new_ver_str = CompareVersionsDialog.getVersionFromValue(new_value);
        DocVersionId new_vid = tempSess.createVersionId(new_ver_str);
        Date new_rev_date = CompareVersionsDialog.getRevisionDateFromValue(new_value);

        DocVersionId old_vid = null;
        Date old_rev_date = null;
        if (! old_value.equalsIgnoreCase("none")) {
            String old_ver_str = CompareVersionsDialog.getVersionFromValue(old_value);
            old_vid = tempSess.createVersionId(old_ver_str);
            old_rev_date = CompareVersionsDialog.getRevisionDateFromValue(old_value);
        }

        StringBuilder buf = new StringBuilder();

        //
        // Get new version of node 
        //
        tempSess.openDocStore(storeId, new_vid);
        if (lang != null) tempSess.enterTranslationMode(lang);
        // else if (tempSess.getTranslationMode() != null) tempSess.leaveTranslationMode();
        String lang_code = tempSess.getLanguageCode();

        DocmaNode nd_new = tempSess.getNodeById(nodeid);
        int h_level = 0;
        if (nd_new != null) {
            if (pub_conf == null) {
                h_level = nd_new.getDepth();
            } else {
                h_level = nd_new.getDepthRelativeTo(pub_conf.getContentRoot());
            }
            if (nd_new.isContent()) {
                String str;
                if (new_rev_date == null) {
                    str = nd_new.getContentString();
                } else {
                    str = CompareVersionsDialog.getNodeRevision(nd_new, new_rev_date);
                }
                if (str != null) buf.append(str);
            } else {
                // Get section preview
                DocmaAppUtil.getNodePreview(buf, nd_new, h_level, tempSess, pub_conf, out_conf);
            }
        }
        tempSess.closeDocStore();

        //
        // Build CSS URL
        //
        // String serv_context = request.getContextPath();
        // if (! serv_context.endsWith("/")) serv_context += "/";
        StringBuffer req_url = request.getRequestURL();
        req_url.setLength(req_url.lastIndexOf("/diff/") + 1);
        String docSessId = docmaSess.getSessionId();
        // Note: The sessionId used in the MediaServlet path has to be
        //       connected to the MainWindow. Therefore the sessionId of docmaSess
        //       is used instead of the tempSess Id. Alternatively: Set MainWindow
        //       for the related web-session object of tempSess.
        String base_url = req_url + // serv_context +
                          "servmedia/" + docSessId + "/" +
                          storeId + "/" + new_vid + "/" + lang_code + "/";
        String css_url = response.encodeURL(// base_url +
                                            "css/content.css?desk=" + deskid +
                                            "&expire=" + System.currentTimeMillis());
%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%
        if (old_vid == null) {  // no comparison, only view selected version/revision 
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>View content</title>
<base href="<%= base_url %>">
<link rel="stylesheet" type="text/css" href="<%= css_url %>">
</head>
<body>
<%
    if (! showImages) {
        ImageURLTransformer transformer = new ImageURLTransformer_Constant("");
        String html = buf.toString();
        buf.setLength(0);
        DocmaAppUtil.transformImageURLs(html, transformer, buf);
    }
    out.print(buf.toString());
%>
</body>
</html>
<%
        } else {
            // Create temporary directory for diffing
            File diff_dir = new File(temp_path, "diff");
            if (!diff_dir.exists()) diff_dir.mkdir();
            
            // Write new version of node to file f_new
            File f_new = createTempFile(diff_dir, userId);
            writeToFile(f_new, buf);

            //
            // Get old version of node and write to file f_old
            //
            tempSess.openDocStore(storeId, old_vid);
            if (lang != null) tempSess.enterTranslationMode(lang);
            // else if (tempSess.getTranslationMode() != null) tempSess.leaveTranslationMode();
            DocmaNode nd_old = tempSess.getNodeById(nodeid);
            buf.setLength(0);  // clear buffer
            if (nd_old != null) {
                if (nd_old.isContent()) {
                    String str;
                    if (old_rev_date == null) {
                        str = nd_old.getContentString();
                    } else {
                        str = CompareVersionsDialog.getNodeRevision(nd_old, old_rev_date);
                    }
                    if (str != null) buf.append(str);
                } else {
                    // Get section preview
                    DocmaAppUtil.getNodePreview(buf, nd_old, h_level, tempSess, pub_conf, out_conf);
                }
            }
            tempSess.closeDocStore();
            File f_old = createTempFile(diff_dir, userId);
            writeToFile(f_old, buf);

            //
            // Use daisydiff to compare f_old and f_new. Write result to f_result.
            //
            File f_result = createTempFile(diff_dir, userId);
            String[] diffargs = {
                f_old.getAbsolutePath(),
                f_new.getAbsolutePath(),
                "--file=" + f_result.getAbsolutePath() };
            org.outerj.daisy.diff.Main.main(diffargs);

            //
            // Read diffing result. Insert CSS reference in head part of result.
            //
            FileInputStream fin = new FileInputStream(f_result);
            BufferedReader in = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
            StringWriter sout = new StringWriter(buf.capacity() + 10000);
            PrintWriter pout = new PrintWriter(sout);
            String line;
            boolean do_insert = true;
            while ((line = in.readLine()) != null) {
                if (do_insert) {
                    final String HEAD_TAG = "<head>";
                    int p = line.indexOf(HEAD_TAG);
                    if (p >= 0) {
                        int after_head = p + HEAD_TAG.length();
                        pout.println(line.substring(0, after_head));
                        line = line.substring(after_head);
                        // out.println("<base href=\"" + base_url + "\">");
                        pout.println("<link href=\"" + base_url + css_url +
                                     "\" type=\"text/css\" rel=\"stylesheet\"/>");
                        do_insert = false;
                    }
                }
                pout.println(line);
            }
            pout.close();

            // Transform image URLs to absolute URLs or hide images
            ImageURLTransformer transformer;
            if (showImages) {
                transformer = new ImageURLTransformer_Prefix(base_url + "image/");
            } else {
                transformer = new ImageURLTransformer_Constant(""); // hide images
            }
            buf.setLength(0);
            DocmaAppUtil.transformImageURLs(sout.toString(), transformer, buf);

            // Skip XML declaration and DOCTYPE
            int p = 0;
            int buf_len = buf.length();
            while ((p < buf_len) && Character.isWhitespace(buf.charAt(p))) ++p;
            if ((p < buf_len) && (buf.charAt(p) == '<')) {
                int p1 = p + 1;
                if (p1 < buf_len) {
                    char ch = buf.charAt(p1);
                    if ((ch == '?') || (ch == '!')) {  // declaration or DOCTYPE
                        int end_pos = buf.indexOf(">", p);
                        if (end_pos > 0) p = end_pos + 1;
                    }
                }
            }

            // Write diffing result to out.
            out.append(buf, p, buf_len);

            // Try to delete temporary files
            try {
                f_new.delete();
                f_old.delete();
                f_result.delete();
                cleanDiffDir(diff_dir);
            } catch (Exception ex) {}
        }
    } catch (Exception ex) {
        out.println("<html><body><h1>Error</h1> " + ex.getMessage() + "</body></html>");
        ex.printStackTrace();
    } finally {
        tempSess.closeSession();
    }
%>