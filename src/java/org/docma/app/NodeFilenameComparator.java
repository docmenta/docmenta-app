/*
 * NodeFilenameComparator.java
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
public class NodeFilenameComparator implements Comparator
{

    public int compare(Object arg1, Object arg2)
    {
        DocmaNode node1 = (DocmaNode) arg1;
        DocmaNode node2 = (DocmaNode) arg2;
        boolean isfolder1 = node1.isFolder();
        boolean isfolder2 = node2.isFolder();

        // Place folders before other nodes
        if (isfolder1 && !isfolder2) return -1;
        if (isfolder2 && !isfolder1) return 1;

        String fn1 = node1.isImageContent() ? node1.getAlias() : node1.getDefaultFileName();
        String fn2 = node2.isImageContent() ? node2.getAlias() : node2.getDefaultFileName();
        if (fn1 == null) fn1 = "";
        if (fn2 == null) fn2 = "";
        return fn1.compareToIgnoreCase(fn2);
    }

}
