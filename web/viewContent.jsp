<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="java.util.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*"
%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String searchTerm = request.getParameter("search");
    String searchCase = request.getParameter("ignorecase");
    String inc_struct = request.getParameter("incstruct");
    String inc_inline = request.getParameter("incinline");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    MainWindow mainWin = docmaWebSess.getMainWindow();
    DocmaPublicationConfig previewPubConf = mainWin.getPreviewPubConfig();
    DocmaOutputConfig previewOutConf = mainWin.getPreviewOutputConfig();
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    StringBuffer req_url = request.getRequestURL();
    req_url.setLength(req_url.lastIndexOf("/") + 1);
    String base_url = req_url + "servmedia/" + docmaSess.getSessionId() + "/" +
                      docmaSess.getStoreId() + "/" + docmaSess.getVersionId() +
                      "/" + docmaSess.getLanguageCode() + "/";
    String css_url = response.encodeURL("css/content.css?desk=" + deskid +
                                        "&expire=" + System.currentTimeMillis());
    String serv_context = request.getContextPath();
    if (! serv_context.endsWith("/")) serv_context += "/";

    boolean mode_search = (searchTerm != null) && (searchTerm.trim().length() > 0);
    boolean ignoreCase = (searchCase != null) && searchCase.equalsIgnoreCase("true");
    boolean resolveStruct = (inc_struct != null) && inc_struct.equalsIgnoreCase("true");
    boolean resolveInline = (inc_inline != null) && inc_inline.equalsIgnoreCase("true");

    String path_opened = docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_PATH_OPENED);
    boolean is_path_opened = (path_opened == null) || path_opened.equals("") ||
                              path_opened.equals("true");
    final String NOPATH_TOP_OFFSET = "14px";  // body top-margin for closed path bar
    final String PATH_TOP_OFFSET = "36px";    // body top-margin for opened path bar

    DocmaNode node = docmaSess.getNodeById(nodeid);
    
    String toggle_icon = req_url + (is_path_opened ? "img/navpath_opened.png" : "img/navpath_closed.png");
    String bar_display = is_path_opened ? "display:block;" : "display:none;";

    // Calculate node path
    StringBuilder path = new StringBuilder();
    DocmaNode n = node;
    boolean first_node = true;
    while ((n != null) && (! n.isDocumentRoot()) && (! n.isRoot())) {
        if (first_node) {
            path.append(n.getTitle());
            first_node = false;
        } else {
            path.insert(0, "<a href=\"javascript:parent.previewNodeById('" + n.getId() +
                           "');\" class=\"docma_path_link\">" + n.getTitle() + "</a> &rarr; ");
        }
        n = n.getParent();
    }

    StringBuilder buf = new StringBuilder();
    DocmaSearchResult searchResult = null;
    try {
        if (mode_search) {
            searchResult = new DocmaSearchResult();
            searchResult.setFinished(false);
            docmaWebSess.setSearchResult(searchResult);
            DocmaAppUtil.getSearchReplacePreview(buf, node, docmaSess, previewPubConf,
                                                 previewOutConf, response, serv_context,
                                                 deskid, searchTerm, ignoreCase,
                                                 resolveStruct, resolveInline, searchResult);
            searchResult.setFinished(true);
        } else {
            DocmaAppUtil.getEditPreview(buf, node, docmaSess, previewPubConf, previewOutConf,
                                        response, serv_context, deskid);
        }
    } catch (DocmaLimitException dle) {
        buf.setLength(0);
        buf.append("<div style=\"background-color:#CC0000; color:#F0F0F0; font-size:11pt; font-weight:bold; padding:3px;\">");
        buf.append("Preview failed. ").append(dle.getLocalizedMessage());
        buf.append("</div>");
    }
%>
<title>View content</title>
<base href="<%= base_url %>">
<link rel="stylesheet" type="text/css" href="<%= css_url %>">
<style type="text/css">
.docma_pathbtn_cls { position:fixed; left:0px; top:0px; width:10px; padding:0; margin:0; }
.docma_pathbar_cls { position:fixed; left:10px; top:0px; right:0px; padding:4px 0px 3px 6px; 
                     margin:0; font-family: Verdana, sans-serif; font-weight: bold;
                     font-size: 11px; color: #404040; background-color:#FFFFFF; 
                     max-height:36px; overflow-x:hidden; overflow-y:auto; }
