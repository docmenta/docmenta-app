/*
 * ProductsConnectThread.java
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
package org.docma.webapp;

import org.docma.app.*;
import org.docma.app.ui.ProductModel;

/**
 *
 * @author MP
 */
public class ProductsConnectThread extends Thread
{
    private MainWindow mainWin;
    private GUI_List_Products guiList;
    private ProductModel loadingProduct = null;
    private boolean finished;
    private long creationTime;
    
    public ProductsConnectThread(MainWindow main_win, GUI_List_Products gui_list)
    {
        this.mainWin = main_win;
        this.guiList = gui_list;
        this.finished = false;
        this.creationTime = System.currentTimeMillis();
    }

    public void run() 
    {
        this.finished = false;
        DocmaSession mainSess = mainWin.getDocmaSession();
        DocmaSession tempSess = mainSess.createNewSession();
        try {
            loadingProduct = null;
            while ((loadingProduct = guiList.getPendingExtDbProduct()) != null) {
                // System.out.println("--- ConnectThread start loadFromExternalDb: " + loadingProduct.getId());
                loadingProduct.loadFromExternalDb(tempSess);
                // System.out.println("--- ConnectThread end loadFromExternalDb: " + loadingProduct.getId());
                guiList.loadingFinished(loadingProduct);
                loadingProduct = null;
            }
            this.finished = true;
        } finally {
            loadingProduct = null;
            tempSess.closeSession();
        }
    }
    
    public ProductModel getLoadingProduct()
    {
        return loadingProduct;
    }
    
    public boolean isFinished()
    {
        return finished;
    }
    
    public long getCreationTime()
    {
        return creationTime;
    }
    
}
