<%@page contentType="text/css" 
        pageEncoding="UTF-8"
        session="true"
        import="javax.servlet.http.*,org.docma.webapp.*,org.docma.app.*"
%><%
    response.setHeader("expires", "0");
    response.setHeader("cache-control", "no-cache");
    response.setHeader("pragma", "no-cache");

    String deskid = request.getParameter("desk");
    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();
    MainWindow mainWin = docmaWebSess.getMainWindow();

    DocmaAppUtil.writeContentCSS(docmaSess, mainWin.getPreviewHTMLConfig(), false, false, out);
    // DocmaStyle default_style = docmaSess.getStyle("default");
    // String def_css;
    // if (default_style == null) {
    //     def_css = DocmaConstants.STYLE_DEFAULT_CSS;
    // } else {
    //     def_css = default_style.getCSS();
    // }
    // out.println("body {" + DocmaConstants.STYLE_DEFAULT_CSS + "}");
    // out.println("p, div, td, th {" + def_css + "}");
    //
    // for (int i=1; i <= 6; i++) {
    //     DocmaStyle h_style = docmaSess.getStyle("header" + i);
    //     String h_css = (h_style == null) ? "" : h_style.getCSS();
    //     out.println("h" + i + " {" + h_css + "}");
    // }
    //
    // out.print(docmaSess.getCSS());
%>
