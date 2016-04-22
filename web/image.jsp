<%@page session="true"
        import="java.io.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*"
%><%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);

    response.setContentType(node.getContentType());
    int len = (int) node.getContentLength();
    if (len >= 0) response.setContentLength(len);

    // response.setHeader("expires", "0");
    // response.setHeader("cache-control", "no-cache");
    // response.setHeader("pragma", "no-cache");

    OutputStream streamout = response.getOutputStream();
    streamout.write(node.getContent());
%>