a.docma_path_link { font-family: Verdana, sans-serif; font-weight: bold; color: #1E66B0; text-decoration:underline; }
a.docma_path_link:hover { color:#0080FF; }
<%
    if (mode_search) {
%>
.docma-match { background-color:yellow; }
.docma-dupmatch { background-color:#E0E0E0; border: 1px solid yellow; }
<%
    }
%>
</style>
<script type="text/javascript">

  function init_content() {
    for (var i=0; i < document.links.length; i++ ) {
      var loc = document.links[i];
      var ha = loc.hash;
      if ((ha != null) && (ha.length > 0)) {
        var hr = loc.href;
        if ((hr == ha) || (loc.host == window.location.host)) {
          // loc.href = "javascript:jumpToHash('" + ha + "')";
          loc.onclick = docma_jmpHash;
        } else {
          formatExternalLink(loc);
        }
      } else {
        formatExternalLink(loc);
      }
    }
  }

  function formatExternalLink(loc) {
    if (loc.className != "docma_path_link") {
        loc.target = "_blank";
        loc.className = "link_external";
    }
  }

  function jumpToHash(anch_pos) {
      var elem_id = anch_pos;
      if (elem_id.charAt(0) == "#") {
        elem_id = elem_id.substring(1);
      }
      var elem = document.getElementById(elem_id);
      if (elem != null) {
        window.location.hash = anch_pos;
      } else {
        parent.previewNodeByAlias(elem_id);
      }
  }

  function docma_jmpHash() {
      var hr = this.href;
      var pos = hr.indexOf("#");
      if (pos < 0) return true;  // do nothing; follow href
      var ha = hr.substring(pos);
      jumpToHash(ha);
      return false;  // supress default click behaviour; do not follow href
  }

  function togglePathBar() {
    var elem = document.getElementById("docma-pathbar");
    switchPathBar(elem, elem.style.display != 'none');
  }

  function switchPathBar(elem, is_on) {
    var b = document.getElementById("docma-previewbody");
    var im = document.getElementById("docma-pathbtn-img");
    if (is_on) {
      elem.style.display = "none";
      b.style.marginTop = "<%= NOPATH_TOP_OFFSET %>";
      im.src = "<%= req_url %>img/navpath_closed.png";
    } else {
      elem.style.display = "block";
      b.style.marginTop = "<%= PATH_TOP_OFFSET %>";
      im.src = "<%= req_url %>img/navpath_opened.png";
    }
    parent.sendMainWindowEvent("onTogglePreviewPathBar", is_on ? "false" : "true");
  }

<%
    if (mode_search) {
%>
  var match_arr = [ <%
      for (int i=0; i < searchResult.size(); i++) {
          SearchMatch match = (SearchMatch) searchResult.get(i);
          if (i > 0) out.print(", ");
          out.print("\"" + match.getMatchId() + "\"");
      }
  %> ];

  var current_match = -1;

  function setMatch(match_idx) {
      if (match_idx < match_arr.length) {
          if (current_match >= 0) {
              var old_elem = document.getElementById(match_arr[current_match]);
              old_elem.style.backgroundColor = "yellow";
          }
          current_match = match_idx;
          var elem = document.getElementById(match_arr[match_idx]);
          elem.style.backgroundColor = "red";
          window.location.hash = "#" + match_arr[match_idx];
      }
  }

  function replaceWith(replaceTerm, match_idx) {
      var elem = document.getElementById(match_arr[match_idx]);
      var replace_node = document.createTextNode(replaceTerm);
      var child_cnt = elem.childNodes.length;
      for (var i=child_cnt-1; i >= 0; i--) {
          elem.removeChild(elem.childNodes[i]);
      }
      elem.appendChild(replace_node);
  }

  function replaceAll(replaceTerm, idx_arr) {
      for (var i=0; i < idx_arr.length; i++) {
          var idx = idx_arr[i];
          replaceWith(replaceTerm, idx);
      }
  }

<%
    }  // mode_search

    boolean has_path = (path.length() > 0);
    String init_search = mode_search ? "setMatch(0);" : "";
    String body_style = "margin:" + ((has_path && is_path_opened) ? PATH_TOP_OFFSET : NOPATH_TOP_OFFSET) + 
                        " 10px 10px 12px; padding:0;";
%>
</script>
</head>
<body id="docma-previewbody" style="<%= body_style %>">
<% 
  if (has_path) {
%>
<div class="docma_pathbtn_cls" ><img id="docma-pathbtn-img" src="<%= toggle_icon %>" height="24" width="10" onclick="togglePathBar();"></div>
<div class="docma_pathbar_cls" id="docma-pathbar" style="<%= bar_display %>"><%= path.toString() %></div>
<% 
  }
%>
<%= buf.toString() %>
<script type="text/javascript"><%= init_search %> init_content();</script>
</body>
</html>
