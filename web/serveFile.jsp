<%@page session="true"
        import="java.io.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*,org.docma.util.*"
%><%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);

    String cont_type = node.getContentType();
    if ((cont_type == null) || (cont_type.length() == 0)) {
        cont_type = "application/octet-stream";
    }
    response.setContentType(cont_type);
    int len = (int) node.getContentLength();
    if (len >= 0) response.setContentLength(len);

    // response.setHeader("expires", "0");
    // response.setHeader("cache-control", "no-cache");
    // response.setHeader("pragma", "no-cache");

    OutputStream streamout = response.getOutputStream();
    InputStream streamin = node.getContentStream();
    DocmaUtil.copyStream(streamin, streamout);
    streamin.close();
    // streamout.write(node.getContent());
%>