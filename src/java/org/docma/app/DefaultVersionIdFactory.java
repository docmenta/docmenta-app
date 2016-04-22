/*
 * DefaultVersionIdFactory.java
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

import org.docma.coreapi.implementation.*;
import java.lang.reflect.*;

import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class DefaultVersionIdFactory implements VersionIdFactory
{
    Class verIdClass = null;

    public DefaultVersionIdFactory() 
    {
    }

    public DefaultVersionIdFactory(Class verIdClass)
    {
        this.verIdClass = verIdClass;
    }

    public DocVersionId createVersionId(String ver_id) throws DocException
    {
        if (verIdClass == null) {
            return new DefaultVersionId(ver_id);
        } else {
            try {
                Constructor con = verIdClass.getConstructor(new Class[] { String.class });
                Object obj = con.newInstance(new Object[] { ver_id });
                return (DocVersionId) obj;
            } catch (InvocationTargetException ite) {
                throw (DocException) ite.getTargetException();
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
        }
    }

}
