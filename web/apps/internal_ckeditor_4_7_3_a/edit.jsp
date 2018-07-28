<%@page contentType="text/html" pageEncoding="UTF-8" session="true"
        import="java.util.*,java.net.URLEncoder,org.docma.plugin.*,org.docma.plugin.web.*,org.docma.plugin.ckeditor.*"
%><!DOCTYPE html>
<html>
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
    
    Locale uiLocale = webSess.getCurrentLocale();
    String uiLang = (uiLocale == null) ? "en" : uiLocale.getLanguage();
    String contentLang = storeConn.getCurrentLanguage().getCode();

    CKEditHandler appHandler = (CKEditHandler) webSess.getContentAppHandler(editorId);

    //
    // Initialize URLs
    //
    String rel_app_url = appHandler.getRelativeAppURL();
    if (! rel_app_url.endsWith("/")) { 
        rel_app_url += "/";
    }
    String serv_url = request.getContextPath();
    if (! serv_url.endsWith("/")) { 
        serv_url += "/";
    }
    String app_url = serv_url + rel_app_url;
    String base_url = serv_url + "servmedia/" + docsess + "/" + storeid + "/" + verid +
                      "/" + storeConn.getCurrentLanguage().getCode() + "/";
    String post_url = response.encodeURL(serv_url + "saveContent.jsp?" + node_params +
                                         "&docsess=" + docsess + lang_param);
    String css_url = response.encodeURL(base_url + "css/content.css?mode=edit&expire=" + stamp);
    String stylelist_url = response.encodeURL(base_url + "stylelist/json/styles.js?nodeid=" + nodeid +
                                              "&expire=" + stamp);
    String linklist_url = response.encodeURL(base_url + "linklist/json/linklist.js?nodeid=" + nodeid +
                                             "&expire=" + stamp);
    String imagelist_url = response.encodeURL(base_url + "imagelist/json/imglist.js?nodeid=" + nodeid +
                                              "&expire=" + stamp);
    // String videolist_url = response.encodeURL("videolist/json/videolist.js?nodeid=" + nodeid +
    //                                           "&expire=" + stamp);
    // String filelist_url = response.encodeURL("filelist/json/videolist.js?nodeid=" + nodeid +
    //                                           "&expire=" + stamp);
    String docsess_enc = URLEncoder.encode(docsess, "UTF-8");
    String templates_url = response.encodeURL(app_url + 
            "loadDocmaTemplates.jsp?docsess=" + docsess_enc + "&appid=" + URLEncoder.encode(editorId, "UTF-8"));
    String fileupload_url = response.encodeURL(app_url + 
            "uploadFile.jsp?docsess=" + docsess_enc + "&ctxNodeId=" + nodeid);

    // Default configuration values
    String dialogBgOpacity = "0.5";
    boolean dialogNoConfirm = true;
    boolean disableNativeSpell = true;
    boolean disableNativeTable = true;
    boolean entitiesHtml = true;
    // String entitiesAdditional = "";  //  "#39";  comma-separated list
    boolean entitiesGreek = true;
    boolean entitiesLatin = true;
    String entitiesNum = "false";   // or "true" or "'force'"
    boolean figureEnabled = false;
    boolean forcePastePlain = false;
    boolean ignoreEmptyPara = true;
    boolean imgAltRequired = false;
    String imgCaptionedCls = null;
    boolean imgDisableResize = false;
    String removeBtns = "";
    String skin = "moono-lisa";
    String toolbar = appHandler.getCKEditorToolbarTemplate();
    String toolbarLoc = "top";
    String uiColor = "#9AB8F3";
    CKEditPlugin ckPlugin = appHandler.getPlugin();
    
    // Overwrite default configuration with CKEditor plugin configuration
    if (ckPlugin != null) {
        dialogBgOpacity = ckPlugin.getDialogBgOpacity();
        dialogNoConfirm = ckPlugin.isDialogNoConfirmCancel();
        disableNativeSpell = ckPlugin.isDisableNativeSpellChecker();
        disableNativeTable = ckPlugin.isDisableNativeTableHandles();
        entitiesHtml = ckPlugin.isEntitiesHTML();
        // if (ckPlugin.isEntitiesUser()) {
        //     if (ckPlugin.isEntitiesUserNumerical()) {
        //         entitiesAdditional = appHandler.getEntitiesAdditionalNumeric();
        //     } else {
        //         entitiesAdditional = appHandler.getEntitiesAdditionalSymbolic();
        //     }
        // }
        entitiesGreek = ckPlugin.isEntitiesGreek();
        entitiesLatin = ckPlugin.isEntitiesLatin();
        entitiesNum = ckPlugin.getEntitiesProcessNumerical();
        if ((entitiesNum == null) || entitiesNum.equals("")) {
            entitiesNum = "false";
        } else if (entitiesNum.equals("force")) {
            entitiesNum = "'force'";
        }
        figureEnabled = ckPlugin.isFigureTagEnabled(storeid);
        forcePastePlain = ckPlugin.isPastePlainText();
        ignoreEmptyPara = ckPlugin.isIgnoreEmptyPara();
        imgAltRequired = ckPlugin.isImgAltRequired();
        imgCaptionedCls = ckPlugin.getImgCaptionedClass();
        imgDisableResize = ckPlugin.isImgDisableResizer();
        removeBtns = ckPlugin.getRemoveButtons();
        skin = ckPlugin.getSkin();
        if ((skin == null) || skin.equals("")) {
            skin = ckPlugin.getFirstAvailableCKSkin();
        }
        // toolbar = ;
        toolbarLoc = ckPlugin.getToolbarLocation();
        uiColor = ckPlugin.getUIColor();
    }
    if ((imgCaptionedCls == null) || imgCaptionedCls.trim().equals("")) {
        imgCaptionedCls = CKEditHandler.FALLBACK_FIGURE_CSS_CLS;
    }
    
    String ckInitTempl = appHandler.getCKEditorInitTemplate();
    Map<String, String> inimap = new HashMap<String, String>();
    inimap.put("###allowedContent###", "true");  // disable content filtering (allow all elements)
    inimap.put("###baseHref###", base_url);
    inimap.put("###contentsCss###", css_url);
    inimap.put("###contentsLanguage###", contentLang);
    inimap.put("###dialog_backgroundCoverOpacity###", dialogBgOpacity);
    inimap.put("###dialog_noConfirmCancel###", dialogNoConfirm ? "true" : "false");
    inimap.put("###disableNativeSpellChecker###", disableNativeSpell ? "true" : "false");
    inimap.put("###disableNativeTableHandles###", disableNativeTable ? "true" : "false");
    inimap.put("###entities###", entitiesHtml ? "true" : "false");
    // inimap.put("###entities_additional###", entitiesAdditional);
    inimap.put("###entities_greek###", entitiesGreek ? "true" : "false");
    inimap.put("###entities_latin###", entitiesLatin ? "true" : "false");
    inimap.put("###entities_processNumerical###", entitiesNum);
    inimap.put("###extraPlugins###", "docmaatts,docmalink");
    inimap.put("###filebrowserUploadUrl###", fileupload_url);
    inimap.put("###filebrowserWindowWidth###", "640");
    inimap.put("###filebrowserWindowHeight###", "480");
    inimap.put("###forcePasteAsPlainText###", forcePastePlain ? "true" : "false");
    inimap.put("###ignoreEmptyParagraph###", ignoreEmptyPara ? "true" : "false");
    inimap.put("###image2_altRequired###", imgAltRequired ? "true" : "false");
    inimap.put("###image2_captionedClass###", imgCaptionedCls.replace('\'', ' '));
    inimap.put("###image2_disableResizer###", imgDisableResize ? "true" : "false");
    inimap.put("###language###", uiLang);
    inimap.put("###removeButtons###", removeBtns.replace('\'', ' '));
    inimap.put("###serv_url###", serv_url);
    inimap.put("###skin###", skin);
    inimap.put("###specialChars###", appHandler.getSpecialCharsJSArray());
    inimap.put("###templates_files###", templates_url);
    inimap.put("###toolbar###", toolbar);
    inimap.put("###toolbarLocation###", toolbarLoc);
    inimap.put("###uiColor###", uiColor);
    String ckInitObj = appHandler.replacePlaceholders(ckInitTempl, inimap);
