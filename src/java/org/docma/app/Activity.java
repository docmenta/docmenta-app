/*
 * Activity.java
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
package org.docma.app;

import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public interface Activity extends ProgressCallback
{
    long     getActivityId();
    void     setTitle(String labelKey, Object... args);
    String   getTitleKey();
    Object[] getTitleArgs();   
    void     start(Runnable task);
    boolean  isRunning();
}
