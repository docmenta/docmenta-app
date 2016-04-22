  // *********************************************
  // *********    Global variables   *************
  // *********************************************

  // ****  Document tree variables  **** 
  var ID_PREFIX = "entry_";
  var EMPTY_PREFIX = "empty_entry_";
  var ROOT_ID = "root";

  var selectedNode = null;
  
  // ****  Popup menu variables  **** 
  var popupVisible = false;
  var popupHasFocus = false;
  var allowPopup = true;
  var popupX = 0;
  var popupY = 0;
  var popupScreenX = 0;
  var popupScreenY = 0;
  var lastOver = null;
  // var entryHash = new Object();


  // ******************************************************
  // ************    Popup menu functions   ***************
  // ******************************************************

  function newsection() {
    popupHide();
    // alert("" + popupScreenX + " / " + popupScreenY);
    var winx = (popupScreenX > 20) ? (popupScreenX - 20) : 0;
    var winy = (popupScreenY > 20) ? (popupScreenY - 20) : 0;
    var w = window.open(URI_NEWSEC, "win_newsec", "left=" + winx + ",top=" + winy + 
               ",dependent=yes,height=130,width=400,location=no,menubar=no,resizable=no,status=no,toolbar=no");
    w.focus();
  }

  function ok_newsection(sectitle, secid) {
    var insnode = selectedNode;
    var insid = insnode.getAttribute("id");
    var isappend = (insid.indexOf(EMPTY_PREFIX) == 0);
    // alert(insid);
    var groupObj = new DomDummy();
    groupObj.nodeName = "group";
    groupObj.alias = secid;
    groupObj.title = sectitle;
    groupObj.closed = "true";

    // var newnode = insnode.cloneNode(true);
    var node = insnode;
    var nid = insid;
    var gObj = groupObj;
    while ((nid != null) && (nid != ID_PREFIX + ROOT_ID)) {
      node = node.parentNode.parentNode;
      nid = node.getAttribute("id");
      // alert("nid: " + nid);

      var dummyParent = new DomDummy();
      dummyParent.childNodes[0] = gObj;
      dummyParent.nodeName = "group";
      gObj.parentNode = dummyParent;
      gObj = dummyParent;
    }
    
    var newnode = createGroupNode(groupObj);
    insnode.parentNode.insertBefore(newnode, insnode);

  }


  // ******************************************************
  // ***********    Document tree functions   *************
  // ******************************************************

  function loaddoctree() {
    // var domdoc = makeCall(URI_LOADTREE, null, 0);
    var domres = docma_getDocTree();
    if (domres.isError()) {
      window.alert(domres.getErrorMsg());
    } else {
      showLoadedDocTree(domres.getDOMDocument());
    }
  }

  function showLoadedDocTree(domdoc) {
    // alert(domdoc.documentElement.firstChild.nodeName);
    var doctree = domdoc.documentElement.firstChild;
    var rootnode = doctree.firstChild;
    ROOT_ID = rootnode.getAttribute("alias");
    var mynode = createGroupNode(rootnode); 
    var docroot = document.getElementById("docroot");
    docroot.appendChild(mynode);
  }

  function appendLevelImgs(divObj, domObj) {
    var domParent = domObj.parentNode;
    if ((domParent != null) && (domParent.nodeName == "group")) {  // if not root
      var levelimg = document.createElement("img");
      // var childarr = domParent.childNodes;
      // if ((childarr.length == 0) || (domObj == childarr[childarr.length-1])) {  
      if (domObj.isInsertLine) {
        levelimg.src = "img/foldlineend.gif";
      } else {
        levelimg.src = "img/foldlinemid.gif";
      }
      divObj.appendChild(levelimg);
      var tempGroup = domParent;
      domParent = tempGroup.parentNode;
      while ((domParent != null) && (domParent.nodeName == "group")) {
        var foldimg = document.createElement("img");
        // if (tempGroup == domParent.childNodes[domParent.childNodes.length-1]) {  // is last child
        //   foldimg.src = "img/foldspace.gif";
        // } else {
        foldimg.src = "img/foldlinevert.gif";
        // }
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
  
  function createInsertLine(parGroup) {
    var dummyObj = new Object();
    dummyObj.isInsertLine = true;
    dummyObj.parentNode = parGroup;
    var container = document.createElement("div");
    container.setAttribute("id", EMPTY_PREFIX + parGroup.getAttribute("alias"));
    var node = document.createElement("div");
    appendLevelImgs(node, dummyObj);
    addEntryEventHandler(node);
    var inode = document.createElement("i");
    inode.style.color = "#999999";
    inode.style.fontWeight = "normal";
    inode.appendChild(document.createTextNode(" anf\xfcgen"));
    // inode.innerHTML = "einf&uuml;gen";
    node.appendChild(inode);
    container.appendChild(node);
    return container;
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
    var btitle = document.createElement("b");
    btitle.appendChild(document.createTextNode(" " + groupObj.getAttribute("title")));
    headDiv.appendChild(btitle);
    var cnt = groupObj.childNodes.length;
    if (cnt == 0) {
        contentDiv.appendChild(createInsertLine(groupObj));
    } else {
        for (var i=0; i < cnt; i++) {
          contentDiv.appendChild(createEntryNode(groupObj.childNodes[i]));
        }
        contentDiv.appendChild(createInsertLine(groupObj));
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
    var bnode = document.createElement("b");
    bnode.appendChild(document.createTextNode(" " + docObj.getAttribute("title")));
    var span = document.createElement("span");
    span.appendChild(document.createTextNode("  [Alias: "));
    var b2node = document.createElement("b");
    b2node.appendChild(document.createTextNode(docObj.getAttribute("alias")));
    var span2 = document.createElement("span");
    span2.appendChild(document.createTextNode("]  "));
    var img_web = document.createElement("img");
    img_web.src = "img/webdoc.gif";
    var img_pdf = document.createElement("img");
    img_pdf.src = "img/pdfdoc.gif";
    var a_checkout = document.createElement("a");
    a_checkout.href = "";
    a_checkout.setAttribute("class", "colink");
    var abnode = document.createElement("b");
    if (docObj.getAttribute("checkedout") == "true") {
      abnode.appendChild(document.createTextNode("checkin"));
      abnode.style.color = "#CC0000";
    } else {
      abnode.appendChild(document.createTextNode("checkout"));
      abnode.style.color = "#00CC00";
    }
    a_checkout.appendChild(abnode);

    node.appendChild(docimg);
    node.appendChild(bnode);
    node.appendChild(span);
    node.appendChild(b2node);
    node.appendChild(span2);
    node.appendChild(img_web);
    node.appendChild(document.createTextNode(" "));
    node.appendChild(img_pdf);
    node.appendChild(document.createTextNode("  "));
    node.appendChild(a_checkout);

    var warnmsg = docObj.getAttribute("warn");
    if ((warnmsg != null) && (warnmsg != "")) {
      var warnspan = document.createElement("span");
      warnspan.appendChild(document.createTextNode("  [" + warnmsg + "]"));
      warnspan.style.color = "CC0000";
      node.appendChild(warnspan);
    }
    
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

  
  // ********************************************************
  // ***********    Popup menu implementation   *************
  // ********************************************************

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
    var node_id = this.parentNode.getAttribute("id");
    if (node_id == ID_PREFIX + ROOT_ID) return;
    if (!eventobj) eventobj = window.event;
    var pos = getClickPos(eventobj);
    popupX = pos.x;
    popupY = pos.y;
    popupScreenX = eventobj.screenX;
    popupScreenY = eventobj.screenY;
    
    // alert("X: " + eventobj.offsetX + "  Y: "+ eventobj.offsetY);
    // if (document.getElementById) {

    selectedNode = this.parentNode;  // the node that has the id attribute

    var isappend = (node_id != null) && (node_id.indexOf(EMPTY_PREFIX) == 0);
    var dispvalue = isappend ? "none" : "block";
    document.getElementById("pme_rename").style.display = dispvalue;
    document.getElementById("pme_sep1").style.display = dispvalue;
    document.getElementById("pme_cut").style.display = dispvalue;
    document.getElementById("pme_copy").style.display = dispvalue;
    document.getElementById("pme_paste").style.display = dispvalue;
    document.getElementById("pme_delete").style.display = dispvalue;
    document.getElementById("pme_sep2").style.display = dispvalue;
    document.getElementById("pme_revisions").style.display = dispvalue;

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
  

  // **************************************************
  // *************    Helper functions    *************
  // **************************************************

  function DomDummy() {
    this.parentNode = null;
    this.childNodes = new Array();
    this.getAttribute = dom_getAttribute;
  }

  function dom_getAttribute(attname) {
    return this[attname];
  }
