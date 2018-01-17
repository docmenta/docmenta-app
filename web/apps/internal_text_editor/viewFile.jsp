<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.plugin.*,org.docma.plugin.web.*,org.docma.plugin.internals.*"
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
.hint_msg { color:#909090; }
.comment { }
.commentHidden { display:none; }
#filenameBox { float: left; font-weight:bold; font-size: 13px; }
#commentBox { float: right; color:#909090; }
#docmaPlugToolbar { margin-right: 14px; }
#docmaPlugToolbar a { font-size: 12px; font-weight: normal; text-decoration: none; }
</style>
<%
    boolean isFile = false;
    boolean isTextFile = false;

    String sessid = request.getParameter("docsess");  // String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String appid = request.getParameter("appid");
    String action = request.getParameter("action");
    String iswin_str = request.getParameter("iswin");
    boolean iswin = (iswin_str != null) && iswin_str.equals("true");
    String edit_str = request.getParameter("edit");
    boolean start_edit = (edit_str != null) && edit_str.equals("true");

    WebUserSession webSess = WebPluginUtil.getUserSession(application, sessid);
    StoreConnection storeConn = webSess.getOpenedStore();
    if (storeConn == null) {
        throw new Exception("Store connection of session is closed!");
    }
    Node node = storeConn.getNodeById(nodeid);

    TextFileHandler appHandler = (appid == null) ? null : (TextFileHandler) webSess.getContentAppHandler(appid);

    // String rel_path = request.getContextPath();
    // if (! rel_path.endsWith("/")) rel_path += "/";
    // if (appHandler != null) {
    //     rel_path += appHandler.getRelativeAppURL();
    // }
    
    long now = System.currentTimeMillis();
    String app_param = (appid == null) ? "" : ("&appid=" + appid);
    String url = "viewFile.jsp?docsess=" + sessid + "&nodeid=" + nodeid +
                 app_param + "&stamp=" + now;
    String self_url = response.encodeURL(url);
    String preview_url = self_url;
    String self_url_win = response.encodeURL(url + "&iswin=true");
    if (iswin) {
        self_url = self_url_win;
    }
    String content_url = response.encodeURL("viewEditTxtArea.jsp?docsess=" + sessid +
                                            "&nodeid=" + nodeid + "&stamp=" + now);

    String js_open_viewer = null;
    if (appHandler != null) {
        js_open_viewer = appHandler.getJSOpenWindow(webSess, self_url_win);
    }

    String ext = "";
    if (node instanceof FileContent) {
        isFile = true;
        ext = ((FileContent) node).getFileExtension();
        if (ext == null) ext = "";
        ext = ext.toLowerCase();
        isTextFile = storeConn.isTextFileExtension(ext);
    }

    if ((action != null) && action.equals("change_encoding") && isTextFile) {
        String charsetName = request.getParameter("file_encoding");
        ((FileContent) node).setCharset(charsetName);
    }

    String readonly_msg = null; 
    try {
        ((Content) node).checkEditContentAllowed();
    } catch (Exception ex) {
        start_edit = false;
        readonly_msg = ex.getLocalizedMessage();
    }
    
    String unsaved_msg = webSess.getLabel("confirm.discard_unsaved").replace('"', ' ');
%>
<script type="text/javascript">
    var currentStatus = 'view';
    var closeAfterSave = false;
    var reselectAfterSave = false;
    var isWin = <%= iswin ? "true" : "false" %>;

    function isUnsaved() {
        return (currentStatus == 'edit') && getEditTxtFrame().isContentChanged();
    }

    function doBeforeUnload() {
        if (isUnsaved()) {
            return "<%= unsaved_msg %>";
        }
    }

    function initAfterLoad() {
      <%=  start_edit ? "doEdit();" : "switchToViewMode();" %>
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
        getEditTxtFrame().docmaEnterEdit();
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
        document.getElementById("commentBox").className = 'commentHidden';
        
        window.onbeforeunload = doBeforeUnload;
    }
<% 
  }
