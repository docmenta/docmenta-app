<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="java.util.*,javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*,org.zkoss.zul.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<%
    String deskid = request.getParameter("desk");
    String clsName = request.getParameter("cls");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    MainWindow mainwin = docmaWebSess.getMainWindow();
    Window dialog = (Window) mainwin.getPage().getFellow("RuleConfigDialog");
    RuleConfigComposer composer = (RuleConfigComposer) dialog.getAttribute("$composer");
    String info = composer.getLongInfo(clsName);
    String title = clsName;
%>
<title><%= title %></title>
</head>
<body>
<h3><%= clsName %></h3>
<div style="font-family:Arial,sans-serif; font-size:0.9em;">
<%= info %>
</div>
</body>
</html>
