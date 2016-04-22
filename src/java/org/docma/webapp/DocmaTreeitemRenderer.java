/*
 * DocmaTreeitemRenderer.java
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

import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
// import org.zkoss.zul.Menupopup;
// import org.zkoss.zul.Treechildren;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.event.MouseEvent;

import org.docma.app.*;
import org.docma.app.ui.UserModel;
import org.docma.util.Log;


/**
 *
 * @author MP
 */
public class DocmaTreeitemRenderer implements TreeitemRenderer, EventListener
{
    private String LABEL_PRIORITY_HIGH;

    private SimpleDateFormat dateformat = new SimpleDateFormat();
    private MainWindow mainWin;


    public DocmaTreeitemRenderer(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        LABEL_PRIORITY_HIGH = mainWin.i18("label.node.priority") + ": " +
                              mainWin.i18("label.node.priority.high");
    }

    public void applyUserSettings(UserModel usr)
    {
        String pattern = (usr == null) ? null : usr.getDateFormat();
        if ((pattern == null) || (pattern.trim().equals(""))) {
            pattern = DocmaConstants.DEFAULT_DATE_FORMAT;
        }
        try {
            dateformat.applyPattern(pattern);
        } catch (IllegalArgumentException ex) {
            Log.error("User " + usr.getUserId() + " has invalid date format pattern: " + pattern);
            dateformat.applyPattern(DocmaConstants.DEFAULT_DATE_FORMAT);
        }
    }

    public void render(Treeitem item, Object obj, int index) throws Exception
    {
        Treerow tr = item.getTreerow();
        Treecell[] cells = new Treecell[7];
        if (tr != null) {
            List childlist = tr.getChildren();
            if (childlist.size() != cells.length) {
                tr.detach();
                tr = null;
            } else {
                try {
                    cells = (Treecell[]) childlist.toArray(cells);
                } catch (Exception ex) {
                    tr.detach();
                    tr = null;
                }
            }
        }
        if (tr == null) {
            tr = new Treerow();
            for (int i=0; i < cells.length; i++) {
                cells[i] = new Treecell();
                cells[i].setParent(tr);
            }
            tr.setParent(item);
        }

        if (obj instanceof DocmaNode) {
            renderDocmaNode((DocmaNode) obj, item, tr, cells);
            item.setContext("treemenu");
            item.addForward("onDoubleClick", "mainWin", "onEditContent");
            item.addEventListener("onRightClick", this);
        } else
        if (obj instanceof DocmaAnchor) {
            renderDocmaAnchor((DocmaAnchor) obj, cells);
            item.setContext("no_treemenu");
        }
        item.setValue(obj);
        // Treeitem item = new Treeitem();
        // newitem.setLabel("Neuer Inhalt");
        // Treechildren children = new Treechildren();
        // children.setParent(item);
    }

