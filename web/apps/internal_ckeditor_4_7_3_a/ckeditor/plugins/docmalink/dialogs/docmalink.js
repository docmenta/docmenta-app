var docma_link_dialog;
var docma_href_old = '';
var docma_last_len = 0;
var docma_link_arr = null;
var docma_fill_started = false;
var docma_fill_finished = false;
var docma_default_href = '#';

function initDocmaLinkDialog(dialog) {
    docma_link_dialog = dialog;
    var urlBox = dialog.getContentElement('docmaLinkTab', 'docmaURL');
    var href = urlBox.getValue();
    docma_href_old = href;

    var urlInput = urlBox.getInputElement().$;
    urlInput.onkeyup = hrefKeyTyping;
    if (href == '#') {
        window.setTimeout('setHrefCursorToEnd()', 50);
    }
    window.setInterval('checkHrefChange()', 400);
}

function setHrefCursorToEnd() {
    var urlBox = docma_link_dialog.getContentElement('docmaLinkTab', 'docmaURL');
    var url = urlBox.getValue();
    var inp = urlBox.getInputElement().$;
    inp.setSelectionRange(url.length, url.length);
}

function checkHrefChange() {
    if (! docma_fill_finished) return;
    var hval = docma_link_dialog.getContentElement( 'docmaLinkTab', 'docmaURL' ).getValue();
    if (hval != docma_href_old) {
        var targetBox = docma_link_dialog.getContentElement( 'docmaLinkTab', 'docmaLinkUseTargetTitle' );
        var isTarget = linkExists(hval);
        if (targetBox.getValue() != isTarget) {
            targetBox.setValue(isTarget);  // ,true
        }
        hrefChanged(docma_link_dialog);    
        docma_href_old = hval;
    }
}

function updateGenTextParams(dialog) {
    var txtField = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkText' );
    var titleField = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkTitle' );
    var targetBox = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkUseTargetTitle' );
    var printBox = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkPrintOnly' );
    var gen1Box = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkGen1Select' );
    var gen2Box = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkGen2Select' );
    var pageBox = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkPageSelect' );
    var is_target = targetBox.getValue();
    var is_print_only = printBox.getValue();
    if (is_target) {
        var s = is_print_only ? '%target_print%' : '%target%';
        s = gentextAppend(s, gen1Box.getValue());
        s = gentextAppend(s, gen2Box.getValue());
        s = gentextAppend(s, pageBox.getValue());
        if (is_print_only) {
            txtField.enable();
            titleField.setValue(s, true);
            titleField.disable();
        } else {
            txtField.setValue(s, true);
            txtField.disable();
            clearGentextTitle(titleField);
        }
    } else {
        txtField.enable();
        var p = pageBox.getValue();
        if (p != '') {
            titleField.setValue('%' + p, true);
            titleField.disable();
        } else {
            clearGentextTitle(titleField);
        }
    }
}

function clearGentextTitle(titleField) {
    var t = titleField.getValue();
    if ((t.indexOf('%target_print%') >= 0) ||
        (t.indexOf('%page') >= 0) || 
        (t.indexOf('%nopage') >= 0)) {
        titleField.setValue('', true);
    }
    titleField.enable();
}

function gentextAppend(str, s) {
    if ((s == null) || (s == '')) {
        return str;
    } else {
        return (str + ' ' + s).trim();
    }
}

function fillDataList(dialog) {
    var linkList = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkList' );
    var ck_doc = dialog.getElement().getDocument();
    var dnode = ck_doc.getById('docma_alias_list').$;
    var dopts = dnode.getElementsByTagName('option');
    var snode = linkList.getInputElement().$; // document.getElementById('link_list');
    var sopts = snode.getElementsByTagName('option');
    if (dopts.length == 0) {
        if (docma_fill_started) return;  // avoid parallel filling
        docma_fill_started = true;
        docma_link_arr = new Array();
        var dtemp = dnode.cloneNode(true);
        for (var i = 0; i < sopts.length; i++) {
          var opt = document.createElement('option');
          var txt = document.createTextNode(sopts[i].text);
          opt.appendChild(txt);
          var hrefurl = sopts[i].value;
          opt.value = hrefurl;
          dtemp.appendChild(opt);
          docma_link_arr.push(hrefurl);
        }
        if (dtemp.getAttribute('id') != 'docma_alias_list') dtemp.setAttribute('id', 'docma_alias_list');
        dnode.parentNode.replaceChild(dtemp, dnode);
        // document.getElementById('href').setAttribute('list', 'docma_alias_list');
        docma_link_arr.sort();
        docma_fill_finished = true;
    }
}

