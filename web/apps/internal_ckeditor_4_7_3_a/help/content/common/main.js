var webhelp_options = {  // default options
  search_ui_mode: "tab",
  tree_title: false,
  treeview: true,
  treeview_collapsed: true,
  treeview_unique: false,
  treeview_animated: "medium",
  content_top: 104,
  content_left: 280,
  breadcrumbs_hidden: false,
  breadcrumbs_separator_img: "",
  search_button_text_hidden: false,
  accordion_enabled: false
};
webhelp_options.search_ui_mode = "fixed";
webhelp_options.tree_title = true;
webhelp_options.treeview = true;
webhelp_options.treeview_collapsed = true;
webhelp_options.treeview_unique = false;
webhelp_options.treeview_animated = "slow";
webhelp_options.content_top = 80;
webhelp_options.content_left = 240;
webhelp_options.search_width = 0;
webhelp_options.showhidetoc_function = customShowHideToc;
webhelp_options.resizetoc_function = customResizeToc;
webhelp_options.resizetoc_enabled = true;
webhelp_options.breadcrumbs_hidden = false;
webhelp_options.breadcrumbs_separator_img = "";
webhelp_options.search_button_text_hidden = true;
webhelp_options.accordion_enabled = false;

function customShowHideToc() {
    // var headerDiv = $("#header");
    var showHideButton = $("#showHideButton");
    var leftNavigation = $("#leftnavigation");
    var content = $("#content");
    // var partnav = $("#bookpartsnavigation");
    var crumbsnav = $("#breadcrumbsnavigation");

    if (showHideButton != undefined && showHideButton.hasClass("pointLeft")) {
        //Hide TOC
        showHideButton.removeClass('pointLeft').addClass('pointRight');
        content.css("margin-left", "0px");
        // partnav.css("left", "0px");
        crumbsnav.css("left", "0px");
        // headerDiv.css("left", (-webhelp_options.content_left - 100) + "px");  // hide by moving out of display area
        leftNavigation.css("display","none");
        showHideButton.attr("title", "Show the TOC tree");
        $.cookie('webhelpTocState', 'hidden');
    } else {
        //Show the TOC
        showHideButton.removeClass('pointRight').addClass('pointLeft');
        content.css("margin-left", webhelp_options.content_left + "px");
        // partnav.css("left", webhelp_options.content_left + "px");
        crumbsnav.css("left", webhelp_options.content_left + "px");
        // headerDiv.css("left","0px");
        leftNavigation.css("display","block");
        showHideButton.attr("title", "Hide the TOC Tree");
        $.cookie('webhelpTocState', '');
        // Restore resized toc width
        if (webhelp_options.resizetoc_enabled) {
          var nav_w = $.cookie('docma-leftnav-width');
          if ((nav_w != null) && (nav_w > 0)) {
            webhelp_options.resizetoc_function(nav_w);
          }
        }
    }
}

function customResizeToc(nav_width) {
  $("#leftnavigation").css("width", nav_width + "px");
  $("#leftnavigation").css("height", "auto");
  $("#content").css("margin-left", nav_width + "px");
  // $("#bookpartsnavigation").css("left", nav_width + "px");
  $("#breadcrumbsnavigation").css("left", nav_width + "px");
  // $("#header").css("width", nav_width + "px");
  if ((webhelp_options.search_ui_mode == "fixed") && ((webhelp_options.search_width == null) || (webhelp_options.search_width <= 0))) {
    $("#search").css("width", nav_width + "px");
  }
}


var has_search_tab = (webhelp_options.search_ui_mode == "tab");
var has_bread_sep_image = (webhelp_options.breadcrumbs_separator_img != null) && (webhelp_options.breadcrumbs_separator_img.length > 0);

/**
 * Miscellaneous js functions for WebHelp
 * Kasun Gajasinghe, http://kasunbg.blogspot.com
 * David Cramer, http://www.thingbag.net
 * Adapted by Manfred Paula, http://www.docmenta.org
 */