    private void renderDocmaNode(DocmaNode model,
                                 Treeitem item,
                                 Treerow tr,
                                 Treecell[] cells)
    {
        if (DocmaConstants.DEBUG) {
            System.out.println("Render Treeitem: " + model.getId() + "/" + model.getAlias());
        }
        // if (is_locked) {
        //     item.setStyle("color:#FF0000;");  // use red font color for locked nodes
        // }
        if (mainWin.isInCutList(model)) {
            for (int i=0; i < cells.length; i++) cells[i].setStyle("text-decoration:line-through; color:#888888;");
        } else {
            for (int i=0; i < cells.length; i++) cells[i].setStyle("");
        }
        boolean is_img = false;
        boolean is_file = false;
        String imgsrc = null;

        if (model.isImageContent()) {
            is_img = true;
            imgsrc = "img/image_icon.gif";
        } else
        if (model.isHTMLContent()) {
            imgsrc = model.isLocked() ? "img/doc_locked.png" : "img/doc_icon.png";
        } else
        if (model.isContent()) {
            is_file = true;
            imgsrc = "img/file_icon.gif";
        } else
        if (model.isSection()) {
            if (model.getId().equals(mainWin.getPreviewPubContentRoot())) {
                imgsrc = "img/pub_icon.png";
            } else {
                imgsrc = "img/section_icon.png";
            }
        } else
        if (model.isImageFolder()) {
            imgsrc = "img/img_folder_icon.gif";
        } else
        if (model.isSystemFolder()) {
            if (model.isSystemRoot()) {
                imgsrc = "img/sys_folder_icon.gif";
            } else {
                item.addEventListener("onOpen", this);
                imgsrc = "img/folderclosed.gif";
            }
        }

        if (model.isIncludeReference()) {
            DocmaNode refnode = model.getReferencedNode();
            boolean is_img_ref = false;
            if (refnode == null) {
                imgsrc = "img/ref_invalid.gif";
            } else {
                if (model.isImageIncludeReference() && refnode.isImageContent()) {
                    imgsrc = "img/ref_img.gif";
                    is_img_ref = true;
                } else
                if (model.isContentIncludeReference() && refnode.isContent()) {
                    imgsrc = "img/ref_doc.png";
                } else
                if (model.isSectionIncludeReference() && refnode.isSection()) {
                    imgsrc = "img/ref_section.png";
                } else {
                    imgsrc = "img/ref_invalid.gif";
                }
            }
            String tit = model.getTitle();
            if ((tit == null) || tit.equals("")) {
                if (refnode != null) {
                    tit = refnode.getTitle();
                    if (((tit == null) || tit.equals("")) && is_img_ref) {
                        tit = refnode.getDefaultFileName();
                    }
                    tit = "[" + tit + "]";
                }
                else tit = "[?]";
            }
            cells[0].setLabel(tit);
            // cells[2].setLabel("[" + model.getReferenceTarget() + "]");
            cells[2].setLabel(model.getReferenceTarget());
            cells[2].setImage("img/ref_icon.gif");
        } else {
            if (is_img) {
                String t = model.getDefaultFileName(); // model.getTitle();
                // if ((t == null) || (t.equals(""))) t = model.getDefaultFileName();
                cells[0].setLabel(t);
            } else
            if (is_file) {
                cells[0].setLabel(model.getDefaultFileName());
            } else 
            if (model.isDocumentRoot()) {
                DocmaSession docmaSess = mainWin.getDocmaSession();
                String vstr = docmaSess.getVersionId().toString();
                if (vstr.equals(DocmaConstants.DEFAULT_LATEST_VERSION_ID)) {
                    vstr = mainWin.i18("label.versionid.latest");
                } else 
                if (Character.isDigit(vstr.charAt(0))) {
                    vstr = "V" + vstr;
                }
                String transMode = docmaSess.getTranslationMode();
                if (transMode != null) vstr += " " + transMode.toUpperCase();
                cells[0].setLabel(model.getTitle() + "  [" + vstr + "]");
            } else {
                cells[0].setLabel(model.getTitle());
            }
            cells[2].setLabel(model.getAlias());
        }
        if (imgsrc != null) { 
            cells[0].setImage(imgsrc);
        }
        cells[0].setSclass("doctreecell");

        String lang_code = model.getTranslationMode();
        boolean is_translation_mode = (lang_code != null);
        boolean is_translated = false;
        if (is_translation_mode) {
            is_translated = model.isSection() ? model.isTitleTranslated() : model.isTranslated();
        }
        cells[1].setLabel((is_translated) ? lang_code.toUpperCase() : "-");

        cells[3].setLabel(model.getApplicability());

        String comment = model.getComment();
        String prio = model.getPriority();
        boolean isHighPrio = prio.equals("2");
        boolean showComment = (comment.length() > 0);
        if (isHighPrio && !showComment) {
            comment = LABEL_PRIORITY_HIGH;
            showComment = true;
        }
        String comment_icon = cells[4].getImage();
        boolean oldCommentExists = (comment_icon != null) && (comment_icon.length() > 0);
        if (oldCommentExists && !showComment) {
            // Icon has to be deleted
            // Due to a bug, setImage(null) or setImage("") does not fully remove the image.
            cells[4] = new Treecell();
            // tr.getChildren().set(4, cells[4]);   // replace old cell by new cell
            List cell_list = tr.getChildren();
            cell_list.remove(4);
            cell_list.add(4, cells[4]);
        }
        cells[4].setLabel(model.getWorkflowStatusLabel());

        if (showComment) {
            if (isHighPrio) comment_icon = "img/comment_red.png";
            else if (prio.equals("0")) comment_icon = "img/comment_grey.png";
            else comment_icon = "img/comment_yellow.png";
            cells[4].setImage(comment_icon);
            if (oldCommentExists) cells[4].getChildren().clear();  // remove any existing popup object
            Label commentLabel = new Label();
            commentLabel.setMultiline(true);
            commentLabel.setValue(comment);
            Popup popup = new Popup();
            popup.setStyle("max-width:450px; max-height:600px; overflow:hidden;");
            popup.appendChild(commentLabel);
            cells[4].appendChild(popup);
            cells[4].setTooltip(popup);
        }
        // else {
            // comment_icon = cells[4].getImage();
            // if ((comment_icon != null) && (comment_icon.length() > 0)) {
                // Note: due to a bug, setImage(null) or setImage("") does not fully remove the image.
                // Workaround: replace existing icon by transparent dummy icon.
            //     cells[4].setImage("img/spacer.gif");
            // }
            // cells[4].setTooltip((String) null);
        // }
        cells[4].setSclass("doctreecell");

        int prog_val = model.getProgress();
        // String prog_str = (prog_val == 100) ? "Finished" : "" + prog_val + "%";
        String prog_str = (prog_val < 0) ? "-" : ("" + prog_val + "%");
        cells[5].setLabel(prog_str);

        Date dt = model.getLastModifiedDate();
        String lastmod_date = (dt == null) ? "" : dateformat.format(dt);
        String lastmod_usr = model.getLastModifiedBy();  // get user id
        if (lastmod_usr != null) {
            UserModel usr_model = mainWin.getUser(lastmod_usr);
            if (usr_model != null) lastmod_usr = usr_model.getDisplayName();
        } else {
            lastmod_usr = "";
        }
        if ((lastmod_date.length() > 0) && (lastmod_usr.length() > 0)) {
            lastmod_date += "; ";
        }
        cells[6].setLabel(lastmod_date + lastmod_usr);
        // String[] authors = model.getAuthors();
        // String authstr = "";
        // if (authors != null) {
        //     for (int i=0; i < authors.length; i++) {
        //         if (i == 0) authstr = authors[0];
        //         else authstr += ", " + authors[i];
        //     }
        // }

    }