%>
<head>
  <meta charset="utf-8" />
  <title>Edit content</title>
  <script src="ckeditor/ckeditor.js"></script>
  <script src="edit.js"></script>
  <script type="text/javascript">
    // Initialize fields used in edit.js
    stylelistUrl = "<%= stylelist_url %>";
    linklistUrl = "<%= linklist_url %>";
    imagelistUrl = "<%= imagelist_url %>";

    docmaStyleLoadTime = (new Date()).getTime();
    docmaStyles = <% CKEditUtils.writeStyleList_JSON(out, storeConn); %>;
    
    var isDocmaFigureEnabled = <%= figureEnabled ? "true" : "false" %>;
    var docmaFigCls = '<%= imgCaptionedCls %>';
    
    initCKStyles();  // see edit.js

    function initEditor() {
      CKEDITOR.replace('docmacontent', <%= 
        ckInitObj 
      %>
      );
        // extraAllowedContent: 'dl dt dd strong[id,title]',
        // disallowedContent: 'iframe form script'
        // image2_alignClasses: [ 'image-left', 'image-center', 'image-right' ],
        // image2_captionedClass: '', // 'image-captioned',
        // stylesheetParser_validSelectors: /\^\.\w+/,
        // stylesheetParser_skipSelectors: /(^body\.|^caption\.|\.high)/i,
        // uploadUrl: '/uploader/upload.jsp';  // used for file uploads of pasted and dragged files; not needed if filebrowserUploadUrl is specified
        // imageUploadUrl: '/uploader/upload.jsp';  // used for uploadimage plugin; not needed if uploadUrl is specified
      
      refreshDocmaLinks();
    }  // initEditor()

    function docmaCKInstanceReady(evt) {
      evt.editor.addCommand('docmaSaveContent', {  // called by shortcut Ctrl+S
          exec: function(editor) { onEditorSaveClick(); } 
      });
      evt.editor.execCommand('maximize'); // switch to fullscreen
      customizeDialogs();
    }
    
    function docmaCKFocus(evt) {
      refreshDocmaLists();    
    }
    
    function docmaCKPluginsLoaded(evt) {
      // evt.editor.widgets.onWidget( 'image', 'instanceCreated', function(evt2) {
      evt.editor.widgets.on( 'instanceCreated', function(evt2) {
          // Set alt attribute as default caption 
          var widget = evt2.data;
          if ( widget.name == 'image' ) {
            widget.on( 'data', function(dataEvt) {
              var placeholder = evt.editor.lang.image2.captionPlaceholder;
              var altTxt = this.data.alt;
              var hasAlt = Boolean(altTxt); // not an empty string
              var captEditable = this.editables.caption;
              // If alt is set and caption editable is already defined and its value equals
              // the placeholder's value (the default one) then set the alt as the value.
              if ( hasAlt && captEditable && ( captEditable.getData() == placeholder ) ) {
                captEditable.setData(altTxt);
              }
              // If caption has been enabled, move attributes from img to figure.
              if (this.element.getName() == 'figure') {
                  var fig = this.element;
                  var img = fig.findOne( 'img' );
                  if (img.hasAttribute('id') && !fig.hasAttribute('id')) {
                      fig.setAttribute('id', img.getAttribute('id'));
                      img.removeAttribute('id');
                  }
                  if (img.hasAttribute('style') && !fig.hasAttribute('style')) {
                      fig.setAttribute('style', img.getAttribute('style'));
                      img.removeAttribute('style');
                  }
                  if (img.hasAttribute('class')) {
                      var arr = img.getAttribute('class').split(' ');
                      img.removeAttribute('class');
                      for (var i=0; i < arr.length; i++) {
                          fig.addClass(arr[i]);
                      }
                  }
                  // Move title attribute to figcaption element
                  if (img.hasAttribute('title') && captEditable) {
                      captEditable.setData(img.getAttribute('title'));
                      img.removeAttribute('title');
                  }
              }
            });
          }
      });
    }

    function customizeDialogs() {
      CKEDITOR.on( 'dialogDefinition', function(evt) {
              var dialogName = evt.data.name;
              var dialogDefinition = evt.data.definition;

              if ( dialogName == 'image2' ) {
                dialogDefinition.minWidth = 400;
                var baseTab = dialogDefinition.getContents( 'info' );
                var urlField = baseTab.get('src');
                // urlField['default'] = 'image/';
                urlField['onFocus'] = function(evt) {
                    docmaImgDialog = this.getDialog();
                    if (this.getValue() == '') {
                        this.setValue('image/', true);
                        window.setTimeout('setImgSrcCursorToEnd()', 50);
                    }
                    startSrcEditing();
                };
                urlField['onBlur'] = function(evt) {
                    if (this.getValue() == 'image/') this.setValue('', true);
                };
                var captField = baseTab.get('hasCaption');
                captField['label'] = captField['label'] + ' (HTML5 figure)';
                if (! isDocmaFigureEnabled) {
                    captField['hidden'] = true;
                }
                baseTab.add( {
                    type: 'text',
                    label: evt.editor.lang.docmaatts.titleLabel,
                    id: 'docmaImgTitle',
                    'default': '',
                    setup: function( widget ) {
                        var isFig = (widget.element.getName() == 'figure');
                        var captEdit = widget.editables.caption;
                        var title = (isFig && captEdit) ? captEdit.getData() : widget.element.getAttribute("title");
                        if ((title != null) && (title != "")) this.setValue(title);
                        else this.setValue("");
                    },
                    commit: function( widget ) {
                        var hasCapt = this.getDialog().getContentElement( 'info', 'hasCaption' ).getValue();
                        var captEdit = hasCapt ? widget.editables.caption : null;
                        var val = this.getValue();
                        if (captEdit != null) {  // figure element: set figcaption
                            if (val != "") captEdit.setData(val);
                            // Remove title attribute (replaced by figcaption)
                            widget.element.removeAttribute( "title" );
                        } else {  // img element: set title attribute
                            if (val != "") widget.element.setAttribute( "title", val );
                            else widget.element.removeAttribute( "title" );
                        }
                    }
                }, 'alt');
                baseTab.add( {
                    type: 'text',
                    label: evt.editor.lang.docmaatts.idLabel,
                    id: 'docmaImgID',
                    'default': '',
                    setup: function( widget ) {
                        var id_val = widget.element.getAttribute("id");
                        this.setValue(id_val == null ? "" : id_val);
                    },
                    commit: function( widget ) {
                        var val = this.getValue();
                        var noCapt = !this.getDialog().getContentElement( 'info', 'hasCaption' ).getValue();
                        var elem = widget.element;
                        if (noCapt && (elem.getName() == 'figure')) { // caption checkbox has been unchecked
                            elem = elem.findOne('img'); // add id to inner img element, because enclosing figure will be removed
                        }
                        if (val != "") {
                            elem.setAttribute( "id", val );
                        } else {
                            elem.removeAttribute( "id" );
                        }
                    }
                });
                baseTab.add( {
                    type : 'html',
                    html : '<datalist id="docma_img_aliases"><option value="testval">Test Value</option></datalist>',
                    onShow: function() {
                        docmaImgDialog = this.getDialog();
                        initImgDialog();
                    }
                });

                dialogDefinition.addContents( {
                  id : 'docmaExtraTab',
                  label : evt.editor.lang.docmaatts.extraImageTabTitle,
                  elements : [
                     {
                        type: 'hbox',
                        widths: [ '65%', '35%' ],
                        children: [ {
                            type: 'text',
                            label: evt.editor.lang.docmaatts.styleIdsLabel,
                            id: 'docmaImgStyles',
                            'default': '',
                            setup: function( widget ) {
                                var cls_val = widget.element.getAttribute("class");
                                if (cls_val == null) cls_val = "";
                                this.docmaOldClass = cls_val;
                                var cls_tmp = ' ' + cls_val + ' ';
                                this.docmaWidgetClsRemoved = cls_tmp.indexOf(' cke_widget_element ') >= 0;
                                this.docmaFigureClsRemoved = cls_tmp.indexOf(' ' + docmaFigCls + ' ') >= 0;
                                if (this.docmaWidgetClsRemoved) {
                                    cls_val = removeStyleClass(cls_val, 'cke_widget_element');
                                }
                                if (this.docmaFigureClsRemoved) {
                                    cls_val = removeStyleClass(cls_val, docmaFigCls);
                                }
                                this.setValue( cls_val );
                            },
                            commit: function( widget ) {
                                var val = trimSpaces(this.getValue());
                                if (this.docmaWidgetClsRemoved) {
                                    val = 'cke_widget_element ' + val;
                                }
                                if (this.docmaFigureClsRemoved) {
                                    val = docmaFigCls + ' ' + val;
                                }
                                val = trimSpaces(val);
                                if (val != "") {
                                    widget.element.setAttribute( "class", val );
                                } else {
                                    widget.element.removeAttribute( "class" );
                                }
                            }
                         },
                         {
                            type: 'select',
                            label: ' ',
                            id: 'docmaImageStyleSelect',
                            items: [], // getImageClassSelection(),
                            'default': '',
                            onShow: function() {
                                setupClassOptions(this, getImageClassSelection());
                                this.setValue('', true);
                            },
                            onChange: function( evt ) {
                                var clsField = this.getDialog().getContentElement( 'docmaExtraTab', 'docmaImgStyles' );
                                var stylesVal = clsField.getValue();
                                var sel = this.getValue();
                                var valNew = addStyleClass(stylesVal, sel);
                                if (stylesVal != valNew) {
                                    clsField.setValue(valNew, true);
                                }
                                this.setValue('', true);
                            }
                        }]
                     },
                     {
                        type: 'text',
                        label: evt.editor.lang.docmaatts.inlineStyleLabel,
                        id: 'docmaImgInlineStyle',
                        'default': '',
                        setup: function( widget ) {
                            this.setValue( widget.element.getAttribute("style") ) ;
                        },
                        commit: function( widget ) {
                            var val = this.getValue();
                            if (val != "") {
                                widget.element.setAttribute( "style", val );
                            } else {
                                widget.element.removeAttribute( "style" );
                            }
                        }
                     }
                  ]
                });

              }  // if image2

              if ( dialogName == 'table' || dialogName == 'tableProperties' ) {
                var baseTab = dialogDefinition.getContents( 'info' );
                var spaceField = baseTab.get('txtCellSpace');
                spaceField['default'] = '0';   // Change default cellspacing from 1 to 0.
                baseTab.add( {
                        type: 'hbox',
                        widths: [ '65%', '35%' ],
                        children: [
                        {
                            type: 'text',
                            id: 'docmaTableStyles',
                            label: evt.editor.lang.docmaatts.styleIdsLabel,
                            'default': '',
                            setup: function( element ) {
                                var val = (element != null) ? element.getAttribute("class") : "";
                                if (val == null) val = "";
                                // var p = val.indexOf('cke_table-faked-selection-table');
                                // if (p >= 0) {
                                //     val = val.substring(0, p).trim();
                                // }
                                this.setValue( removeStyleClass(val, 'keep_together_auto') );
                            },
                            onChange: function( evt ) {
                                setTableClassField( this.getDialog(), this.getValue() );
                            }
                        },
                        {
                            type: 'select',
                            label: ' ',
                            id: 'docmaTableStyleSelect',
                            items: [], // getTableClassSelection(),
                            'default': '',
                            // setup: function( element ) {
                            //   var clsVal = (element != null) ? element.getAttribute("class") : "";
                            //   setTableDialogStyleID(this, clsVal);
                            // },
                            onShow: function() {
                                setupClassOptions(this, getTableClassSelection());
                                this.setValue('', true);
                            },
                            onChange: function( evt ) {
                                // var output = '';
                                // for (var property in evt.data) {
                                //   output += property + ': ' + evt.data[property]+'; ';
                                // }
                                // alert(output);
                                var docmaClsField = this.getDialog().getContentElement( 'info', 'docmaTableStyles' );
                                var stylesVal = docmaClsField.getValue();
                                var sel = this.getValue();
                                var valNew = removeStyleClass(stylesVal, 'keep_together_auto');
                                if (sel != '') {
                                    // add selected style
                                    valNew = addStyleClass(valNew, sel);
                                }
                                if (stylesVal != valNew) {
                                    docmaClsField.setValue(valNew, true);
                                }
                                setTableClassField( this.getDialog(), valNew );
                                this.setValue('', true);
                            }
                        }]
                });

                var advTab = dialogDefinition.getContents( 'advanced' );
                var clsField = advTab.get( 'advCSSClasses' );
                // clsField ['default'] = 'my_own_class';
                // clsField.setValue('my_own_class');
                clsField ['hidden'] = true;  // hide the class field...user shall enter styles in docmaTableStyles textbox instead
                clsField ['onChange'] = function() {
                    var cls = this.getValue();
                    // Update page break checkbox
                    var pageBreakBox = this.getDialog().getContentElement( 'advanced', 'docmaTableAllowPageBreak' );
                    if (pageBreakBox != null) {
                        var valOld = pageBreakBox.getValue();
                        var valNew = cls.indexOf('keep_together_auto') >= 0;
                        if (valNew != valOld) pageBreakBox.setValue(valNew, true);
                    }
                    // Update styles textbox
                    var txtBox = this.getDialog().getContentElement( 'info', 'docmaTableStyles' );
                    if (txtBox != null) {
                        var stylesNew = removeStyleClass(cls, 'keep_together_auto');
                        if (stylesNew != txtBox.getValue()) {
                            txtBox.setValue(stylesNew, true);
                        }
                    }
                };
                advTab.add( {
                    type: 'checkbox',
                    label: evt.editor.lang.docmaatts.allowTableBreak,
                    id: 'docmaTableAllowPageBreak',
                    'default': false,
                    setup: function( element ) {
                        var val = (element != null) ? ' ' + element.getAttribute("class") + ' ' : "";
                        this.setValue( val.indexOf(' keep_together_auto ') >= 0 );
                    },
                    // onClick: function() {},
                    onChange: function() {
                        var clsField = this.getDialog().getContentElement( 'advanced', 'advCSSClasses' );
                        var clsVal = clsField.getValue();
                        if (this.getValue()) {  // add keep_together_auto
                            clsField.setValue(addStyleClass(clsVal, 'keep_together_auto'), true);
                        } else {  // remove keep_together_auto
                            clsField.setValue(removeStyleClass(clsVal, 'keep_together_auto'));
                        }
                    }
                });
              } // if table
       });  // CKEDITOR.on( 'dialogDefinition' , ...
    }  // function customizeDialogs()
    
    function postContent(progress_value, is_close, win_x, win_y, win_width, win_height) {
      document.forms.docmaform.progress.value = progress_value;
      document.forms.docmaform.isclosewin.value = is_close ? "true" : "false";
      document.forms.docmaform.winx.value = win_x;
      document.forms.docmaform.winy.value = win_y;
      document.forms.docmaform.winwidth.value = win_width;
      document.forms.docmaform.winheight.value = win_height;
      document.forms.docmaform.nodecontent.value = CKEDITOR.instances.docmacontent.getData();
      document.forms.docmaform.submit();
    }

    function postCancel() {
      document.forms.docmaform.isclosewin.value = "cancel";
      document.forms.docmaform.nodecontent.value = CKEDITOR.instances.docmacontent.getData();
      document.forms.docmaform.submit();
    }

    function onEditorSaveClick() {
      parent.onSaveClick();
    }

    // function resetContent() {  // currently not used
    // }

    // function checkDirty() {  // currently not used
    // }
    
  </script>
</head>
<body onload="initEditor()">
<div id="docmacontent">
<%
    Content node = (Content) storeConn.getNodeById(nodeid);
    out.print(appHandler.prepareContentForEdit(node.getContentString(), webSess));
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
<input type="hidden" name="editor" value="<%= editorId %>">
<input type="hidden" name="nodecontent" value="">
</form>
</body>
</html>
