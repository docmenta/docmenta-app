/*
 * IdRegistry.java
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
 * 
 * Created on 16. Oktober 2007, 21:08
 *
 */

package org.docma.coreapi.fsimplementation;

import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class IdRegistry {
    
    private String storeId;
    private int max_id = 0;
    
    
    /**
     * Creates a new instance of IdRegistry
     */
    public IdRegistry(String storeId) {
        this.storeId = storeId;
    }
    
    public int currentMaxId() {
        return max_id;
    }
    
    public int newId() {
        return ++max_id;
    }
    
    // public String newIdAsDOMString() {
    //     return idToDOMString(newId());
    // }
    
    public void registerId(int id) {
        if (id > max_id) max_id = id; 
    }
    
    public void registerDOMId(String idstr) {
        registerId(DOMStringToId(idstr));
    }
    
    public static String idToDOMString(int idnr) {
        String idstr = String.valueOf(idnr);
        return idStringToDOMString(idstr);
    }
    
    public static String idStringToDOMString(String idstr) {
        return "d" + "00000000".substring(idstr.length()) + idstr;
    }

    public static int DOMStringToId(String idstr) throws DocRuntimeException {
        if (idstr.startsWith("d")) {   //  (idstr.length() == 9) && ... 
            try {
                return Integer.parseInt(idstr.substring(1));
            } catch (NumberFormatException ex) {}
        }
        throw new DocRuntimeException("Invalid id string.");
    }
    
}
