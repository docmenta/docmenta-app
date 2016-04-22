/*
 * DocmaListitemRenderer.java
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

import java.util.*;
import java.text.SimpleDateFormat;

import org.docma.app.*;
import org.docma.app.ui.*;
import org.docma.coreapi.*;
import org.docma.util.*;

import org.zkoss.zul.Listitem;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zhtml.Div;
import org.zkoss.zhtml.Span;
import org.zkoss.zhtml.Text;

/**
 *
 * @author MP
 */
public class DocmaListitemRenderer implements ListitemRenderer
{
    private final SimpleDateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final MainWindow mainWin;
    private DocmaI18 docmaI18;   // see method i18nLabel()

    public DocmaListitemRenderer(MainWindow mainWin)
    {
        this.mainWin = mainWin;
    }

    public void applyUserSettings()
    {
        String userId = mainWin.getDocmaSession().getUserId();
        UserModel usr = mainWin.getUser(userId);
        String pattern = (usr == null) ? null : usr.getDateFormat();
        if ((pattern == null) || (pattern.trim().equals(""))) {
            pattern = DocmaConstants.DEFAULT_DATE_FORMAT;
        }
        try {
            dateformat.applyPattern(pattern);
        } catch (IllegalArgumentException ex) {
            Log.error("User " + userId + " has invalid date format pattern: " + pattern);
            dateformat.applyPattern(DocmaConstants.DEFAULT_DATE_FORMAT);
        }
    }


