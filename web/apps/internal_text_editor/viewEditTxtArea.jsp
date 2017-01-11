<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="java.net.*,org.docma.plugin.*,org.docma.plugin.web.*,org.docma.plugin.internals.*"
%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>Text File Content</title>
<style type="text/css">
pre { font-size:medium }
.errormsg { font-size:12px; font-weight:bold; color:red; }
</style>
<%
    request.setCharacterEncoding("UTF-8");
    String sessid = request.getParameter("docsess");
    String nodeid = request.getParameter("nodeid");
    // String charset_name = request.getParameter("charset_name");

    WebUserSession webSess = WebPluginUtil.getUserSession(application, sessid);
    StoreConnection storeConn = webSess.getOpenedStore();
    if (storeConn == null) {
        throw new Exception("Store connection is closed!");
    }
    Node node = storeConn.getNodeById(nodeid);
    String storeId = storeConn.getStoreId();
    VersionId versionId = storeConn.getVersionId();
    String lang = storeConn.getCurrentLanguage().getCode();

    String save_url = response.encodeURL("saveTxtArea.jsp?docsess=" + URLEncoder.encode(sessid, "UTF-8") + 
                                         "&nodeid=" + URLEncoder.encode(nodeid, "UTF-8") +
                                         "&stamp=" + System.currentTimeMillis());

    boolean isCont = node instanceof Content;
    boolean isFile = node instanceof FileContent;
    String ext = isFile ? ((FileContent) node).getFileExtension() : "content";
    String error_msg = isCont ? "" : "Node type is not supported.";
    boolean hasError = (error_msg != null) && !error_msg.equals("");
%>
<script type="text/javascript">
    function docmaInit() {
        <%= hasError ? "" : "parent.initAfterLoad(); " + TextFileHandler.getJSOnLoad(webSess, ext) %>
    }

    function docmaBeforeSave() {
        try {
            <%= TextFileHandler.getJSBeforeSave(webSess, ext) %>
        } catch (err) {
            alert(err.message);
            return false;
        }
        return true;
    }

    function docmaEnterEdit() {
        var ef = document.forms["editform"];
        ef.file_content.readOnly = false;
        ef.file_content.style.backgroundColor = "#FFFFFF";
        ef.file_content.focus();
        try {
            <%= TextFileHandler.getJSEnterEdit(webSess, ext) %>
        } catch (err) {}
    }

    function docmaEnterView() {
        var ef = document.forms["editform"];
        ef.file_content.readOnly = true;
        ef.file_content.style.backgroundColor = "#F4F4F4";
        try {
            <%= TextFileHandler.getJSEnterView(webSess, ext) %>
        } catch (err) {}
    }
</script>
<%= TextFileHandler.getHTMLHead(webSess, ext) %>
</head>
<body onload="docmaInit();" style="background:#E0E0E0; font-family:Arial,sans-serif; margin:0; padding:0; overflow:hidden;">
<%
    if (hasError) {
        out.print("<div class=\"errormsg\">");
        out.print(error_msg);
        out.println("</div>");
    } else {
        out.println(TextFileHandler.getHTMLBodyStart(webSess, ext));
%>
<form name="editform" action="<%= save_url %>" method="post" target="filesave_frm" style="padding:0; margin:0; width:100%; height:100%;">
<textarea id="file_content" name="file_content" wrap="off" style="width:100%; height:100%; background-color:#F4F4F4; border-width:0px;" readonly><%
    String str = ((Content) node).getContentString();
    if (str != null) {
        out.print(str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
    }
%></textarea>
<input type="hidden" name="charset_name" value="" />
<input type="hidden" name="store_id" value="<%= storeId %>" />
<input type="hidden" name="version_id" value="<%= versionId.toString() %>" />
<input type="hidden" name="lang" value="<%= lang %>" />
</form>
<%
        out.println(TextFileHandler.getHTMLBodyEnd(webSess, ext));
    }
%>
</body>
</html>
