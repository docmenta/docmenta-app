<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.io.*,org.docma.util.*,org.docma.webapp.*,org.docma.app.*,org.zkoss.zul.*,org.zkoss.zkplus.embed.*"
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

    String serv_url = "servmedia/" + docmaSess.getSessionId() + "/" +
                      docmaSess.getStoreId() + "/" + docmaSess.getVersionId() +
                      "/" + docmaSess.getLanguageCode();

    DocmaNode node = docmaSess.getNodeById(nodeid);

    Menupopup contextMenu = MenuUtil.createFileViewContextMenu(docmaWebSess);
    String contextMenuId = contextMenu.getId();
    int contextItemCount = contextMenu.getChildren().size();
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="expires" content="0" />
<meta http-equiv="cache-control" content="no-cache" />
<meta http-equiv="pragma" content="no-cache" />
<title>Image folder</title>
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
%>
          <table border="0" cellspacing="2" cellpadding="0" width="100%" style="width:100%;">
              <tr oncontextmenu="return showDocmaContext(event);">
                  <th width="*" colspan="2">Alias / Title</th>
                  <% if (isTransMode) out.print("<th width=\"20\">Lang</th>"); %>
                  <th width="30">Format</th>
                  <th width="50">Size</th>
                  <th width="<%= thumbDim.getBoxWidth() %>">Preview</th>
              </tr>
<%
        }
        int cnt = 0;
        long total_size = 0;
        for (int i=0; i < node.getChildCount(); i++) {
            DocmaNode imgnode = node.getChild(i);
            String child_id = imgnode.getId();
            String pos_id = "pos_" + child_id;
            if (imgnode.isImageContent()) {
                cnt++;
                long img_size = imgnode.getContentLength();
                String img_size_str = DocmaUtil.formatByteSize(img_size);
                total_size += img_size;
                java.util.Date lastmoddate = imgnode.getLastModifiedDate();
                long lastmod = (lastmoddate != null) ? lastmoddate.getTime() : System.currentTimeMillis();
                String alias = imgnode.getAlias();
                String applic = imgnode.getApplicability();
                String title = imgnode.getTitleEntityEncoded();
                if (title == null) title = "";
                String cont_filename = imgnode.getDefaultFileName();
                String thumb_url = response.encodeURL("thumb.jsp?desk=" + deskid +
                                                      "&nodeid=" + child_id +
                                                      "&lastmod=" + lastmod + 
                                                      "&size=" + thumbDim.getSize());
                String open_url = response.encodeURL("image.jsp?desk=" + deskid +
                                                     "&nodeid=" + child_id +
                                                     "&lastmod=" + lastmod);
                String download_url = response.encodeURL(serv_url + "/download/" +
                                                         child_id + "/" +
                                                         lastmod + "/" +
                                                         cont_filename);
                if (isList) {
                    String format = imgnode.getFileExtension();
                    if (format == null) format = "";
                    if (format.startsWith(".")) format = format.substring(1);
                    format = format.toUpperCase();
%>
<tr class="row_normal" id="node_<%= child_id %>" ondblclick="dblClickRow('<%= child_id %>')"
    onmouseover="enterRow(this)" onmouseout="leaveRow(this, true)" oncontextmenu="return showDocmaNodeContext('<%= child_id %>', event);">
    <td class="fecell" width="20"><input type="checkbox" name="selbox" value="<%= child_id %>"
        onclick="return changeSelection('<%= child_id %>', event);"/></td>
    <td class="fecell" onclick="selectClick('<%= child_id %>', event);"><a class="fileref" 
        href="<%= download_url %>"><%= alias %></a>&nbsp;<a class="blindlink"
        href="javascript:editProps('<%= child_id %>');"><img src="img/edit_props.gif" height="17" border="0"
        style="vertical-align:text-bottom;" title="Edit properties" alt="Edit properties" />&nbsp;</a><br />
    <span class="imgtitle"><%= title %></span></td>
    <%
        if (isTransMode) {
            String langstr = imgnode.isTranslated() ? lang.toUpperCase() : "-";
            out.print("<td class=\"fecell\">" + langstr + "</td>");
        }
    %>
    <td class="fecell" onclick="selectClick('<%= child_id %>', event);"><%= format %></td>
    <td class="fecell" onclick="selectClick('<%= child_id %>', event);"><%= img_size_str %></td>
    <td class="fecell" align="center">
        <div class="listpic"><a name="<%= pos_id %>" id="<%= pos_id %>"></a><a
             href="<%= open_url %>" target="_blank"><img src="<%= thumb_url %>" /></a></div>
    </td>
</tr>
<%
                } else {   // Gallery mode
%>
<div class="pic" id="node_<%= child_id %>" ondblclick="dblClickRow('<%= child_id %>')"
     oncontextmenu="return showDocmaNodeContext('<%= child_id %>', event);"><ul>
<li class="title"><a href="<%= download_url %>" class="fileref"><%= alias %></a></li>
</ul><div class="viewport" align="center"><a name="<%= pos_id %>" id="<%= pos_id %>"></a><a
    href="<%= open_url %>" target="_blank"><img src="<%= thumb_url %>"
    title="<%= title.replace('"', ' ').replace('\\', ' ') %>" /></a></div>
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
          <td class="summary" align="right">Number of Files:</td>
          <td class="summary" style="padding-left:3px;"><%= cnt %></td>
      </tr>
      <tr>
          <td class="summary" align="right">Total size:</td>
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
