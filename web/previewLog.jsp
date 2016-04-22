<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.util.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="expires" content="0">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<title>Preview Log</title>
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
  <h2>Preview Log</h2>
<%
    String deskid = request.getParameter("desk");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaExportLog preview_log = docmaWebSess.getPreviewLog();
    if (preview_log == null) {
        out.println("<p><b>Preview log is empty!</b></p>");
    } else {
        out.println(preview_log.toHTMLString());
    }
%>
</body>
</html>
