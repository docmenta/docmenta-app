<%@page contentType="application/pdf"
        session="true"
        import="java.io.*,org.docma.webapp.*,org.docma.app.*,org.docma.coreapi.*"
%><%
    String deskid = request.getParameter("desk");
    String nodeid = request.getParameter("nodeid");
    String pubid = request.getParameter("pub");
    String outid = request.getParameter("out");
    String temp_outid = request.getParameter("tempout");  // see MediaConfigDialog.onHeaderFooterPreview()
    String draft = request.getParameter("draft");

    DocmaWebSession docmaWebSess = GUIUtil.getDocmaWebSession(session, deskid);
    DocmaSession docmaSess = docmaWebSess.getDocmaSession();

    OutputStream streamout = response.getOutputStream();

    DocmaPublicationConfig pubconf = null;
    if ((pubid != null) && (pubid.trim().length() > 0)) {
        pubconf = docmaSess.getPublicationConfig(pubid);
    }
    DocmaOutputConfig outconf = null;
    if ((outid != null) && (outid.trim().length() > 0)) {
        outconf = docmaSess.getOutputConfig(outid);
    } else
    if ((temp_outid != null) && (temp_outid.trim().length() > 0)) {
        outconf = (DocmaOutputConfig) docmaWebSess.getSessionObject(temp_outid);
    }

    if ((draft != null) && draft.equals("true") && (pubconf != null)) {
        pubconf.setDraft(true);
    }
    
    DocmaExportContext export_ctx = new DocmaExportContext(docmaSess, pubconf, outconf, true);
    DocmaExportLog preview_log = export_ctx.getDocmaExportLog();
    docmaWebSess.setPreviewLog(preview_log);
    docmaSess.previewPDF(streamout, nodeid, export_ctx);
    export_ctx.finished();
%>