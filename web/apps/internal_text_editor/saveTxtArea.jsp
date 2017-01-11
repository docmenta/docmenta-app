<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.plugin.*,org.docma.plugin.web.*"
%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>Text File Content</title>
<style type="text/css">
.savemsg { font-size:12px; padding-top:2px; }
</style>
<%
    //
    // This page is shown in the iframe filesave_frm of the parent window
    //
    request.setCharacterEncoding("UTF-8");
    String sessid = request.getParameter("docsess");
    String nodeid = request.getParameter("nodeid");
    String old_storeId = request.getParameter("store_id");
    String old_verId = request.getParameter("version_id");
    String old_lang = request.getParameter("lang");
    String file_content = request.getParameter("file_content");
    String charset_name = request.getParameter("charset_name");
    boolean doSave = (file_content != null);

    String onload_str = doSave ? "onload=\"parent.saveFinished();\"" : "";

    String error_msg = "";
    Node node = null;
    String storeId = null;
    VersionId versionId = null;
    String lang = null;
    
    WebUserSession webSess = WebPluginUtil.getUserSession(application, sessid);
    StoreConnection storeConn = webSess.getOpenedStore();
    
    boolean sameStore = false;
    if (storeConn != null) {
        node = storeConn.getNodeById(nodeid);
        storeId = storeConn.getStoreId();
        versionId = storeConn.getVersionId();
        lang = storeConn.getCurrentLanguage().getCode();
        sameStore = old_storeId.equals(storeId) &&
                    (versionId != null) && 
                    old_verId.equals(versionId.toString()) &&
                    old_lang.equals(lang);
    }

    if (! sameStore) {
        error_msg = "Store connection has been closed!";
    } else if (! (node instanceof Content)) {
        error_msg = "No valid content node";
    } else if (! doSave) {
        error_msg = "Missing content parameter";
    }
    
    if (error_msg.equals("")) {
        try {
            Content cont = (Content) node;
            boolean isFile = cont instanceof FileContent;
            if (isFile && (charset_name != null) && !charset_name.equals("")) {
                String old_charset = cont.getCharset();
                if ((old_charset == null) || !charset_name.equals(old_charset)) {
                    ((FileContent) cont).setCharset(charset_name);
                }
            }
            cont.setContentString(file_content);
        } catch (Exception ex) {
            error_msg = ex.getLocalizedMessage();
        }
    }
%>
<script type="text/javascript">
    function getErrorMsg() {
        return '<%= error_msg.replace("'", " ") %>';
    }
</script>
</head>
<body <%= onload_str %> style="background:#E0E0E0; font-family:Arial,sans-serif; margin:0; padding:0; overflow:hidden;">
  <div class="savemsg"></div>
</body>
</html>