$(document).ready(function() {  
    // Show/hide breadcrumbs navigation. 
    // If separator image is configured, replace separator-character by image.
    if (webhelp_options.breadcrumbs_hidden) {
      $("#breadcrumbsnavigation").remove();
    }
    if (has_bread_sep_image) {
      $("#breadcrumbsnavigation .breadcrumb_separator").replaceWith('<img src="' + webhelp_options.breadcrumbs_separator_img + '" class="breadcrumb_sep_image" />');
    }
    // When you click on a link to an anchor, scroll down 
    // 105 px to cope with the fact that the banner
    // hides the top 95px or so of the page.
    // This code deals with the problem when 
    // you click on a link within a page.
    $('a[href*=#]').click(function() {
        if (location.pathname.replace(/^\//,'') == this.pathname.replace(/^\//,'')
            && location.hostname == this.hostname) {
            var $target = $(this.hash);
            $target = $target.length && $target
            || $('[name=' + this.hash.slice(1) +']');
        if (!(this.hash == "#searchDiv" || this.hash == "#treeDiv"  || this.hash == "") && $target.length) {
            var targetOffset = $target.offset().top - webhelp_options.content_top;
            $('html,body')
                .animate({scrollTop: targetOffset}, 200);
            return false;
            }
        }
        });

    //  $("#showHideHighlight").button(); //add jquery button styling to 'Go' button
    //Generate tabs in nav-pane with JQuery
    if (has_search_tab) {
      $("#tocsearchnavigation").attr("style","display:block;");
      $(function() {
            $("#tabs").tabs({
                cookie: {
                    // store cookie for 2 days.
                    expires: 2
                }
            });
        });
    } else {
      $("#tocsearchnavigation").attr("style","display:none;");
    }
    
    if (webhelp_options.tree_title) {
      $("#treenavigationtitle").attr("style","display:block;");
    }

    //Generate the tree
    $("#ulTreeDiv").attr("style","display:block;");  // must be visible before rendering treeview!
    if (webhelp_options.treeview) {
      var tree_root = $("#tree"); 
      if (webhelp_options.accordion_enabled) { 
          // tree_root.attr("style","padding:0; margin:0;");
          tree_root.removeClass("filetree");
          var li_roots = tree_root.children("li");
          // li_roots.attr("style","list-style:none; padding:0; margin:0; border-width:0;");  
          li_roots.addClass("nav_accordion_li");
          li_roots.children("span").removeClass("file").addClass("nav_accordion_tab");
          ul_roots = li_roots.children("ul");
          ul_roots.attr("style","display:none;");  // initially all accordion tabs are closed
          var current_li = $("#webhelp-currentid").closest(".nav_accordion_li");
          if (current_li.length <= 0) {
            tree_root = ul_roots.first();
          } else {
            tree_root = current_li.children("ul");
          }
          tree_root.attr("style","display:block;"); // open selected tab
          tree_root.addClass("filetree");
      }
      tree_root.treeview({
        collapsed: webhelp_options.treeview_collapsed,
        animated: webhelp_options.treeview_animated,
        // control: "#sidetreecontrol",
        unique: webhelp_options.treeview_unique,
        persist: "cookie"
      });
    }

    //after toc fully styled, display it. Until loading, a 'loading' image will be displayed
    $("#tocLoading").attr("style","display:none;");
    // $("#ulTreeDiv").attr("style","display:block;");

    var showHideButton = $("#showHideButton");
    if ((showHideButton != undefined) && showHideButton.hasClass("pointLeft") && ($.cookie('webhelpTocState') == 'hidden')) {
        showHideToc()
    }

    //.searchButton is the css class applied to 'Go' button 
    $(function() {
        $("button", ".searchButton").button();
        $("button", ".searchButton").click(function() { return false; });
    });

    //'ui-tabs-1' is the cookie name which is used for the persistence of the tabs.(Content/Search tab)
    if ((! has_search_tab) || ($.cookie('ui-tabs-1') === '1')) {    //search tab is visible 
        if ($.cookie('textToSearch') != undefined && $.cookie('textToSearch').length > 0) {
            document.getElementById('textToSearch').value = $.cookie('textToSearch');
            Verifie('diaSearch_Form');
            searchHighlight($.cookie('textToSearch'));
            $("#showHideHighlight").css("display","block");
            if (! has_search_tab) {
              $("#ulTreeDiv").attr("style","display:none;");
              $("#closeSearchResults").attr("style","display:block;");
            }
        }
    }

    syncToc(); //Synchronize the toc tree with the content pane, when loading the page.
    //$("#doSearch").button(); //add jquery button styling to 'Go' button
    if (webhelp_options.search_button_text_hidden) {
      var sbtn = document.getElementById("doSearch");
      if (sbtn != null) {
        sbtn.value=" ";
      }
    }

    if (webhelp_options.resizetoc_enabled) {
      initResizeToc();
    }
});

/**
 * Synchronize with the tableOfContents 
 */
function syncToc(){
    var a = document.getElementById("webhelp-currentid");
    if (a != undefined) {
        var a_cls = a.className;
        var b = a.getElementsByTagName("a")[0];
        if (b != undefined) {
            var b_cls = b.className;
            if ((a_cls != null) && (a_cls.length > 0)) {
                a.className = a.className + " webtree_current_block";
            } else {
                a.className = "webtree_current_block";
            }
            if ((b_cls != null) && (b_cls.length > 0)) {
                b.className = b.className + " webtree_current_text";
            } else {
                b.className = "webtree_current_text";
            }
            if (b.scrollIntoView) b.scrollIntoView(false);
        }
        var ulchildren = $(a).children("ul");
        if ((a.nodeName.toLowerCase() == "li") && ulchildren.length) {
            if ((a_cls == null) || (a_cls.indexOf("nav_accordion_li") < 0)) {
                a.setAttribute("class", "collapsable");
                a.firstChild.setAttribute("class", "hitarea collapsable-hitarea ");
            }
        }
        ulchildren.show();
        if (a.scrollIntoView) a.scrollIntoView(false);

        //shows the node related to current content.
        //goes a recursive call from current node to ancestor nodes, displaying all of them.
        while (a.parentNode && a.parentNode.nodeName) {
            var parentNode = a.parentNode;
            var nodeName = parentNode.nodeName;
            var clsName = parentNode.className;

            if (nodeName.toLowerCase() == "ul") {
                parentNode.setAttribute("style", "display: block;");
            } else if (nodeName.toLowerCase() == "li") {
                if ((clsName == null) || (clsName.indexOf("nav_accordion_li") < 0)) {
                    parentNode.setAttribute("class", "collapsable");
                    parentNode.firstChild.setAttribute("class", "hitarea collapsable-hitarea ");
                }
            }
            a = parentNode;
        }
    }
}

/**
 * Code for Show/Hide TOC
 *
 */
function showHideToc() {
    if (webhelp_options.showhidetoc_function) {
        webhelp_options.showhidetoc_function();
        return;
    }
    
    var showHideButton = $("#showHideButton");
    var leftNavigation = $("#leftnavigation");
    var content = $("#content");

    if (showHideButton != undefined && showHideButton.hasClass("pointLeft")) {
        //Hide TOC
        showHideButton.removeClass('pointLeft').addClass('pointRight');
        content.css("margin-left", "0px");
        leftNavigation.css("display","none");
        showHideButton.attr("title", "Show the TOC tree");
        $.cookie('webhelpTocState', 'hidden');
    } else {
        //Show the TOC
        showHideButton.removeClass('pointRight').addClass('pointLeft');
        content.css("margin-left", webhelp_options.content_left + "px");
        leftNavigation.css("display","block");
        showHideButton.attr("title", "Hide the TOC Tree");
        $.cookie('webhelpTocState', '');
        resizeToc();  // Restore resized toc width
    }
}

function initResizeToc() {
    var leftnav = $("#leftnavigation");
    leftnav.addClass("leftnav-resizable");
    resizeToc();
    leftnav.resizable({
      ghost:true,
      handles: "e",
      stop: function( event, ui ) {  // user has resized the toc
        webhelp_options.resizetoc_function(ui.size.width);
        $.cookie('docma-leftnav-width', ui.size.width);
      }
    });
}

function resizeToc() {
    if (webhelp_options.resizetoc_enabled && ($.cookie('webhelpTocState') != 'hidden')) {
        var nav_w = $.cookie('docma-leftnav-width');
        if ((nav_w != null) && (nav_w > 0)) {
            webhelp_options.resizetoc_function(nav_w);
        }
    }
}

/**
 * Code for searh highlighting
 */
var highlightOn = true;
function searchHighlight(searchText) {
    highlightOn = true;
    if (searchText != undefined) {
        var wList;
        var sList = new Array();    //stem list 
        //Highlight the search terms
        searchText = searchText.toLowerCase().replace(/<\//g, "_st_").replace(/\$_/g, "_di_").replace(/\.|%2C|%3B|%21|%3A|@|\/|\*/g, " ").replace(/(%20)+/g, " ").replace(/_st_/g, "</").replace(/_di_/g, "%24_")
        searchText = searchText.replace(/  +/g, " ");
        searchText = searchText.replace(/ $/, "").replace(/^ /, "");

        wList = searchText.split(" ");
        $("#content").highlight(wList); //Highlight the search input

        if(typeof stemmer != "undefined" ){
            //Highlight the stems
            for (var i = 0; i < wList.length; i++) {
                var stemW = stemmer(wList[i]);
                sList.push(stemW);
            }
        } else {
            sList = wList;
        }
        $("#content").highlight(sList); //Highlight the search input's all stems
    } 
}

function searchUnhighlight(){
    highlightOn = false;
     //unhighlight the search input's all stems
    $("#content").unhighlight();
    $("#content").unhighlight();
}

function toggleHighlight(){
    if(highlightOn) {
        searchUnhighlight();
    } else {
        searchHighlight($.cookie('textToSearch'));
    }
}

function startWebHelpSearch(search_form) {
    if (! has_search_tab) {
        $("#ulTreeDiv").attr("style","display:none;");
        $("#closeSearchResults").attr("style","display:block;");
        $("#searchResults").attr("style","display:block;");
    }
    Verifie(search_form);
}

function closeSearchShowToc() {
    if (! has_search_tab) {
        $("#ulTreeDiv").attr("style","display:block;");
        $("#closeSearchResults").attr("style","display:none;");
        $("#searchResults").attr("style","display:none;");
        document.getElementById('textToSearch').value = '';
        $.cookie('textToSearch', '');
    }
}
