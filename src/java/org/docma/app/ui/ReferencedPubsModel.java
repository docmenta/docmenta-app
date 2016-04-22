/*
 * ReferencedPubsModel.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.app.ui;

import java.util.*;
import org.docma.util.DocmaUtil;

/**
 *
 * @author MP
 */
public class ReferencedPubsModel 
{
    private String[] referencedPubs;

    public ReferencedPubsModel(String id_list)
    {
        setIds(id_list);
    }

    public String getIdsAsString()
    {
        return DocmaUtil.concatStrings(getIds(), ", ");
    }

    public String[] getIds()
    {
        return referencedPubs;
    }

    public void setIds(String[] ids) 
    {
        if (ids == null) {
            ids = new String[0];
        }
        referencedPubs = ids;
    }

    public void setIds(String id_list) 
    {
        if ((id_list == null) || id_list.trim().equals("")) {
            referencedPubs = new String[0];
        } else {
            referencedPubs = id_list.split("[ \\t,]+");
        }
    }

    public boolean containsId(String id)
    {
        return Arrays.asList(getIds()).contains(id);
    }

    public void retainIds(String[] id_arr) 
    {
        if ((referencedPubs == null) || (referencedPubs.length == 0)) {
            return;
        }
        int removed = 0;
        List retain_list = Arrays.asList(id_arr);
        for (int i=0; i < referencedPubs.length; i++) {
            if (! retain_list.contains(referencedPubs[i])) {
                referencedPubs[i] = null;
                removed++;
            }
        }
        if (removed > 0) {
            String[] new_arr = new String[referencedPubs.length - removed];
            int k = 0;
            for (String id : referencedPubs) {
                if (id != null) new_arr[k++] = id;
            }
            referencedPubs = new_arr;
        }
    }
}
