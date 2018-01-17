<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.io.*,org.docma.util.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*,org.zkoss.zul.*,org.zkoss.zkplus.embed.*"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String viewmode = request.getParameter("view");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    String lang = docmaSess.getTranslationMode();
    boolean isTransMode = (lang != null);

    String userview = docmaSess.getUserProperty(GUIConstants.PROP_USER_IMAGE_PREVIEW_MODE);
    if ((userview == null) || userview.equals("")) userview = GUIConstants.IMAGE_PREVIEW_MODE_LIST;
    if ((viewmode == null) || viewmode.equals("")) viewmode = userview;
    if (! viewmode.equals(userview)) {
        docmaSess.setUserProperty(GUIConstants.PROP_USER_IMAGE_PREVIEW_MODE, viewmode);
        userview = viewmode;
    }
    boolean isGalleryNormal = viewmode.equals(GUIConstants.IMAGE_PREVIEW_MODE_GALLERY);
    boolean isGalleryBig    = viewmode.equals(GUIConstants.IMAGE_PREVIEW_MODE_GALLERY_BIG);
    boolean isListNormal    = viewmode.equals(GUIConstants.IMAGE_PREVIEW_MODE_LIST);
    boolean isListBig       = viewmode.equals(GUIConstants.IMAGE_PREVIEW_MODE_LIST_BIG);
    boolean isGallery = isGalleryNormal || isGalleryBig;
    boolean isList = isListNormal || isListBig;
    // boolean isBig = isGalleryBig || isListBig;
    ThumbDimensions thumbDim = ThumbDimensions.getDimensionsForViewMode(viewmode);

    String self_url = response.encodeURL("viewImageFolder.jsp?desk=" + deskid +
                                         "&nodeid=" + nodeid +
                                         "&stamp=" + System.currentTimeMillis());

    Menupopup contextMenu = MenuUtil.createFileViewContextMenu(docmaWebSess);
    String contextMenuId = contextMenu.getId();
    int contextItemCount = contextMenu.getChildren().size();

    DocmaNode node = docmaSess.getNodeById(nodeid);
    DocI18n i18n = docmaSess.getI18n();
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="expires" content="0" />
<meta http-equiv="cache-control" content="no-cache" />
<meta http-equiv="pragma" content="no-cache" />
<title>Media folder</title>
<style type="text/css">
body {background: #EAEAEA; margin:0px 15px 8px 15px; }
form {padding:0; margin:0;}
tr.row_even { background-color:#E8E8E8; }
tr.row_normal { background-color:#EAEAEA; }
tr.row_selected { background-color:#CFEAFA; }
tr.row_mover { background-color:#F4F4F4; }
th { font-family: Verdana, sans-serif; font-weight: bold; font-size: 75%;
     text-align: left; color:#EEEEDD; background-color:#404040; padding:1px 3px 2px 3px;}
td.fecell { font-family: Arial, Verdana, sans-serif; font-size:75%; border-width:0 0 1px 0;
            border-style:solid; border-color:#D8D8D8; padding:2px; vertical-align: middle; }
div#footer {clear: both; padding-top: 1.5em; font: 75% Verdana, sans-serif;}
div.pic {float: left; height: <%= thumbDim.getBoxHeight() %>px; width: <%= thumbDim.getBoxWidth() %>px;
  padding: 10px; margin: 5px 3px; background: white;
  border: 1px solid; border-color: #AAA #444 #444 #AAA;}
div.listpic {width:<%= thumbDim.getBoxWidth() %>px; padding: 5px; margin: 2px; background: white;
  border: 1px solid; border-color: #AAA #444 #444 #AAA; text-align:center; }
div.listpic img {border: 1px solid; border-color: #444 #AAA #AAA #444; }
div.viewport img {border: 1px solid; border-color: #444 #AAA #AAA #444; margin:6px 0 0 0;}
div.viewport { width:<%= thumbDim.getSize() %>px; height:<%= thumbDim.getSize() %>px; margin:0; padding:0; }
div.pic ul {margin: 0 0 0 0; padding: 0; font: small Arial, Verdana, sans-serif;}
div.pic li {display: block; text-align: center;}
div.pic li.title {font-weight: bold; margin:0; padding:0; }
div.pic li.imgsize {text-align: center; margin: 0 0 0 0; padding: 0 0.5em 0 0;}
div.pic li.imgapplic {text-align: center; font-style: italic; margin: 0 0 0 0; padding: 0 0 0 0.5em; }
div.picfoot { text-align:center; padding:3px; }
div.folderpath { font-family: Verdana, sans-serif; font-weight: bold;
                 font-size: 80%; color: #404040; margin:0; max-height:36px; }
a.blindlink { border-style:none; text-decoration:none; }
a.fileref { font-family: Arial, Verdana, sans-serif; font-weight:bold; color: #000000; text-decoration: none; }
a.fileref:hover { color: #0080FF; }
a.fpath { font-family: Verdana, sans-serif; font-weight: bold; color: #404040;
          text-decoration: none; }
a.fpath:hover { color: #0080FF; }
table.toolbar { width:100%; margin-bottom:4px; position:fixed; top:0px; left:0px;
                background-color:#EAEAEA; padding:10px 15px 4px 15px; }
.imgtitle { font-style:italic; color:#404040; }
.summary { font: 75% Verdana, sans-serif; }
</style>
<%@include file="viewfolder_js.jspf"%>
<script type="text/javascript">
  function changeViewMode() {
      var selbox = document.formpreviewmode.previewmodebox;
      var selmode = selbox.options[selbox.selectedIndex].value;
      var urlstr = '<%= self_url %>' + '&view=' + selmode;
      window.location.href = urlstr;
  }
</script>
</head>
<body onload="initList();">
<%
    boolean isImgFolder = node.isImageFolder();
    if (isImgFolder) {
%>
      <%@include file="viewfolder_bar.jspf"%>

      <form name="filelistform" style="margin:45px 0 0 0; padding:0;">
<%
        if (isList) {
            String head_fn_title = i18n.getLabel("folder.column_header.filename") + " / " + 
                                   i18n.getLabel("folder.column_header.title");
%>
          <table border="0" cellspacing="2" cellpadding="0" width="100%" style="width:100%;">
              <tr oncontextmenu="return showDocmaContext(event);">
                  <th width="*" colspan="2"><%= head_fn_title %></th>
                  <% 
                      if (isTransMode) { 
                          out.print("<th width=\"20\">");
                          out.print(i18n.getLabel("folder.column_header.lang"));
                          out.print("</th>");
                      }
                  %>
                  <th><%= i18n.getLabel("folder.column_header.alias") %></th>
                  <th width="50"><%= i18n.getLabel("folder.column_header.filesize") %></th>
                  <th width="<%= thumbDim.getBoxWidth() %>"><%= i18n.getLabel("folder.column_header.preview") %></th>
              </tr>
<%
        }

        String serv_url = docmaWebSess.getServMediaBasePath();
        int cnt = 0;
        long total_size = 0;
        int child_count = node.getChildCount();
        for (int i=0; i < child_count; i++) {
            WebFolderEntry fe = new WebFolderEntry(node.getChild(i), deskid, serv_url);
            String row_cls = "row_normal"; // ((cnt % 2) == 0) ? "row_even" : "row_odd";
            String child_id = fe.getNodeId();
            String pos_id = "pos_" + child_id;
            if (fe.isImage() || fe.isFile()) {
                cnt++;
                total_size += fe.getFileSize();

                String download_url = response.encodeURL(fe.getDownloadURL());
                String preview_tag = "";
                String img_title = "";
                if (fe.isImage()) {
                    String thumb_url = response.encodeURL(fe.getThumbURL(thumbDim));
                    String img_url = response.encodeURL(fe.getImgURL());
                    img_title = fe.getTitleEntityEncoded();
                    String titleAtt = img_title.equals("") ? 
                        "" : ("title=\"" + img_title.replace('"', ' ').replace('\\', ' ') + "\" ");
                    preview_tag = "<a href=\"" + img_url + 
                                  "\" target=\"_blank\"><img src=\"" + thumb_url + 
                                  "\" " + titleAtt + "/></a>";
                } else if (fe.isVideo()) {
                    preview_tag = "<video src=\"" + download_url + "\" width=\"" + 
                                  thumbDim.getSize() + 
                                  "\" preload=\"metadata\" controls=\"controls\" ></video>";
                }
                String langstr = null;
                if (isTransMode) {
                    langstr = fe.isTranslated() ? lang.toUpperCase() : "-";
                }

                if (isList) {
                    String title_tag = "";
                    if (! img_title.equals("")) {
                        title_tag = "<span class=\"imgtitle\">" + img_title + "</span>";
                    }
%>
<tr class="<%= row_cls %>" id="node_<%= child_id %>" ondblclick="dblClickRow('<%= child_id %>')"
    onmouseover="enterRow(this)" onmouseout="leaveRow(this, true)" oncontextmenu="return showDocmaNodeContext('<%= child_id %>', event);">
    <td class="fecell" width="20"><input type="checkbox" name="selbox" value="<%= child_id %>"
        onclick="return changeSelection('<%= child_id %>', event);"/></td>
    <td class="fecell" onclick="selectClick('<%= child_id %>', event);"><a class="fileref" 
        href="<%= download_url %>"><%= fe.getFileName() %></a>&nbsp;<a class="blindlink"
        href="javascript:editProps('<%= child_id %>');"><img src="img/edit_props.gif" height="17" border="0"
        style="vertical-align:text-bottom;" title="Edit properties" alt="Edit properties" />&nbsp;</a><br /><%= title_tag %></td>
    <%
        if (isTransMode) {
            out.print("<td class=\"fecell\">" + langstr + "</td>");
        }
    %>
    <td class="fecell" onclick="selectClick('<%= child_id %>', event);"><%= fe.getAlias() %></td>
    <td class="fecell" onclick="selectClick('<%= child_id %>', event);"><%= fe.getFileSizeFormatted() %></td>
    <td class="fecell" align="center">
        <div class="listpic"><a name="<%= pos_id %>" id="<%= pos_id %>"></a><%= preview_tag %></div>
    </td>
</tr>
<%
                } else {   // Gallery mode
%>
<div class="pic" id="node_<%= child_id %>" ondblclick="dblClickRow('<%= child_id %>')"
     oncontextmenu="return showDocmaNodeContext('<%= child_id %>', event);"><ul>
<li class="title"><a href="<%= download_url %>" class="fileref"><%= fe.getFileName() %></a></li>
</ul><div class="viewport" align="center"><a name="<%= pos_id %>" id="<%= pos_id %>"></a><%= preview_tag %></div>
    <div class="picfoot"><input type="checkbox" name="selbox" value="<%= child_id %>"
        onclick="return changeSelection('<%= child_id %>', event);" /></div><%-- <ul>
<li class="imgsize"><%= img_size_str %></li>
<li class="imgapplic"><%= applic %></li>
</ul> --%></div>
<%
                }
            }
        }  // for-loop
        if (isList) {
%>
          </table>
<%
        }
%>
  </form>
  <div width="100%" oncontextmenu="return showDocmaContext(event);" style="text-align:left;" >
    <table style="clear: both; margin-top: 0.8em;">
      <tr>
          <td class="summary" align="right"><%= i18n.getLabel("folder.number_of_files") %>:</td>
          <td class="summary" style="padding-left:3px;"><%= cnt %></td>
      </tr>
      <tr>
          <td class="summary" align="right"><%= i18n.getLabel("folder.total_filesize") %>:</td>
          <td class="summary" style="padding-left:3px;"><%= DocmaUtil.formatByteSize(total_size) %></td>
      </tr>
    </table>
  </div>
<%@include file="viewfolder_comments.jspf"%>
<%
    }

    // Write ZK context menu
    StringWriter zk_buf = new StringWriter();
    Div ctxdiv = new Div();
    ctxdiv.appendChild(contextMenu);
    Renders.render(pageContext.getServletContext(), request, response, ctxdiv, null, zk_buf);
    out.write(zk_buf.toString());
%>
</body>
</html>
