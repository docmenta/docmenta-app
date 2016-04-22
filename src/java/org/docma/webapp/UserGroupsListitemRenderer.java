/*
 * UserGroupsListitemRenderer.java
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

package org.docma.webapp;

import org.docma.app.*;
import org.docma.app.ui.*;
import org.docma.coreapi.*;
import org.docma.util.*;

import java.util.*;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.ListitemRenderer;

/**
 *
 * @author MP
 */
public class UserGroupsListitemRenderer implements ListitemRenderer
{
    DocmaI18 i18;

    public UserGroupsListitemRenderer(MainWindow mainWin)
    {
        i18 = GUIUtil.i18(mainWin);
    }

    public void render(Listitem item, Object data, int index) throws Exception
    {
        if (data instanceof UserGroupModel) {
            UserGroupModel grp = (UserGroupModel) data;
            Listcell c1 = new Listcell(grp.getGroupId());
            Listcell c2 = new Listcell(grp.getGroupName());
            c2.setImage(grp.isDirectoryGroup() ? "img/dir_group.png" : "img/group.png");
            item.appendChild(c1);
            item.appendChild(c2);
            item.addForward("onDoubleClick", "mainWin", "onEditUserGroup");
        } else
        if (data instanceof AccessRights) {
            AccessRights ar = (AccessRights) data;
            Listcell c1 = new Listcell(ar.getStoreId());
            String[] rights_i18 = i18Rights(ar.getRights());
            String rights = DocmaUtil.concatStrings(Arrays.asList(rights_i18), ", ");
            Listcell c2 = new Listcell(rights);
            item.appendChild(c1);
            item.appendChild(c2);
            item.addForward("onDoubleClick", "mainWin", "onEditRights");
        } else
        if (data instanceof UserModel) {
            UserModel usr = (UserModel) data;
            Listcell c1 = new Listcell(usr.getLoginName());
            c1.setImage(usr.isDirectoryUser() ? "img/dir_user.png" : "img/user.png");
            Listcell c2 = new Listcell(usr.getLastName());
            Listcell c3 = new Listcell(usr.getFirstName());
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.addForward("onDoubleClick", "mainWin", "onEditMember");
        }
        item.setValue(data);
    }

    private String[] i18Rights(String[] rights)
    {
        for (int i=0; i < rights.length; i++) {
            String label = i18.getLabel("label.accessright.role." + rights[i]);
            if (label != null) rights[i] = label.trim().replace(' ', '_');
        }
        return rights;
    }

}
