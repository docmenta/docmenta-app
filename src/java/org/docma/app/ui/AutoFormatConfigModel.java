/*
 * AutoFormatConfigModel.java
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

package org.docma.app.ui;

/**
 *
 * @author MP
 */
public class AutoFormatConfigModel
{
    private String autoFormatClassName;
    private String shortInfo;
    private String longInfo;
    private boolean loadFailed;

    public AutoFormatConfigModel(String afClassName, 
                                 String shortInfo,
                                 String longInfo,
                                 boolean loadFailure)
    {
        this.autoFormatClassName = afClassName;
        this.shortInfo = shortInfo;
        this.longInfo = longInfo;
        this.loadFailed = loadFailure;
    }

    public String getAutoFormatClassName()
    {
        return (autoFormatClassName != null) ? autoFormatClassName : "";
    }

    public String getShortInfo()
    {
        return (shortInfo != null) ? shortInfo : "";
    }

    public String getLongInfo()
    {
        return (longInfo != null) ? longInfo : "";
    }

    public boolean loadingFailed()
    {
        return loadFailed;
    }


}