function hrefChanged(dialog) {
    // updateLinkTitleValue();
    updateLinkList(dialog);
}

function updateLinkList(dialog) {
    var urlBox = dialog.getContentElement( 'docmaLinkTab', 'docmaURL' );
    var href = urlBox.getValue();
    var linkList = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkList' );
    var sel = linkList.getValue();
    if (href != sel) {
        if (linkExists(href)) {
            linkList.setValue(href, true);
        } else if ((href.indexOf('#') < 0) && (href.indexOf('/') < 0) && linkExists('#' + href)) {
            var href = '#' + href;
            urlBox.setValue(href, true);
            linkList.setValue(href, true);
        } else {
            linkList.setValue('', true);
        }
    }
}

function updateLinkTitleValue() {
    // to do
}

function linkExists(hrefurl) {
  return (docma_link_arr != null) && (binLinkSearch(0, docma_link_arr.length - 1, hrefurl) >= 0);
}

function binLinkSearch(i,j,val) {
   if (j < i) { return -(i+1); }
  else {
    var m = Math.floor((i+j)/2);
    if (val < docma_link_arr[m]) {
      return binLinkSearch(i,m-1,val);
    } else
    if (val > docma_link_arr[m]) {
      return binLinkSearch(m+1,j,val);
    } else {
      return m;
    }
  }
}

function hrefKeyTyping(evt) {
  if (!evt) evt = window.event;
  if (evt.shiftKey || evt.altKey || (evt.keyCode == 16) || (evt.keyCode == 18)) return;
  
  var urlBox = docma_link_dialog.getContentElement( 'docmaLinkTab', 'docmaURL' );
  var inp = urlBox.getInputElement().$;
  var href_val = inp.value;
  // if ((docma_last_len == 0) && (href_val == 'image')) {
  //   inp.value="";
  // }
  var added_char = (href_val.length > docma_last_len);
  docma_last_len = href_val.length;
  if (! added_char) return;
  if (! docma_fill_finished) return;
  if (inp.setSelectionRange) {
    var pos = binLinkSearch(0, docma_link_arr.length - 1, href_val);
    if (pos < 0) {
      pos = -(pos + 1);
      if (pos >= docma_link_arr.length) return;
      var hrefurl = docma_link_arr[pos];
      if (startsWith(hrefurl, href_val) &&
          ((pos == 0) || !startsWith(docma_link_arr[pos-1], href_val)) &&
          ((pos >= (docma_link_arr.length-1)) || !startsWith(docma_link_arr[pos+1], href_val))) {
          inp.value = hrefurl;
          inp.setSelectionRange(href_val.length, hrefurl.length);
      }
    }
  }
}

function startsWith(str, prefix) {
  return (prefix.length <= str.length) && (prefix == str.substr(0,prefix.length));
}

