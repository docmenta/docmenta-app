<%@page session="true"
        import="java.io.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*"
%><%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String thumb_size = request.getParameter("size");
    boolean is_big = false;
    if (thumb_size != null) {
        try {
            int sz = Integer.parseInt(thumb_size);
            is_big = (sz >= ThumbDimensions.SIZE_BIG);
        } catch (Exception ex) {}
    }
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);

    // response.setHeader("expires", "0");
    // response.setHeader("cache-control", "no-cache");
    // response.setHeader("pragma", "no-cache");

    // String cont_type = node.getContentType();
    
    String rend_name = is_big ? ImageRenditions.NAME_THUMB_BIG 
                              : ImageRenditions.NAME_THUMB_DEFAULT;
    DocImageRendition rend = ImageRenditions.getImageRenditionInfo(rend_name);
    String mime_type = DocImageRendition.getMIMETypeFromFormat(rend.getFormat());
    
    byte[] thumb = node.getImageRendition(rend);

    OutputStream streamout = response.getOutputStream();
    response.setContentType(mime_type);  // "image/png"
    response.setContentLength(thumb.length);
    
    streamout.write(thumb);
%>