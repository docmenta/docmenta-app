var stylelistUrl;  // initialized in edit.jsp
var linklistUrl;   // initialized in edit.jsp
var imagelistUrl;  // initialized in edit.jsp

var docmaStyleLoadTime;  // initialized in edit.jsp
var docmaStyles;         // initialized in edit.jsp

var docmaLinkLoadTime = 0;
var docmaLinks = [];   // loaded by refreshDocmaLinks(); see initEditor()

var docmaImgDialog = null;  // Initialized in onShow of datalist.
var docmaImgList = [];      // Initialized in refreshDocmaImgList().
var docmaImgSrcList = [];   // Initialized in refreshDocmaImgList().
var docmaImgTitles = new Object();   // Initialized in refreshDocmaImgList().
var docmaLastImgSrcLen = 0; // See imgSrcTyping().
var docmaImgSrcOld = '';

var extraImgClassOptions = [ ['print_width_10'], ['print_width_20'], ['print_width_30'], ['print_width_40'], 
                             ['print_width_50'], ['print_width_60'], ['print_width_70'], ['print_width_80'], 
                             ['print_width_90'], ['print_width_100'] ];

var extraTableClassOptions = [ ['landscape_table'] ];

function initCKStyles() {
    CKEDITOR.stylesSet.add( 'docma_styles', docmaStyles );
}

function refreshDocmaStyles() {
    var now = (new Date()).getTime();
    if ((now - docmaStyleLoadTime) > (1000 * 60)) {   // refresh if older than 1 minute
        docmaStyleLoadTime = now;
        ajaxPost(stylelistUrl, "", function() {
            if ((this.readyState == 4) && (this.status == 200)) {
                docmaStyles = JSON.parse(this.responseText);
                // initCKStyles();
            }
        });
    }
}
    
function refreshDocmaLinks() {
    var now = (new Date()).getTime();
    if ((now - docmaLinkLoadTime) > (1000 * 60)) {   // refresh if older than 1 minute
        docmaLinkLoadTime = now;
        ajaxPost(linklistUrl, "", function() {
            if ((this.readyState == 4) && (this.status == 200)) {
                docmaLinks = JSON.parse(this.responseText);
            }
        });
    }
}

function initImgDialog() {
    var urlBox = docmaImgDialog.getContentElement( 'info', 'src' );
    var inp = urlBox.getInputElement().$;
    inp.setAttribute('list', 'docma_img_aliases');
    // inp.onkeyup = imgSrcTyping;
    refreshDocmaImgList(docmaImgDialog);
    docmaImgSrcOld = '';  // clear field
}

function startSrcEditing() {
    var urlBox = docmaImgDialog.getContentElement( 'info', 'src' );
    var inp = urlBox.getInputElement().$;
    docmaImgSrcOld = urlBox.getValue();  // initial src (see function setImgTitleOnChange)
    inp.onkeyup = imgSrcTyping;
    inp.oninput = setImgTitleOnChange;
}

function setImgTitleOnChange() {
    var imgListLoaded = (docmaImgList != null) && (docmaImgList.length > 0);
    if (! imgListLoaded) return;
    var urlBox = docmaImgDialog.getContentElement( 'info', 'src' );
    var src = urlBox.getValue();
    if (src == 'image/') return;  // do not set docmaImgSrcOld
    if ((src != '') && (src != docmaImgSrcOld) && imageExists(src)) {
        var title = getImgTitle(src);
        if ((title != null) && (title != '')) {
            var altBox = docmaImgDialog.getContentElement( 'info', 'alt' );
            altBox.setValue(title);
            var titleBox = docmaImgDialog.getContentElement( 'info', 'docmaImgTitle' );
            if (titleBox != null) {
                titleBox.setValue(title);
            }
        }
    }
    docmaImgSrcOld = src;
}

function refreshDocmaImgList(ckImgDialog) {
    ajaxPost(imagelistUrl, "", function() {
        if ((this.readyState == 4) && (this.status == 200)) {
            docmaImgList = JSON.parse(this.responseText);
            docmaImgSrcList = new Array();
            // docmaImgTitles = new Object();
            for (var i = 0; i < docmaImgList.length; i++) {
                var src = docmaImgList[i][1];
                docmaImgSrcList.push(src);
                docmaImgTitles[src] = docmaImgList[i][0]; // map image url to title
            }
            docmaImgSrcList.sort();
            if (ckImgDialog != null) {
                var dnode = ckImgDialog.getElement().getDocument().getById('docma_img_aliases').$;
                setDocmaImgDataList(dnode);
            }
        }
    });
}

