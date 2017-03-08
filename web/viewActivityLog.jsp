<%@page import="java.util.Arrays"%>
<%@page contentType="text/html" pageEncoding="UTF-8"
        session="true"
        import="org.docma.util.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*,org.docma.coreapi.implementation.*,org.docma.plugin.*" 
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
    String activityId = request.getParameter("activity");
    String storeId = request.getParameter("storeid");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    DocmaWebApplication docmaWebApp = docmaWebSess.getDocmaWebApplication();
    DocI18n i18n = docmaWebApp.getI18n();
    String err_msg = null;
    Activity act;
    if ((activityId != null) && !activityId.equals("")) {
        act = docmaSess.getActivityById(Long.parseLong(activityId));
        if (act == null) {
            err_msg = i18n.getLabel("activity.not_found", activityId);
        }
    } else {
        act = docmaSess.getDocStoreActivity(storeId);
        if (act == null) {
            err_msg = i18n.getLabel("activity.not_found", storeId);
        }
    }
    String act_title = (act == null) ? "Log" : i18n.getLabel(act.getTitleKey(), act.getTitleArgs());
%>
<title>Log: <%= act_title %></title>
<style type="text/css">
  body, pre { font-family:Arial,sans-serif; }
  .log_msg { margin-top:10pt; }
  .log_header0 { font-size:1.4em; font-weight:bold; }
  .log_header1 { font-size:1.3em; font-weight:bold; font-style:italic; }
  .log_header2 { font-size:1.2em; font-weight:bold; }
  .log_header3 { font-size:1.1em; font-weight:bold; font-style:italic; }
  .msg_head_error   { font-weight:bold; color:red; margin-left:15pt; }
  .msg_head_warning { font-weight:bold; color:blue; margin-left:15pt; }
  .msg_head_info    { font-weight:bold; color:black; margin-left:15pt; }
  .msg_content { margin-top:0px; margin-left:15pt; padding:3pt; }
  .msg_head_error + .msg_content { color:red; }
  .msg_head_warning + .msg_content { color:blue; }
</style>
</head>
<body>
  <h2><%= act_title %></h2>
<%
    if (act == null) {
        out.println("<p><b>" + err_msg + "</b></p>");
    } else {
        int total = 0;
        int lastPos = 0;
        boolean finished;
        do {
            finished = act.isFinished();
            int cnt = act.getLogCount();
            if (cnt > lastPos) {
                LogEntry[] log = act.getLog(lastPos, cnt);
                out.println(DefaultLog.toHTMLString(log));
                total += log.length;
                lastPos = cnt;
            } else if (! finished) {
                Thread.sleep(50);
            }
        } while (! finished);
        if (total == 0) {
            out.println("<p><b>" + i18n.getLabel("activity.log_empty") + "</b></p>");
        }
    }
%>
</body>
</html>
