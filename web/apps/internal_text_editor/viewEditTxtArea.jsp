<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.plugin.*,org.docma.plugin.web.*,org.docma.plugin.internaleditor.*"
%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>Text File Content</title>
<style type="text/css">
pre { font-size:medium }
.savemsg { font-size:12px; padding-top:2px; }
</style>
<%
    request.setCharacterEncoding("UTF-8");
    String sessid = request.getParameter("docsess");
    String nodeid = request.getParameter("nodeid");
    String file_content = request.getParameter("file_content");
    String charset_name = request.getParameter("charset_name");
    boolean doSave = (file_content != null);

    String onload_str = doSave ? "onload=\"parent.saveFinished();\"" : "onload=\"plugInit();\"";

    WebUserSession webSess = WebPluginUtil.getUserSession(application, sessid);
    StoreConnection storeConn = webSess.getOpenedStore();
    Node node = storeConn.getNodeById(nodeid);
    String storeId = storeConn.getStoreId();
    VersionId versionId = storeConn.getVersionId();
    String lang = storeConn.getCurrentLanguage().getCode();

    String url = "viewEditTxtArea.jsp?docsess=" + sessid + "&nodeid=" + nodeid +
                 "&stamp=" + System.currentTimeMillis();
    String self_url = response.encodeURL(url);

    boolean isCont = node instanceof Content;
    boolean isFile = node instanceof FileContent;
    String error_msg = isCont ? "" : "No valid content node";
    if (doSave && isCont) {
        try {
            Content cont = (Content) node;
            String old_storeId = request.getParameter("store_id");
            String old_verId = request.getParameter("version_id");
            String old_lang = request.getParameter("lang");
            if (old_storeId.equals(storeId) &&
                (versionId != null) && old_verId.equals(versionId.toString()) &&
                old_lang.equals(lang)) {
                if (isFile && (charset_name != null) && !charset_name.equals("")) {
                    String old_charset = cont.getCharset();
                    if ((old_charset == null) || !charset_name.equals(old_charset)) {
                        ((FileContent) cont).setCharset(charset_name);
                    }
                }
                cont.setContentString(file_content);
            } else {
                error_msg = "Product has been closed!";
            }
        } catch (Exception ex) {
            error_msg = ex.getLocalizedMessage();
        }
    }
%>
<script type="text/javascript">
    function getErrorMsg() {
        return '<%= error_msg.replace("'", " ") %>';
    }
    
    function plugInit() {
        <%= TextFileHandler.getHTMLOnLoadStatement() %>
    }
</script>
<%= TextFileHandler.getHTMLHead() %>
</head>
<body <%= onload_str %> style="background:#E0E0E0; font-family:Arial,sans-serif; margin:0; padding:0; overflow:hidden;">
<%
    if (doSave) {
        // is shown in the iframe filesave_frm of the parent window
        out.println("<div class=\"savemsg\"></div>");
    } else if (isCont) {
        out.println(TextFileHandler.getHTMLBodyStart());
%>
<form name="editform" action="<%= self_url %>" method="post" target="filesave_frm" style="padding:0; margin:0; width:100%; height:100%;">
<textarea id="file_content" name="file_content" wrap="off" style="width:100%; height:100%; background-color:#F4F4F4; border-width:0px;" readonly><%
    String str = ((Content) node).getContentString();
    if (str != null) {
        out.print(str.replace("<", "&lt;").replace(">", "&gt;"));
    }
%></textarea>
<input type="hidden" name="charset_name" value="" />
<input type="hidden" name="store_id" value="<%= storeId %>" />
<input type="hidden" name="version_id" value="<%= versionId.toString() %>" />
<input type="hidden" name="lang" value="<%= lang %>" />
</form>
<%
    }
    out.println(TextFileHandler.getHTMLBodyEnd());
%>
</body>
</html>
