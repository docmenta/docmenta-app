<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="java.util.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>View image</title>
</head>
<body>
<%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaNode node = docmaSess.getNodeById(nodeid);

    String title = node.getTitle();
    if ((title == null) || title.trim().equals("")) {
        title = node.getDefaultFileName();
    }
    if (title == null) title = node.getId();
    title = title.replace('"', ' ');
    title = title.replace('\\', ' ');

    Date lastmod_date = node.getLastModifiedDate();
    long lastmod = (lastmod_date == null) ? System.currentTimeMillis() : lastmod_date.getTime();

    String url = response.encodeURL("image.jsp?desk=" + deskid +
                                    "&nodeid=" + nodeid +
                                    "&lastmod=" + lastmod);

%>
<img src="<%= url %>" alt="<%= title %>">
</body>
</html>
