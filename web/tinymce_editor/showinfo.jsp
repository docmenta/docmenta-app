<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page session="true"%>
<%@page import="java.io.*" %>
<%@page import="java.util.*" %>
<%! 

    private String createtab(HttpServletResponse resp, int tabnum, String title, int seltab) {
        return "<li" + 
                ((tabnum == seltab) ? " id=\"current\">" : ">") + 
                "<a href=\"" + 
                resp.encodeURL("main.jsp?tab=" + tabnum) + "\">" +
                title + "</a></li>";
    }
%>

<% 
    HttpServletRequest req = request;
    HttpServletResponse resp = response;

    // DocmaService docService = (DocmaService) session.getAttribute(DocmaService.class.getName());
    // if (docService == null) {  
    //     response.sendRedirect("login.jsp");
    //     return;
    // }
    
    String param_tab = req.getParameter("tab");
    
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Cache-Control" content="no-cache">
    <%-- <meta http-equiv="Expires" content="0"> --%>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Servlet Info</title>
</head>
<body>

            <select>
                  <option>MeinProdukt</option>
                  <% 
                      for (int i=0; i < 4; i++) {
                          String sel = "";
                          String sid = "";
                          // if ((openedStore != null) && (openedStore.equals(sid))) sel = "selected";
                          out.println("<option " + sel + ">" + sid + "</option>");
                      }
                  %>
            </select>

            <p>
                ContextPath: <%= request.getContextPath() %> <br>
                Locale: <%= request.getLocale().toString() %> <br>
                PathInfo: <%= request.getPathInfo() %> <br>
                PathTranslated: <%= request.getPathTranslated() %> <br>
                QueryString: <%= request.getQueryString() %> <br>
                RequestURI: <%= request.getRequestURI() %> <br>
                RequestURL: <%= request.getRequestURL().toString() %> <br>
                ServletPath: <%= request.getServletPath() %> <br>
            </p>

    
</body>
</html>
