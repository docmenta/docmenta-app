  var ajaxReq = null;
  
  var popupVisible = false;
  var popupHasFocus = false;
  var allowPopup = true;
  var popupX = 0;
  var popupY = 0;
  var lastOver = null;
  var ID_PREFIX = "entry_";
  var EMPTY_PREFIX = "empty_entry_";
  // var entryHash = new Object();

  function testalert() {
    alert("soweit ok");
  }

  function abortWaitingCall() {
    if ((ajaxReq.readyState == 2) || (ajaxReq.readyState == 3)) { 
        ajaxReq.abort();
    } 
  }

  function makeCall(uri, callbackhandler, timeout) {
    if (ajaxReq == null) {
        if (window.XMLHttpRequest) {
            ajaxReq = new XMLHttpRequest();
        } else 
        if (window.ActiveXObject) {
            ajaxReq = new ActiveXObject("Microsoft.XMLHTTP");
        } else {
            alert("Browser does not support Ajax!");
            return false;
        }
    } else {
        // abortWaitingCall();
    }
    // window.status = "Bitte warten ...";
    // var node = document.getElementById("container");
    // node.style.cursor = 'wait';
    // var timerid = window.setTimeout("abortWaitingCall()", timeout);
    ajaxReq.open("POST", uri, false);
    ajaxReq.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    if (ajaxReq.overrideMimeType)  ajaxReq.overrideMimeType("text/xml");
    // ajaxReq.onreadystatechange = handlertest;
    ajaxReq.send(null);
    // alert("test");
    // window.clearTimeout(timerid); 
    // window.status = "";
    // node.style.cursor = 'default';
    if ((ajaxReq.readyState == 4) && (ajaxReq.status == 200)) {
       // alert(ajaxReq.getAllResponseHeaders());
       callbackhandler(ajaxReq.responseXML);
    } else {
        alert("Fehlerhafte Antwort vom Server: " + ajaxReq.statusText + 
              " (readyState=" + ajaxReq.readyState + " status=" + ajaxReq.status + ")");
    }
  }

  function handlertest() {
    if ((ajaxReq.readyState == 4) && (ajaxReq.status == 200)) {
        alert("OK");
    } else {
       alert("Fehlerhafte Antwort vom Server: " + ajaxReq.statusText + 
             " (readyState=" + ajaxReq.readyState + " status=" + ajaxReq.status + ")");
    }
  }

  function loaddoctree() {
    makeCall("treedata.jsp", showLoadedDocTree);
  }

  function showLoadedDocTree(domdoc) {
    alert(domdoc.documentElement.firstChild.nodeName);
    var mynode = createGroupNode(domdoc.documentElement.firstChild); 
    var docroot = document.getElementById("docroot");
    docroot.appendChild(mynode);
  }

  function appendLevelImgs(divObj, domObj) {
    var domParent = domObj.parentNode;
    if ((domParent != null) && (domParent.nodeName == "group")) {
      var levelimg = document.createElement("img");
      var childarr = domParent.childNodes;
      if ((childarr.length == 0) || (domObj == childarr[childarr.length-1])) {  // is last child
        levelimg.src = "img/foldlineend.gif";
      } else {
        levelimg.src = "img/foldlinemid.gif";
      }
      divObj.appendChild(levelimg);
      var tempGroup = domParent;
      domParent = tempGroup.parentNode;
      while ((domParent != null) && (domParent.nodeName == "group")) {
        var foldimg = document.createElement("img");
        if (tempGroup == domParent.childNodes[domParent.childNodes.length-1]) {  // is last child
          foldimg.src = "img/foldspace.gif";
        } else {
          foldimg.src = "img/foldlinevert.gif";
        }
        divObj.insertBefore(foldimg, divObj.firstChild);
        tempGroup = domParent;
        domParent = tempGroup.parentNode;
      }
    }
  }
  
  function addEntryEventHandler(divObj) {
    divObj.onclick = popupShow;
    divObj.onmouseover = mOver;
    divObj.onmouseout = mOut;
  }

  function createEntryNode(domObj) {
    if (domObj.nodeName == "group") return createGroupNode(domObj);
    else return createDocNode(domObj);
  }
  
  function createEmptyNode(parGroup) {
    var dummyObj = new Object();
    dummyObj.parentNode = parGroup;
    var node = document.createElement("div");
    node.setAttribute("id", EMPTY_PREFIX + parGroup.getAttribute("alias"));
    appendLevelImgs(node, dummyObj);
    addEntryEventHandler(node);
    var inode = document.createElement("i");
    inode.style.color = "#999999";
    inode.style.fontWeight = "normal";
    inode.appendChild(document.createTextNode(" einf\xfcgen"));
    // inode.innerHTML = "einf&uuml;gen";
    node.appendChild(inode);
    return node;
  }
  
  function createGroupNode(groupObj) {
    var node = document.createElement("div");
    node.setAttribute("id", ID_PREFIX + groupObj.getAttribute("alias"));
    var headDiv = document.createElement("div");
    var contentDiv = document.createElement("div");
    appendLevelImgs(headDiv, groupObj);
    addEntryEventHandler(headDiv);
    var foldimg = document.createElement("img");
    if (groupObj.getAttribute("closed") == "true") {
      foldimg.src = "img/folderclosed.gif";
      contentDiv.style.display = "none";  // contentDiv.setAttribute("style", "display:none");
    } else {
      foldimg.src = "img/folderopen.gif";
      contentDiv.style.display = "block"; 
    }
    var anode = document.createElement("a");
    anode.href = "javascript:swapFolder('" + groupObj.getAttribute("alias") + "');";
    anode.onmouseover = disablePopup;
    anode.onmouseout = enablePopup;
    foldimg.setAttribute("border", "0");
    anode.appendChild(foldimg);
    headDiv.appendChild(anode);
    headDiv.appendChild(document.createTextNode(" " + groupObj.getAttribute("title")));
    var cnt = groupObj.childNodes.length;
    if (cnt == 0) {
        contentDiv.appendChild(createEmptyNode(groupObj));
    } else {
        for (var i=0; i < cnt; i++) {
          contentDiv.appendChild(createEntryNode(groupObj.childNodes[i]));
        }
        contentDiv.appendChild(createEmptyNode(groupObj));
    }
    node.appendChild(headDiv);
    node.appendChild(contentDiv);
    return node;
  }
  
  function createDocNode(docObj) {
    var node = document.createElement("div");
    node.setAttribute("id", ID_PREFIX + docObj.getAttribute("alias"));
    appendLevelImgs(node, docObj);
    addEntryEventHandler(node);
    var docimg = document.createElement("img");
    docimg.src = "img/docicon.gif";
    node.appendChild(docimg);
    node.appendChild(document.createTextNode(" " + docObj.getAttribute("title")));
    return node;
  }

  function swapFolder(alias) {
    // var entryObj = entryHash[alias];
    var entryNode = document.getElementById(ID_PREFIX + alias);
    var headdiv = entryNode.firstChild;
    var contdiv = entryNode.childNodes[1];
    var anode = null;
    for (var i=0; i < headdiv.childNodes.length; i++) {
      var tempnode = headdiv.childNodes[i];
      if (tempnode.nodeName.toLowerCase() == "a") anode = tempnode;
    }
    if (anode != null) {
      // entryObj.closed = !entryObj.closed;
      if (contdiv.style.display != "none") { 
        anode.firstChild.src = "img/folderclosed.gif";
        contdiv.style.display = "none";
      } else {
        anode.firstChild.src = "img/folderopen.gif";
        contdiv.style.display = "block";
      }
    }
  }

  function getClickPos(eventobj) {
    var pos = new Object();
    if (eventobj.pageX != null) {
      pos.x = eventobj.pageX;
      pos.y = eventobj.pageY;
    } else {  
      pos.x = eventobj.offsetX + 5;
      pos.y = eventobj.offsetY + 5;
    }
    return pos;
  }
  
  function popupShow(eventobj) {
    if (!allowPopup) return;
    if (!eventobj) eventobj = window.event;
    var pos = getClickPos(eventobj);
    popupX = pos.x
    popupY = pos.y
    // alert("X: " + eventobj.offsetX + "  Y: "+ eventobj.offsetY);
    // if (document.getElementById) {
    document.getElementById("popupmenu").style.position = "absolute";
    document.getElementById("popupmenu").style.left  = popupX + "px";
    document.getElementById("popupmenu").style.top = popupY + "px";
    document.getElementById("popupmenu").style.visibility  = "visible";
    popupVisible = true;
    // } else if (document.all) {
    //   document.all.popupmenu.style.left = eventobj.clientX;
    //   document.all.popupmenu.style.top = eventobj.clientY;
    // }
  }

  function popupHide() {
    document.getElementById("popupmenu").style.visibility  = "hidden";    
    popupVisible = false;
  }
  
  function enablePopup() {
    allowPopup = true;
  }

  function disablePopup() {
    allowPopup = false;
  }
  
  function popupOver(eventobj) {
    // if (!eventobj) eventobj = window.event;
    popupHasFocus = true;
  }

  function popupOut(eventobj) {
    // if (!eventobj) eventobj = window.event;
    popupHasFocus = false;
  }
    
  function mOver() {
    if (lastOver != null) lastOver.style.backgroundColor = 'transparent';
    this.style.backgroundColor = '#E7F1FA'; //'#EEEEEE';
    lastOver = this; //divobj;
    popupHide();
  }
  
  function mOut(eventobj) {
    if (!popupVisible && (lastOver != null)) lastOver.style.backgroundColor = 'transparent';
    if (!eventobj) eventobj = window.event;
    if (!popupHasFocus) {
      var now = getClickPos(eventobj);
      if ((now.x < popupX - 50) || (now.x > popupX + 100) || (now.y < popupY - 10)) popupHide();
    }
  }
  
