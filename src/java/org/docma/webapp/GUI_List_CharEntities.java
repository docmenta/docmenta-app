/*
 * GUI_List_CharEntities.java
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

import org.docma.app.*;
import java.util.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class GUI_List_CharEntities
{
    private MainWindow mainWin;

    private Listbox        char_entities_listbox;
    private ListModelList  char_entities_listmodel;

    private List cutList = new ArrayList();


    public GUI_List_CharEntities(MainWindow mainWin, Listbox entities_list)
    {
        this.mainWin = mainWin;
        char_entities_listbox = entities_list;
        char_entities_listmodel = new ListModelList();
        char_entities_listmodel.setMultiple(true);
        char_entities_listbox.setModel(char_entities_listmodel);
        char_entities_listbox.setItemRenderer(mainWin.getListitemRenderer());
    }

    public void loadCharEntities()
    {
        char_entities_listmodel.clear();
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaCharEntity[] char_entities = docmaSess.getCharEntities();
        // for (int i=0; i < char_entities.length; i++) {
        //     char_entities_listmodel.add(char_entities[i]);
        // }
        char_entities_listmodel.addAll(Arrays.asList(char_entities));
        // char_entities_listbox.setVflex(true);
        // char_entities_listbox.setFixedLayout(true);
        // char_entities_listbox.setHeight("100%");
        // char_entities_listbox.setRows(char_entities_listmodel.getSize());
        // Borderlayout bl = (Borderlayout) getFellow("EntitiesBorderLayout");
        // bl.resize();
        cutList.clear();
    }

    public boolean isEmpty()
    {
        return char_entities_listmodel.isEmpty();
    }

    public boolean isCutListEmpty()
    {
        return cutList.size() == 0;
    }

    public boolean refreshIfDirty()
    {
        if (! isCutListEmpty()) {
            loadCharEntities();
            // cutList.clear();  // already included in loadCharEntities()
            return true;
        }
        return false;
    }

    public void doInsertEntity() throws Exception
    {
        CharEntityDialog dialog = (CharEntityDialog) mainWin.getPage().getFellow("CharEntityDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaCharEntity[] all_entities = new DocmaCharEntity[char_entities_listmodel.size()];
        all_entities = (DocmaCharEntity[]) char_entities_listmodel.toArray(all_entities);
        int ins_pos;
        if (char_entities_listbox.getSelectedCount() > 0) {
            ins_pos = char_entities_listbox.getSelectedIndex();
        } else {
            ins_pos = all_entities.length;  // insert new entity at end of list
        }
        DocmaCharEntity ent = new DocmaCharEntity();
        dialog.setMode_NewEntity();
        if (dialog.doEditEntity(ent, all_entities)) {
            DocmaCharEntity[] new_arr = new DocmaCharEntity[all_entities.length + 1];
            for (int i=0; i < ins_pos; i++) new_arr[i] = all_entities[i];
            for (int i=ins_pos; i < all_entities.length; i++) new_arr[i+1] = all_entities[i];
            new_arr[ins_pos] = ent;
            docmaSess.setCharEntities(new_arr);
            char_entities_listmodel.add(ins_pos, ent);
            char_entities_listbox.setSelectedIndex(ins_pos);
        }
    }

    public void doEditEntity() throws Exception
    {
        if (char_entities_listbox.getSelectedCount() > 0) {
            int sel_idx = char_entities_listbox.getSelectedIndex();
            CharEntityDialog dialog = (CharEntityDialog) mainWin.getPage().getFellow("CharEntityDialog");
            DocmaSession docmaSess = mainWin.getDocmaSession();
            DocmaCharEntity[] all_entities = new DocmaCharEntity[char_entities_listmodel.size()];
            all_entities = (DocmaCharEntity[]) char_entities_listmodel.toArray(all_entities);
            DocmaCharEntity ent = (DocmaCharEntity) all_entities[sel_idx];
            dialog.setMode_EditEntity();
            if (dialog.doEditEntity(ent, all_entities)) {
                all_entities[sel_idx] = ent;
                docmaSess.setCharEntities(all_entities);
                char_entities_listmodel.set(sel_idx, ent);
                char_entities_listbox.setSelectedIndex(sel_idx);
            }
        } else {
            Messagebox.show("Please select an entity!");
        }
    }

    public void doDeleteEntity() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        int sel_cnt = char_entities_listbox.getSelectedCount();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more entities!");
            return;
        }
        try {
            Iterator it = char_entities_listbox.getSelectedItems().iterator();
            int[] sel_idx = new int[sel_cnt];
            for (int i=0; i < sel_cnt; i++) {
                Listitem item = (Listitem) it.next();
                sel_idx[i] = item.getIndex();
            }
            Arrays.sort(sel_idx);
            String msg;
            if (sel_cnt == 1) {
                DocmaCharEntity delchar = (DocmaCharEntity) char_entities_listmodel.get(sel_idx[0]);
                msg = "Delete entity " + delchar.getNumeric() + "?";
            } else {
                msg = "Delete " + sel_cnt + " entities?";
            }
            if (Messagebox.show(msg, "Delete?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

                for (int i=sel_cnt - 1; i >= 0; i--) {
                    char_entities_listmodel.remove(sel_idx[i]);
                }
                DocmaCharEntity[] all_entities = new DocmaCharEntity[char_entities_listmodel.size()];
                all_entities = (DocmaCharEntity[]) char_entities_listmodel.toArray(all_entities);
                docmaSess.setCharEntities(all_entities);

                // if (char_entities_listmodel.getSize() >= MIN_LISTBOX_SIZE) {
                //     char_entities_listbox.setRows(char_entities_listmodel.getSize());
                // }
            }
        } catch(Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            loadCharEntities(); // reload all character entities
        }
    }

    public void doCutEntity() throws Exception
    {
        int sel_cnt = char_entities_listbox.getSelectedCount();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more entities!");
            return;
        }
        try {
            Iterator it = char_entities_listbox.getSelectedItems().iterator();
            int[] sel_idx = new int[sel_cnt];
            for (int i=0; i < sel_cnt; i++) {
                Listitem item = (Listitem) it.next();
                sel_idx[i] = item.getIndex();
            }
            Arrays.sort(sel_idx);
            DocmaCharEntity[] sel_chars = new DocmaCharEntity[sel_cnt];
            for (int i=sel_cnt - 1; i >= 0; i--) {
                sel_chars[i] = (DocmaCharEntity) char_entities_listmodel.remove(sel_idx[i]);
            }
            cutList.addAll(Arrays.asList(sel_chars));
            // if (char_entities_listmodel.getSize() >= MIN_LISTBOX_SIZE) {
            //     char_entities_listbox.setRows(char_entities_listmodel.getSize());
            // }
        } catch(Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            loadCharEntities(); // reload all character entities
            // cutList.clear();  // already included in loadCharEntities()
        }
    }

    public void doPasteEntity() throws Exception
    {
        try {
            if (cutList.size() == 0) {
                return;
            }
            DocmaSession docmaSess = mainWin.getDocmaSession();
            int sel_cnt = char_entities_listbox.getSelectedCount();
            if (sel_cnt != 1) {
                Messagebox.show("Please select the insert position!");
                return;
            }
            int insert_pos = char_entities_listbox.getSelectedIndex();
            char_entities_listmodel.addAll(insert_pos, cutList);
            cutList.clear();

            DocmaCharEntity[] all_entities = new DocmaCharEntity[char_entities_listmodel.size()];
            all_entities = (DocmaCharEntity[]) char_entities_listmodel.toArray(all_entities);
            docmaSess.setCharEntities(all_entities);
        } catch(Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            loadCharEntities(); // reload all character entities
        }
    }

    public void doMoveEntityUp() throws Exception
    {
        moveEntity(-1);
    }

    public void doMoveEntityDown() throws Exception
    {
        moveEntity(1);
    }

    private void moveEntity(int direction) throws Exception
    {
        final int UP = -1;
        final int DOWN = 1;

        DocmaSession docmaSess = mainWin.getDocmaSession();
        int sel_cnt = char_entities_listbox.getSelectedCount();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select on or more entities!");
            return;
        }
        try {
            Iterator it = char_entities_listbox.getSelectedItems().iterator();
            int[] sel_idx = new int[sel_cnt];
            for (int i=0; i < sel_cnt; i++) {
                Listitem item = (Listitem) it.next();
                sel_idx[i] = item.getIndex();
            }
            Arrays.sort(sel_idx);
            int all_cnt = char_entities_listmodel.size();
            if ((direction == UP) && (sel_idx[0] == 0)) return; // cannot move up
            if ((direction == DOWN) && (sel_idx[sel_cnt-1] == all_cnt-1)) return; // cannot move down

            if (direction == UP) {
                for (int i=0; i < sel_idx.length; i++) {
                    int char_idx = sel_idx[i];
                    Object prev_char = char_entities_listmodel.get(char_idx - 1);
                    char_entities_listmodel.set(char_idx - 1, char_entities_listmodel.get(char_idx));
                    char_entities_listmodel.set(char_idx, prev_char);
                    // DocmaCharEntity prev_char = all_entities[char_idx - 1];
                    // all_entities[char_idx - 1] = all_entities[char_idx];
                    // all_entities[char_idx] = prev_char;
                }
            }
            if (direction == DOWN) {
                for (int i=sel_idx.length-1; i >=0 ; i--) {
                    int char_idx = sel_idx[i];
                    Object next_char = char_entities_listmodel.get(char_idx + 1);
                    char_entities_listmodel.set(char_idx + 1, char_entities_listmodel.get(char_idx));
                    char_entities_listmodel.set(char_idx, next_char);
                    // DocmaCharEntity next_char = all_entities[char_idx + 1];
                    // all_entities[char_idx + 1] = all_entities[char_idx];
                    // all_entities[char_idx] = next_char;
                }
            }
            DocmaCharEntity[] all_entities = new DocmaCharEntity[all_cnt];
            all_entities = (DocmaCharEntity[]) char_entities_listmodel.toArray(all_entities);
            docmaSess.setCharEntities(all_entities);
        } catch(Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            loadCharEntities(); // reload all character entities
        }
    }

}
