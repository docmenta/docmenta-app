  var XML_START = '<?xml version="1.0" encoding="UTF-8"?>';
  
  function docma_connect(usrname, usrpwd) {
    var params = XML_START + 
      "<connect>" + 
        "<userName>" + xesc(usrname) + "</userName>" + 
        "<userPwd>" + xesc(usrpwd) +  "</userPwd>" + 
      "</connect>"; 

    var domdoc = makeCall(URI_DOCMASERVICE, params, 0);
    return new DOMResult(domdoc);
  }

  function docma_getDocTree() {
    var params = XML_START + "<getDocTree />"; 

    var domdoc = makeCall(URI_DOCMASERVICE, params, 0);
    return new DOMResult(domdoc);
  }


  // **************************************************
  // ********    Helper object: DOMResult    **********
  // **************************************************

  function DOMResult(domobj) {
    this.domObject = domobj;
    this.childNodes = new Array();
    this.getDOMDocument = domres_getDOMDocument;
    this.isError = domres_isError;
    this.getErrorMsg = domres_getErrorMsg;
    this.getString = domres_getString;
    this.getStringArray = domres_getStringArray;
  }

  function domres_getDOMDocument() {
    return this.domObject;
  }

  function domres_isError() {
    var root = this.domObject.documentElement;
    return (root.nodeName.toLowerCase() == "error");
  }

  function domres_getErrorMsg() {
    if (this.isError()) {
      var mnode = this.domObject.documentElement.getElementsByTagName("message")[0];
      return getNodeText(mnode);
    } else {
      return "";
    }
  }

  function domres_getString(paramName) {
    var root = this.domObject.documentElement;
    var paramNode = root.getElementsByTagName(paramName)[0];
    return getNodeText(paramNode);
  }

  function domres_getStringArray(paramName) {
    var root = this.domObject.documentElement;
    var paramNode = root.getElementsByTagName(paramName)[0];
    return getChildNodeTexts(paramNode, "value");
  }

  // **************************************************
  // *************    Helper functions    *************
  // **************************************************

  function getNodeText(domnode) {
    var str = "";
    for (var i=0; i < domnode.childNodes.length; i++) {
      str += domnode.childNodes[i].nodeValue;
    }
    return str;
  }

  function getChildNodeTexts(domnode, childname) {
    var valNodes = domnode.getElementsByTagName(childname);
    var resarr = new Array(valNodes.length);
    for (var i=0; i < valNodes.length; i++) {
      resarr[i] = getNodeText(valNodes[i]);
    }
    return resarr;
  }

  function xesc(value) {
    return value;
  }

