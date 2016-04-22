<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.util.*,org.docma.webapp.*,org.docma.app.*"
%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>View/Edit File</title>
<style type="text/css">
div.docma_msg { margin: 1em; }
.labeltxt { font-size:12px; }
</style>
<%
    boolean isTextFile = false;

    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String action = request.getParameter("action");
    String iswin_str = request.getParameter("iswin");
    boolean iswin = (iswin_str != null) && iswin_str.equals("true");
    String edit_str = request.getParameter("edit");
    boolean start_edit = (edit_str != null) && edit_str.equals("true");

    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);

    long now = System.currentTimeMillis();
    String url = "viewFile.jsp?desk=" + deskid + "&nodeid=" + nodeid +
                 "&stamp=" + now;
    String self_url = response.encodeURL(url);
    String self_url_frame = self_url;
    String self_url_win = response.encodeURL(url + "&iswin=true");
    if (iswin) self_url = self_url_win;
    String content_url = response.encodeURL("viewEditTxtArea.jsp?desk=" + deskid +
                                            "&nodeid=" + nodeid + "&stamp=" + now);

    String ext = "";
    if (node.isFileContent()) {
        ext = node.getFileExtension();
        if (ext == null) ext = "";
        ext = ext.toLowerCase();
        isTextFile = docmaSess.isTextFileExtension(ext);
    }

    if ((action != null) && action.equals("change_encoding") && isTextFile) {
        String charsetName = request.getParameter("file_encoding");
        node.setFileCharset(charsetName);
    }

    MainWindow mainWin = docmaWebSess.getMainWindow();
    String readonly_msg = GUIUtil.checkEditVersionAllowed(mainWin, docmaSess);
    if (readonly_msg != null) {
        start_edit = false;
    }
    if (start_edit) {
        readonly_msg = GUIUtil.checkEditFileAllowed(mainWin, docmaSess, node);
        if (readonly_msg != null) {
            start_edit = false;
        }
    }
%>
<script type="text/javascript">
    var currentStatus = 'view';
    var closeAfterSave = false;
    var isWin = <%= iswin ? "true" : "false" %>;

    function doInit() {
      <%=  start_edit ? "doEdit();" : "" %>
    }
<% 
  if (readonly_msg != null) {
%>
    function doEdit() {
        alert("<%= readonly_msg.replace('"', '\'') %>");
    }
<%
  } else {
%>
    function doEdit() {
        var editform = window.frames['edit_txt_frame'].document.forms["editform"];
        editform.file_content.readOnly = false;
        editform.file_content.style.backgroundColor = "#FFFFFF";
        document.forms["btnform"].editBtn.style.display = 'none';
        document.forms["btnform"].saveBtn.style.display = 'inline';
        if (isWin) {
            document.forms["btnform"].saveCloseBtn.style.display = 'inline';
            // document.forms["btnform"].closeBtn.style.display = 'none';
        } else {
            document.forms["btnform"].cancelBtn.style.display = 'inline';
            document.forms["btnform"].openWinBtn.style.display = 'none';
        }
        currentStatus = 'edit';
    }
<% 
  }
%>
    function doSave() {
        var editform = window.frames['edit_txt_frame'].document.forms["editform"];
        var sel = document.forms["btnform"].file_encoding;
        editform.charset_name.value = sel.options[sel.selectedIndex].value;
        editform.submit();
        document.forms["btnform"].saveBtn.disabled = true;
        if (isWin) {
          document.forms["btnform"].saveCloseBtn.disabled = true;
        }
        closeAfterSave = false;
    }
    
    function doSaveAndClose() {
        doSave();
        closeAfterSave = true;
    }

    function openInNewWin() {
        parent.open('<%= self_url_win %>', '_blank', 'width=600,height=700,resizable=yes,scrollbars=no,location=no,menubar=no,status=yes');
    }

    function doClose() {
        // if (currentStatus == 'edit') {
        //     var check = window.confirm("Close without saving?");
        //     if (! check) return;
        // }
        window.close();
    }

    function cancelEdit()
    {
        window.location.reload();
    }

    function saveFinished() {
        var errmsg = window.frames['filesave_frm'].getErrorMsg();
        if (errmsg != '') {
            window.alert(errmsg);
            document.forms["btnform"].saveBtn.disabled = false;  // enable button to try again
            if (isWin) {
                document.forms["btnform"].saveCloseBtn.disabled = false;  // enable button to try again
            }
            return;
        }
        switchToViewMode();
        
        var main_win = isWin ? opener : parent;
        main_win.processDeferredDocmaEvents();  // update modification date in product tree
        if (isWin) {
            // If same file is currently opened in preview frame, reload the file to show the new content
            var p_url = main_win.frames['viewcontentfrm'].location.href;
            var node_pattern = "nodeid=<%= nodeid %>";
            if ((p_url.indexOf('viewFile.jsp') >= 0) && (p_url.indexOf(node_pattern) >= 0)) {
                main_win.frames['viewcontentfrm'].location.replace('<%= self_url_frame %>');
            }
        }
        
        if (closeAfterSave) {
            doClose();
        }
    }

    function switchToViewMode() {
        var editform = window.frames['edit_txt_frame'].document.forms["editform"];
        editform.file_content.readOnly = true;
        editform.file_content.style.backgroundColor = "#F4F4F4";
        document.forms["btnform"].saveBtn.disabled = false;
        document.forms["btnform"].saveBtn.style.display = 'none';
        document.forms["btnform"].editBtn.style.display = 'inline';
        if (isWin) {
            document.forms["btnform"].saveCloseBtn.disabled = false;
            document.forms["btnform"].saveCloseBtn.style.display = 'none';
            document.forms["btnform"].closeBtn.style.display = 'inline';
        } else {
            document.forms["btnform"].cancelBtn.style.display = 'none';
            document.forms["btnform"].openWinBtn.style.display = 'inline';
        }
        currentStatus = 'view';
    }

    function changeEncoding() {
        if (currentStatus == 'view') {
            document.forms["btnform"].action.value = 'change_encoding';
            document.forms["btnform"].submit();
        }
    }
