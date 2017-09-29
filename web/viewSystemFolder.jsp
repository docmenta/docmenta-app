<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.io.*,org.docma.util.*,org.docma.webapp.*,org.docma.app.*,org.zkoss.zul.*,org.zkoss.zkplus.embed.*"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    
    String lang = docmaSess.getTranslationMode();
    boolean isTransMode = (lang != null);
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
<title>Folder</title>
<style type="text/css">
body {background: #F4F4F4; margin:0px 15px 8px 15px;}
form {padding:0; margin:0;}
tr.row_even { background-color:#E8E8E8; }
tr.row_normal { background-color:#F4F4F4; }
tr.row_selected { background-color:#CFEAFA; }
tr.row_mover { background-color:#FCFCFC; }
th { font-family: Verdana, sans-serif; font-weight: bold; font-size: 75%;
     text-align: left; color:#EEEEDD; background-color:#404040; padding-left: 3px;}
td.fecell { font-family: Arial, Verdana, sans-serif; border-width:0 0 1px 0;
            border-style:solid; border-color:#D8D8D8; padding:3px 2px 3px 2px; vertical-align: middle; }
a.fileref { font-family: Arial, Verdana, sans-serif; color: #000000; text-decoration: none; }
a.fileref:hover { color: #0080FF; }
a.blindlink { border-style:none; text-decoration:none; }
div.listpic {width:130px; padding: 5px; margin: 2px; background: white;
  border: 1px solid; border-color: #AAA #444 #444 #AAA; text-align:center; }
div.listpic img {border: 1px solid; border-color: #444 #AAA #AAA #444; }
div.folderpath { font-family: Verdana, sans-serif; font-weight: bold;
                 font-size: 80%; color: #404040; margin:0; max-height:36px; }
a.fpath { font-family: Verdana, sans-serif; font-weight: bold; color: #404040;
          text-decoration: none; }
a.fpath:hover { color: #0080FF; }
table.toolbar { width:100%; margin-bottom:4px; position:fixed; top:0px; left:0px;
                background-color:#F4F4F4; padding:10px 15px 4px 15px; }
.summary { font: 75% Verdana, sans-serif; }
</style>
<%@include file="viewfolder_js.jspf"%>
</head>
<body onload="initList();">
<%
    if (node.isFolder()) {
        boolean isImgFolder = node.isImageFolder();
        boolean isGalleryNormal = false;
        boolean isGalleryBig = false;
        boolean isListNormal = true; 
        boolean isListBig = false; 
%>
       <%@include file="viewfolder_bar.jspf"%>
<% 
        String serv_url = "servmedia/" + docmaSess.getSessionId() + "/" +
                      docmaSess.getStoreId() + "/" + docmaSess.getVersionId() +
                      "/" + docmaSess.getLanguageCode();

        int file_cnt = 0;
        long total_size = 0;
        boolean has_image = false;
        // long timestamp = System.currentTimeMillis();
        int child_count = node.getChildCount();
        String[] node_id = new String[child_count];
        String[] node_icon = new String[child_count];
        String[] open_url = new String[child_count];
        String[] target = new String[child_count];
        String[] node_filename = new String[child_count];
        String[] cont_size_str = new String[child_count];
        String[] thumb_url = new String[child_count];
        String[] img_url = new String[child_count];
        for (int i=0; i < child_count; i++) {
            DocmaNode childnode = node.getChild(i);
            node_id[i] = childnode.getId();
            boolean is_file = childnode.isFileContent();
            boolean is_image = childnode.isImageContent();
            node_icon[i] = GUIUtil.getNodeIconPath(childnode);
            thumb_url[i] = null;
            img_url[i] = null;
            if (is_file || is_image) {
                file_cnt++;
                node_filename[i] = childnode.getDefaultFileName();
                java.util.Date lastmoddate = childnode.getLastModifiedDate();
                long lastmod = (lastmoddate != null) ? lastmoddate.getTime() : System.currentTimeMillis();
                open_url[i] = response.encodeURL(serv_url + "/download/" +
                                              node_id[i] + "/" +
                                              lastmod + "/" + node_filename[i]);
                target[i] = "target=\"_blank\"";
                long cont_size = childnode.getContentLength();
                cont_size_str[i] = DocmaUtil.formatByteSize(cont_size);
                total_size += cont_size;
                if (is_image) {
                    has_image = true;
                    thumb_url[i] = response.encodeURL("thumb.jsp?desk=" + deskid +
                                                      "&nodeid=" + node_id[i] +
                                                      "&lastmod=" + lastmod);
                    img_url[i] = response.encodeURL("image.jsp?desk=" + deskid +
                                                     "&nodeid=" + node_id[i] +
                                                     "&lastmod=" + lastmod);
                }
            } else
            if (childnode.isContent()) {  // other content
                node_filename[i] = childnode.getTitleEntityEncoded();
                open_url[i] = "javascript:previewNode('" + node_id[i] + "');";
                target[i] = "";
                cont_size_str[i] = "-";
            } else
            if (childnode.isFolder()) {
                node_filename[i] = childnode.getTitleEntityEncoded();
                open_url[i] = "javascript:previewNode('" + node_id[i] + "');";
                target[i] = "";
                cont_size_str[i] = "-";
            }
        }  // for-loop
%>
  <form name="filelistform" style="margin:45px 0 0 0; padding:0;">
  <table border="0" cellspacing="0" width="100%" style="width:100%; margin:45px 0 0 0;">
      <tr oncontextmenu="return showDocmaContext(event);">
          <th colspan="3">Filename</th>
          <th width="100">Size</th>
          <% if (has_image) out.print("<th width=\"130\">Preview</th>"); %>
      </tr>
<%
        for (int i=0; i < child_count; i++) {
            String row_cls = "row_normal"; // ((cnt % 2) == 0) ? "row_even" : "row_odd";
            String child_id = node_id[i];
            String pos_id = "pos_" + child_id;
            if (node_filename[i] != null) {
%>
    <tr class="<%= row_cls %>" id="node_<%= child_id %>" ondblclick="dblClickRow('<%= child_id %>')"
        onmouseover="enterRow(this)" onmouseout="leaveRow(this, true)" oncontextmenu="return showDocmaNodeContext('<%= child_id %>', event);">
      <td class="fecell" width="20"><input type="checkbox" name="selbox" value="<%= child_id %>"
                onclick="return changeSelection('<%= child_id %>', event);"/></td>
      <td class="fecell" width="20" onclick="selectClick('<%= child_id %>', event);"><img src="<%= node_icon[i] %>" width="18" height="18" title="" alt="" /></td>
      <td class="fecell" width="80%" onclick="selectClick('<%= child_id %>', event);"><a class="fileref"
          href="<%= open_url[i] %>" <%= target[i] %> ><%= node_filename[i] %></a>&nbsp;<a class="blindlink"
          href="javascript:editProps('<%= child_id %>');"><img src="img/edit_props.gif" height="17" border="0"
          style="vertical-align:text-bottom;" title="Edit properties" 
          alt="Edit properties" />&nbsp;</a><a name="<%= pos_id %>" id="<%= pos_id %>"></a>
      </td>
      <td class="fecell" nowrap="nowrap" onclick="selectClick('<%= child_id %>', event);"><%= cont_size_str[i] %></td>
<% 
            if (has_image) {
%>
      <td class="fecell" width="130" align="center">
<% 
                if (thumb_url[i] != null) {
%>
        <div class="listpic"><a href="<%= img_url[i] %>" target="_blank"><img src="<%= thumb_url[i] %>" /></a></div>
<% 
                }
%>
      </td>
<% 
           }
%>
    </tr>
<%
            }
        }  // for-loop
%>
  </table>
  <div width="100%" oncontextmenu="return showDocmaContext(event);" style="text-align:left;" >
    <table style="clear: both; margin-top: 0.8em;">
      <tr>
          <td class="summary" align="right">Number of Files:</td>
          <td class="summary" style="padding-left:3px;"><%= file_cnt %></td>
      </tr>
      <tr>
          <td class="summary" align="right">Total size:</td>
          <td class="summary" style="padding-left:3px;"><%= DocmaUtil.formatByteSize(total_size) %></td>
      </tr>
    </table>
  </div>
<%
    }
%>
</form>
<%@include file="viewfolder_comments.jspf"%>
<%
    // Write ZK context menu
    StringWriter zk_buf = new StringWriter();
    Div ctxdiv = new Div();
    ctxdiv.appendChild(contextMenu);
    Renders.render(pageContext.getServletContext(), request, response, ctxdiv, null, zk_buf);
    out.write(zk_buf.toString());
%>
</body>
</html>