%>
    function startSave() {
        getEditTxtFrame().docmaBeforeSave();
        var editform = getEditTxtFrame().document.forms["editform"];
        var sel = document.forms["btnform"].file_encoding;
        editform.charset_name.value = sel.options[sel.selectedIndex].value;
        editform.submit();
        document.forms["btnform"].saveBtn.disabled = true;
        if (isWin) {
          document.forms["btnform"].saveCloseBtn.disabled = true;
        }
    }
    
    function doSave() {
        closeAfterSave = false;
        reselectAfterSave = false;
        startSave();
    }
    
    function doSaveAndClose() {
        closeAfterSave = true;
        reselectAfterSave = false;
        startSave();
    }

    function saveAndReselect() 
    {
        closeAfterSave = false;
        reselectAfterSave = true;
        startSave();
    }
    
    function doClose() {
        // if (currentStatus == 'edit') {
        //     var check = window.confirm("Close without saving?");
        //     if (! check) return;
        // }
        window.close();
    }

    function cancelEdit() {
        window.onbeforeunload = null;  // disable unsaved changes message 
        window.location.replace('<%= self_url %>');
    }
    
    function setLabelMsg(msg) {
        var elem = document.getElementById("saveLabel");
        while (elem.hasChildNodes()) {
            elem.removeChild(elem.lastChild);
        }
        elem.appendChild(document.createTextNode(msg));
    }

    function saveFinished() {
        var errmsg = window.frames['filesave_frm'].getErrorMsg();
        if (isWin || (errmsg != '')) {
            document.forms["btnform"].saveBtn.disabled = false;
            if (isWin) {
                document.forms["btnform"].saveCloseBtn.disabled = false;
            }
        }
        if (errmsg != '') {
            window.alert(errmsg);
            return;
        }

        // Save was successful
        if (isWin) {
            setLabelMsg(window.frames['filesave_frm'].getSaveMsg());
            window.setTimeout("setLabelMsg('')", 10000);  // clear after 10 sec
        } else {
            switchToViewMode();
        }
        getEditTxtFrame().docmaAfterSave();

        var main_win;
        if (isWin) {
            main_win = (typeof opener.processDeferredDocmaEvents == 'undefined') ? opener.parent : opener;
        } else {
            main_win = parent;
        }
        main_win.processDeferredDocmaEvents();  // update modification date in product tree
        if (isWin) {
            // If same file is currently opened in preview frame, reload the file to show the new content
            var p_url = main_win.frames['viewcontentfrm'].location.href;
            var node_pattern = "nodeid=<%= nodeid %>";
            if (p_url.indexOf(node_pattern) >= 0) {
              if (p_url.indexOf('viewFile.jsp') >= 0) {
                  main_win.frames['viewcontentfrm'].location.replace('<%= preview_url %>');
              } else {
                  main_win.previewContentRefresh();
              }
            }
        }
        
        if (reselectAfterSave) {
            reselectAfterSave = false;
            main_win.reselectTreeNode();
        }
        
        if (closeAfterSave) {
            doClose();
        }
    }

    function switchToViewMode() {
        getEditTxtFrame().docmaEnterView();
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
        document.getElementById("commentBox").className = 'comment';
    }

    function changeEncoding() {
        if (currentStatus == 'view') {
            document.forms["btnform"].action.value = 'change_encoding';
            document.forms["btnform"].submit();
        }
    }

    function openInNewWin() {
        <%= (js_open_viewer == null) ? "" : js_open_viewer  %>
    }

    function getEditTxtFrame() {
        return window.frames['edit_txt_frame'];
    }
