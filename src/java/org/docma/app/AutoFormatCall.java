/*
 * AutoFormatCall.java
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
 * Important: Objects of this class have to be immutable; see method DocmaStyle.clone()
 *
 * @author MP
 */
public class AutoFormatCall
{
    private String clsName;
    private String argLine;
    private String[] arguments;

    public AutoFormatCall(String clsName, String argLine)
    {
        this.clsName = clsName;
        argLine = (argLine != null) ? argLine.trim() : "";
        this.argLine = argLine;

        if (argLine.equals("")) {
            arguments = new String[0];
        } else {
            arguments = argLine.split("[ ]+");
        }
    }

    public String getClassName()
    {
        return clsName;
    }

    public int getArgumentCount()
    {
        return arguments.length;
    }

    public String getArgument(int idx)
    {
        return arguments[idx];
    }

    public String getArgumentsLine()
    {
        return argLine;
    }

}
