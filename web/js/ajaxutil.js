
var ajaxReq = null;
var currentReqNr = 0;

function docmaCall(data) {
    return makeCall("DocmaServlet", data, 0);
}

function makeCall(uri, data, timeout) {
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
        abortIfWaitingCall(currentReqNr);
    }
    currentReqNr++;
    // window.status = "Bitte warten ...";
    // var node = document.getElementById("container");
    // node.style.cursor = 'wait';
    // var timerid = window.setTimeout("abortIfWaitingCall(" + currentReqNr + ")", timeout);
    ajaxReq.open("POST", uri, false);
    ajaxReq.setRequestHeader("Content-Type", "text/xml");
    if (ajaxReq.overrideMimeType)  ajaxReq.overrideMimeType("text/xml");
    // ajaxReq.onreadystatechange = handlertest;
    ajaxReq.send(data);
    // alert("test");
    // window.clearTimeout(timerid); 
    // window.status = "";
    // node.style.cursor = 'default';
    if ((ajaxReq.readyState == 4) && (ajaxReq.status == 200)) {
        // alert(ajaxReq.getAllResponseHeaders());
        return ajaxReq.responseXML;
    } else {
        alert("Fehlerhafte Antwort vom Server: " + ajaxReq.statusText + 
              " (readyState=" + ajaxReq.readyState + " status=" + ajaxReq.status + ")");
    }
}


function abortIfWaitingCall(req_nr) {
    if (req_nr == currentReqNr) {
        if ((ajaxReq.readyState == 2) || (ajaxReq.readyState == 3)) {
            ajaxReq.abort();
            window.status = "Call aborted!";  // debug 
        }
    }
}

