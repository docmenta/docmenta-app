<%@page session="true"
        import="java.io.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*"
%><%!
    DocImageRendition rendition_default = null;
    DocImageRendition rendition_big = null;
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
    
    DocImageRendition rend;
    if (is_big) {
        if (rendition_big == null) {
            rendition_big = new DocImageRendition("thumb_big", 
                                                  DocImageRendition.FORMAT_PNG,
                                                  ThumbDimensions.SIZE_BIG, ThumbDimensions.SIZE_BIG);
        }
        rend = rendition_big;
    } else {
        if (rendition_default == null) {
            rendition_default = new DocImageRendition("thumb", 
                                                      DocImageRendition.FORMAT_PNG,
                                                      ThumbDimensions.SIZE_NORMAL, ThumbDimensions.SIZE_NORMAL);
        }
        rend = rendition_default;
    }

    OutputStream streamout = response.getOutputStream();
    byte[] thumb = node.getImageRendition(rend);

    response.setContentType("image/png");
    response.setContentLength(thumb.length);

    streamout.write(thumb);
%>