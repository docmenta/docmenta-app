/*
 * AccessRightsDialog.java
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
import org.docma.app.ui.*;
import org.docma.userapi.*;
import org.zkoss.zul.*;

import java.util.*;

/**
 *
 * @author MP
 */
public class AccessRightsDialog extends Window
{
    private int modalResult = -1;

    private Listbox productsBox;
    private Checkbox viewContentBox;
    private Checkbox editContentBox;
    private Checkbox approveContentBox;
    private Checkbox translateContentBox;
    private Checkbox editStylesBox;
    private Checkbox manageVersionsBox;
    private Checkbox managePublicationsBox;
    private Checkbox administrationBox;

    private UserGroupModel grp;
    private String selectedProduct = null;
    private SortedMap rights_map = new TreeMap();

    public void onOkayClick()
    {
        saveCheckboxesState();
        modalResult = GUIConstants.MODAL_OKAY;
        setVisible(false);
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }

    public void onSelectAll()
    {
        setAllCheckboxes(true);
    }

    public void onUnselectAll()
    {
        setAllCheckboxes(false);
    }

    public void onSelectProduct()
    {
        saveCheckboxesState();
        updateCheckboxes();
    }

    // public void onChangeAccessRight()
    // {
    //
    // }

    public boolean doEditRights(UserGroupModel grp, AccessRights selectedRights, DocmaSession docmaSess)
    throws Exception
    {
        this.grp = grp;
        initRightsMap();

        initFields();
        initProductsList(docmaSess, selectedRights);
        updateCheckboxes();
        UserManager um = docmaSess.getUserManager();
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            updateModel(um);
            return true;
        } while (true);
    }

    private void initFields()
    {
        productsBox = (Listbox) getFellow("AccessRightProductListbox");
        viewContentBox = (Checkbox) getFellow("AccessRightViewContentCheckbox");
        editContentBox = (Checkbox) getFellow("AccessRightEditContentCheckbox");
        approveContentBox = (Checkbox) getFellow("AccessRightApproveContentCheckbox");
        translateContentBox = (Checkbox) getFellow("AccessRightTranslateContentCheckbox");
        editStylesBox = (Checkbox) getFellow("AccessRightEditStylesCheckbox");
        manageVersionsBox = (Checkbox) getFellow("AccessRightManageVersionsCheckbox");
        managePublicationsBox = (Checkbox) getFellow("AccessRightManagePublicationsCheckbox");
        administrationBox = (Checkbox) getFellow("AccessRightAdministrationCheckbox");
    }

    private void initRightsMap()
    {
        rights_map.clear();
        AccessRights[] arr = grp.getAccessRights();
        for (int i=0; i < arr.length; i++) {
            AccessRights rights = arr[i];
            List rights_list = (List) rights_map.get(rights.getStoreId());
            if (rights_list == null) {
                rights_list = new ArrayList();
                rights_map.put(rights.getStoreId(), rights_list);
            }
            rights_list.addAll(Arrays.asList(rights.getRights()));
        }
    }

    private void initProductsList(DocmaSession docmaSess, AccessRights selectedRights)
    {
        String sel_store = (selectedRights != null) ? selectedRights.getStoreId() : "";
        int sel_idx = 0;
        String[] storeIds = docmaSess.listDocStores();
        productsBox.getItems().clear();
        productsBox.appendItem("[ALL]", "*");
        for (int i=0; i<storeIds.length; i++) {
            String sid = storeIds[i];
            productsBox.appendItem(sid, sid);
            if (sid.equals(sel_store)) sel_idx = i+1;
        }
        productsBox.setSelectedIndex(sel_idx);
    }

    private void updateCheckboxes()
    {
        Listitem item = productsBox.getSelectedItem();
        boolean isViewContent = false;
        boolean isEditContent = false;
        boolean isApproveContent = false;
        boolean isTranslateContent = false;
        boolean isEditStyles = false;
        boolean isManageVersions = false;
        boolean isManagePublications = false;
        boolean isAdministration = false;

        selectedProduct = null;
        if (item != null) {
            selectedProduct = item.getValue().toString();
            List rights = (List) rights_map.get(selectedProduct);
            // String[] p_rights = getProductRights(selectedProduct, grp.getAccessRights());
            if (rights != null) {
                isViewContent = rights.contains(AccessRights.RIGHT_VIEW_CONTENT);
                isEditContent = rights.contains(AccessRights.RIGHT_EDIT_CONTENT);
                isApproveContent = rights.contains(AccessRights.RIGHT_APPROVE_CONTENT);
                isTranslateContent = rights.contains(AccessRights.RIGHT_TRANSLATE_CONTENT);
                isEditStyles = rights.contains(AccessRights.RIGHT_EDIT_STYLES);
                isManageVersions = rights.contains(AccessRights.RIGHT_MANAGE_VERSIONS);
                isManagePublications = rights.contains(AccessRights.RIGHT_MANAGE_PUBLICATIONS);
                isAdministration = rights.contains(AccessRights.RIGHT_ADMINISTRATION);
            }
        }

        viewContentBox.setChecked(isViewContent);
        editContentBox.setChecked(isEditContent);
        approveContentBox.setChecked(isApproveContent);
        translateContentBox.setChecked(isTranslateContent);
        editStylesBox.setChecked(isEditStyles);
        manageVersionsBox.setChecked(isManageVersions);
        managePublicationsBox.setChecked(isManagePublications);
        administrationBox.setChecked(isAdministration);
    }

    private void saveCheckboxesState()
    {
        List rights = (List) rights_map.get(selectedProduct);
        if (rights == null) {
            rights = new ArrayList();
            rights_map.put(selectedProduct, rights);
        } else {
            rights.clear();
        }

        if (viewContentBox.isChecked()) rights.add(AccessRights.RIGHT_VIEW_CONTENT);
        if (editContentBox.isChecked()) rights.add(AccessRights.RIGHT_EDIT_CONTENT);
        if (approveContentBox.isChecked()) rights.add(AccessRights.RIGHT_APPROVE_CONTENT);
        if (translateContentBox.isChecked()) rights.add(AccessRights.RIGHT_TRANSLATE_CONTENT);
        if (editStylesBox.isChecked()) rights.add(AccessRights.RIGHT_EDIT_STYLES);
        if (manageVersionsBox.isChecked()) rights.add(AccessRights.RIGHT_MANAGE_VERSIONS);
        if (managePublicationsBox.isChecked()) rights.add(AccessRights.RIGHT_MANAGE_PUBLICATIONS);
        if (administrationBox.isChecked()) rights.add(AccessRights.RIGHT_ADMINISTRATION);
    }

    private void setAllCheckboxes(boolean checked_state)
    {
        viewContentBox.setChecked(checked_state);
        editContentBox.setChecked(checked_state);
        approveContentBox.setChecked(checked_state);
        translateContentBox.setChecked(checked_state);
        editStylesBox.setChecked(checked_state);
        manageVersionsBox.setChecked(checked_state);
        managePublicationsBox.setChecked(checked_state);
        administrationBox.setChecked(checked_state);
    }

    // private String[] getProductRights(String productId, AccessRights[] rights_arr)
    // {
    //     for (int i=0; i < rights_arr.length; i++) {
    //         if (productId.equals(rights_arr[i].getStoreId())) {
    //             return rights_arr[i].getRights();
    //         }
    //     }
    //     return null;
    // }

    private void updateModel(UserManager um) throws Exception
    {
        List<AccessRights> ar_list = new ArrayList<AccessRights>(rights_map.size());
        Iterator it = rights_map.keySet().iterator();
        while (it.hasNext()) {
            String storeId = (String) it.next();
            List rights = (List) rights_map.get(storeId);
            if (rights.size() > 0) {
                String[] str_arr = new String[rights.size()];
                str_arr = (String[]) rights.toArray(str_arr);
                ar_list.add(new AccessRights(storeId, str_arr));
            }
        }
        AccessRights[] ar_arr = ar_list.toArray(new AccessRights[ar_list.size()]);
        String rights_str = AccessRights.toString(ar_arr);
        um.setGroupProperty(grp.getGroupId(), DocmaConstants.PROP_USERGROUP_RIGHTS, rights_str);
        grp.setAccessRights(ar_arr);
    }

}
