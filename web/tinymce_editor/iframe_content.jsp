<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.util.*,java.net.URLEncoder,org.docma.plugin.*,org.docma.plugin.web.*,org.docma.plugin.tinymce.*"
%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%
    long stamp = System.currentTimeMillis();
    String docsess = request.getParameter("docsess");
    String nodeid = request.getParameter("nodeid");
    String editorId = request.getParameter("appid");
    WebUserSession webSess = WebPluginUtil.getUserSession(application, docsess);
    StoreConnection storeConn = webSess.getOpenedStore();
    if (storeConn == null) {
        throw new Exception("Store connection of user session is closed!");
    }
    String storeid = storeConn.getStoreId();
    String verid = storeConn.getVersionId().toString();
    String node_params = "storeid=" + URLEncoder.encode(storeid, "UTF-8") +
                         "&verid=" + URLEncoder.encode(verid, "UTF-8") +
                         "&nodeid=" + nodeid;
    String lang = storeConn.getTranslationMode();
    String lang_param = (lang == null) ? "" : "&lang=" + lang;

    String post_url = response.encodeURL("saveContent.jsp?" + node_params +
                                         "&docsess=" + docsess + lang_param);
    String serv_url = request.getContextPath();
    if (! serv_url.endsWith("/")) serv_url += "/";
    String base_url = serv_url + "servmedia/" + docsess + "/" + storeid + "/" + verid +
                      "/" + storeConn.getCurrentLanguage().getCode() + "/";
    String css_url = response.encodeURL("css/content.css?mode=edit&expire=" + stamp);
    String imagelist_url = response.encodeURL("imagelist/imglist.js?nodeid=" + nodeid +
                                              "&expire=" + stamp);
    String linklist_url = response.encodeURL("linklist/linklist.js?nodeid=" + nodeid +
                                             "&expire=" + stamp);
    String videolist_url = response.encodeURL("videolist/videolist.js?nodeid=" + nodeid +
                                              "&expire=" + stamp);
    String templatelist_url = response.encodeURL("templatelist/tmplist.js?nodeid=" + nodeid +
                                                 "&expire=" + stamp);

    String para_indent = null;
    OutputConfig outConf = webSess.getPreviewHTMLConfig();
    if (outConf != null) {
        para_indent = outConf.getProperty("paraIndent");
    } else {
        org.docma.util.Log.warning("Missing preview output configuration for editor.");
    }
    if (para_indent == null) {
        para_indent = DefaultContentAppHandler.DEFAULT_PARA_INDENT;
    }
    // round to next higher int because Tinymce can only handle int values:
    para_indent = TinyEditorUtil.roundIndentToHigherInt(para_indent);

    String lists_plugin = editorId.startsWith("tinymce_3_3") ? "" : ",lists";
    String doc_plugins = TinyInitInsertion.getPlugins(webSess);
    String doc_options = TinyInitInsertion.getOptions(webSess);
    String doc_buttons3 = TinyInitInsertion.getButtons3(webSess);
    String doc_buttons4 = TinyInitInsertion.getButtons4(webSess);
    if (doc_buttons4.startsWith(",")) {
        doc_buttons4 = doc_buttons4.substring(1).trim();
    }

    ContentAppHandler apphandler = webSess.getContentAppHandler(editorId);
    String entity_conf = (apphandler instanceof OldTinymceHandler) ?
        ((OldTinymceHandler) apphandler).getCharEntitiesConfigString() : "";
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>Edit content</title>
<%--
<link rel="stylesheet" type="text/css" href="<%= base_url + css_url %>">
--%>
<!-- TinyMCE -->
<script type="text/javascript" src="jscripts/<%= editorId %>/tiny_mce.js"></script>
<script type="text/javascript">
  tinyMCE.init({
    // General options
    mode : "exact",
    elements : "docmacontent",
    theme : "advanced",
    skin : "o2k7",
    // encoding : "xml",
    extended_valid_elements : "-div[!class|id|title|style],-li[style|value],-blockquote",
    indentation : "<%= para_indent %>",
    entity_encoding : "numeric",
    entities : "<%= entity_conf %>",
    save_enablewhendirty : false, // plugin: save; disabled because does not work
    save_onsavecallback : "onEditorSaveClick",
    // dialog_type : "modal",     // plugin: inlinepopups
    plugins : "pagebreak,style,table,save,advimage,advlist<%= lists_plugin %>,iespell,preview,media,searchreplace,print,contextmenu,paste,directionality,fullscreen,noneditable,visualchars,nonbreaking,xhtmlxtras,template,wordcount<%= doc_plugins %>",

    // Theme options
    theme_advanced_buttons1 : "save,bold,italic,underline,strikethrough,|,sub,sup,|,justifyleft,justifycenter,justifyright,justifyfull,|,bullist,numlist,|,outdent,indent,|,removeformat,attribs,visualchars,|,styleselect",
    theme_advanced_buttons2 : "cut,copy,paste,pastetext,|,search,replace,|,undo,redo,|,link,unlink,cite,image,media,charmap,nonbreaking,template,|,code,|,print",
    theme_advanced_buttons3 : "tablecontrols,|,visualaid,|,del,ins,|,iespell,|,ltr,rtl<%= doc_buttons3 %>",
    theme_advanced_buttons4 : "<%= doc_buttons4 %>",
    theme_advanced_toolbar_location : "top",
    theme_advanced_toolbar_align : "left",
    theme_advanced_statusbar_location : "bottom",
    theme_advanced_resizing : true,

    // Example content CSS (should be your site CSS)
    content_css : "<%= css_url %>",
    document_base_url : "<%= base_url %>",

    // Drop lists for link/image/media/template dialogs
    template_external_list_url : "<%= templatelist_url %>",
    external_link_list_url : "<%= linklist_url %>",
    external_image_list_url : "<%= imagelist_url %>",
    media_external_list_url : "<%= videolist_url %>",

    // Style formats
    style_formats : [
      // {title : 'Inline styles'},
<%
      Style[] styles = storeConn.getStyles();
      List blockstyles = new ArrayList(styles.length);
      for (int i=0; i < styles.length; i++) {
          Style style = styles[i];
          String s_id = style.getId();
          if (! (style.isVariant() || style.isHidden() || style.isInternalStyle() || "indexterm".equals(s_id))) {
              if (style.isInlineStyle()) {
%>
            {title : '<%= style.getTitle() %>', inline : 'span', classes : '<%= s_id %>'},
<%
              } else {
                  blockstyles.add(style);
              }
          }
      }
%>
      {title : 'Index'},
      {title : 'Term', inline : 'span', classes : 'indexterm'},
<%
      out.print("{title : 'Block styles'}");
      for (int i=0; i < blockstyles.size(); i++) {
          Style style = (Style) blockstyles.get(i);
          String sid = style.getId();
          // boolean is_para  = sid.equals("paragraph");
          // String is_wrap = "" + (! is_para);
          // String html_elem = is_para ? "p" : "div";
          out.print(",{title : '");
          out.print(style.getTitle());
          out.print("', block : 'div', classes : '");
          out.print(sid);
          out.print("', wrapper : true }");
      }
%>
      ,{title : 'Special'},
      {title : 'Keep Together', block : 'div', classes : 'keep_together', wrapper : true }
    ],

    formats : {
      alignleft : {selector : 'p,h1,h2,h3,h4,h5,h6,td,th,div,ul,ol,li,table,img', classes : 'align-left'},
      aligncenter : {selector : 'p,h1,h2,h3,h4,h5,h6,td,th,div,ul,ol,li,table,img', classes : 'align-center'},
      alignright : {selector : 'p,h1,h2,h3,h4,h5,h6,td,th,div,ul,ol,li,table,img', classes : 'align-right'},
      alignfull : {selector : 'p,h1,h2,h3,h4,h5,h6,td,th,div,ul,ol,li,table,img', classes : 'align-full'},
      bold : {inline : 'strong'},
      italic : {inline : 'em'},
      underline : {inline : 'span', 'classes' : 'underline', exact : true},
      strikethrough : {inline : 'span', 'classes' : 'strike', exact : true}
    },

    // Replace values for the template plugin
    template_replace_values : {}
    <%= doc_options %>
    <%-- , oninit : doFullScreen --%>
  });

  function doFullScreen() {
      if (!tinyMCE.get('docmacontent')) {
        window.setTimeout('doFullScreen()', 100);
        return;
      }
      tinyMCE.get('docmacontent').execCommand('mceFullScreen');
  }

