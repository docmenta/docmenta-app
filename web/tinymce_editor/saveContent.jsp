<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.io.*,org.docma.plugin.*,org.docma.plugin.web.*,org.docma.plugin.internals.*,org.docma.coreapi.*,org.docma.plugin.tinymce.*"%>
<% 
    int error_code = 0;  // no error
    String error_msg = "";
    String docsess = request.getParameter("docsess");
    String storeid = request.getParameter("storeid");
    String verstr = request.getParameter("verid");
    String nodeid = request.getParameter("nodeid");
    String lang = request.getParameter("lang");
    String prog_str = request.getParameter("progress");
    String close_str = request.getParameter("isclosewin");
    String win_xpos = request.getParameter("winx");
    String win_ypos = request.getParameter("winy");
    String win_width = request.getParameter("winwidth");
    String win_height = request.getParameter("winheight");
    String para_indent = request.getParameter("indent");
    String editorId = request.getParameter("editor");
    String cont = request.getParameter("nodecontent");
    WebUserSession webSess = WebPluginUtil.getUserSession(application, docsess);
    StoreConnection storeConn = webSess.getOpenedStore();

    // Save content or cancel (confirm discard if content changed)
    boolean isCancel = (close_str != null) &&
                       close_str.trim().equalsIgnoreCase("cancel");
    boolean isSave = !isCancel;
    boolean isCloseWin = isCancel || 
                        ((close_str != null) && close_str.trim().equalsIgnoreCase("true"));
    String cancelAction = "";
    try {
        int progress = -1;
        if (isSave && (prog_str != null)) {
            try {
                progress = Integer.parseInt(prog_str);
            } catch (Exception ex) {}
        }
        if (storeConn == null) {
            throw new Exception("Store connection of user session is closed!");
        }
        
        // Prepare content for saving (editor specific and general XHTML cleaning)
        cont = TinyEditorUtil.prepareContentForSave(cont, editorId, para_indent);
        StringBuilder cont_buf = new StringBuilder(cont);

        // Apply HTML rules (transform quick links, trim empty paragraphs,...) 
        boolean allowCorrect = true; // isSave;
        LogEntries res = storeConn.prepareHTMLForSave(cont_buf, nodeid, allowCorrect, null);
        if (isSave && (res.getErrorCount() > 0)) {
            throw new Exception(res.getErrors()[0].getMessage());
        }
        cont = cont_buf.toString();
        
        VersionId verid = webSess.createVersionId(verstr);
        String sessStoreid = storeConn.getStoreId();
        VersionId sessVerid = storeConn.getVersionId();
        String sessLang = storeConn.getTranslationMode();
        if (sessLang == null) sessLang = "";
        if (lang == null) lang = "";
        boolean isTempConn = !(sessStoreid.equals(storeid) &&
                               sessVerid.equals(verid) &&
                               sessLang.equals(lang));
        if (isTempConn) {
            StoreConnection tempConn = webSess.createTempStoreConnection(storeid, verid);
            if (lang.length() > 0) tempConn.enterTranslationMode(lang);
            storeConn = tempConn;
        }

        // If content has changed, save content and create revision
        try {
            Content node = (Content) storeConn.getNodeById(nodeid);
            String old_cont = node.getContentString();
            if (isCancel) {
                // If content changed, show confirmation message
                if (TinyEditorUtil.contentIsEqual(cont, old_cont, false)) {
                    node.removeLock();
                    cancelAction = "parent.doCancel();";
                } else {
                    cancelAction = "parent.confirmCancel();";
                }
            } else {
                // Save content
                if (! TinyEditorUtil.contentIsEqual(cont, old_cont, true)) {
                    storeConn.startTransaction();
                    try {
                        node.makeRevision();
                    } catch (Exception ex) {
                        ex.printStackTrace(); 
                    }
                    node.setContentString(cont);
                    if ((progress >= 0) && (progress != node.getProgress())) {
                        node.setProgress(progress);
                    }
                    storeConn.commitTransaction();
                } else {
                    error_code = -1;  // nothing changed
                }
                // No error occured
                if (isCloseWin) {
                    // If editor window is closed after saving, remove lock
                    node.removeLock();
                }
            }
        } catch (Exception excpt) {
            if (storeConn.runningTransaction()) {
                storeConn.rollbackTransaction(); 
            }
            throw excpt;  // re-throw exception
        } finally {
            if (isTempConn) {
                try {
                    storeConn.close();
                } catch (Exception ex) {
                    System.out.println("Failed to close temporary connection: " + ex.getMessage());
                }
            }
        }
    } catch (Exception ex) {
        error_code = 1;
        error_msg = ex.getLocalizedMessage();
        // ex.printStackTrace();
    }

    if (! isCancel) {
        // Save window position and size:
        try {
            ContentAppHandler apphandler = webSess.getContentAppHandler(editorId);
            if (apphandler instanceof WindowPositionStorage) {
                ((WindowPositionStorage) apphandler).setWindowPos(webSess, win_xpos, win_ypos);
            } 
            if (apphandler instanceof WindowSizeStorage) {
                ((WindowSizeStorage) apphandler).setWindowSize(webSess, win_width, win_height);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    String jsaction = isSave ? "saveFinished();" : cancelAction;
%>
<html>
<head>
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>Save content</title>
<script type="text/javascript">
  function saveFinished() {
      parent.doSaveFinished(<%= error_code %>, "<%= error_msg.replace('"', '\'') %>");
  }
</script>
</head>
<body onload="<%= jsaction %>"
      bgcolor="#262626" style="background-color:#262626; margin:0px">
</body>
</html>
