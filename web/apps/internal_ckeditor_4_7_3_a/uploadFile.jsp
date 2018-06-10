<%@page pageEncoding="UTF-8" session="true"
        import="java.io.*, java.util.*, org.docma.plugin.*, org.docma.plugin.web.*, org.docma.plugin.implementation.StoreHelper, org.apache.commons.io.FilenameUtils, org.apache.commons.fileupload.*, org.apache.commons.fileupload.disk.DiskFileItemFactory, org.apache.commons.fileupload.servlet.ServletFileUpload"
%><% 
    final String CK_FUNC_NUM = "CKEditorFuncNum";
    
    String error = null;
    String fileName = null;
    FileItem fileItem = null;
    String alias = null;
    
    try {
        
        List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
        
        for (FileItem item : items) {
            if (item.isFormField()) {
                // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                // System.out.println("fieldName: " + item.getFieldName() + "\n" +
                //                    "fieldValue: " + item.getString());
            } else {
                // File field (input type="file").
                fileItem = item;
                break;

                // String fieldName = item.getFieldName();
                // String s = "fieldName: " + fieldName + "\n" +
                //            "fileName: " + fileName + "\n";
            }
        }
        
        if (fileItem != null) {
            String docsess = request.getParameter("docsess");
            String ctxNodeId = request.getParameter("ctxNodeId");

            WebUserSession webSess = WebPluginUtil.getUserSession(application, docsess);
            StoreConnection storeConn = webSess.getOpenedStore();
            if (storeConn == null) {
                throw new Exception("Store connection of user session is closed!");
            }
        
            fileName = FilenameUtils.getName(fileItem.getName());
            InputStream fileContent = fileItem.getInputStream();
            
            alias = StoreHelper.saveImageFile(fileName, fileContent, storeConn, ctxNodeId);
        } else {
            error = "Missing file part!";
        }
    } catch (FileUploadException fe) {
        error = "Cannot parse multipart request: " + fe.getMessage();
        fe.printStackTrace();
    } catch (Exception ex) {
        error = "Exception: " + ex.getMessage();
        ex.printStackTrace();
    }

    String responseType = request.getParameter("responseType");
    String funcNum = request.getParameter(CK_FUNC_NUM);
    // String langCode = request.getParameter("langCode");
    // String upType = request.getParameter("type");   // e.g. "Images"

    boolean isJSON = "json".equalsIgnoreCase(responseType);
    String fileUrl = "image/" + alias;
    int p = fileName.lastIndexOf('.');
    // Use alias with file extension as new filename
    fileName = (p < 0) ? alias : alias + fileName.substring(p); 

    String res;

    if (isJSON) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (error == null) {
            res = "{ \"uploaded\": 1," +
                  "  \"fileName\": \"" + fileName + "\"," +
                  "  \"url\": \"" + fileUrl + "\" " +
                  "}";
        } else {
            error = error.replace('"', ' ');
            res = "{ \"uploaded\": 0," +
                  "  \"error\": { \"message\": \"" + error + "\" } " +
                  "}";
        }
    } else {
        // See CKEditor documentation:
        // https://docs.ckeditor.com/ckeditor4/latest/guide/dev_file_browser_api.html

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        res = "<html><body><script type=\"text/javascript\">" + 
              "window.parent.CKEDITOR.tools.callFunction(" + funcNum + ", '" + fileUrl;
        if (error != null) {
            res += "', '" + error.replace('\'', ' ');
        }
        res += "')</script></body></html>";
    }
    response.setHeader("Cache-Control", "no-cache");
    out.print(res);
    // out.flush();
%>