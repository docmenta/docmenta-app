<%@page import="java.util.Arrays"%>
<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.util.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*,org.docma.coreapi.implementation.*" 
%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<% 
    String deskid = request.getParameter("desk");
    String storeId = request.getParameter("storeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaWebApplication docmaWebApp = docmaWebSess.getDocmaWebApplication();
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    Activity act = docmaSess.getDocStoreActivity(storeId);
    DocI18n i18n = docmaWebApp.getI18n();
    String act_title = i18n.getLabel(act.getTitleKey(), act.getTitleArgs());
%>
<title>Log: <%= act_title %></title>
<style type="text/css">
  body, pre { font-family:Arial,sans-serif; }
  .log_msg { margin-top:10pt; }
  .msg_head_error   { font-weight:bold; color:red; }
  .msg_head_warning { font-weight:bold; color:blue; }
  .msg_head_info    { font-weight:bold; color:black; }
  .msg_content { margin-top:0px; margin-left:15pt; padding:3pt; background-color:#F0F0F0; }
</style>
</head>
<body>
  <h2>Log: <%= act_title %></h2>
<%
    LogMessage[] log = act.getLog(true, true, true);
    if (log.length == 0) {
        out.println("<p><b>Log is empty!</b></p>");
    } else {
        out.println(DefaultLog.toHTMLString(Arrays.asList(log)));
    }
%>
</body>
</html>
