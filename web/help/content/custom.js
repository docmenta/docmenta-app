//
// Put your custom JavaScript code here and select this file in the HTML output configuration.
//

function initpage() {
  var cont = document.getElementById("content");
  var imgarr = cont.getElementsByTagName("img");
  var img_cnt = imgarr.length;
  for (var i=0; i < img_cnt; i++) {
    var img = imgarr[i];
    if (img.parentNode.className == 'mediaobject') { 
      img.onclick = openpic;
    }
  }
}

function openpic() {
  window.open(this.src, '_blank', 'location=no,menubar=yes,resizable=yes,scrollbars=yes,status=yes,toolbar=yes');
}

window.onload = initpage;