function refreshDocmaLists() {
    refreshDocmaStyles();
    refreshDocmaLinks();
}

function getDocmaClassSelection() {
    var res = new Array();
    res.push(['']);  // allows to remove class attribute
    for (var i = 0; i < docmaStyles.length; i++) {
        var s = docmaStyles[i];
        if ((s.attributes != null) && (s.attributes['class'] != null)) {
            res.push( [ s.name, s.attributes['class'] ] );
        }
    }
    return res;
}

function getTableClassSelection() {
    return getDocmaClassSelection().concat(extraTableClassOptions).sort(compareSelOptions);
}

function getImageClassSelection() {
    return getDocmaClassSelection().concat(extraImgClassOptions).sort(compareSelOptions);
}

function getDocmaLinkSelection() {
    return docmaLinks;
}

function ajaxPost(url, formdata, ajax_handler) {
    var ajaxReq;
    if (window.XMLHttpRequest) {
        ajaxReq = new XMLHttpRequest();
        // } else 
        // if (window.ActiveXObject) {
        //     ajaxReq = new ActiveXObject("Microsoft.XMLHTTP");
    } else {
        window.status = "Browser does not support Ajax!";
        return false;
    }

    ajaxReq.open("POST", url, true);  // true is async
    ajaxReq.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    // if (ajaxReq.overrideMimeType)  ajaxReq.overrideMimeType("text/xml");
    ajaxReq.onreadystatechange = ajax_handler;
    ajaxReq.send(formdata);
    return true;
}

function setDocmaImgDataList(dnode)
{
    if (dnode == null) return;
    var dtemp = document.createElement('datalist'); // dnode.cloneNode(false);
    var TAB = "....................";
    for (var i = 0; i < docmaImgList.length; i++) {
        var imgData = docmaImgList[i];
        var opt = document.createElement('option');
        var txt = imgData[0];
        var url = imgData[1];
        if (txt == '') {
            txt = url;
        } else {
            var space = " ..." + ((url.length < TAB.length) ? TAB.substring(url.length) : "")
            txt = url + space + txt;
        }
        opt.appendChild(document.createTextNode(txt));
        opt.value = url;
        dtemp.appendChild(opt);
    }
    // if (dtemp.getAttribute('id') != 'docma_img_aliases') {
    dtemp.setAttribute('id', 'docma_img_aliases');
    // }
    dnode.parentNode.replaceChild(dtemp, dnode);  // update data list
}

function imgSrcTyping(evt) {
  if (!evt) evt = window.event;
  if (evt.shiftKey || evt.altKey || (evt.keyCode == 16) || (evt.keyCode == 18)) return;

  var srcBox = docmaImgDialog.getContentElement( 'info', 'src' );
  var inp = srcBox.getInputElement().$;
  var src_val = inp.value;
  // if ((docma_last_len == 0) && (src_val == 'image')) {
  //   inp.value="";
  // }
  var fillFinished = (docmaImgList != null) && (docmaImgList.length > 0);
  var added_char = (src_val.length > docmaLastImgSrcLen);
  docmaLastImgSrcLen = src_val.length;
  if (! added_char) return;
  if (! fillFinished) return;
  if (inp.setSelectionRange) {
    var pos = binImgSearch(0, docmaImgSrcList.length - 1, src_val);
    if (pos < 0) {
      pos = -(pos + 1);
      if (pos >= docmaImgSrcList.length) return;
      var imgurl = docmaImgSrcList[pos];
      if (startsWith(imgurl, src_val) &&
          ((pos == 0) || !startsWith(docmaImgSrcList[pos-1], src_val)) &&
          ((pos >= (docmaImgSrcList.length-1)) || !startsWith(docmaImgSrcList[pos+1], src_val))) {
          inp.value = imgurl;
          inp.setSelectionRange(src_val.length, imgurl.length);
          setImgTitleOnChange();  // update title fields immediately 
      }
    }
  }
}