</script>
</head>
<body onload="doInit();" style="background:#E0E0E0; font-family:Arial,sans-serif; margin:0 3px 0 0; padding:0; width:100%; max-width:100%; height:100%; overflow:hidden;">
<%
    if (!isTextFile) {
        out.println("<div class=\"docma_msg\"><b>No preview available for " +
                    node.getDefaultFileName() + 
                    "</b><br /><br /><i>If files with extension '." + ext + 
                    "' are text-files, then add this extension as text-file extension in the application settings.</i></div>");
    } else {
        String charsetName = node.getFileCharset();
        if (charsetName == null) {
            String alias = node.getAlias();
            String fn = node.getDefaultFileName();
            if (((alias != null) && alias.equals("gentext")) || fn.startsWith("gentext")) {
                charsetName = "ISO-8859-1";
            } else {
                charsetName = "UTF-8";
            }
        }
%>
<form name="btnform" action="<%= self_url %>" method="post" style="padding:0; margin:0; width:100%; height:100%;">
<table border="0" cellspacing="0" cellpadding="0" style="width:100%; height:100%;">
<tr>
    <td style="padding:0 0 1px 0; width:100%;">
        <iframe name="edit_txt_frame" src="<%= content_url %>" width="100%" height="100%"></iframe>
    </td>
</tr>
<tr>
  <td height="40" valign="middle" style="padding:5px; border-top:1px solid #D0D0D0;">
    <table border="0" width="100%" cellspacing="0" cellpadding="0">
      <tr>
          <td valign="middle" nowrap>
            <button type="button" name="editBtn" onclick="doEdit();"
                    style="vertical-align:middle; height:28px;">
                <img src="img/editfile.gif" style="vertical-align:middle;" />&nbsp;Edit
            </button>
            <button type="button" name="saveBtn" onclick="doSave();" 
                    style="display:none; vertical-align:middle; height:28px;">
                <img src="img/save.gif" style="vertical-align:middle;" />&nbsp;Save
            </button>
<%
    if (iswin) {
%>
            <button type="button" name="saveCloseBtn" onclick="doSaveAndClose();" 
                    style="display:none; vertical-align:middle; height:28px;">
                <img src="img/save_and_close.gif" style="vertical-align:middle;" />&nbsp;Save&#160;&amp;&#160;Close
            </button>
            &nbsp;
            <button type="button" name="closeBtn" onclick="doClose();"
                    style="vertical-align:middle; height:28px;">
                <img src="img/close.gif" style="vertical-align:middle;" />&nbsp;Close
            </button>
<%
    } else {
%>
            &nbsp;
            <button type="button" name="openWinBtn" onclick="openInNewWin();"
                    style="vertical-align:middle; height:28px;">
                <img src="img/open_win.gif" style="vertical-align:middle;" />&nbsp;Open in Window</button>
            <button type="button" name="cancelBtn" onclick="cancelEdit();"
                    style="display:none; vertical-align:middle; height:28px;">
                <img src="img/cancel.gif" style="vertical-align:middle;" />&nbsp;Cancel
            </button>
<%
    }
%>
            <iframe name="filesave_frm" src="empty_preview.html" width="4" height="20"
                    scrolling="no" marginheight="0" marginwidth="0" frameborder="0"></iframe>
          </td>
          <td align="right" valign="middle" nowrap>
            <table border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td valign="middle"><span class="labeltxt">Encoding:&nbsp;</span></td>
                <td valign="middle">
    <select name="file_encoding" size="1" onchange="changeEncoding();">
<%
    String[] charsets = new String[] { "ISO-8859-1", "US-ASCII", "UTF-8",
                                       "UTF-16BE", "UTF-16LE", "UTF-16" };
    for (int i = 0; i < charsets.length; i++) {
        String cs = charsets[i];
%>
      <option value="<%= cs %>" <%= cs.equalsIgnoreCase(charsetName) ? "selected" : "" %> ><%= cs %></option>
<%
    }
%>
    </select>
    <input type="hidden" name="action" value="" />
                </td>
              </tr>
            </table>
          </td>
      </tr>
    </table>
  </td>
</tr>
</table>
</form>
<%
    }
%>
</body>
</html>