CKEDITOR.dialog.add("docmalinkDialog", function(editor) {
    return {
        allowedContent: "a[href,target]",
        title: editor.lang.docmalink.linkDialogTitle,
        minWidth: 550,
        minHeight: 100,
        resizable: CKEDITOR.DIALOG_RESIZE_NONE,
        contents:[{
            id: "docmaLinkTab",
            label: editor.lang.docmalink.baseTabTitle,
            elements:[{
                type: "text",
                label: editor.lang.docmalink.urlLabel,
                id: "docmaURL",
                validate: function() {
                    var href = this.getValue();
                    if ( (href == '') || (href == '#')) {
                        alert( editor.lang.docmalink.urlEmptyMsg );
                        return false;
                    }
                }, 
                setup: function( element ) {
                    var href = element.getAttribute("href");
                    // var isExternalURL = /^(http|https):\/\//;
                    if (href == null) {
                        href = docma_default_href;
                    }
                    this.setValue(href, true);
                    
                    var inp = this.getInputElement();
                    inp.$.setAttribute('list', 'docma_alias_list');
                },
                // onFocus : function( evt ) {
                //    // fillDataList(this.getDialog()); 
                // },
                onChange : function( evt ) {
                    hrefChanged(this.getDialog());
                },
                onKeyPress : function( evt ) {
                    hrefChanged(this.getDialog());
                },
                // onKeyUp : function( evt ) {
                //     hrefChanged(this.getDialog());
                // },
                commit: function(element) {
                    var href = this.getValue();
                    // var isExternalURL = /^(http|https):\/\//;
                    if (href == null) {
                        href = '';
                    }
                    element.setAttribute("href", href);
                    // if (! element.getText()) {
                    //     element.setText((href == '') ? 'Link' : href);
                    // }
                }
            }, {
                type: 'select',
                id: 'docmaLinkList',
                label: editor.lang.docmalink.linkListLabel,
                items: [], // getDocmaLinkSelection(),
                'default': '',
                onShow: function() {
                    setupLinkOptions(this, getDocmaLinkSelection());
                    var dialog = this.getDialog();
                    fillDataList(dialog);
                    updateLinkList(dialog);
                    // this.setValue('', true);
                },
                onChange: function( evt ) {
                    var urlField = this.getDialog().getContentElement( 'docmaLinkTab', 'docmaURL' );
                    var sel = this.getValue();
                    urlField.setValue(sel, true);
                }
            }, 
            // {
            //    type: "button",
            //    id: "docmaLinkBrowseBtn",
            //    label: editor.lang.docmalink.browseServer,
            //    hidden: true,
            //    filebrowser: 'docmaLinkTab:docmaURL'
            // }, 
            {
                type: 'hbox',
                widths: [ '35%', '65%' ],
                children: [ {
                    type: 'checkbox',
                    label: editor.lang.docmalink.displayTargetTitle,
                    id: 'docmaLinkUseTargetTitle',
                    'default': false,
                    setup: function( element ) {
                        var dialog = this.getDialog();
                        var printBox = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkPrintOnly' );
                        var gen1Box = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkGen1Select' );
                        var gen2Box = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkGen2Select' );
                        var pageBox = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkPageSelect' );
                        
                        var link_text = '';
                        if ((element != null) && element.getText) {  // edit link
                            link_text = element.getText(); 
                        }
                        var is_target_txt = (link_text.indexOf('%target%') >= 0);
                        var title_val = ((element != null) && element.getAttribute) ? element.getAttribute("title") : "";
                        if (title_val == null) {
                            title_val = '';
                        }
                        var is_print_att = (title_val.indexOf('%target_print%') >= 0);
                        var is_target_att = (title_val.indexOf('%target%') >= 0);
                        
                        var use_label = '';
                        var use_title = '';
                        var use_page = '';
                        printBox.setValue(is_print_att, true);
                        if ( is_target_txt || is_target_att || is_print_att ) {
                            this.setValue(true, true);
                            printBox.enable();
                            if (is_target_txt) {
                                var txtField = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkText' );
                                txtField.disable();
                            }
                            var s = is_target_txt ? link_text : title_val;
                            s = s.toLowerCase();
                            if (s.indexOf('labelname') > 0) use_label = 'labelname';
                            else if (s.indexOf('labelnumber') > 0) use_label = 'labelnumber';
                            else if (s.indexOf('label') > 0) use_label = 'label';

                            if (s.indexOf('quotedtitle') > 0) use_title = 'quotedtitle';
                            else if (s.indexOf('title') > 0) use_title = 'title';

                            if (s.indexOf('nopage') > 0) use_page = 'nopage';
                            else if (s.indexOf('page') > 0) use_page = 'page';
                            
                            gen1Box.enable();
                            gen2Box.enable();
                            gen1Box.setValue(use_label, true);
                            gen2Box.setValue(use_title, true);
                            pageBox.setValue(use_page, true);
                        } else {
                            this.setValue(false, true);
                            printBox.disable();
                            gen1Box.setValue('', true);
                            gen1Box.disable();
                            gen2Box.setValue('', true);
                            gen2Box.disable();
                            
                            var s = title_val.toLowerCase();
                            if (s.indexOf('%page') >= 0) use_page = 'page';
                            else if (s.indexOf('%nopage') >= 0) use_page = 'nopage';
                            
                            pageBox.setValue(use_page, true);
                        }
                        dialog.docmaOldTxt = is_target_txt ? "" : link_text;
                    },
                    onChange: function() {
                        var dialog = this.getDialog();
                        var txtField = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkText' );
                        var printBox = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkPrintOnly' );
                        var gen1Box = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkGen1Select' );
                        var gen2Box = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkGen2Select' );
                        if (this.getValue()) {  // checkbox has been checked
                            dialog.docmaOldTxt = txtField.getValue();
                            printBox.enable();
                            printBox.setValue( false , true );
                            gen1Box.enable();
                            gen2Box.enable();
                        } else {  // checkbox has been unchecked
                            printBox.setValue( false , true );
                            printBox.disable();
                            txtField.setValue(dialog.docmaOldTxt);
                            gen1Box.setValue('');
                            gen1Box.disable();
                            gen2Box.setValue('');
                            gen2Box.disable();
                        }
                        updateGenTextParams(dialog);
                    }
                },{
                    type: 'checkbox',
                    label: editor.lang.docmalink.forPrintOnly,
                    id: 'docmaLinkPrintOnly',
                    'default': false,
                    setup: function( element ) {
                        // var title_val = ((element != null) && element.getAttribute) ? element.getAttribute("title") : "";
                        // var is_print_att = (title_val.indexOf('%target_print%') >= 0);
                        // this.setValue( is_print_att );
                    },
                    onChange: function() {
                        var dialog = this.getDialog();
                        var txtField = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkText' );
                        var titleField = dialog.getContentElement( 'docmaLinkTab', 'docmaLinkTitle' );
                        if (this.getValue()) {  // checkbox has been checked
                            updateGenTextParams(dialog);
                            txtField.setValue(dialog.docmaOldTxt);
                        } else {  // checkbox has been unchecked
                            dialog.docmaOldTxt = txtField.getValue();
                            updateGenTextParams(dialog);
                        }
                    }
                }]
            }, {
                type: 'hbox',
                widths: [ '30%', '30%', '40%' ],
                children: [ {
                    type: 'select',
                    label: editor.lang.docmalink.generatedText,
                    id: 'docmaLinkGen1Select',
                    items: [ [ editor.lang.docmalink.txtDefault, '' ], 
                             [ editor.lang.docmalink.txtLabelNumber, 'label' ], 
                             [ editor.lang.docmalink.txtLabel, 'labelname' ], 
                             [ editor.lang.docmalink.txtNumber, 'labelnumber' ] ],
                    'default': '',
                    onChange: function() {
                        updateGenTextParams(this.getDialog());
                    }
                },{
                    type: 'select',
                    label: ' ',
                    id: 'docmaLinkGen2Select',
                    items: [ [ editor.lang.docmalink.txtDefault, '' ], 
                             [ editor.lang.docmalink.txtTitle, 'title' ], 
                             [ editor.lang.docmalink.txtQuotedTitle, 'quotedtitle' ] ],
                    'default': '',
                    onChange: function() {
                        updateGenTextParams(this.getDialog());
                    }
                },{
                    type: 'select',
                    label: editor.lang.docmalink.pageReference,
                    id: 'docmaLinkPageSelect',
                    items: [ [ editor.lang.docmalink.txtDefault, '' ], 
                             [ editor.lang.docmalink.txtPage, 'page' ], 
                             [ editor.lang.docmalink.txtNoPage, 'nopage' ] ],
                    'default': '',
                    onChange: function() {
                        updateGenTextParams(this.getDialog());
                    }
                }]
            }, {
                type: "text",
                label: editor.lang.docmalink.linkText,
                id: "docmaLinkText",
                setup: function( element ) {
                    this.setValue( element.getText() );
                },
                commit: function(element) {
                    var currentValue = this.getValue();
                    if ((currentValue !== "") && (currentValue !== null)) {
                        element.setText(currentValue);
                    } else {
                        element.setText(editor.lang.docmalink.defaultlinkText);
                    }
                }
            }, {
                type: "text",
                label: editor.lang.docmalink.printOutput,
                id: "docmaLinkTitle",
                setup: function( element ) {
                    var title = element.getAttribute("title");
                    if (title == null) {
                        title = '';
                    }
                    this.setValue(title);
                    var is_print = (title.indexOf('%target_print%') >= 0) || 
                                   (title.indexOf('%page') >= 0) || 
                                   (title.indexOf('%nopage') >= 0);
                    if (is_print) {
                        this.disable();
                    }
                },
                commit: function(element) {
                    var title = this.getValue().trim();
                    if (title == null) {
                        title = '';
                    }
                    if (title == '') {
                        element.removeAttribute("title");
                    } else {
                        element.setAttribute("title", title);
                    }
                }
            }, {
                type: 'select',
                label: editor.lang.docmalink.linkTarget,
                id: 'docmaLinkTarget',
                items: [ [ editor.lang.docmalink.txtDefault, '' ], 
                         [ editor.lang.docmalink.targetSelf, '_self' ], 
                         [ editor.lang.docmalink.targetBlank, '_blank' ], 
                         [ editor.lang.docmalink.targetParent, '_parent' ], 
                         [ editor.lang.docmalink.targetTop, '_top' ] ],
                'default': '',
                setup: function( element ) {
                    var tar = element.getAttribute("target");
                    if (tar == null) {
                        tar = '';
                    }
                    this.setValue(tar);
                },
                commit: function(element) {
                    var tar = this.getValue();
                    if ((tar == null) || (tar == '')) {
                        element.removeAttribute("target");
                    } else {
                        element.setAttribute("target", tar);
                    }
                }
            }, {
                type: 'hbox',
                widths: [ '65%', '35%' ],
                children: [ 
                {
                    type: 'text',
                    id: 'docmaLinkClass',
                    label: editor.lang.docmalink.styleIdsLabel,
                    'default': '',
                    setup: function( element ) {
                        var val = (element != null) ? element.getAttribute("class") : "";
                        this.setValue( val );
                    },
                    commit: function( element ) {
                        var val = this.getValue().trim();
                        if (val != "") {
                            element.setAttribute( "class", val );
                        } else {
                            element.removeAttribute( "class" );
                        }
                    }
                }, {
                    type: 'select',
                    id: 'docmaLinkStyleSelect',
                    label: ' ',
                    items: [], // getDocmaClassSelection(),
                    'default': '',
                    onShow: function() {
                        setupClassOptions(this, getDocmaClassSelection());
                        this.setValue('', true);
                    },
                    onChange: function( evt ) {
                        var clsField = this.getDialog().getContentElement( 'docmaLinkTab', 'docmaLinkClass' );
                        var clsVal = clsField.getValue().trim();
                        var clsVal2 = ' ' + clsVal + ' ';
                        var sel = this.getValue();
                        if (sel != '') {
                            if (clsVal2.indexOf(' ' + sel + ' ') < 0) {
                                clsField.setValue((clsVal + ' ' + sel).trim(), true);
                            }
                        }
                        this.setValue('', true);
                    }
                }]
            }, {
                type : 'html',
                html : '<datalist id="docma_alias_list"></datalist>'
            }]
        }],
        onShow: function() {
            var selection = editor.getSelection();
            var selector = selection.getStartElement()
            var element;
            
            if (selector) {
                 element = selector.getAscendant( 'a', true );
            }
            
            if ( !element || element.getName() != 'a' ) {
                element = editor.document.createElement( 'a' );
                // element.setAttribute("target", "_blank");
                if (selection) {
                    element.setText(selection.getSelectedText());
                }
                this.insertMode = true;
            } else {
                this.insertMode = false;
            }
            
            this.element = element;
            this.setupContent(this.element);
            
            initDocmaLinkDialog(this);
        },
        onOk: function() {
            var dialog = this;
            var anchorElement = this.element;
            
            this.commitContent(this.element);

            if(this.insertMode) {
                editor.insertElement(this.element);
            }
        }
    };
});
