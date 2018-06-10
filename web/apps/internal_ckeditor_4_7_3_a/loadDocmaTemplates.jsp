<%@page contentType="text/javascript" pageEncoding="UTF-8" session="true"
        import="org.docma.plugin.*,org.docma.plugin.web.*" 
%>
<% 
    String docsess = request.getParameter("docsess");
    // String editorId = request.getParameter("appid");
    WebUserSession webSess = WebPluginUtil.getUserSession(application, docsess);
    StoreConnection storeConn = webSess.getOpenedStore();
    if (storeConn == null) {
        throw new Exception("Store connection of user session is closed!");
    }
    String storeid = storeConn.getStoreId();
    String verid = storeConn.getVersionId().toString();
    
    String serv_url = request.getContextPath();
    if (! serv_url.endsWith("/")) { 
        serv_url += "/";
    }
    String base_url = serv_url + "servmedia/" + docsess + "/" + storeid + "/" + verid +
                      "/" + storeConn.getCurrentLanguage().getCode() + "/";
    
    Node[] templNodes = null;
    Node templFolder = storeConn.getNodeByAlias("system_templates");
    if (templFolder instanceof Group) {
        templNodes = ((Group) templFolder).getChildren();
    }
%>
CKEDITOR.addTemplates( 'docma_templates', {
    // The name of sub folder which hold the shortcut preview images of the
    // templates.
    // imagesPath: CKEDITOR.getUrl( CKEDITOR.plugins.getPath( 'templates' ) + 'templates/images/' ),
    imagesPath: '<%= base_url %>image/',

    // The templates definitions.
    templates: [ 
<% 
    if (templNodes != null) {
        boolean notfirst = false;
        for (Node nd : templNodes) {
            if (! (nd instanceof PubContent)) {
                continue;
            }
            PubContent templ = (PubContent) nd;
            // String mime = templ.getContentType();
            // boolean isHtml = (mime != null) && (mime.contains("/html") || mime.contains("/xhtml"));
            // if (! isHtml) {
            //     continue;
            // }
            
            // String templId = templ.getId();
            String iconAlias = null;
            String alias = templ.getAlias();
            if (alias != null) {
                iconAlias = alias + "_icon";
            }
            Node icon = storeConn.getNodeByAlias(iconAlias);
            
            String txt = templ.getTitleEntityEncoded();
            if ((txt == null) || txt.equals("")) {
                continue;  // skip templates without title
            }
            txt = txt.replace('"', ' ').replace('\\', '_');
            
            String comment = templ.getAttributeEntityEncoded("comment");
            comment = (comment != null) ? comment.trim() : "";
            comment = comment.replace('"', ' ').replace('\\', '_')
                              .replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f");
            
            String htm = templ.getContentString();
            if ((htm == null) || htm.equals("")) {
                continue;
            }
            htm = htm.replace("\"", "&quot;")
                     .replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f");
            
            if (notfirst) { 
                out.println(",");
            }
            out.println(" { ");
            out.println(" title: \"" + txt + "\", ");
            if (icon instanceof ImageFile) {
                out.println(" image: \"" + iconAlias + "\", ");
            }
            if (! comment.equals("")) {
                out.println(" description: \"" + comment + "\", ");
            }
            out.println(" html: \"" + htm + "\"");
            out.println(" } ");
            
            notfirst = true;
        }  // for
    }
%>
    ]
} );
