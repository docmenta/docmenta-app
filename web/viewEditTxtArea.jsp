<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.util.*,org.docma.coreapi.*,org.docma.webapp.*,org.docma.app.*,org.zkoss.zk.ui.event.*"
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
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String file_content = request.getParameter("file_content");
    String charset_name = request.getParameter("charset_name");
    boolean doSave = (file_content != null);

    String onload_str = doSave ? "onload=\"parent.saveFinished();\"" : "";

    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);
    String storeId = docmaSess.getStoreId();
    DocVersionId versionId = docmaSess.getVersionId();
    String lang = docmaSess.getLanguageCode();

    String url = "viewEditTxtArea.jsp?desk=" + deskid + "&nodeid=" + nodeid +
                 "&stamp=" + System.currentTimeMillis();
    String self_url = response.encodeURL(url);

    boolean isFile = node.isFileContent();
    String error_msg = isFile ? "" : "Not a file";
    if (doSave && isFile) {
        try {
            String old_storeId = request.getParameter("store_id");
            String old_verId = request.getParameter("version_id");
            String old_lang = request.getParameter("lang");
            if (old_storeId.equals(storeId) &&
                (versionId != null) && old_verId.equals(versionId.toString()) &&
                old_lang.equals(lang)) {
                if ((charset_name != null) && !charset_name.equals("")) {
                    String old_charset = node.getFileCharset();
                    if ((old_charset == null) || !charset_name.equals(old_charset)) {
                        node.setFileCharset(charset_name);
                    }
                }
                node.setContentString(file_content);
            } else {
                error_msg = "Product has been closed!";
            }
        } catch (Exception ex) {
            error_msg = ex.getMessage();
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
<%
    if (doSave) {
        // is shown in the iframe filesave_frm of the parent window
        out.println("<div class=\"savemsg\"></div>");
    } else {
        // out.print("<pre>");
        // out.print(node.getContentString());
        // out.print("</pre>");
%>
<form name="editform" action="<%= self_url %>" method="post" target="filesave_frm" style="padding:0; margin:0; width:100%; height:100%;">
<textarea name="file_content" wrap="off" style="width:100%; height:100%; background-color:#F4F4F4; border-width:0px;" readonly><%
    String cont = node.getContentString();
    if (cont != null) out.print(cont.replace("<", "&lt;").replace(">", "&gt;"));
%></textarea>
<input type="hidden" name="charset_name" value="" />
<input type="hidden" name="store_id" value="<%= storeId %>" />
<input type="hidden" name="version_id" value="<%= versionId.toString() %>" />
<input type="hidden" name="lang" value="<%= lang %>" />
</form>
<%
    }
%>
</body>
</html>