    private void renderDocmaAnchor(DocmaAnchor model, Treecell[] cells)
    {
        if (DocmaConstants.DEBUG) {
            System.out.println("Render Treeitem for anchor: " + model.getAlias());
        }
        cells[0].setImage("img/anchor.gif");
        cells[0].setLabel(model.getTitle());
        cells[1].setLabel("");
        cells[2].setLabel(model.getAlias());
        cells[3].setLabel("-");
        cells[4].setLabel("-");
        cells[5].setLabel("-");
        cells[6].setLabel("-");
    }

    public void onEvent(Event evt) throws Exception
    {
        if (evt instanceof OpenEvent) {
            OpenEvent oe = (OpenEvent) evt;
            Component comp = oe.getTarget();
            if (! (comp instanceof Treeitem)) return;
            Treeitem item = (Treeitem) comp;
            Object obj = item.getValue();
            if (! (obj instanceof DocmaNode)) return;
            DocmaNode node = (DocmaNode) obj;
            if (! node.isSystemFolder()) return;
            Treerow tr = item.getTreerow();
            if (tr == null) return;
            Component cell = tr.getFirstChild();
            if ((cell == null) || !(cell instanceof Treecell)) return;
            String imgsrc = oe.isOpen() ? "img/folderopen.gif" : "img/folderclosed.gif";
            ((Treecell) cell).setImage(imgsrc);
        } else
        if ((evt instanceof MouseEvent) && evt.getName().equalsIgnoreCase("onRightClick")) {
            // OpenEvent oe = (OpenEvent) evt;
            Component comp = evt.getTarget();
            if (! (comp instanceof Treeitem)) return;
            Treeitem item = (Treeitem) comp;
            if (! item.isSelected()) item.setSelected(true);
        }
    }

}
