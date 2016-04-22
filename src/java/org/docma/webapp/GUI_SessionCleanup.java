/*
 * GUI_SessionCleanup.java
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

import org.docma.util.*;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.*;

/**
 *
 * @author MP
 */
public class GUI_SessionCleanup implements SessionCleanup
{

    public void cleanup(Session gui_sess) throws Exception
    {
        try {
            GUIUtil.closeAllDocmaSessions(gui_sess);
        } catch (Exception ex) {
            // ex.printStackTrace();
            Log.warning("Exception in closeAllDocmaSessions() during session cleanup: " + ex.getMessage());
        }
    }

}
