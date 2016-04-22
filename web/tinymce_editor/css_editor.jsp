<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page session="true"%>
<%@page import="java.util.*"%>
<%@page import="org.docma.webapp.*"%>
<%@page import="org.docma.app.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"  style="margin:0;padding:0;">
<%
    String deskid = request.getParameter("desk");
    String editorId = request.getParameter("appid");
    String styletype = request.getParameter("styletype");
    String styleid = request.getParameter("styleid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    String css = "";
    DocmaStyle docstyle = null;
    if ((styleid != null) && (styleid.trim().length() > 0)) {
        docstyle = docmaSess.getStyle(styleid);
        css = docstyle.getCSS();
    }
    boolean isInlineStyle = styletype.equalsIgnoreCase(DocmaStyle.INLINE_STYLE);
%>
<head>
<title>CSS Editor</title>
<!-- TinyMCE -->
<script type="text/javascript" src="jscripts/<%= editorId %>/tiny_mce.js"></script>
<script type="text/javascript">
    tinyMCE.init({
        // General options
        mode : "exact", // "none",
        elements : "StyleCSSDiv",
        theme : "advanced",
        plugins : "style",  // ,inlinepopups
        readonly : 1,

        // Theme options
        theme_advanced_buttons1 : "",
        theme_advanced_buttons2 : "",
        theme_advanced_buttons3 : "",
        theme_advanced_buttons4 : "",
        theme_advanced_toolbar_location : "external",
        theme_advanced_toolbar_align : "left",
        theme_advanced_statusbar_location : "none",
        theme_advanced_resizing : false,
        theme_advanced_resizing_min_width : 20,
        theme_advanced_resizing_min_height : 20

        // Example content CSS (should be your site CSS)
        // content_css : "css/content.css",
    });

    var initDone = false;
    var resultCssText = "<%= css %>";

    function initCSSCloseHandler() {
      if (initDone) return;
      tinyMCE.get('StyleCSSDiv').windowManager.onClose.add(function() {unselect();});
      initDone = true;
    }

    function showCSSEditor() {
        showCSSEditor2();
    }

    function showCSSEditor2() {
      <%--
        // if (!tinyMCE.get('StyleCSSDiv')) {
        //     window.setTimeout('showCSSEditor2()', 100);
        //     return;
        // }
        // minimizeEditor();
        // initCSSCloseHandler();
      --%>
        // tinyMCE.execCommand('mceSelectNode',false,document.getElementById("stylesel"));
        tinyMCE.execCommand('mceSelectNode',false,tinyMCE.get('StyleCSSDiv').getDoc().getElementById("stylesel"));
        // tinyMCE.execCommand('mceSelectNode',false,tinyMCE.activeEditor.getDoc().firstChild);
        tinyMCE.execCommand('mceStyleProps');  // Opens CSS dialog; Click on "Update" sets resultCssText
        // unselect();
    }

    function minimizeEditor() {
        tinymce.get('StyleCSSDiv').theme.resizeTo(80,40);
    }

    function getStyleTxt() {
        return resultCssText;  <%-- // return tinyMCE.get('StyleCSSDiv').getDoc().getElementById("stylesel").style.cssText; --%>
    }

    /* Called by editor (tinymce) */
    function updateStyleTxt(csstxt) {
        resultCssText = csstxt;
        if (parent.cssUpdateCallback) {
            parent.cssUpdateCallback(csstxt);
        }
    }
    
    /* Called by Docmenta when user has edited CSS property in dialog. */
    function setStyleTxt(csstxt) {
        resultCssText = csstxt;
        var elem = tinyMCE.get('StyleCSSDiv').getDoc().getElementById("stylesel");
        elem.setAttribute("style", csstxt);
    }
    
    function unselect() {
      if (tinyMCE.get('StyleCSSDiv')) {
        tinyMCE.execCommand('mceSelectNode',false,tinyMCE.get('StyleCSSDiv').getDoc().getElementById("nosel"));
      }
    }
</script>
<!-- /TinyMCE -->

</head>
<body style="margin:0;padding:0;" >
<div id="StyleCSSDiv" width="240px" height="60px" style="padding:6px;background-color:#FFFFFF;">
<% 
    if (isInlineStyle) {
%>
    <span id="stylesel" style="<%= css %>">Abc def...</span><span id="nosel"></span>
<% 
    } else {
%>
    <div id="stylesel" style="<%= css %>">Abc def...<span id="nosel"></span></div>
<%
    }
%>
</div>
</body>
</html>
