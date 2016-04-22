/*
 * DebugUtil.java
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

/**
 *
 * @author MP
 */
public class DebugUtil
{

    public static void printChildren(DocmaNode nd)
    {
        int cnt = nd.getChildCount();
        System.out.println("---------------------------");
        System.out.println("Number of children: " + cnt);
        for (int i=0; i < cnt; i++) {
            DocmaNode child = nd.getChild(i);
            System.out.println("" + i + ". " + child.getId() + ": " + child.getTitle() +
                               " / " + child.getAlias());
        }
        System.out.println("---------------------------");
    }
}