function setImgSrcCursorToEnd() {
    var urlBox = docmaImgDialog.getContentElement( 'info', 'src' );
    var url = urlBox.getValue();
    var inp = urlBox.getInputElement().$;
    inp.setSelectionRange(url.length, url.length);
}

function startsWith(str, prefix) {
    return (prefix.length <= str.length) && (prefix == str.substr(0,prefix.length));
}

function getImgTitle(imgurl) {
    return docmaImgTitles[imgurl];
}

function imageExists(imgurl) {
    return (binImgSearch(0, docmaImgSrcList.length - 1, imgurl) >= 0);
}

function binImgSearch(i,j,val) {
    if (j < i) { return -(i+1); }
    else {
      var m = Math.floor((i+j)/2);
      if (val < docmaImgSrcList[m]) {
        return binImgSearch(i,m-1,val);
      } else
      if (val > docmaImgSrcList[m]) {
        return binImgSearch(m+1,j,val);
      } else {
        return m;
      }
    }
}

function compareSelOptions(a, b) {
    var s1 = a[0].toLowerCase();
    var s2 = b[0].toLowerCase();
    return (s1 < s2) ? -1 : ((s1 == s2) ? 0 : 1);
}

function setupCKList(ckSelect, arr) {
    ckSelect.clear();
    for (var i = 0; i < arr.length; i++) {
        var opt = arr[i];
        if (opt.length > 1) ckSelect.add(opt[0], opt[1]);
        else ckSelect.add(opt[0]);
    }
}

function setupClassOptions(ckSelect, arr) {
    setupCKList(ckSelect, arr);
}

function setupLinkOptions(ckSelect, arr) {
    setupCKList(ckSelect, arr);
}

function setTableClassField( dialog, stylesNew ) {
    var clsField = dialog.getContentElement( 'advanced', 'advCSSClasses' );
    var pageBreakBox = dialog.getContentElement( 'advanced', 'docmaTableAllowPageBreak' );
    var clsVal = pageBreakBox.getValue() ? addStyleClass(stylesNew, 'keep_together_auto') : stylesNew;
    clsField.setValue(clsVal, true);
}

function removeStyleClass(str, cls) {
    if ((cls == null) || (cls == '')) {
        return str;
    }
    var str2 = ' ' + str + ' ';
    var pos = str2.indexOf(' ' + cls + ' ');
    if (pos >= 0)  {
        return (str2.substring(0, pos + 1) + str2.substring(pos + cls.length + 2)).trim();
    } else {
        return str;
    }
}

function addStyleClass(str, cls) {
    if ((cls == null) || (cls == '')) {
        return str;
    }
    if ((str == null) || (str == '')) {
        return cls;
    }
    var str2 = ' ' + str + ' ';
    if (str2.indexOf(' ' + cls + ' ') < 0) {
        return trimSpaces(str)  + ' ' + cls;
    } else {
        return str;
    }
}

// function setTableDialogStyleID(uiSelectbox, clsVal) {
//     var clsArr = clsVal.split(' ');
//     var clsVal2 = ' ' + clsVal + ' ';
//     for (var i=0; i < clsArr.length; i++) {
//         var clsName = clsArr[i];
//         for (var j=0; j < uiSelectbox.items.length; j++) {
//             if (uiSelectbox.items[j][0] == clsName) {
//                 uiSelectbox.setValue(clsName, true);
//                 uiSelectbox.docmaOldValue = clsName;
//                 return;
//             }
//         }
//     }
//     uiSelectbox.setValue('', true);
//     uiSelectbox.docmaOldValue = '';
// }

function trimSpaces(txt) {
    while ((txt.length > 0) && (txt[0] == ' ')) txt = txt.substr(1);
    var p = txt.length - 1;
    while ((p >= 0) && (txt[p] == ' ')) { 
        txt = txt.substr(0, p--);
    }
    return txt;
}

function arrayContains(arr, val) {
    for (var i = 0; i < arr.length; i++) {
        if (arr[i] == val) return true;
    }
    return false;
}

function alertObject(obj) {
    if (obj == null) {
       alert("Object is null: " + obj);
    }
    var output = '';
    for (var property in obj) {
      output += property + ': ' + obj[property]+'; ';
    }
    alert(output);
}
