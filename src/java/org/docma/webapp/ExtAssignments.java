/*
 * ExtAssignments.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.webapp;

import java.util.*;
import java.io.IOException;
import javax.servlet.ServletContext;

import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class ExtAssignments 
{
    // public final static String APP_TYPE_EDITORS = "editors";
    // public final static String APP_TYPE_VIEWERS = "viewers";
    
    private final ServletContext servletCtx;
    // private final AppsLoader appsLoader;
    // private final String appType;
    
    // List of all assignments.
    private final List<ExtAssignment> assignments = new ArrayList<ExtAssignment>();
    // Map file extension to assigned application.
    private final Map<String, ExtAssignment> extMap = new HashMap<String, ExtAssignment>();
    // Map MIME type to assigned application.
    private final Map<String, ExtAssignment> mimeMap = new HashMap<String, ExtAssignment>();
    // Map file extension to default application id (given in constructor).
    // private final ExtAssignments defaultAssignments;
    
    ExtAssignments(ServletContext servletCtx) // required to map file extension to MIME type
    {
        this.servletCtx = servletCtx;
        // this.appsLoader = appsLoader;
        // this.appType = appType;
        // this.defaultAssignments = defaultAssignments;
//        if (defaultAssignments != null) {
//            for (ExtAssignment ea : defaultAssignments) {
//                String app_id = ea.getApplication();
//                if (app_id != null) {
//                    for (String ext : ea.listExtensions()) {
//                        defaultsMap.put(ext, app_id);
//                    }
//                }
//            }
//        }
    }

    public synchronized ExtAssignment[] listAssignments()
    {
        int cnt = assignments.size();
        ExtAssignment[] res = new ExtAssignment[cnt];
        try {
            for (int i = 0; i < cnt; i++) {
                res[i] = (ExtAssignment) assignments.get(i).clone();
            }
        } catch (Exception ex) {  // should never occur, but required for declared clone exception.
            ex.printStackTrace();
        }
        return res;
    }
    
    public synchronized void setAssignment(String ext, String applicationId)
    {
        if ((applicationId != null) && applicationId.equals("")) {
            applicationId = null;
        }
        ext = ExtAssignment.normalizeExt(ext);
        ExtAssignment ext_a = extMap.get(ext);
        
        if (ext_a != null) {
            ext_a.setAssignedApplication(applicationId);
            return;
        }
        
        // If extension does not exist yet, see if extension with same MIME type
        // already exists.
        String mime = getMimeTypeForExt(ext);
        if (mime != null) {
            if (mime.equals("")) {
                mime = null;
            } else {
                ext_a = mimeMap.get(mime);
            }
        }

        // If extension with same MIME type does not exist, then create new
        // assignment.
        if (ext_a == null) {
            ext_a = new ExtAssignment();
            assignments.add(ext_a);
            extMap.put(ext, ext_a);
            ext_a.setMimeType(mime);  // may be null
            if (mime != null) {
                mimeMap.put(mime, ext_a);
            }
        }
        ext_a.addExtension(ext);
        ext_a.setAssignedApplication(applicationId);
    }
    
//    public ExtAssignment getExtAssignmentByMimeType(String mime)
//    {
//        return mimeMap.get(mime);
//    }

    public void setAssignments(String[] exts, String applicationId)
    {
        for (String e : exts) {
            setAssignment(e, applicationId);
        }
    }

    public String getAssignedApplication(String ext)
    {
        ext = ExtAssignment.normalizeExt(ext);
        ExtAssignment ea = extMap.get(ext);
        return (ea == null) ? null : ea.getAssignedApplication();
    }
    
//    /**
//     * Returns the default application for the given file extension.
//     * @param ext The file extension.
//     * @return The default application id for the given file extension or null if no default exists.
//     */
//    public String getDefaultApplication(String ext) 
//    {
//        // ext = ExtAssignment.normalizeExt(ext);
//        // return defaultsMap.get(ext);
//        if (defaultAssignments == null) {
//            return null;
//        }
//        String app_id = defaultAssignments.getAssignedApplication(ext);
//        if (app_id == null) {
//            // Follow the chain of default assignments
//            app_id = defaultAssignments.getDefaultApplication(ext);
//        }
//        return app_id;
//    }
    
//    public String[] listAvailableApplications(String[] extensions)
//    {
//        if ((appType == null) || (appsLoader == null)) {
//            return new String[0];
//        }
//        if (appType.equals(APP_TYPE_EDITORS)) {
//            return appsLoader.listEditors(extensions);
//        } else if (appType.equals(APP_TYPE_VIEWERS)) {
//            return appsLoader.listViewers(extensions);
//        } else {
//            return new String[0];
//        }
//    }
//    
//    public String getApplicationName(String app_id)
//    {
//        if (appsLoader != null) {
//            return appsLoader.getApplicationName(app_id);
//        } else {
//            return app_id.substring(0, 1).toUpperCase() + app_id.substring(1);
//        }
//    }
    
    void readAssignmentsFromString(String line)
    {
        StringTokenizer st = new StringTokenizer(line, ";");
        while (st.hasMoreTokens()) {
            String astr = st.nextToken();
            int p = astr.indexOf(':');
            if (p > 0) {
                String exts = astr.substring(0, p).trim();
                String appid = astr.substring(p + 1).trim();
                setAssignments(exts.split("\\|"), appid);
            } else {
                Log.warning("Cannot read extension assignment: " + astr);
            }
        }
    }
    
    String writeAssignmentsToString() 
    {
        StringBuilder sb = new StringBuilder();
        try {
            writeAssignments(sb);
        } catch (Exception ex) {
            Log.error("Failed to write file extension assignments to string: " + ex.getMessage());
            // return "";
        }
        return sb.toString();
    }
    
    synchronized void writeAssignments(Appendable sbuf) throws IOException
    {
        for (ExtAssignment ea : assignments) {
            assignmentToString(ea, sbuf);
        }
    }
    
//    public static void main(String[] args) throws Exception
//    {
//        ExtAssignments eas = new ExtAssignments(null);
//        eas.readAssignmentsFromString("jpg|jpeg:myapp;tiff|tif:;");
//        
//        StringBuffer sb = new StringBuffer();
//        for (ExtAssignment ea : eas.listAssignments()) {
//            sb.setLength(0);
//            eas.assignmentToString(ea, sb);
//            System.out.println(sb.toString());
//        }
//    }
    
    private boolean assignmentToString(ExtAssignment exta, Appendable sbuf) throws IOException
    {
        String appid = exta.getAssignedApplication();
        if ((appid == null) || appid.equals("")) {
            return false;
        }
        boolean is_first = true;
        for (String ext : exta.listExtensions()) {
            if (! is_first) {
                sbuf.append("|");
            }
            sbuf.append(ext);
            is_first = false;
        }
        sbuf.append(":").append(appid).append(";");
        return true;
    }

    private String getMimeTypeForExt(String ext)
    {
        if (servletCtx == null) {
            return null;
        }
        return servletCtx.getMimeType(getDummyFilename(ext));
    }
    
    private String getDummyFilename(String ext)
    {
        ext = ExtAssignment.normalizeExt(ext);
        return "dummy." + ext;
    }
            
}
