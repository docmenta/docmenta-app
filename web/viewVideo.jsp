<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="java.util.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*"
%><!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>View Video</title>
</head>
<body>
<%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);

    WebFolderEntry fe = new WebFolderEntry(node, deskid, docmaWebSess.getServMediaBasePath());
    String url = response.encodeURL(fe.getDownloadURL());
%>
<video src="<%= url %>" preload="auto" controls>
    Your browser does not support the video tag. <a href="<%= url %>">Download link</a>
</video>
</body>
</html>
