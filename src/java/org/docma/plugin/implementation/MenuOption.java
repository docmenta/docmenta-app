/*
 * MenuOption.java
 *
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.implementation;

/**
 *
 * @author MP
 */
public class MenuOption 
{
    public static final MenuOption LABEL = new MenuOption("LABEL");
    public static final MenuOption IMAGE = new MenuOption("IMAGE");
    public static final MenuOption CHECKED = new MenuOption("CHECKED");
    public static final MenuOption CHECKMARK = new MenuOption("CHECKMARK");
    public static final MenuOption DISABLED = new MenuOption("DISABLED");
    public static final MenuOption VISIBLE = new MenuOption("VISIBLE");
    
    private final String option_name;

    MenuOption(String name)
    {
        this.option_name = name.toUpperCase();
    }

    @Override
    public int hashCode() 
    {
        int hash = 7;
        hash = 67 * hash + (this.option_name != null ? this.option_name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MenuOption other = (MenuOption) obj;
        if ((this.option_name == null) ? (other.option_name != null) : 
                                         !this.option_name.equals(other.option_name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() 
    {
        return option_name;
    }
    
}