<%--
  function editContent() {
      // tinyMCE.get('elm1').show();
      if (!tinyMCE.get('docmacontent')) {
        window.setTimeout('editContent()', 100);
        return;
      }
      tinyMCE.get('docmacontent').execCommand('mceFullScreen');
      // tinyMCE.get('docmacontent').isNotDirty = true; // force not dirty state
      // if (!tinyMCE.get('elm1')) {
      //    window.focus();
      //    tinyMCE.execCommand('mceAddControl', false, 'elm1');
      //    tinyMCE.get('elm1').focus(true);
      // }
  }
    
  function viewContent() {
      tinyMCE.activeEditor.execCommand('mceFullScreen');
      tinyMCE.get('docmacontent').hide();
      tinyMCE.get('docmacontent').save();
      // if (tinyMCE.get('docmacontent')) {
      //    window.focus();
      //    tinyMCE.execCommand('mceRemoveControl', false, 'docmacontent');
      // }
  }
--%>

  function postContent(progress_value, is_close, win_x, win_y, win_width, win_height) {
      document.forms.docmaform.progress.value = progress_value;
      document.forms.docmaform.isclosewin.value = is_close ? "true" : "false";
      document.forms.docmaform.winx.value = win_x;
      document.forms.docmaform.winy.value = win_y;
      document.forms.docmaform.winwidth.value = win_width;
      document.forms.docmaform.winheight.value = win_height;
      document.forms.docmaform.nodecontent.value = tinyMCE.get('mce_fullscreen').getContent(); // tinyMCE.activeEditor.getContent();
      document.forms.docmaform.submit();
<%--  // tinyMCE.get('docmacontent').save();
      // window.alert(tinyMCE.get('docmacontent').getContent());
      // tinyMCE.get('docmacontent').execCommand('mceSave');
      // tinyMCE.triggerSave();
      // tinyMCE.activeEditor.execCommand('mceSave'); --%>
  }

  function postCancel() {
      document.forms.docmaform.isclosewin.value = "cancel";
      document.forms.docmaform.nodecontent.value = tinyMCE.get('mce_fullscreen').getContent();
      document.forms.docmaform.submit();
  }

  function onEditorSaveClick() {
      parent.onSaveClick();
  }

  function resetContent() {
      tinyMCE.get('docmacontent').execCommand('mceCancel');
  }

  function checkDirty() {
      return tinyMCE.get('mce_fullscreen').isDirty();
  }

</script>
<!-- /TinyMCE -->

</head>
<body style="margin:0;padding:0;height:100%;overflow:hidden;" onload="doFullScreen();">
<div id="docmacontent" style="width:100%; height:100%;">
<%
    Content node = (Content) storeConn.getNodeById(nodeid);
    out.print(TinyEditorUtil.prepareContentForEdit(node.getContentString(), editorId, para_indent));
%>
</div>
<form name="docmaform" method="post" action="<%= post_url %>"
      target="docmaresultfrm" style="height:0px; display:none;">
<input type="hidden" name="progress" value="">
<input type="hidden" name="isclosewin" value="">
<input type="hidden" name="winx" value="">
<input type="hidden" name="winy" value="">
<input type="hidden" name="winwidth" value="">
<input type="hidden" name="winheight" value="">
<input type="hidden" name="indent" value="<%= para_indent %>">
<input type="hidden" name="editor" value="<%= editorId %>">
<input type="hidden" name="nodecontent" value="">
<%--
<textarea id="docmacontent" name="docmacontent" style="width:100%;height:100%">
</textarea>
--%>
</form>
</body>
</html>
