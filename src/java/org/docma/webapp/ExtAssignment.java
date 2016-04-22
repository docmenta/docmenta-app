/*
 * ExtAssignment.java
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

/**
 *
 * @author MP
 */
public class ExtAssignment implements Cloneable
{
    // private final ExtAssignments assignments;
    private TreeSet<String> extensions = new TreeSet<String>();
    private String mimeType = null;
    private String applicationId = null;

    ExtAssignment()
    {
        // this.assignments = assignments;
    }
    
    public String[] listExtensions() 
    {
        return extensions.toArray(new String[extensions.size()]);
    }
    
    boolean addExtension(String ext) 
    {
        ext = normalizeExt(ext);
        if (ext.length() > 0) {
            return extensions.add(ext);
        } else {
            return false;
        }
    }

    boolean removeExtension(String ext) 
    {
        ext = normalizeExt(ext);
        if (ext.length() > 0) {
            return extensions.remove(ext);
        } else {
            return false;
        }
    }
    
    public boolean hasExtension(String ext)
    {
        return extensions.contains(normalizeExt(ext));
    }
    
    public String getExtensionsString()
    {
        StringBuilder sb = new StringBuilder();
        for (String ext : extensions) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(ext);
        }
        return sb.toString();
    }

    public String getMimeType() 
    {
        return mimeType;
    }

    void setMimeType(String mimeType) 
    {
        this.mimeType = mimeType;
    }

    public String getAssignedApplication() 
    {
        return applicationId;
    }

    void setAssignedApplication(String appId) 
    {
        if ((appId != null) && appId.equals("")) {  // transform empty string to null
            appId = null;
        }
        this.applicationId = appId;
    }
    
//    public String getDefaultApplication()
//    {
//        for (String e : extensions) {
//            String app_id = assignments.getDefaultApplication(e);
//            if ((app_id != null) && !app_id.equals("")) {
//                return app_id;
//            }
//        }
//        return null;
//    }
    
//    /** 
//     * Returns the assigned application or in case no application has been assigned,
//     * returns the default application for the file extensions represented by 
//     * this object. If no application has been assigned and also no default
//     * application exists, then this method returns null.
//     * @return The application id for the file extensions or null.
//     */
//    public String getApplication()
//    {
//        String app_id = getAssignedApplication();
//        return (app_id == null) ? getDefaultApplication() : app_id;
//    }
    
//    public String[] listAvailableApplications()
//    {
//        return assignments.listAvailableApplications(listExtensions());
//    }
//
//    public String getApplicationName(String application_id)
//    {
//        return assignments.getApplicationName(application_id);
//    }

    @Override
    protected Object clone() throws CloneNotSupportedException 
    {
        ExtAssignment clone_obj = (ExtAssignment) super.clone();
        if (this.extensions != null) {
            clone_obj.extensions = (TreeSet<String>) this.extensions.clone();
        }
        return clone_obj;
    }
    
    
    static String normalizeExt(String ext)
    {
        ext = ext.trim();
        if (ext.startsWith(".")) {
            ext = ext.substring(1).trim();
        }
        return ext.toLowerCase();
    }
    
}