    public void render(Listitem item, Object data, int index) throws Exception
    {
        if (data instanceof ProductVersionModel) {
            ProductVersionModel vm = (ProductVersionModel) data;
            String ver_str = vm.getVersionId().toString();
            String lang_str = vm.getLanguageCode().toUpperCase();
            String state_str = vm.getState();
            if (state_str.equals(DocmaConstants.VERSION_STATE_DRAFT)) {
                state_str = i18nLabel("label.versionstatus.draft");
            } else
            if (state_str.equals(DocmaConstants.VERSION_STATE_RELEASED)) {
                state_str = i18nLabel("label.versionstatus.released");
            } else
            if (state_str.equals(DocmaConstants.VERSION_STATE_TRANSLATION_PENDING)) {
                state_str = i18nLabel("label.versionstatus.translationpending");
            }
            Date creationDate = vm.getCreationDate();
            // Date lastModDate = vm.getLastModifiedDate();
            Date releaseDate = vm.getReleaseDate();
            DocVersionId derivedFrom = vm.getDerivedFrom();
            String derived_str = (derivedFrom == null) ? "-" : derivedFrom.toString();
            String creation_str = (creationDate == null) ? "- -" : dateformat.format(creationDate);
            // String lastMod_str = (lastModDate == null) ? "- -" : dateformat.format(lastModDate);
            String release_str = (releaseDate == null) ? "- -" : dateformat.format(releaseDate);
            String comment = vm.getComment();

            // item.setValue(pm.getId());
            Listcell c1 = new Listcell(ver_str);
            Listcell c2 = new Listcell(lang_str);
            Listcell c3 = new Listcell(state_str);
            Listcell c4 = new Listcell(creation_str);
            // Listcell c5 = new Listcell(lastMod_str);
            Listcell c6 = new Listcell(release_str);
            Listcell c7 = new Listcell(derived_str);
            Listcell c8 = new Listcell(comment);
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            // item.appendChild(c5);
            item.appendChild(c6);
            item.appendChild(c7);
            item.appendChild(c8);
            item.addForward("onDoubleClick", "mainWin", "onRenameVersion");
        } else
        if (data instanceof DocmaStyle) {
            DocmaStyle docstyle = (DocmaStyle) data;
            String base_id = docstyle.getBaseId();
            Listcell c1_base = new Listcell(base_id);
            Listcell c1_variant = new Listcell(docstyle.getVariantId());
            Listcell c2 = new Listcell(docstyle.getName());
            Listcell c3 = new Listcell();
            Listcell c4 = new Listcell();

            // Construct hidden cell
            if (! docstyle.isVariant()) {
                Checkbox cb = new Checkbox();
                if (DocmaStyleUtil.isInternalBlockStyle(base_id)) {
                    cb.setChecked(true);
                    cb.setDisabled(true);
                } else {
                    cb.setChecked(docstyle.isHidden());
                    cb.addForward("onCheck", "mainWin", "onChangeStyleHidden", base_id);
                }
                c3.appendChild(cb);
            }

            // Construct preview cell
            Div div = new Div();
            div.setId("preview_div_" + docstyle.getId());
            div.setStyle("width:150px;padding:4px;");
            Span span1 = new Span();
            span1.setId("style_preview_" + docstyle.getId());
            span1.setStyle(docstyle.getCSS());
            span1.appendChild(new Text("Abc def..."));
            div.appendChild(span1);
            c4.appendChild(div);

            item.appendChild(c1_base);
            item.appendChild(c1_variant);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.addForward("onDoubleClick", "mainWin", "onEditStyle");
        } else
        if (data instanceof DocmaLanguageModel) {
            DocmaLanguageModel doclang = (DocmaLanguageModel) data;
            String lang_str = doclang.getDescription();
            if (! doclang.isTranslation()) {
                lang_str += " (Original)";
            }
            item.setLabel(lang_str);
        } else
        if (data instanceof DocmaCharEntity) {
            DocmaCharEntity ent = (DocmaCharEntity) data;
            String num = ent.getNumeric().trim();
            Listcell c1 = new Listcell(num);
            Listcell c2 = new Listcell(ent.getSymbolic());
            String picklist = ent.isSelectable() ? "yes" : "no";
            Listcell c3 = new Listcell(picklist);
            Listcell c4 = new Listcell(ent.getDescription());
            char decoded_char = ' ';
            if (num.startsWith("&#") && num.endsWith(";")) {
                try {
                    int char_code = Integer.parseInt(num.substring(2, num.length() - 1));
                    decoded_char = (char) char_code;
                } catch (Exception ex) {
                    Log.warning("Invalid numeric character entity: " + num);
                }
            }
            Listcell c5 = new Listcell("" + decoded_char);
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.appendChild(c5);
            item.addForward("onDoubleClick", "mainWin", "onEditEntity");
        } else
        if (data instanceof DocmaPublicationConfig) {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            DocmaPublicationConfig pubconf = (DocmaPublicationConfig) data;
            String cont_root_id = pubconf.getContentRoot();
            String cont_root_title = "";
            if (cont_root_id != null) {
                DocmaNode nd = docmaSess.getNodeById(cont_root_id);
                if (nd != null) cont_root_title = nd.getTitle();
                if (cont_root_title == null) cont_root_title = "";
            }
            Listcell c1 = new Listcell(pubconf.getId());
            Listcell c2 = new Listcell(pubconf.getTitle());
            Listcell c3 = new Listcell(cont_root_title);
            Listcell c4 = new Listcell(pubconf.getFilterSetting());
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.addForward("onDoubleClick", "mainWin", "onEditPublicationConfig");
        } else
        if (data instanceof DocmaOutputConfig) {
            DocmaOutputConfig outconf = (DocmaOutputConfig) data;
            Listcell c1 = new Listcell(outconf.getId());
            String fstr = outconf.getSubformat();
            if (fstr.equals("webhelp") || fstr.equals("webhelp1")) {
                fstr = "WEBHELP V1";
            } else
            if (fstr.equals("webhelp2")) {
                fstr = "WEBHELP V2";
            } else
            if (fstr.length() > 0) {
                fstr = fstr.toUpperCase();
            } else {
                fstr = outconf.getFormat().toUpperCase();
            }
            Listcell c2 = new Listcell(fstr);
            Listcell c3 = new Listcell(outconf.getStyleVariant());
            Listcell c4 = new Listcell(outconf.getFilterSetting());
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.addForward("onDoubleClick", "mainWin", "onEditMediaConfig");
        } else
        if (data instanceof DocmaPublication) {
            DocmaPublication pub = (DocmaPublication) data;
            Listcell c1 = new Listcell(pub.getPublicationConfigId());
            Listcell c2 = new Listcell(pub.getOutputConfigId());
            Listcell c3 = new Listcell(pub.getPublicationStateLabel());
            Date releaseDate = pub.getReleaseDate();
            String release_str = (releaseDate == null) ? "-" : dateformat.format(releaseDate);
            Listcell c4 = new Listcell(release_str);
            Date expDate = pub.getExportDate();
            String expDate_str = (expDate == null) ? "-" : dateformat.format(expDate);
            Listcell c5 = new Listcell(expDate_str);
            String userId = pub.getExportedByUser();
            UserModel usrmodel = mainWin.getUser(userId);
            String userName = userId;
            if (usrmodel != null) {
                userName = usrmodel.getDisplayName();
            }
            Listcell c6 = new Listcell(userName);
            Listcell c7 = new Listcell();
            boolean is_export_finished = pub.isExportFinished();
            if (is_export_finished) {
                Vbox vb = new Vbox();
                vb.setSpacing("0");
                Toolbarbutton file_btn = new Toolbarbutton(pub.getFilename());
                file_btn.setStyle("color:#000080;font-size:1em;");
                file_btn.addForward("onClick", "mainWin", "onDownloadPublicationClick", pub.getId());
                vb.appendChild(file_btn);
                String err_str = mainWin.i18("label.errors");
                String warn_str = mainWin.i18("label.warnings");
                int err_cnt = pub.getErrorCount();
                String log_label = "Log (" + err_cnt + " " + err_str +
                                   ", " + pub.getWarningCount() + " " + warn_str + ")";
                Toolbarbutton log_btn = new Toolbarbutton(log_label);
                if (err_cnt > 0) {
                    log_btn.setStyle("color:#C00000;font-size:1em;");  // mark red if errors exist
                } else {
                    log_btn.setStyle("color:#000080;font-size:1em;");
                }
                log_btn.addForward("onClick", "mainWin", "onDownloadPublicationLogClick", pub.getId());
                vb.appendChild(log_btn);
                c7.appendChild(vb);
            } else {
                String exp_msg = pub.getExportProgressMessage();
                if (exp_msg == null) exp_msg = "";
                c7.setLabel(exp_msg);
            }
            Listcell c8 = new Listcell();
            if (is_export_finished) {
                Hbox hb = new Hbox();
                Checkbox cb = new Checkbox();
                boolean is_online = pub.isOnline();
                cb.setChecked(is_online);
                cb.addForward("onCheck", "mainWin", "onChangePublicationIsOnline", pub.getId());
                hb.appendChild(cb);
                if (is_online) {
                    Toolbarbutton open_btn = new Toolbarbutton("Open");
                    open_btn.setStyle("color:#000080;font-size:1em;");
                    open_btn.addForward("onClick", "mainWin", "onOpenPublicationClick", pub.getId());
                    hb.appendChild(open_btn);
                }
                c8.appendChild(hb);
            } else {
                item.setStyle("background-color:#FFFF88;");
            }

            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.appendChild(c5);
            item.appendChild(c6);
            item.appendChild(c7);
            item.appendChild(c8);
            item.addForward("onDoubleClick", "mainWin", "onEditPublication");
        } else
        if (data instanceof UserModel) {
            UserModel usr = (UserModel) data;
            Listcell c1 = new Listcell(usr.getUserId());
            Listcell c2 = new Listcell(usr.getLoginName());
            c2.setImage(usr.isDirectoryUser() ? "img/dir_user.png" : "img/user.png");
            Listcell c3 = new Listcell(usr.getLastName());
            Listcell c4 = new Listcell(usr.getFirstName());
            Listcell c5 = new Listcell(usr.getEmail());
            Date last_login = usr.getLastLogin();
            String date_str = (last_login == null) ? "-" : dateformat.format(last_login);
            Listcell c6 = new Listcell(date_str);
            Listcell c7 = new Listcell(usr.getGroupNamesAsString());
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.appendChild(c5);
            item.appendChild(c6);
            item.appendChild(c7);
            item.addForward("onDoubleClick", "mainWin", "onEditUser");
        } else
        if (data instanceof ConnectedUserModel) {
            ConnectedUserModel conn = (ConnectedUserModel) data;
            Listcell c1 = new Listcell(conn.getUserName());
            Listcell c2 = new Listcell(conn.getLastName());
            Listcell c3 = new Listcell(conn.getFirstName());
            Listcell c4 = new Listcell(conn.getSessionId());
            Listcell c5 = new Listcell(conn.getStoreId());
            Listcell c6 = new Listcell(conn.getVersionId());
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.appendChild(c5);
            item.appendChild(c6);
        } else
        if (data instanceof DocmaExportJob) {
            DocmaExportJob job = (DocmaExportJob) data;

            String userId = job.getUserId();
            UserModel usrmodel = mainWin.getUser(userId);
            String user_name = userId;
            if (usrmodel != null) {
                user_name = usrmodel.getDisplayName();
            }

            Date creation_date = new Date(job.getCreationTime());
            String creation_time_str = dateformat.format(creation_date);

            String state_str;
            int job_state = job.getState();
            switch (job_state) {
                case DocmaExportJob.STATE_QUEUED:
                    state_str = "QUEUED";
                    break;
                case DocmaExportJob.STATE_RUNNING:
                    state_str = "RUNNING";
                    break;
                case DocmaExportJob.STATE_CANCELED:
                    state_str = "STOPPING...";
                    break;
                default:
                    state_str = "unknown";
                    break;
            }

            Listcell c1 = new Listcell(user_name);
            Listcell c2 = new Listcell(job.getStoreId());
            Listcell c3 = new Listcell(job.getVersionId().toString());
            Listcell c4 = new Listcell(job.getPublicationId());
            Listcell c5 = new Listcell(creation_time_str);
            Listcell c6 = new Listcell(state_str);
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            item.appendChild(c5);
            item.appendChild(c6);
        } else
        if (data instanceof AutoFormatConfigModel) {
            AutoFormatConfigModel afm = (AutoFormatConfigModel) data;
            Listcell c1 = new Listcell(afm.getAutoFormatClassName());
            Listcell c2 = new Listcell(afm.getShortInfo());
            if (afm.loadingFailed()) {
                c2.setStyle("color:#AA0000;");  // highlight failure with red font color
            }
            item.appendChild(c1);
            item.appendChild(c2);
        }

//        if (data instanceof FilterSettingModel) {
//            FilterSettingModel fsm = (FilterSettingModel) data;
//            Listcell c1 = new Listcell(fsm.getName());
//            Listcell c2 = new Listcell(fsm.getApplicsAsString());
//            item.appendChild(c1);
//            item.appendChild(c2);
//            item.addForward("onDoubleClick", "mainWin", "onEditFilter");
//        }
    }


    private String i18nLabel(String key)
    {
        if (this.docmaI18 == null) {
            this.docmaI18 = mainWin.getDocmaI18();
        }
        return this.docmaI18.getLabel(key);
    }
}
