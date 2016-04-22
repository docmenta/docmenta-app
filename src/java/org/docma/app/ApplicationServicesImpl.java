/*
 * ApplicationServicesImpl.java
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
public class ApplicationServicesImpl implements ApplicationServices
{
    private Map<String, Object> serviceInstances = new HashMap<String, Object>();

    public boolean hasInstance(String instanceName) 
    {
        return serviceInstances.containsKey(instanceName);
    }

    public Object getInstance(String instanceName) 
    {
        return serviceInstances.get(instanceName);
    }

    public void setInstance(String instanceName, Object serviceInstance) 
    {
        serviceInstances.put(instanceName, serviceInstance);
    }


}
