/*
 * ExportQueueComposer.java
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

import java.util.Arrays;
import java.util.Set;

import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Window;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Timer;

import org.docma.app.*;
import org.docma.app.ui.*;

/**
 *
 * @author MP
 */
public class ExportQueueComposer extends SelectorComposer<Component> 
{
    @Wire Window exportQueueDialog;
    @Wire Listbox exportQueueListbox;
    @Wire Timer exportQueueRefreshTimer;

    private MainWindow mainWin;
    private DocmaSession docmaSess;
    private ListModelList queue_listmodel = null;
    private DocmaListitemRenderer listitem_renderer = null;
    

    public void showExportQueue(DocmaSession docmaSess) throws Exception
    {
        this.docmaSess = docmaSess;
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(exportQueueDialog);
        mainWin = webSess.getMainWindow();
        listitem_renderer = mainWin.getListitemRenderer();
        updateQueueList();
        setRefreshEnabled(true);
        exportQueueDialog.doHighlighted();
    }

    @Listen("onClick = #exportQueueHelpBtn")
    public void onHelpClick() throws Exception
    {
        MainWindow.openHelp("help/show_export_queue.html");
    }
    
    @Listen("onClick = #exportQueueCloseBtn")
    public void onCloseClick()
    {
        setRefreshEnabled(false);  // stop refresh timer
        exportQueueDialog.setVisible(false);
        docmaSess = null;
    }

    @Listen("onClick = #exportQueueCancelBtn")
    public void onCancelExportClick() throws Exception
    {
        Set sel = queue_listmodel.getSelection();
        int sel_cnt = sel.size();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select an export from the list!");
            return;
        }
        final DocmaExportJob[] sel_jobs = (DocmaExportJob[]) sel.toArray(new DocmaExportJob[sel_cnt]);
        String msg;
        if (sel_cnt == 1) {
            String exp_id = sel_jobs[0].getPublicationId();
            String uid = sel_jobs[0].getUserId();
            UserModel um = mainWin.getUser(uid);
            String usr_name = (um != null) ?  um.getLoginName() : uid;
            msg = "Cancel export '" + exp_id +
                    "' of user '" + usr_name + "'?";
        } else {
            msg = "Cancel selected exports?";
        }
        Messagebox.show(msg, "Cancel?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, 
            new EventListener() {
                public void onEvent(Event evt) throws Exception {
                    if ("onYes".equalsIgnoreCase(evt.getName())) {
                        cancelExports(sel_jobs);
                    }
                }
            }
        );
    }
                
    private void cancelExports(DocmaExportJob[] sel_jobs)
    {
        String uid_self = docmaSess.getUserId();
        StringBuilder skipped = new StringBuilder();
        int moreSkipped = 0;
        for (DocmaExportJob job : sel_jobs) {
            String export_store_id = job.getStoreId();
            String export_id = job.getPublicationId();
            String uid_job = job.getUserId();
            boolean is_self = uid_self.equals(uid_job);  // user wants to cancel his own export?

            if (is_self || docmaSess.hasAdminRight(export_store_id)) {
                job.cancel();
            } else {
                int len = skipped.length();
                if (len > 100) {
                    ++moreSkipped;
                } else if (len > 0) {
                    skipped.append(", ").append(export_id);
                } else {  // len == 0
                    skipped.append(export_id);
                }
            }
        }
        if (skipped.length() > 0) {
            if (moreSkipped > 0) {
                skipped.append(" (").append(moreSkipped).append(" more)");
            }
            skipped.append('.');
            Messagebox.show("Cannot cancel export. Missing administration rights: " + skipped);
        }
        updateQueueList();
    }

    @Listen("onTimer = #exportQueueRefreshTimer")
    public void onRefreshExportQueue() throws Exception
    {
        updateQueueList();
    }

    private synchronized void updateQueueList()
    {
        if (queue_listmodel == null) {
            queue_listmodel = new ListModelList();
            queue_listmodel.setMultiple(true);
            exportQueueListbox.setModel(queue_listmodel);
            exportQueueListbox.setItemRenderer(listitem_renderer);
        }
        queue_listmodel.clear();

        DocmaExportJob[] jobs = docmaSess.getExportQueue();
        queue_listmodel.addAll(Arrays.asList(jobs));
    }

    private void setRefreshEnabled(boolean enabled)
    {
        exportQueueRefreshTimer.setRunning(enabled);
    }


}
