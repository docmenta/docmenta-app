webhelp_options.search_ui_mode = ###search_ui_mode###;
webhelp_options.tree_title = ###tree_title_visible###;
webhelp_options.treeview = true;
webhelp_options.treeview_collapsed = ###tree_collapsed###;
webhelp_options.treeview_unique = ###tree_unique###;
webhelp_options.treeview_animated = ###tree_animated###;
webhelp_options.content_top = ###content_top_integer###;
webhelp_options.content_left = ###navigation_width_integer###;
webhelp_options.search_width = ###search_width_integer###;
webhelp_options.showhidetoc_function = customShowHideToc;
webhelp_options.resizetoc_function = customResizeToc;
webhelp_options.resizetoc_enabled = ###resizetoc_enabled###;
webhelp_options.breadcrumbs_hidden = ###breadcrumbs_hidden###;
webhelp_options.breadcrumbs_separator_img = ###breadcrumbs_separator_image_url###;
webhelp_options.search_button_text_hidden = ###search_button_text_hidden###;
webhelp_options.accordion_enabled = ###accordion_enabled###;

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