</script>
</head>
<body style="background:#E0E0E0; font-family:Arial,sans-serif; margin:0 3px 0 0; padding:0; width:100%; max-width:100%; height:100%; overflow:hidden;">
<%
    if (! (isTextFile || node instanceof PubContent)) {
        if (isFile) {
            String no_preview = webSess.getLabel("texteditor.no_file_preview_available", ((FileContent) node).getFileName());
            String preview_hint = webSess.getLabel("texteditor.no_file_preview_hint", ext);
            out.println("<div class=\"docma_msg\"><b>" + no_preview +
                        "</b><br /><br /><i class=\"hint_msg\">" + preview_hint + "</i></div>");
        } else {
            String no_preview = webSess.getLabel("texteditor.no_node_preview_available", node.getId());
            out.println("<div class=\"docma_msg\"><b>" + no_preview + "</div>");
        }
    } else {
        String fn = isFile ? ((FileContent) node).getFileName() : "";
        String charsetName = ((Content) node).getCharset();
        if (charsetName == null) {
            String alias = node.getAlias();
            if (((alias != null) && alias.equals("gentext")) || fn.startsWith("gentext")) {
                charsetName = "ISO-8859-1";
            } else {
                charsetName = "UTF-8";
            }
        }
        
        String displayTitle;
        if (fn.equals("")) {
            displayTitle = "<b>" + webSess.getLabel("texteditor.node_id_label") + ":</b>&nbsp;" + node.getId();
            if (node instanceof PubContent) {
                displayTitle += " <b>" + webSess.getLabel("label.node.title") + ":</b>&nbsp;" + 
                                ((PubContent) node).getTitleEntityEncoded();
            }
        } else {
            displayTitle = fn;
        }
        String editComment = webSess.getLabel("texteditor.start_edit_comment");
        String editLabel = webSess.getLabel("texteditor.edit.btn");
        String saveLabel = webSess.getLabel("texteditor.save.btn");
        String saveCloseLabel = webSess.getLabel("texteditor.save_close.btn").replace("&", "&amp;").replace(" ", "&#160;");
        String closeLabel = webSess.getLabel("texteditor.close.btn");
        String openWinLabel = webSess.getLabel("texteditor.open_in_win.btn");
        String cancelLabel = webSess.getLabel("texteditor.cancel.btn");
        String encodingLabel = webSess.getLabel("texteditor.encoding");
%>
<form name="btnform" action="<%= self_url %>" method="post" style="padding:0; margin:0; width:100%; height:100%;">
<table border="0" cellspacing="0" cellpadding="0" style="width:100%; height:100%;">
<tr>
    <td height="28" valign="middle" style="padding:4px 5px 4px 5px; border-bottom:1px solid #D0D0D0;">
        <div id="filenameBox"><%= displayTitle %></div>
        <div id="commentBox" class="commentHidden"><%= editComment %></div>
    </td>
</tr>
<tr>
    <td style="padding:0 0 1px 0; width:100%;">
        <iframe name="edit_txt_frame" src="<%= content_url %>" width="100%" height="100%" style="border-width:1px;"></iframe>
    </td>
</tr>
<tr>
  <td height="40" valign="middle" style="padding:5px; border-top:1px solid #D0D0D0;">
    <table border="0" width="100%" cellspacing="0" cellpadding="0">
      <tr>
          <td valign="middle" nowrap>
            <button type="button" name="editBtn" onclick="doEdit();"
                    style="vertical-align:middle; height:28px;">
                <img src="img/editfile.gif" style="vertical-align:middle;" />&nbsp;<%= editLabel %>
            </button>
            <button type="button" name="saveBtn" onclick="doSave();" 
                    style="display:none; vertical-align:middle; height:28px;">
                <img src="img/save.gif" style="vertical-align:middle;" />&nbsp;<%= saveLabel %>
            </button>
<%
    if (iswin) {
%>
            <button type="button" name="saveCloseBtn" onclick="doSaveAndClose();" 
                    style="display:none; vertical-align:middle; height:28px;">
                <img src="img/save_and_close.gif" style="vertical-align:middle;" />&nbsp;<%= saveCloseLabel %>
            </button>
            &nbsp;
            <button type="button" name="closeBtn" onclick="doClose();"
                    style="vertical-align:middle; height:28px;">
                <img src="img/close.gif" style="vertical-align:middle;" />&nbsp;<%= closeLabel %>
            </button>
<%
    } else {
%>
            &nbsp;
            <button type="button" name="openWinBtn" onclick="openInNewWin();"
                    style="vertical-align:middle; height:28px;">
                <img src="img/open_win.gif" style="vertical-align:middle;" />&nbsp;<%= openWinLabel %></button>
            <button type="button" name="cancelBtn" onclick="cancelEdit();"
                    style="display:none; vertical-align:middle; height:28px;">
                <img src="img/cancel.gif" style="vertical-align:middle;" />&nbsp;<%= cancelLabel %>
            </button>
<%
    }
%>
            &nbsp;
            <span id="saveLabel" class="labeltxt"></span>
            <iframe name="filesave_frm" src="empty_preview.html" width="4" height="20"
                    scrolling="no" marginheight="0" marginwidth="0" frameborder="0"></iframe>
          </td>
          <td align="right" valign="middle" nowrap>
            <table border="0" cellspacing="0" cellpadding="0">
              <tr><% 
    String plug_toolbar = TextFileHandler.getHTMLToolbar(webSess, ext);
    if ((plug_toolbar != null) && !plug_toolbar.equals("")) {
        out.print("<td valign=\"middle\"><div id=\"docmaPlugToolbar\">");
        out.print(plug_toolbar);
        out.println("</div></td>");
    }
%>
                <td valign="middle"><span class="labeltxt"><%= encodingLabel %>&nbsp;</span></td>
                <td valign="middle">
    <select name="file_encoding" size="1" onchange="changeEncoding();">
<%
    String[] charsets;
    if (node instanceof PubContent) {
        charsets = new String[] { "UTF-8" };
    } else {
        charsets = new String[] { "ISO-8859-1", "US-ASCII", "UTF-8",
                                  "UTF-16BE", "UTF-16LE", "UTF-16" };
    }
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
