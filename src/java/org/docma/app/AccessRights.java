/*
 * AccessRights.java
 * 
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.app;

import java.util.*;

/**
 *
 * @author MP
 */
public class AccessRights
{
    public static final String RIGHT_VIEW_CONTENT = "view_content";
    public static final String RIGHT_EDIT_CONTENT = "edit_content";
    public static final String RIGHT_APPROVE_CONTENT = "approve_content";
    public static final String RIGHT_TRANSLATE_CONTENT = "translate_content";
    public static final String RIGHT_EDIT_STYLES = "edit_styles";
    public static final String RIGHT_MANAGE_VERSIONS = "manage_versions";
    public static final String RIGHT_MANAGE_PUBLICATIONS = "manage_publications";
    public static final String RIGHT_ADMINISTRATION = "administration";

    private String storeId;
    private String[] rights;

    public AccessRights(String storeId, String[] rights)
    {
        this.storeId = storeId;
        this.rights = (String[]) rights.clone();
    }

    public String[] getRights()
    {
        return (String[]) rights.clone();
    }

    public void setRights(String[] rights)
    {
        this.rights = (String[]) rights.clone();
    }

    public String getStoreId()
    {
        return storeId;
    }

    public void setStoreId(String storeId)
    {
        this.storeId = storeId;
    }

    public boolean containsRightAnyOf(String... right_list)
    {
        if (rights != null) {
            for (String r : right_list) {
                for (String one_of_all : rights) {
                    if (r.equals(one_of_all)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public String toString()
    {
        if ((rights == null) || (rights.length == 0)) {
            return storeId + ":";
        }
        StringBuffer buf = new StringBuffer(storeId + ":" + rights[0]);
        for (int i=1; i < rights.length; i++) {
            buf.append("|").append(rights[i]);
        }
        return buf.toString();
    }

    public static String toString(AccessRights[] rights_arr)
    {
        if ((rights_arr == null) || (rights_arr.length == 0)) {
            return "";
        }
        StringBuffer buf = new StringBuffer(rights_arr[0].toString());
        for (int i=1; i < rights_arr.length; i++) {
            buf.append(";").append(rights_arr[i].toString());
        }
        return buf.toString();
    }

    public static AccessRights[] parseAccessRights(String rights_str)
    {
        if (rights_str != null) {
            rights_str = rights_str.trim();
            if (rights_str.length() > 0) {
                String[] arr = rights_str.split(";");
                List<AccessRights> rights_list = new ArrayList<AccessRights>();
                for (int i = 0; i < arr.length; i++) {
                    String store_rights = arr[i].trim();
                    int pos = store_rights.indexOf(':');
                    if (pos > 0) {
                        String storeId = store_rights.substring(0, pos);
                        String rstr = store_rights.substring(pos + 1).trim();
                        String[] rarr = (rstr.length() > 0) ? rstr.split("\\|") : new String[0];
                        rights_list.add(new AccessRights(storeId, rarr));
                    }
                }
                return rights_list.toArray(new AccessRights[rights_list.size()]);
            }
        }
        return new AccessRights[0];
    }

    public static String[] getAllRights()
    {
        return new String[] {
           RIGHT_ADMINISTRATION, RIGHT_APPROVE_CONTENT, RIGHT_EDIT_CONTENT, RIGHT_EDIT_STYLES,
           RIGHT_MANAGE_PUBLICATIONS, RIGHT_MANAGE_VERSIONS,
           RIGHT_TRANSLATE_CONTENT, RIGHT_VIEW_CONTENT };
    }

}
