/*
 * ApplicationPropertiesImpl.java
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

package org.docma.coreapi.fsimplementation;

import java.io.*;
import org.docma.coreapi.*;
import org.docma.util.PropertiesLoader;

/**
 *
 * @author MP
 */
public class ApplicationPropertiesImpl implements ApplicationProperties
{
    private static final String PROPERTIES_FILENAME = "application.properties";

    private File baseDir;
    private File propFile;
    private PropertiesLoader propLoader;

    public ApplicationPropertiesImpl(String baseDirectory) throws DocException
    {
        baseDir = new File(baseDirectory);
        propFile = new File(baseDir, PROPERTIES_FILENAME);
        if (! propFile.exists()) {
            try {
                propFile.createNewFile();
            } catch (IOException ex) {
                throw new DocException("Could not create application properties file: " +
                                       propFile.getAbsolutePath());
            }
        }
        propLoader = new PropertiesLoader(propFile);
    }

    public String getProperty(String name)
    {
        return propLoader.getProp(name);
    }

    public void setProperty(String name, String value) throws DocException
    {
        propLoader.setProp(name, value);
        propLoader.savePropFile("Docma application properties.");
    }

    public void setProperties(String[] names, String[] values) throws DocException
    {
        for (int i=0; i < names.length; i++) {
            propLoader.setProp(names[i], values[i]);
        }
        propLoader.savePropFile("Docma application properties.");
    }

}
