/*
 * WebDesignerProps.java
 *
 *  Copyright (C) 2015  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.webdesigner;

import java.io.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class WebDesignerProps 
{
    private static final String PROP_LAYOUT = "webhelp_layout";                    // e.g. "uni_v1"
    private static final String PROP_BODY_BGCOLOR = "body_bgcolor";
    private static final String PROP_CONTENT_TOP = "content_top";                  // e.g. "0px"
    private static final String PROP_CONTENT_PADDING_LEFT = "content_padding_left";
    private static final String PROP_CONTENT_PADDING_RIGHT = "content_padding_right";
    private static final String PROP_CONTENT_WIDTH_MIN = "content_width_min";
    private static final String PROP_CONTENT_WIDTH_MAX = "content_width_max";
    private static final String PROP_WATERMARK_POSX = "watermark_posx";
    private static final String PROP_WATERMARK_POSY = "watermark_posy";
    private static final String PROP_HEADER_HEIGHT = "header_height";              // e.g. "95px"
    private static final String PROP_HEADER_PADDING_TOP = "header_padding_top";
    private static final String PROP_HEADER_PADDING_BOTTOM = "header_padding_bottom";
    private static final String PROP_HEADER_PADDING_LEFT = "header_padding_left";
    private static final String PROP_HEADER_PADDING_RIGHT = "header_padding_right";
    private static final String PROP_HEADER_TITLE1_ENABLED = "header_title1_enabled";
    private static final String PROP_HEADER_TITLE2_ENABLED = "header_title2_enabled";
    private static final String PROP_HEADER_TITLE1_FONT = "header_title1_font";
    private static final String PROP_HEADER_TITLE2_FONT = "header_title2_font";
    private static final String PROP_HEADER_TITLE_ALIGN = "header_title_align";
    private static final String PROP_HEADER_TITLE_MARGIN_TOP = "header_title_margin_top";
    private static final String PROP_HEADER_TITLE_MARGIN_BOTTOM = "header_title_margin_bottom";
    private static final String PROP_HEADER_TITLE_MARGIN_LEFT = "header_title_margin_left";
    private static final String PROP_HEADER_TITLE_MARGIN_RIGHT = "header_title_margin_right";
    private static final String PROP_HEADER_BORDER = "header_border";
    private static final String PROP_HEADER_BGCOLOR = "header_bgcolor";
    private static final String PROP_HEADER_BGIMAGE = "header_bgimage";
    private static final String PROP_HEADER_BGREPEAT = "header_bgrepeat";
    private static final String PROP_BREADCRUMBS_DISPLAY = "breadcrumbs_display";  // "none"/"block"
    private static final String PROP_LOGO1_VISIBLE = "logo1_visible";              // "true"/"false"
    private static final String PROP_LOGO1_POSITION = "logo1_position";            // "left"/"right"/"fixed-top-left"/...
    private static final String PROP_LOGO1_WIDTH = "logo1_width";
    private static final String PROP_LOGO1_HEIGHT = "logo1_height";
    private static final String PROP_LOGO1_POSX = "logo1_posx";
    private static final String PROP_LOGO1_POSY = "logo1_posy";
    private static final String PROP_LOGO2_VISIBLE = "logo2_visible";              // "true"/"false"
    private static final String PROP_LOGO2_POSITION = "logo2_position";            // "left"/"right"/"fixed-top-left"/...
    private static final String PROP_LOGO2_WIDTH = "logo2_width";
    private static final String PROP_LOGO2_HEIGHT = "logo2_height";
    private static final String PROP_LOGO2_POSX = "logo2_posx";
    private static final String PROP_LOGO2_POSY = "logo2_posy";
    private static final String PROP_NAVIGATION_WIDTH = "navigation_width";
    private static final String PROP_NAVIGATION_MARGIN_TOP = "navigation_margin_top";
    private static final String PROP_NAVIGATION_MARGIN_BOTTOM = "navigation_margin_bottom";
    private static final String PROP_NAVIGATION_PADDING_TOP = "navigation_padding_top";
    private static final String PROP_NAVIGATION_PADDING_BOTTOM = "navigation_padding_bottom";
    private static final String PROP_NAVIGATION_PADDING_LEFT = "navigation_padding_left";
    private static final String PROP_NAVIGATION_PADDING_RIGHT = "navigation_padding_right";
    private static final String PROP_NAVIGATION_BORDER = "navigation_border";
    private static final String PROP_NAVIGATION_BGCOLOR = "navigation_bgcolor";
    private static final String PROP_NAVIGATION_BGIMAGE = "navigation_bgimage";
    private static final String PROP_NAVIGATION_BGREPEAT = "navigation_bgrepeat";
    private static final String PROP_NAVIGATION_TREE_BORDER = "navigation_tree_border";
    private static final String PROP_NAVIGATION_TREE_BGCOLOR = "navigation_tree_bgcolor";
    private static final String PROP_NAVIGATION_TREE_BGIMAGE = "navigation_tree_bgimage";
    private static final String PROP_NAVIGATION_TREE_BGREPEAT = "navigation_tree_bgrepeat";
    private static final String PROP_NAVIGATION_TREE_TITLE = "navigation_tree_title";
    private static final String PROP_NAVIGATION_TREE_COLLAPSED = "navigation_tree_collapsed";
    private static final String PROP_NAVIGATION_TREE_AUTOCLOSE = "navigation_tree_autoclose";
    private static final String PROP_NAVIGATION_TREE_ANIMATION = "navigation_tree_animation";
    private static final String PROP_NAVIGATION_CONTENT_ICON_WIDTH = "navigation_content_icon_width";
    private static final String PROP_NAVIGATION_TREE_FONT_DEFAULT = "navigation_tree_font";
    private static final String PROP_NAVIGATION_TREE_FONT_HOVER = "navigation_tree_font_hover";
    private static final String PROP_NAVIGATION_TREE_FONT_CURRENT = "navigation_tree_font_current";
    private static final String PROP_NAVIGATION_TREE_FONT_TITLE = "navigation_tree_font_title";
    private static final String PROP_NAVIGATION_TOGGLE_BUTTON_IMAGE = "navigation_toggle_button_image";
    private static final String PROP_NAVIGATION_TOGGLE_BUTTON_WIDTH = "navigation_toggle_button_width";
    private static final String PROP_NAVIGATION_RESIZE_ENABLED = "navigation_resize_enabled";
    private static final String PROP_ACCORDION_ENABLED = "accordion_enabled";
    private static final String PROP_ACCORDION_BORDER = "accordion_border";
    private static final String PROP_ACCORDION_BGCOLOR = "accordion_bgcolor";
    private static final String PROP_ACCORDION_BGIMAGE = "accordion_bgimage";
    private static final String PROP_ACCORDION_BGREPEAT = "accordion_bgrepeat";
    private static final String PROP_ACCORDION_FONT = "accordion_font";
    private static final String PROP_ACCORDION_PADDING_X = "accordion_padding_horizontal";
    private static final String PROP_ACCORDION_HEIGHT = "accordion_height";
    private static final String PROP_ACCORDION_SPACING = "accordion_spacing";
    private static final String PROP_PART_TABS_MARGIN_TOP = "part_tabs_margin_top";
    private static final String PROP_PART_TABS_MARGIN_LEFT = "part_tabs_margin_left";
    private static final String PROP_PART_TAB_HEIGHT = "part_tab_height";
    private static final String PROP_PART_TAB_SPACING = "part_tab_spacing";
    private static final String PROP_PART_TAB_BG_HOVER_ENABLED = "part_tab_bg_hover_enabled";
    private static final String PROP_PART_TAB_BG_CURRENT_ENABLED = "part_tab_bg_current_enabled";
    private static final String PROP_PART_TAB_BG_DEFAULT = "part_tab_bg";
    private static final String PROP_PART_TAB_BG_HOVER = "part_tab_bg_hover";
    private static final String PROP_PART_TAB_BG_CURRENT = "part_tab_bg_current";
    private static final String PROP_PART_TAB_BGIMAGE_DEFAULT = "part_tab_bgimage";
    private static final String PROP_PART_TAB_BGIMAGE_HOVER = "part_tab_bgimage_hover";
    private static final String PROP_PART_TAB_BGIMAGE_CURRENT = "part_tab_bgimage_current";
    private static final String PROP_PART_TAB_BGREPEAT_DEFAULT = "part_tab_bgrepeat";
    private static final String PROP_PART_TAB_BGREPEAT_HOVER = "part_tab_bgrepeat_hover";
    private static final String PROP_PART_TAB_BGREPEAT_CURRENT = "part_tab_bgrepeat_current";
    private static final String PROP_PART_TAB_FONT_DEFAULT = "part_tab_font";
    private static final String PROP_PART_TAB_FONT_HOVER = "part_tab_font_hover";
    private static final String PROP_PART_TAB_FONT_CURRENT = "part_tab_font_current";
    private static final String PROP_SEARCH_UI_MODE = "search_ui_mode";
    private static final String PROP_SEARCH_POSITION = "search_position";
    private static final String PROP_SEARCH_WIDTH = "search_width";
    private static final String PROP_SEARCH_HEIGHT = "search_height";
    private static final String PROP_SEARCH_POSX = "search_posx";
    private static final String PROP_SEARCH_POSY = "search_posy";
    private static final String PROP_SEARCH_BORDER = "search_border";
    private static final String PROP_SEARCH_BGCOLOR = "search_bgcolor";
    private static final String PROP_SEARCH_BGIMAGE = "search_bgimage";
    private static final String PROP_SEARCH_BGREPEAT = "search_bgrepeat";
    private static final String PROP_SEARCH_INNER_BORDER = "search_inner_border";
    private static final String PROP_SEARCH_INNER_BGCOLOR = "search_inner_bgcolor";
    private static final String PROP_SEARCH_INNER_BGIMAGE = "search_inner_bgimage";
    private static final String PROP_SEARCH_INNER_BGREPEAT = "search_inner_bgrepeat";
    private static final String PROP_SEARCH_INNER_TOP_OFFSET = "search_inner_top_offset";
    private static final String PROP_SEARCH_INNER_LEFT_OFFSET = "search_inner_left_offset";
    private static final String PROP_SEARCH_INNER_RIGHT_OFFSET = "search_inner_right_offset";
    private static final String PROP_SEARCH_INPUT_BORDER = "search_input_border";
    private static final String PROP_SEARCH_INPUT_BGCOLOR = "search_input_bgcolor";
    private static final String PROP_SEARCH_INPUT_BGIMAGE = "search_input_bgimage";
    private static final String PROP_SEARCH_INPUT_BGREPEAT = "search_input_bgrepeat";
    private static final String PROP_SEARCH_BUTTON_WIDTH = "search_button_width";
    private static final String PROP_SEARCH_BUTTON_HEIGHT = "search_button_height";
    private static final String PROP_SEARCH_BUTTON_TEXT_VISIBLE = "search_button_text_visible";
    private static final String PROP_SEARCH_BUTTON_BORDER = "search_button_border";
    private static final String PROP_SEARCH_BUTTON_BGCOLOR = "search_button_bgcolor";
    private static final String PROP_SEARCH_BUTTON_BGIMAGE = "search_button_bgimage";
    private static final String PROP_SEARCH_BUTTON_BGREPEAT = "search_button_bgrepeat";
    private static final String PROP_SEARCH_BUTTON_FONT = "search_button_font";
    private static final String PROP_SEARCH_LEGEND_FONT = "search_legend_font";
    private static final String PROP_SEARCH_INPUT_FONT = "search_input_font";
    private static final String PROP_SEARCH_TITLE_FONT = "search_title_font";
    private static final String PROP_SEARCH_EXPRESSION_FONT = "search_expression_font";
    private static final String PROP_SEARCH_RESULT_FONT = "search_result_font";
    private static final String PROP_SEARCH_CLOSE_FONT = "search_close_font";
    private static final String PROP_SEARCH_HIT_FONT = "search_hit_font";
    private static final String PROP_SEARCH_CLOSE_IMAGE = "search_close_image";
    private static final String PROP_SEARCH_TOGGLE_BUTTON_IMAGE = "search_toggle_button_image";
    private static final String PROP_SEARCH_TOGGLE_BUTTON_WIDTH = "search_toggle_button_width";
    private static final String PROP_SEARCH_TAB_HEIGHT = "search_tab_height";
    private static final String PROP_SEARCH_TAB_BG = "search_tab_bg";
    private static final String PROP_SEARCH_TAB_BGIMAGE = "search_tab_bgimage";
    private static final String PROP_SEARCH_TAB_BGREPEAT = "search_tab_bgrepeat";
    private static final String PROP_SEARCH_TAB_BG_ACTIVE = "search_tab_bg_active";
    private static final String PROP_SEARCH_TAB_BGIMAGE_ACTIVE = "search_tab_bgimage_active";
    private static final String PROP_SEARCH_TAB_BGREPEAT_ACTIVE = "search_tab_bgrepeat_active";
    private static final String PROP_SEARCH_TAB_BG_HOVER_ENABLED = "search_tab_bg_hover_enabled";
    private static final String PROP_SEARCH_TAB_BG_HOVER = "search_tab_bg_hover";
    private static final String PROP_SEARCH_TAB_BGIMAGE_HOVER = "search_tab_bgimage_hover";
    private static final String PROP_SEARCH_TAB_BGREPEAT_HOVER = "search_tab_bgrepeat_hover";
    private static final String PROP_SEARCH_TAB_FONT_DEFAULT = "search_tab_font";
    private static final String PROP_SEARCH_TAB_FONT_ACTIVE = "search_tab_font_active";
    private static final String PROP_SEARCH_TAB_FONT_HOVER = "search_tab_font_hover";
    private static final String PROP_SEARCH_PANEL_PADDING_TOP = "search_panel_padding_top";
    private static final String PROP_SEARCH_PANEL_PADDING_BOTTOM = "search_panel_padding_bottom";
    private static final String PROP_SEARCH_PANEL_PADDING_LEFT = "search_panel_padding_left";
    private static final String PROP_SEARCH_PANEL_PADDING_RIGHT = "search_panel_padding_right";
    private static final String PROP_SEARCH_PANEL_BORDER = "search_panel_border";
    private static final String PROP_SEARCH_PANEL_BGCOLOR = "search_panel_bgcolor";
    private static final String PROP_SEARCH_PANEL_BGIMAGE = "search_panel_bgimage";
    private static final String PROP_SEARCH_PANEL_BGREPEAT = "search_panel_bgrepeat";
    private static final String PROP_LOCAL_NAVIGATION_HEIGHT = "local_navigation_height";
    private static final String PROP_LOCAL_NAVIGATION_MARGIN_TOP = "local_navigation_margin_top";
    private static final String PROP_LOCAL_NAVIGATION_PADDING_TOP = "local_navigation_padding_top";
    private static final String PROP_LOCAL_NAVIGATION_PADDING_BOTTOM = "local_navigation_padding_bottom";
    private static final String PROP_LOCAL_NAVIGATION_PADDING_LEFT = "local_navigation_padding_left";
    private static final String PROP_LOCAL_NAVIGATION_PADDING_RIGHT = "local_navigation_padding_right";
    private static final String PROP_LOCAL_NAVIGATION_BG = "local_navigation_bg";
    private static final String PROP_LOCAL_NAVIGATION_BGIMAGE = "local_navigation_bgimage";
    private static final String PROP_LOCAL_NAVIGATION_BGREPEAT = "local_navigation_bgrepeat";
    private static final String PROP_LOCAL_NAVIGATION_LINK_FONT = "local_navigation_font_link";
    private static final String PROP_LOCAL_NAVIGATION_HOVER_FONT = "local_navigation_font_hover";
    private static final String PROP_LOCAL_NAVIGATION_SEP_FONT = "local_navigation_font_sep";
    private static final String PROP_LOCAL_NAVIGATION_TEXT_VISIBLE = "local_navigation_text_visible";
    private static final String PROP_LOCAL_NAVIGATION_ICONS_VISIBLE = "local_navigation_icons_visible";
    private static final String PROP_LOCAL_NAVIGATION_SEPARATOR_VISIBLE = "local_navigation_separator_visible";
    private static final String PROP_LOCAL_NAVIGATION_ICONS_HEIGHT = "local_navigation_icons_height";
//    private static final String PROP_LOCAL_NAVIGATION_ICON_PREV = "local_navigation_icon_prev";
//    private static final String PROP_LOCAL_NAVIGATION_ICON_NEXT = "local_navigation_icon_next";
//    private static final String PROP_LOCAL_NAVIGATION_ICON_UP = "local_navigation_icon_up";
//    private static final String PROP_LOCAL_NAVIGATION_ICON_HOME = "local_navigation_icon_home";
//    private static final String PROP_LOCAL_NAVIGATION_ICON_TOC = "local_navigation_icon_toc";
    private static final String PROP_BREADCRUMBS_BG = "breadcrumbs_bg";
    private static final String PROP_BREADCRUMBS_BGIMAGE = "breadcrumbs_bgimage";
    private static final String PROP_BREADCRUMBS_BGREPEAT = "breadcrumbs_bgrepeat";
    private static final String PROP_BREADCRUMBS_LINK_FONT = "breadcrumbs_font_link";
    private static final String PROP_BREADCRUMBS_LAST_FONT = "breadcrumbs_font_last";
    private static final String PROP_BREADCRUMBS_HOVER_FONT = "breadcrumbs_font_hover";
    private static final String PROP_BREADCRUMBS_SEP_FONT = "breadcrumbs_font_sep";
    private static final String PROP_BREADCRUMBS_MARGIN_TOP = "breadcrumbs_margin_top";
    private static final String PROP_BREADCRUMBS_MARGIN_BOTTOM = "breadcrumbs_margin_bottom";
    private static final String PROP_BREADCRUMBS_MARGIN_LEFT = "breadcrumbs_margin_left";
    private static final String PROP_BREADCRUMBS_MARGIN_RIGHT = "breadcrumbs_margin_right";
    private static final String PROP_BREADCRUMBS_PADDING_TOP = "breadcrumbs_padding_top";
    private static final String PROP_BREADCRUMBS_PADDING_BOTTOM = "breadcrumbs_padding_bottom";
    private static final String PROP_BREADCRUMBS_PADDING_LEFT = "breadcrumbs_padding_left";
    private static final String PROP_BREADCRUMBS_PADDING_RIGHT = "breadcrumbs_padding_right";
    private static final String PROP_BREADCRUMBS_SEPARATOR_IMAGE = "breadcrumbs_sep_image";
    private static final String PROP_BREADCRUMBS_SEPARATOR_IMAGE_WIDTH = "breadcrumbs_sep_image_width";
    
    private File propFile;
    private Properties props;

    public WebDesignerProps(File f) throws IOException
    {
        this.propFile = f;
        if (f.exists()) {
            this.props = WhdUtils.loadPropertiesFile(f);
        } else {
            this.props = new Properties();
        }
    }
    
    public File getFile()
    {
        return this.propFile;
    }
    
    public void save() throws IOException
    {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(this.propFile);
            props.store(fout, "WebHelp-Designer Properties");
        } finally {
            if (fout != null) fout.close();
        }
    }
    
    public void saveAs(File afile) throws IOException
    {
        this.propFile = afile;
        save();
    }
    
    public String getLayout()
    {
        return props.getProperty(PROP_LAYOUT);
    }

    public void setLayout(String name, String version)
    {
        props.setProperty(PROP_LAYOUT, name + (version.startsWith("v") ? "_" : "_v") + version);
    }

    public String getLayoutId()
    {
        String name = getLayout();
        return name.substring(0, name.lastIndexOf("_v"));
    }
    
    public String getLayoutVersion()
    {
        String name = getLayout();
        return name.substring(name.lastIndexOf("_v") + 2);
    }

    public String getBodyBgColor()
    {
        return props.getProperty(PROP_BODY_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setBodyBgColor(String col)
    {
        props.setProperty(PROP_BODY_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getContentTop()
    {
        return props.getProperty(PROP_CONTENT_TOP, "0px").trim();
    }
    
    public void setContentTop(String top)
    {
        props.setProperty(PROP_CONTENT_TOP, cssLength(top));
    }

    public Integer getContentTopInteger()
    {
        String str = getContentTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getContentWidthMin()
    {
        return props.getProperty(PROP_CONTENT_WIDTH_MIN, "").trim();
    }
    
    public void setContentWidthMin(String minwidth)
    {
        props.setProperty(PROP_CONTENT_WIDTH_MIN, cssLengthOrEmpty(minwidth));
    }

    public Integer getContentWidthMinInteger()
    {
        String str = getContentWidthMin();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getContentWidthMax()
    {
        return props.getProperty(PROP_CONTENT_WIDTH_MAX, "").trim();
    }
    
    public void setContentWidthMax(String maxwidth)
    {
        props.setProperty(PROP_CONTENT_WIDTH_MAX, cssLength(maxwidth));
    }

    public Integer getContentWidthMaxInteger()
    {
        return integerOrNullFromLength(getContentWidthMax());
    }

    public String getContentPaddingLeft()
    {
        return props.getProperty(PROP_CONTENT_PADDING_LEFT, "0px").trim();
    }
    
    public void setContentPaddingLeft(String val)
    {
        props.setProperty(PROP_CONTENT_PADDING_LEFT, cssLength(val));
    }

    public Integer getContentPaddingLeftInteger()
    {
        String str = getContentPaddingLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getContentPaddingRight()
    {
        return props.getProperty(PROP_CONTENT_PADDING_RIGHT, "0px").trim();
    }
    
    public void setContentPaddingRight(String val)
    {
        props.setProperty(PROP_CONTENT_PADDING_RIGHT, cssLength(val));
    }

    public Integer getContentPaddingRightInteger()
    {
        String str = getContentPaddingRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getWatermarkPosX()
    {
        return props.getProperty(PROP_WATERMARK_POSX, "").trim();
    }
    
    public void setWatermarkPosX(String xpos)
    {
        props.setProperty(PROP_WATERMARK_POSX, cssLengthOrEmpty(xpos));
    }

    public Integer getWatermarkPosXInteger()
    {
        return integerOrNullFromLength(getWatermarkPosX());
    }

    public String getWatermarkPosY()
    {
        return props.getProperty(PROP_WATERMARK_POSY, "").trim();
    }
    
    public void setWatermarkPosY(String ypos)
    {
        props.setProperty(PROP_WATERMARK_POSY, cssLengthOrEmpty(ypos));
    }

    public Integer getWatermarkPosYInteger()
    {
        return integerOrNullFromLength(getWatermarkPosY());
    }

    public String getHeaderBorderCSS()
    {
        String css = props.getProperty(PROP_HEADER_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setHeaderBorderCSS(String border)
    {
        props.setProperty(PROP_HEADER_BORDER, (border == null) ? "" : border);
    }

    public String getHeaderBgColor()
    {
        return props.getProperty(PROP_HEADER_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setHeaderBgColor(String col)
    {
        props.setProperty(PROP_HEADER_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getHeaderBgImage()
    {
        return props.getProperty(PROP_HEADER_BGIMAGE, "").trim();
    }
    
    public void setHeaderBgImage(String filename)
    {
        props.setProperty(PROP_HEADER_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getHeaderBgRepeat()
    {
        return props.getProperty(PROP_HEADER_BGREPEAT, "no-repeat").trim();
    }
    
    public void setHeaderBgRepeat(String value)
    {
        props.setProperty(PROP_HEADER_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getHeaderHeight()
    {
        return props.getProperty(PROP_HEADER_HEIGHT, "80px").trim();
    }
    
    public void setHeaderHeight(String height)
    {
        props.setProperty(PROP_HEADER_HEIGHT, cssLength(height));
    }

    public Integer getHeaderHeightInteger()
    {
        String hstr = getHeaderHeight();
        return new Integer(extractLengthValue(hstr));  // remove px, pt, em, ...
    }

    public String getHeaderPaddingTop()
    {
        return props.getProperty(PROP_HEADER_PADDING_TOP, "0px").trim();
    }
    
    public void setHeaderPaddingTop(String top)
    {
        props.setProperty(PROP_HEADER_PADDING_TOP, cssLength(top));
    }

    public Integer getHeaderPaddingTopInteger()
    {
        String str = getHeaderPaddingTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getHeaderPaddingBottom()
    {
        return props.getProperty(PROP_HEADER_PADDING_BOTTOM, "0px").trim();
    }
    
    public void setHeaderPaddingBottom(String val)
    {
        props.setProperty(PROP_HEADER_PADDING_BOTTOM, cssLength(val));
    }

    public Integer getHeaderPaddingBottomInteger()
    {
        String str = getHeaderPaddingBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getHeaderPaddingLeft()
    {
        return props.getProperty(PROP_HEADER_PADDING_LEFT, "0px").trim();
    }
    
    public void setHeaderPaddingLeft(String val)
    {
        props.setProperty(PROP_HEADER_PADDING_LEFT, cssLength(val));
    }

    public Integer getHeaderPaddingLeftInteger()
    {
        String str = getHeaderPaddingLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getHeaderPaddingRight()
    {
        return props.getProperty(PROP_HEADER_PADDING_RIGHT, "0px").trim();
    }
    
    public void setHeaderPaddingRight(String val)
    {
        props.setProperty(PROP_HEADER_PADDING_RIGHT, cssLength(val));
    }

    public Integer getHeaderPaddingRightInteger()
    {
        String str = getHeaderPaddingRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public boolean isHeaderTitle1Enabled()
    {
        return props.getProperty(PROP_HEADER_TITLE1_ENABLED, "true").equalsIgnoreCase("true");
    }
    
    public void setHeaderTitle1Enabled(boolean enabled)
    {
        props.setProperty(PROP_HEADER_TITLE1_ENABLED, enabled ? "true" : "false");
    }

    public boolean isHeaderTitle2Enabled()
    {
        return props.getProperty(PROP_HEADER_TITLE2_ENABLED, "true").equalsIgnoreCase("true");
    }
    
    public void setHeaderTitle2Enabled(boolean enabled)
    {
        props.setProperty(PROP_HEADER_TITLE2_ENABLED, enabled ? "true" : "false");
    }

    public String getHeaderTitle1FontCSS()
    {
        String css = props.getProperty(PROP_HEADER_TITLE1_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setHeaderTitle1FontCSS(String css)
    {
        props.setProperty(PROP_HEADER_TITLE1_FONT, (css == null) ? "" : css);
    }

    public String getHeaderTitle2FontCSS()
    {
        String css = props.getProperty(PROP_HEADER_TITLE2_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setHeaderTitle2FontCSS(String css)
    {
        props.setProperty(PROP_HEADER_TITLE2_FONT, (css == null) ? "" : css);
    }

    public String getHeaderTitleAlignment()
    {
        return props.getProperty(PROP_HEADER_TITLE_ALIGN, "left").trim();
    }
    
    public void setHeaderTitleAlignment(String align)
    {
        props.setProperty(PROP_HEADER_TITLE_ALIGN, ((align == null) || align.equals("")) ? "left" : align);
    }

    public String getHeaderTitleMarginTop()
    {
        return props.getProperty(PROP_HEADER_TITLE_MARGIN_TOP, "0px").trim();
    }
    
    public void setHeaderTitleMarginTop(String top)
    {
        props.setProperty(PROP_HEADER_TITLE_MARGIN_TOP, cssLength(top));
    }

    public Integer getHeaderTitleMarginTopInteger()
    {
        String str = getHeaderTitleMarginTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getHeaderTitleMarginBottom()
    {
        return props.getProperty(PROP_HEADER_TITLE_MARGIN_BOTTOM, "0px").trim();
    }
    
    public void setHeaderTitleMarginBottom(String val)
    {
        props.setProperty(PROP_HEADER_TITLE_MARGIN_BOTTOM, cssLength(val));
    }

    public Integer getHeaderTitleMarginBottomInteger()
    {
        String str = getHeaderTitleMarginBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getHeaderTitleMarginLeft()
    {
        return props.getProperty(PROP_HEADER_TITLE_MARGIN_LEFT, "0px").trim();
    }
    
    public void setHeaderTitleMarginLeft(String val)
    {
        props.setProperty(PROP_HEADER_TITLE_MARGIN_LEFT, cssLength(val));
    }

    public Integer getHeaderTitleMarginLeftInteger()
    {
        String str = getHeaderTitleMarginLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getHeaderTitleMarginRight()
    {
        return props.getProperty(PROP_HEADER_TITLE_MARGIN_RIGHT, "0px").trim();
    }
    
    public void setHeaderTitleMarginRight(String val)
    {
        props.setProperty(PROP_HEADER_TITLE_MARGIN_RIGHT, cssLength(val));
    }

    public Integer getHeaderTitleMarginRightInteger()
    {
        String str = getHeaderTitleMarginRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsDisplay()
    {
        return props.getProperty(PROP_BREADCRUMBS_DISPLAY, "block");
    }
    
    public boolean isBreadcrumbsDisplay()
    {
        return !getBreadcrumbsDisplay().equalsIgnoreCase("none");
    }
    
    public void setBreadcrumbsDisplay(String display)
    {
        props.setProperty(PROP_BREADCRUMBS_DISPLAY, ((display == null) || display.equals("")) ? "none" : display);
    }

    public boolean isLogo1Visible()
    {
        return props.getProperty(PROP_LOGO1_VISIBLE, "false").equalsIgnoreCase("true");
    }
    
    public void setLogo1Visible(boolean visible)
    {
        props.setProperty(PROP_LOGO1_VISIBLE, visible ? "true" : "false");
    }

    public String getLogo1Position()
    {
        return props.getProperty(PROP_LOGO1_POSITION, "left").trim();
    }
    
    public void setLogo1Position(String pos)
    {
        props.setProperty(PROP_LOGO1_POSITION, ((pos == null) || pos.equals("")) ? "left" : pos);
    }

    public String getLogo1Width()
    {
        return props.getProperty(PROP_LOGO1_WIDTH, "100px").trim();
    }
    
    public void setLogo1Width(String width)
    {
        props.setProperty(PROP_LOGO1_WIDTH, cssLength(width));
    }

    public Integer getLogo1WidthInteger()
    {
        String str = getLogo1Width();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLogo1Height()
    {
        return props.getProperty(PROP_LOGO1_HEIGHT, "50px").trim();
    }
    
    public void setLogo1Height(String height)
    {
        props.setProperty(PROP_LOGO1_HEIGHT, cssLength(height));
    }

    public Integer getLogo1HeightInteger()
    {
        String str = getLogo1Height();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLogo1PosX()
    {
        return props.getProperty(PROP_LOGO1_POSX, "0px").trim();
    }
    
    public void setLogo1PosX(String xpos)
    {
        props.setProperty(PROP_LOGO1_POSX, cssLength(xpos));
    }

    public Integer getLogo1PosXInteger()
    {
        String str = getLogo1PosX();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLogo1PosY()
    {
        return props.getProperty(PROP_LOGO1_POSY, "0px").trim();
    }
    
    public void setLogo1PosY(String ypos)
    {
        props.setProperty(PROP_LOGO1_POSY, cssLength(ypos));
    }

    public Integer getLogo1PosYInteger()
    {
        String str = getLogo1PosY();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public boolean isLogo2Visible()
    {
        return props.getProperty(PROP_LOGO2_VISIBLE, "false").equalsIgnoreCase("true");
    }
    
    public void setLogo2Visible(boolean visible)
    {
        props.setProperty(PROP_LOGO2_VISIBLE, visible ? "true" : "false");
    }

    public String getLogo2Position()
    {
        return props.getProperty(PROP_LOGO2_POSITION, "right").trim();
    }
    
    public void setLogo2Position(String pos)
    {
        props.setProperty(PROP_LOGO2_POSITION, ((pos == null) || pos.equals("")) ? "right" : pos);
    }

    public String getLogo2Width()
    {
        return props.getProperty(PROP_LOGO2_WIDTH, "100px").trim();
    }
    
    public void setLogo2Width(String width)
    {
        props.setProperty(PROP_LOGO2_WIDTH, cssLength(width));
    }

    public Integer getLogo2WidthInteger()
    {
        String str = getLogo2Width();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLogo2Height()
    {
        return props.getProperty(PROP_LOGO2_HEIGHT, "50px").trim();
    }
    
    public void setLogo2Height(String height)
    {
        props.setProperty(PROP_LOGO2_HEIGHT, cssLength(height));
    }

    public Integer getLogo2HeightInteger()
    {
        String str = getLogo2Height();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLogo2PosX()
    {
        return props.getProperty(PROP_LOGO2_POSX, "0px").trim();
    }
    
    public void setLogo2PosX(String xpos)
    {
        props.setProperty(PROP_LOGO2_POSX, cssLength(xpos));
    }

    public Integer getLogo2PosXInteger()
    {
        String str = getLogo2PosX();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLogo2PosY()
    {
        return props.getProperty(PROP_LOGO2_POSY, "0px").trim();
    }
    
    public void setLogo2PosY(String ypos)
    {
        props.setProperty(PROP_LOGO2_POSY, cssLength(ypos));
    }

    public Integer getLogo2PosYInteger()
    {
        String str = getLogo2PosY();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationBorderCSS()
    {
        String css = props.getProperty(PROP_NAVIGATION_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setNavigationBorderCSS(String border)
    {
        props.setProperty(PROP_NAVIGATION_BORDER, (border == null) ? "" : border);
    }

    public String getNavigationBgColor()
    {
        return props.getProperty(PROP_NAVIGATION_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setNavigationBgColor(String col)
    {
        props.setProperty(PROP_NAVIGATION_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getNavigationBgImage()
    {
        return props.getProperty(PROP_NAVIGATION_BGIMAGE, "").trim();
    }
    
    public void setNavigationBgImage(String filename)
    {
        props.setProperty(PROP_NAVIGATION_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getNavigationBgRepeat()
    {
        return props.getProperty(PROP_NAVIGATION_BGREPEAT, "no-repeat").trim();
    }
    
    public void setNavigationBgRepeat(String value)
    {
        props.setProperty(PROP_NAVIGATION_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getNavigationTreeBorderCSS()
    {
        String css = props.getProperty(PROP_NAVIGATION_TREE_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setNavigationTreeBorderCSS(String border)
    {
        props.setProperty(PROP_NAVIGATION_TREE_BORDER, (border == null) ? "" : border);
    }

    public String getNavigationTreeBgColor()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setNavigationTreeBgColor(String col)
    {
        props.setProperty(PROP_NAVIGATION_TREE_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getNavigationTreeBgImage()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_BGIMAGE, "").trim();
    }
    
    public void setNavigationTreeBgImage(String filename)
    {
        props.setProperty(PROP_NAVIGATION_TREE_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getNavigationTreeBgRepeat()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_BGREPEAT, "no-repeat").trim();
    }
    
    public void setNavigationTreeBgRepeat(String value)
    {
        props.setProperty(PROP_NAVIGATION_TREE_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getNavigationWidth()
    {
        return props.getProperty(PROP_NAVIGATION_WIDTH, "280px").trim();
    }
    
    public void setNavigationWidth(String width)
    {
        props.setProperty(PROP_NAVIGATION_WIDTH, cssLength(width));
    }

    public Integer getNavigationWidthInteger()
    {
        String wstr = getNavigationWidth();
        return new Integer(extractLengthValue(wstr));  // remove px, pt, em, ...
    }

    public String getNavigationMarginTop()
    {
        return props.getProperty(PROP_NAVIGATION_MARGIN_TOP, "0px").trim();
    }
    
    public void setNavigationMarginTop(String top)
    {
        props.setProperty(PROP_NAVIGATION_MARGIN_TOP, cssLength(top));
    }

    public Integer getNavigationMarginTopInteger()
    {
        String str = getNavigationMarginTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationMarginBottom()
    {
        return props.getProperty(PROP_NAVIGATION_MARGIN_BOTTOM, "0px").trim();
    }
    
    public void setNavigationMarginBottom(String margin)
    {
        props.setProperty(PROP_NAVIGATION_MARGIN_BOTTOM, cssLength(margin));
    }

    public Integer getNavigationMarginBottomInteger()
    {
        String str = getNavigationMarginBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationPaddingTop()
    {
        return props.getProperty(PROP_NAVIGATION_PADDING_TOP, "0px").trim();
    }
    
    public void setNavigationPaddingTop(String top)
    {
        props.setProperty(PROP_NAVIGATION_PADDING_TOP, cssLength(top));
    }

    public Integer getNavigationPaddingTopInteger()
    {
        String str = getNavigationPaddingTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationPaddingBottom()
    {
        return props.getProperty(PROP_NAVIGATION_PADDING_BOTTOM, "0px").trim();
    }
    
    public void setNavigationPaddingBottom(String val)
    {
        props.setProperty(PROP_NAVIGATION_PADDING_BOTTOM, cssLength(val));
    }

    public Integer getNavigationPaddingBottomInteger()
    {
        String str = getNavigationPaddingBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationPaddingLeft()
    {
        return props.getProperty(PROP_NAVIGATION_PADDING_LEFT, "0px").trim();
    }
    
    public void setNavigationPaddingLeft(String val)
    {
        props.setProperty(PROP_NAVIGATION_PADDING_LEFT, cssLength(val));
    }

    public Integer getNavigationPaddingLeftInteger()
    {
        String str = getNavigationPaddingLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationPaddingRight()
    {
        return props.getProperty(PROP_NAVIGATION_PADDING_RIGHT, "0px").trim();
    }
    
    public void setNavigationPaddingRight(String val)
    {
        props.setProperty(PROP_NAVIGATION_PADDING_RIGHT, cssLength(val));
    }

    public Integer getNavigationPaddingRightInteger()
    {
        String str = getNavigationPaddingRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getNavigationTreeAnimation()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_ANIMATION, "off").trim();
    }
    
    public void setNavigationTreeAnimation(String anim)
    {
        props.setProperty(PROP_NAVIGATION_TREE_ANIMATION, ((anim == null) || anim.equals("")) ? "off" : anim);
    }

    public boolean isNavigationTreeTitleVisible()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_TITLE, "false").equalsIgnoreCase("true");
    }
    
    public void setNavigationTreeTitleVisible(boolean visible)
    {
        props.setProperty(PROP_NAVIGATION_TREE_TITLE, visible ? "true" : "false");
    }

    public boolean isNavigationTreeCollapsed()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_COLLAPSED, "false").equalsIgnoreCase("true");
    }
    
    public void setNavigationTreeCollapsed(boolean collapsed)
    {
        props.setProperty(PROP_NAVIGATION_TREE_COLLAPSED, collapsed ? "true" : "false");
    }

    public boolean isNavigationTreeAutoClose()
    {
        return props.getProperty(PROP_NAVIGATION_TREE_AUTOCLOSE, "false").equalsIgnoreCase("true");
    }
    
    public void setNavigationTreeAutoClose(boolean autoclose)
    {
        props.setProperty(PROP_NAVIGATION_TREE_AUTOCLOSE, autoclose ? "true" : "false");
    }

    public String getNavigationContentIconWidth()
    {
        return props.getProperty(PROP_NAVIGATION_CONTENT_ICON_WIDTH, "0px").trim();
    }
    
    public void setNavigationContentIconWidth(String width)
    {
        props.setProperty(PROP_NAVIGATION_CONTENT_ICON_WIDTH, cssLength(width));
    }

    public Integer getNavigationContentIconWidthInteger()
    {
        String wstr = getNavigationContentIconWidth();
        return new Integer(extractLengthValue(wstr));  // remove px, pt, em, ...
    }

    public String getNavigationTreeFontCSS()
    {
        String css = props.getProperty(PROP_NAVIGATION_TREE_FONT_DEFAULT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setNavigationTreeFontCSS(String css)
    {
        props.setProperty(PROP_NAVIGATION_TREE_FONT_DEFAULT, (css == null) ? "" : css);
    }

    public String getNavigationTreeFontHoverCSS()
    {
        String css = props.getProperty(PROP_NAVIGATION_TREE_FONT_HOVER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setNavigationTreeFontHoverCSS(String css)
    {
        props.setProperty(PROP_NAVIGATION_TREE_FONT_HOVER, (css == null) ? "" : css);
    }

    public String getNavigationTreeFontCurrentCSS()
    {
        String css = props.getProperty(PROP_NAVIGATION_TREE_FONT_CURRENT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setNavigationTreeFontCurrentCSS(String css)
    {
        props.setProperty(PROP_NAVIGATION_TREE_FONT_CURRENT, (css == null) ? "" : css);
    }

    public String getNavigationTreeFontTitleCSS()
    {
        String css = props.getProperty(PROP_NAVIGATION_TREE_FONT_TITLE, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setNavigationTreeFontTitleCSS(String css)
    {
        props.setProperty(PROP_NAVIGATION_TREE_FONT_TITLE, (css == null) ? "" : css);
    }

    public String getNavigationToggleButtonImage()
    {
        return props.getProperty(PROP_NAVIGATION_TOGGLE_BUTTON_IMAGE, "").trim();
    }
    
    public void setNavigationToggleButtonImage(String filename)
    {
        props.setProperty(PROP_NAVIGATION_TOGGLE_BUTTON_IMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getNavigationToggleButtonWidth()
    {
        return props.getProperty(PROP_NAVIGATION_TOGGLE_BUTTON_WIDTH, "15px").trim();
    }
    
    public void setNavigationToggleButtonWidth(String width)
    {
        props.setProperty(PROP_NAVIGATION_TOGGLE_BUTTON_WIDTH, cssLength(width));
    }

    public Integer getNavigationToggleButtonWidthInteger()
    {
        String wstr = getNavigationToggleButtonWidth();
        return new Integer(extractLengthValue(wstr));  // remove px, pt, em, ...
    }

    public boolean isNavigationResizeEnabled()
    {
        return props.getProperty(PROP_NAVIGATION_RESIZE_ENABLED, "false").equalsIgnoreCase("true");
    }
    
    public void setNavigationResizeEnabled(boolean enabled)
    {
        props.setProperty(PROP_NAVIGATION_RESIZE_ENABLED, enabled ? "true" : "false");
    }

    public boolean isAccordionEnabled()
    {
        return props.getProperty(PROP_ACCORDION_ENABLED, "false").equalsIgnoreCase("true");
    }
    
    public void setAccordionEnabled(boolean enabled)
    {
        props.setProperty(PROP_ACCORDION_ENABLED, enabled ? "true" : "false");
    }

    public String getAccordionBorderCSS()
    {
        String css = props.getProperty(PROP_ACCORDION_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setAccordionBorderCSS(String border)
    {
        props.setProperty(PROP_ACCORDION_BORDER, (border == null) ? "" : border);
    }

    public String getAccordionBgColor()
    {
        return props.getProperty(PROP_ACCORDION_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setAccordionBgColor(String col)
    {
        props.setProperty(PROP_ACCORDION_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getAccordionBgImage()
    {
        return props.getProperty(PROP_ACCORDION_BGIMAGE, "").trim();
    }
    
    public void setAccordionBgImage(String filename)
    {
        props.setProperty(PROP_ACCORDION_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getAccordionBgRepeat()
    {
        return props.getProperty(PROP_ACCORDION_BGREPEAT, "no-repeat").trim();
    }
    
    public void setAccordionBgRepeat(String value)
    {
        props.setProperty(PROP_ACCORDION_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getAccordionFontCSS()
    {
        String css = props.getProperty(PROP_ACCORDION_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setAccordionFontCSS(String css)
    {
        props.setProperty(PROP_ACCORDION_FONT, (css == null) ? "" : css);
    }

    public String getAccordionPaddingX()
    {
        return props.getProperty(PROP_ACCORDION_PADDING_X, "0px").trim();
    }
    
    public void setAccordionPaddingX(String val)
    {
        props.setProperty(PROP_ACCORDION_PADDING_X, cssLength(val));
    }

    public Integer getAccordionPaddingXInteger()
    {
        String str = getAccordionPaddingX();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getAccordionHeight()
    {
        return props.getProperty(PROP_ACCORDION_HEIGHT, "15px").trim();
    }
    
    public void setAccordionHeight(String val)
    {
        props.setProperty(PROP_ACCORDION_HEIGHT, cssLength(val));
    }

    public Integer getAccordionHeightInteger()
    {
        String str = getAccordionHeight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getAccordionSpacing()
    {
        return props.getProperty(PROP_ACCORDION_SPACING, "0px").trim();
    }
    
    public void setAccordionSpacing(String val)
    {
        props.setProperty(PROP_ACCORDION_SPACING, cssLength(val));
    }

    public Integer getAccordionSpacingInteger()
    {
        String str = getAccordionSpacing();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getPartTabsMarginTop()
    {
        return props.getProperty(PROP_PART_TABS_MARGIN_TOP, "0px").trim();
    }

    public void setPartTabsMarginTop(String len)
    {
        props.setProperty(PROP_PART_TABS_MARGIN_TOP, cssLength(len));
    }

    public Integer getPartTabsMarginTopInteger()
    {
        String str = getPartTabsMarginTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getPartTabsMarginLeft()
    {
        return props.getProperty(PROP_PART_TABS_MARGIN_LEFT, "0px").trim();
    }

    public void setPartTabsMarginLeft(String len)
    {
        props.setProperty(PROP_PART_TABS_MARGIN_LEFT, cssLength(len));
    }

    public Integer getPartTabsMarginLeftInteger()
    {
        String str = getPartTabsMarginLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getPartTabHeight()
    {
        return props.getProperty(PROP_PART_TAB_HEIGHT, "26px").trim();
    }

    public void setPartTabHeight(String height)
    {
        props.setProperty(PROP_PART_TAB_HEIGHT, cssLength(height));
    }

    public Integer getPartTabHeightInteger()
    {
        String hstr = getPartTabHeight();
        return new Integer(extractLengthValue(hstr));  // remove px, pt, em, ...
    }

    public String getPartTabSpacing()
    {
        return props.getProperty(PROP_PART_TAB_SPACING, "4px").trim();
    }

    public void setPartTabSpacing(String space)
    {
        props.setProperty(PROP_PART_TAB_SPACING, cssLength(space));
    }
    
    public Integer getPartTabSpacingInteger()
    {
        String str = getPartTabSpacing();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public boolean isPartTabBgHoverEnabled()
    {
        return props.getProperty(PROP_PART_TAB_BG_HOVER_ENABLED, "false").equalsIgnoreCase("true");
    }
    
    public void setPartTabBgHoverEnabled(boolean enabled)
    {
        props.setProperty(PROP_PART_TAB_BG_HOVER_ENABLED, enabled ? "true" : "false");
    }

    public boolean isPartTabBgCurrentEnabled()
    {
        return props.getProperty(PROP_PART_TAB_BG_CURRENT_ENABLED, "false").equalsIgnoreCase("true");
    }
    
    public void setPartTabBgCurrentEnabled(boolean enabled)
    {
        props.setProperty(PROP_PART_TAB_BG_CURRENT_ENABLED, enabled ? "true" : "false");
    }

    public String getPartTabBgCSS()
    {
        String css = props.getProperty(PROP_PART_TAB_BG_DEFAULT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setPartTabBgCSS(String css)
    {
        props.setProperty(PROP_PART_TAB_BG_DEFAULT, (css == null) ? "" : css);
    }

    public String getPartTabBgCSSCurrent()
    {
        String css = props.getProperty(PROP_PART_TAB_BG_CURRENT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setPartTabBgCSSCurrent(String css)
    {
        props.setProperty(PROP_PART_TAB_BG_CURRENT, (css == null) ? "" : css);
    }

    public String getPartTabBgCSSHover()
    {
        String css = props.getProperty(PROP_PART_TAB_BG_HOVER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setPartTabBgCSSHover(String css)
    {
        props.setProperty(PROP_PART_TAB_BG_HOVER, (css == null) ? "" : css);
    }

    public String getPartTabBgImage()
    {
        return props.getProperty(PROP_PART_TAB_BGIMAGE_DEFAULT, "").trim();
    }
    
    public void setPartTabBgImage(String filename)
    {
        props.setProperty(PROP_PART_TAB_BGIMAGE_DEFAULT, (filename == null) ? "" : filename.trim());
    }

    public String getPartTabBgImageCurrent()
    {
        return props.getProperty(PROP_PART_TAB_BGIMAGE_CURRENT, "").trim();
    }
    
    public void setPartTabBgImageCurrent(String filename)
    {
        props.setProperty(PROP_PART_TAB_BGIMAGE_CURRENT, (filename == null) ? "" : filename.trim());
    }

    public String getPartTabBgImageHover()
    {
        return props.getProperty(PROP_PART_TAB_BGIMAGE_HOVER, "").trim();
    }
    
    public void setPartTabBgImageHover(String filename)
    {
        props.setProperty(PROP_PART_TAB_BGIMAGE_HOVER, (filename == null) ? "" : filename.trim());
    }

    public String getPartTabBgRepeat()
    {
        return props.getProperty(PROP_PART_TAB_BGREPEAT_DEFAULT, "no-repeat").trim();
    }
    
    public void setPartTabBgRepeat(String value)
    {
        props.setProperty(PROP_PART_TAB_BGREPEAT_DEFAULT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getPartTabBgRepeatCurrent()
    {
        return props.getProperty(PROP_PART_TAB_BGREPEAT_CURRENT, "no-repeat").trim();
    }
    
    public void setPartTabBgRepeatCurrent(String value)
    {
        props.setProperty(PROP_PART_TAB_BGREPEAT_CURRENT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getPartTabBgRepeatHover()
    {
        return props.getProperty(PROP_PART_TAB_BGREPEAT_HOVER, "no-repeat").trim();
    }
    
    public void setPartTabBgRepeatHover(String value)
    {
        props.setProperty(PROP_PART_TAB_BGREPEAT_HOVER, (value == null) ? "no-repeat" : value.trim());
    }

    public String getPartTabFontCSS()
    {
        String css = props.getProperty(PROP_PART_TAB_FONT_DEFAULT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setPartTabFontCSS(String css)
    {
        props.setProperty(PROP_PART_TAB_FONT_DEFAULT, (css == null) ? "" : css);
    }

    public String getPartTabFontCSSCurrent()
    {
        String css = props.getProperty(PROP_PART_TAB_FONT_CURRENT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setPartTabFontCSSCurrent(String css)
    {
        props.setProperty(PROP_PART_TAB_FONT_CURRENT, (css == null) ? "" : css);
    }

    public String getPartTabFontCSSHover()
    {
        String css = props.getProperty(PROP_PART_TAB_FONT_HOVER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setPartTabFontCSSHover(String css)
    {
        props.setProperty(PROP_PART_TAB_FONT_HOVER, (css == null) ? "" : css);
    }

    public String getSearchUIMode()
    {
        return props.getProperty(PROP_SEARCH_UI_MODE, "tab").trim();
    }
    
    public void setSearchUIMode(String mode)
    {
        props.setProperty(PROP_SEARCH_UI_MODE, (mode == null) ? "tab" : mode);
    }

    public String getSearchPosition()
    {
        return props.getProperty(PROP_SEARCH_POSITION, "fixed-top-left").trim();
    }
    
    public void setSearchPosition(String pos)
    {
        props.setProperty(PROP_SEARCH_POSITION, ((pos == null) || pos.equals("")) ? "fixed-top-left" : pos);
    }

    public String getSearchTabHeight()
    {
        return props.getProperty(PROP_SEARCH_TAB_HEIGHT, "26px").trim();
    }

    public void setSearchTabHeight(String height)
    {
        props.setProperty(PROP_SEARCH_TAB_HEIGHT, cssLength(height));
    }

    public Integer getSearchTabHeightInteger()
    {
        String hstr = getSearchTabHeight();
        return new Integer(extractLengthValue(hstr));  // remove px, pt, em, ...
    }

    public String getSearchWidth()
    {
        return props.getProperty(PROP_SEARCH_WIDTH, "").trim();
    }
    
    public void setSearchWidth(String width)
    {
        props.setProperty(PROP_SEARCH_WIDTH, cssLengthOrEmpty(width));
    }

    public Integer getSearchWidthInteger()
    {
        return integerOrNullFromLength(getSearchWidth());  // remove px, pt, em, ...
    }

    public String getSearchHeight()
    {
        return props.getProperty(PROP_SEARCH_HEIGHT, "50px").trim();
    }
    
    public void setSearchHeight(String height)
    {
        props.setProperty(PROP_SEARCH_HEIGHT, cssLength(height));
    }

    public Integer getSearchHeightInteger()
    {
        String str = getSearchHeight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchPosX()
    {
        return props.getProperty(PROP_SEARCH_POSX, "0px").trim();
    }
    
    public void setSearchPosX(String xpos)
    {
        props.setProperty(PROP_SEARCH_POSX, cssLength(xpos));
    }

    public Integer getSearchPosXInteger()
    {
        String str = getSearchPosX();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchPosY()
    {
        return props.getProperty(PROP_SEARCH_POSY, "0px").trim();
    }
    
    public void setSearchPosY(String ypos)
    {
        props.setProperty(PROP_SEARCH_POSY, cssLength(ypos));
    }

    public Integer getSearchPosYInteger()
    {
        String str = getSearchPosY();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchBorderCSS()
    {
        String css = props.getProperty(PROP_SEARCH_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchBorderCSS(String border)
    {
        props.setProperty(PROP_SEARCH_BORDER, (border == null) ? "" : border);
    }

    public String getSearchBgColor()
    {
        return props.getProperty(PROP_SEARCH_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setSearchBgColor(String col)
    {
        props.setProperty(PROP_SEARCH_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getSearchBgImage()
    {
        return props.getProperty(PROP_SEARCH_BGIMAGE, "").trim();
    }
    
    public void setSearchBgImage(String filename)
    {
        props.setProperty(PROP_SEARCH_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchBgRepeat()
    {
        return props.getProperty(PROP_SEARCH_BGREPEAT, "no-repeat").trim();
    }
    
    public void setSearchBgRepeat(String value)
    {
        props.setProperty(PROP_SEARCH_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getSearchInnerBorderCSS()
    {
        String css = props.getProperty(PROP_SEARCH_INNER_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchInnerBorderCSS(String border)
    {
        props.setProperty(PROP_SEARCH_INNER_BORDER, (border == null) ? "" : border);
    }

    public String getSearchInnerBgColor()
    {
        return props.getProperty(PROP_SEARCH_INNER_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setSearchInnerBgColor(String col)
    {
        props.setProperty(PROP_SEARCH_INNER_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getSearchInnerBgImage()
    {
        return props.getProperty(PROP_SEARCH_INNER_BGIMAGE, "").trim();
    }
    
    public void setSearchInnerBgImage(String filename)
    {
        props.setProperty(PROP_SEARCH_INNER_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchInnerBgRepeat()
    {
        return props.getProperty(PROP_SEARCH_INNER_BGREPEAT, "no-repeat").trim();
    }
    
    public void setSearchInnerBgRepeat(String value)
    {
        props.setProperty(PROP_SEARCH_INNER_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getSearchInnerTopOffset()
    {
        return props.getProperty(PROP_SEARCH_INNER_TOP_OFFSET, "0px").trim();
    }
    
    public void setSearchInnerTopOffset(String ypos)
    {
        props.setProperty(PROP_SEARCH_INNER_TOP_OFFSET, cssLength(ypos));
    }

    public Integer getSearchInnerTopOffsetInteger()
    {
        String str = getSearchInnerTopOffset();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchInnerLeftOffset()
    {
        return props.getProperty(PROP_SEARCH_INNER_LEFT_OFFSET, "0px").trim();
    }
    
    public void setSearchInnerLeftOffset(String xpos)
    {
        props.setProperty(PROP_SEARCH_INNER_LEFT_OFFSET, cssLength(xpos));
    }

    public Integer getSearchInnerLeftOffsetInteger()
    {
        String str = getSearchInnerLeftOffset();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchInnerRightOffset()
    {
        return props.getProperty(PROP_SEARCH_INNER_RIGHT_OFFSET, "0px").trim();
    }
    
    public void setSearchInnerRightOffset(String xpos)
    {
        props.setProperty(PROP_SEARCH_INNER_RIGHT_OFFSET, cssLength(xpos));
    }

    public Integer getSearchInnerRightOffsetInteger()
    {
        String str = getSearchInnerRightOffset();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchInputBorderCSS()
    {
        String css = props.getProperty(PROP_SEARCH_INPUT_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchInputBorderCSS(String border)
    {
        props.setProperty(PROP_SEARCH_INPUT_BORDER, (border == null) ? "" : border);
    }

    public String getSearchInputBgColor()
    {
        return props.getProperty(PROP_SEARCH_INPUT_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setSearchInputBgColor(String col)
    {
        props.setProperty(PROP_SEARCH_INPUT_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getSearchInputBgImage()
    {
        return props.getProperty(PROP_SEARCH_INPUT_BGIMAGE, "").trim();
    }
    
    public void setSearchInputBgImage(String filename)
    {
        props.setProperty(PROP_SEARCH_INPUT_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchInputBgRepeat()
    {
        return props.getProperty(PROP_SEARCH_INPUT_BGREPEAT, "no-repeat").trim();
    }
    
    public void setSearchInputBgRepeat(String value)
    {
        props.setProperty(PROP_SEARCH_INPUT_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getSearchButtonWidth()
    {
        return props.getProperty(PROP_SEARCH_BUTTON_WIDTH, "28px").trim();
    }
    
    public void setSearchButtonWidth(String width)
    {
        props.setProperty(PROP_SEARCH_BUTTON_WIDTH, cssLengthOrEmpty(width));
    }

    public Integer getSearchButtonWidthInteger()
    {
        return integerOrNullFromLength(getSearchButtonWidth());  // remove px, pt, em, ...
    }

    public String getSearchButtonHeight()
    {
        return props.getProperty(PROP_SEARCH_BUTTON_HEIGHT, "18px").trim();
    }
    
    public void setSearchButtonHeight(String height)
    {
        props.setProperty(PROP_SEARCH_BUTTON_HEIGHT, cssLengthOrEmpty(height));
    }

    public Integer getSearchButtonHeightInteger()
    {
        return integerOrNullFromLength(getSearchButtonHeight());  // remove px, pt, em, ...
    }

    public boolean isSearchButtonTextVisible()
    {
        return props.getProperty(PROP_SEARCH_BUTTON_TEXT_VISIBLE, "false").equalsIgnoreCase("true");
    }
    
    public void setSearchButtonTextVisible(boolean visible)
    {
        props.setProperty(PROP_SEARCH_BUTTON_TEXT_VISIBLE, visible ? "true" : "false");
    }

    public String getSearchButtonBorderCSS()
    {
        String css = props.getProperty(PROP_SEARCH_BUTTON_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchButtonBorderCSS(String border)
    {
        props.setProperty(PROP_SEARCH_BUTTON_BORDER, (border == null) ? "" : border);
    }

    public String getSearchButtonBgColor()
    {
        return props.getProperty(PROP_SEARCH_BUTTON_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setSearchButtonBgColor(String col)
    {
        props.setProperty(PROP_SEARCH_BUTTON_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getSearchButtonBgImage()
    {
        return props.getProperty(PROP_SEARCH_BUTTON_BGIMAGE, "").trim();
    }
    
    public void setSearchButtonBgImage(String filename)
    {
        props.setProperty(PROP_SEARCH_BUTTON_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchButtonBgRepeat()
    {
        return props.getProperty(PROP_SEARCH_BUTTON_BGREPEAT, "no-repeat").trim();
    }
    
    public void setSearchButtonBgRepeat(String value)
    {
        props.setProperty(PROP_SEARCH_BUTTON_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getSearchButtonFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_BUTTON_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }

    public void setSearchButtonFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_BUTTON_FONT, (css == null) ? "" : css);
    }

    public String getSearchLegendFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_LEGEND_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchLegendFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_LEGEND_FONT, (css == null) ? "" : css);
    }

    public String getSearchInputFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_INPUT_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchInputFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_INPUT_FONT, (css == null) ? "" : css);
    }

    public String getSearchTitleFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_TITLE_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTitleFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_TITLE_FONT, (css == null) ? "" : css);
    }

    public String getSearchExpressionFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_EXPRESSION_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchExpressionFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_EXPRESSION_FONT, (css == null) ? "" : css);
    }

    public String getSearchResultFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_RESULT_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchResultFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_RESULT_FONT, (css == null) ? "" : css);
    }

    public String getSearchCloseFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_CLOSE_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchCloseFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_CLOSE_FONT, (css == null) ? "" : css);
    }

    public String getSearchHitFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_HIT_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchHitFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_HIT_FONT, (css == null) ? "" : css);
    }

    public String getSearchCloseImage()
    {
        return props.getProperty(PROP_SEARCH_CLOSE_IMAGE, "").trim();
    }
    
    public void setSearchCloseImage(String filename)
    {
        props.setProperty(PROP_SEARCH_CLOSE_IMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchToggleButtonImage()
    {
        return props.getProperty(PROP_SEARCH_TOGGLE_BUTTON_IMAGE, "").trim();
    }
    
    public void setSearchToggleButtonImage(String filename)
    {
        props.setProperty(PROP_SEARCH_TOGGLE_BUTTON_IMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchToggleButtonWidth()
    {
        return props.getProperty(PROP_SEARCH_TOGGLE_BUTTON_WIDTH, "15px").trim();
    }
    
    public void setSearchToggleButtonWidth(String width)
    {
        props.setProperty(PROP_SEARCH_TOGGLE_BUTTON_WIDTH, cssLength(width));
    }

    public Integer getSearchToggleButtonWidthInteger()
    {
        String wstr = getSearchToggleButtonWidth();
        return new Integer(extractLengthValue(wstr));  // remove px, pt, em, ...
    }

    public String getSearchTabBgCSS()
    {
        String css = props.getProperty(PROP_SEARCH_TAB_BG, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTabBgCSS(String css)
    {
        props.setProperty(PROP_SEARCH_TAB_BG, (css == null) ? "" : css);
    }

    public String getSearchTabBgImage()
    {
        return props.getProperty(PROP_SEARCH_TAB_BGIMAGE, "").trim();
    }
    
    public void setSearchTabBgImage(String filename)
    {
        props.setProperty(PROP_SEARCH_TAB_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchTabBgRepeat()
    {
        return props.getProperty(PROP_SEARCH_TAB_BGREPEAT, "no-repeat").trim();
    }
    
    public void setSearchTabBgRepeat(String value)
    {
        props.setProperty(PROP_SEARCH_TAB_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getSearchTabBgCSSActive()
    {
        String css = props.getProperty(PROP_SEARCH_TAB_BG_ACTIVE, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTabBgCSSActive(String css)
    {
        props.setProperty(PROP_SEARCH_TAB_BG_ACTIVE, (css == null) ? "" : css);
    }

    public String getSearchTabBgImageActive()
    {
        return props.getProperty(PROP_SEARCH_TAB_BGIMAGE_ACTIVE, "").trim();
    }
    
    public void setSearchTabBgImageActive(String filename)
    {
        props.setProperty(PROP_SEARCH_TAB_BGIMAGE_ACTIVE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchTabBgRepeatActive()
    {
        return props.getProperty(PROP_SEARCH_TAB_BGREPEAT_ACTIVE, "no-repeat").trim();
    }
    
    public void setSearchTabBgRepeatActive(String value)
    {
        props.setProperty(PROP_SEARCH_TAB_BGREPEAT_ACTIVE, (value == null) ? "no-repeat" : value.trim());
    }

    public String getSearchTabBgCSSHover()
    {
        String css = props.getProperty(PROP_SEARCH_TAB_BG_HOVER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTabBgCSSHover(String css)
    {
        props.setProperty(PROP_SEARCH_TAB_BG_HOVER, (css == null) ? "" : css);
    }

    public String getSearchTabBgImageHover()
    {
        return props.getProperty(PROP_SEARCH_TAB_BGIMAGE_HOVER, "").trim();
    }
    
    public void setSearchTabBgImageHover(String filename)
    {
        props.setProperty(PROP_SEARCH_TAB_BGIMAGE_HOVER, (filename == null) ? "" : filename.trim());
    }

    public String getSearchTabBgRepeatHover()
    {
        return props.getProperty(PROP_SEARCH_TAB_BGREPEAT_HOVER, "no-repeat").trim();
    }
    
    public void setSearchTabBgRepeatHover(String value)
    {
        props.setProperty(PROP_SEARCH_TAB_BGREPEAT_HOVER, (value == null) ? "no-repeat" : value.trim());
    }

    public boolean isSearchTabBgHoverEnabled()
    {
        return props.getProperty(PROP_SEARCH_TAB_BG_HOVER_ENABLED, "false").equalsIgnoreCase("true");
    }

    public void setSearchTabBgHoverEnabled(boolean enabled)
    {
        props.setProperty(PROP_SEARCH_TAB_BG_HOVER_ENABLED, enabled ? "true" : "false");
    }

    public String getSearchTabFontCSS()
    {
        String css = props.getProperty(PROP_SEARCH_TAB_FONT_DEFAULT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTabFontCSS(String css)
    {
        props.setProperty(PROP_SEARCH_TAB_FONT_DEFAULT, (css == null) ? "" : css);
    }

    public String getSearchTabFontCSSActive()
    {
        String css = props.getProperty(PROP_SEARCH_TAB_FONT_ACTIVE, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTabFontCSSActive(String css)
    {
        props.setProperty(PROP_SEARCH_TAB_FONT_ACTIVE, (css == null) ? "" : css);
    }

    public String getSearchTabFontCSSHover()
    {
        String css = props.getProperty(PROP_SEARCH_TAB_FONT_HOVER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchTabFontCSSHover(String css)
    {
        props.setProperty(PROP_SEARCH_TAB_FONT_HOVER, (css == null) ? "" : css);
    }

    public String getSearchPanelPaddingTop()
    {
        return props.getProperty(PROP_SEARCH_PANEL_PADDING_TOP, "0px").trim();
    }
    
    public void setSearchPanelPaddingTop(String top)
    {
        props.setProperty(PROP_SEARCH_PANEL_PADDING_TOP, cssLength(top));
    }

    public Integer getSearchPanelPaddingTopInteger()
    {
        String str = getSearchPanelPaddingTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchPanelPaddingBottom()
    {
        return props.getProperty(PROP_SEARCH_PANEL_PADDING_BOTTOM, "0px").trim();
    }
    
    public void setSearchPanelPaddingBottom(String val)
    {
        props.setProperty(PROP_SEARCH_PANEL_PADDING_BOTTOM, cssLength(val));
    }

    public Integer getSearchPanelPaddingBottomInteger()
    {
        String str = getSearchPanelPaddingBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchPanelPaddingLeft()
    {
        return props.getProperty(PROP_SEARCH_PANEL_PADDING_LEFT, "0px").trim();
    }
    
    public void setSearchPanelPaddingLeft(String val)
    {
        props.setProperty(PROP_SEARCH_PANEL_PADDING_LEFT, cssLength(val));
    }

    public Integer getSearchPanelPaddingLeftInteger()
    {
        String str = getSearchPanelPaddingLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchPanelPaddingRight()
    {
        return props.getProperty(PROP_SEARCH_PANEL_PADDING_RIGHT, "0px").trim();
    }
    
    public void setSearchPanelPaddingRight(String val)
    {
        props.setProperty(PROP_SEARCH_PANEL_PADDING_RIGHT, cssLength(val));
    }

    public Integer getSearchPanelPaddingRightInteger()
    {
        String str = getSearchPanelPaddingRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getSearchPanelBorderCSS()
    {
        String css = props.getProperty(PROP_SEARCH_PANEL_BORDER, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setSearchPanelBorderCSS(String border)
    {
        props.setProperty(PROP_SEARCH_PANEL_BORDER, (border == null) ? "" : border);
    }

    public String getSearchPanelBgColor()
    {
        return props.getProperty(PROP_SEARCH_PANEL_BGCOLOR, "#FFFFFF").trim();
    }
    
    public void setSearchPanelBgColor(String col)
    {
        props.setProperty(PROP_SEARCH_PANEL_BGCOLOR, (col == null) ? "#FFFFFF" : col);
    }

    public String getSearchPanelBgImage()
    {
        return props.getProperty(PROP_SEARCH_PANEL_BGIMAGE, "").trim();
    }
    
    public void setSearchPanelBgImage(String filename)
    {
        props.setProperty(PROP_SEARCH_PANEL_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getSearchPanelBgRepeat()
    {
        return props.getProperty(PROP_SEARCH_PANEL_BGREPEAT, "no-repeat").trim();
    }
    
    public void setSearchPanelBgRepeat(String value)
    {
        props.setProperty(PROP_SEARCH_PANEL_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getLocalNavigationHeight()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_HEIGHT, "").trim();
    }

    public void setLocalNavigationHeight(String height)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_HEIGHT, cssLengthOrEmpty(height));
    }

    public Integer getLocalNavigationHeightInteger()
    {
        return integerOrNullFromLength(getLocalNavigationHeight());  // remove px, pt, em, ...
    }

    public String getLocalNavigationMarginTop()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_MARGIN_TOP, "0px").trim();
    }
    
    public void setLocalNavigationMarginTop(String top)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_MARGIN_TOP, cssLength(top));
    }

    public Integer getLocalNavigationMarginTopInteger()
    {
        String str = getLocalNavigationMarginTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLocalNavigationPaddingTop()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_PADDING_TOP, "0px").trim();
    }
    
    public void setLocalNavigationPaddingTop(String top)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_PADDING_TOP, cssLength(top));
    }

    public Integer getLocalNavigationPaddingTopInteger()
    {
        String str = getLocalNavigationPaddingTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLocalNavigationPaddingBottom()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_PADDING_BOTTOM, "0px").trim();
    }
    
    public void setLocalNavigationPaddingBottom(String top)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_PADDING_BOTTOM, cssLength(top));
    }

    public Integer getLocalNavigationPaddingBottomInteger()
    {
        String str = getLocalNavigationPaddingBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLocalNavigationPaddingLeft()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_PADDING_LEFT, "0px").trim();
    }
    
    public void setLocalNavigationPaddingLeft(String top)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_PADDING_LEFT, cssLength(top));
    }

    public Integer getLocalNavigationPaddingLeftInteger()
    {
        String str = getLocalNavigationPaddingLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLocalNavigationPaddingRight()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_PADDING_RIGHT, "0px").trim();
    }
    
    public void setLocalNavigationPaddingRight(String top)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_PADDING_RIGHT, cssLength(top));
    }

    public Integer getLocalNavigationPaddingRightInteger()
    {
        String str = getLocalNavigationPaddingRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getLocalNavigationBgCSS()
    {
        String css = props.getProperty(PROP_LOCAL_NAVIGATION_BG, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setLocalNavigationBgCSS(String css)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_BG, (css == null) ? "" : css);
    }

    public String getLocalNavigationBgImage()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_BGIMAGE, "").trim();
    }
    
    public void setLocalNavigationBgImage(String filename)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getLocalNavigationBgRepeat()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_BGREPEAT, "no-repeat").trim();
    }
    
    public void setLocalNavigationBgRepeat(String value)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getLocalNavigationLinkCSS()
    {
        String css = props.getProperty(PROP_LOCAL_NAVIGATION_LINK_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setLocalNavigationLinkCSS(String css)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_LINK_FONT, (css == null) ? "" : css);
    }

    public String getLocalNavigationLinkHoverCSS()
    {
        String css = props.getProperty(PROP_LOCAL_NAVIGATION_HOVER_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setLocalNavigationLinkHoverCSS(String css)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_HOVER_FONT, (css == null) ? "" : css);
    }

    public String getLocalNavigationSeparatorCSS()
    {
        String css = props.getProperty(PROP_LOCAL_NAVIGATION_SEP_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setLocalNavigationSeparatorCSS(String css)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_SEP_FONT, (css == null) ? "" : css);
    }

    public boolean isLocalNavigationTextVisible()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_TEXT_VISIBLE, "true").equalsIgnoreCase("true");
    }
    
    public void setLocalNavigationTextVisible(boolean visible)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_TEXT_VISIBLE, visible ? "true" : "false");
    }

    public boolean isLocalNavigationIconsVisible()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_ICONS_VISIBLE, "false").equalsIgnoreCase("true");
    }
    
    public void setLocalNavigationIconsVisible(boolean visible)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_ICONS_VISIBLE, visible ? "true" : "false");
    }

    public boolean isLocalNavigationSeparatorVisible()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_SEPARATOR_VISIBLE, "true").equalsIgnoreCase("true");
    }
    
    public void setLocalNavigationSeparatorVisible(boolean visible)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_SEPARATOR_VISIBLE, visible ? "true" : "false");
    }

    public String getLocalNavigationIconsHeight()
    {
        return props.getProperty(PROP_LOCAL_NAVIGATION_ICONS_HEIGHT, "16px").trim();
    }
    
    public void setLocalNavigationIconsHeight(String height)
    {
        props.setProperty(PROP_LOCAL_NAVIGATION_ICONS_HEIGHT, cssLength(height));
    }

    public Integer getLocalNavigationIconsHeightInteger()
    {
        String str = getLocalNavigationIconsHeight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

//    public String getLocalNavigationIconPrev()
//    {
//        return props.getProperty(PROP_LOCAL_NAVIGATION_ICON_PREV, "").trim();
//    }
//    
//    public void setLocalNavigationIconPrev(String filename)
//    {
//        props.setProperty(PROP_LOCAL_NAVIGATION_ICON_PREV, (filename == null) ? "" : filename.trim());
//    }
//
//    public String getLocalNavigationIconNext()
//    {
//        return props.getProperty(PROP_LOCAL_NAVIGATION_ICON_NEXT, "").trim();
//    }
//    
//    public void setLocalNavigationIconNext(String filename)
//    {
//        props.setProperty(PROP_LOCAL_NAVIGATION_ICON_NEXT, (filename == null) ? "" : filename.trim());
//    }
//
//    public String getLocalNavigationIconUp()
//    {
//        return props.getProperty(PROP_LOCAL_NAVIGATION_ICON_UP, "").trim();
//    }
//    
//    public void setLocalNavigationIconUp(String filename)
//    {
//        props.setProperty(PROP_LOCAL_NAVIGATION_ICON_UP, (filename == null) ? "" : filename.trim());
//    }
//
//    public String getLocalNavigationIconHome()
//    {
//        return props.getProperty(PROP_LOCAL_NAVIGATION_ICON_HOME, "").trim();
//    }
//    
//    public void setLocalNavigationIconHome(String filename)
//    {
//        props.setProperty(PROP_LOCAL_NAVIGATION_ICON_HOME, (filename == null) ? "" : filename.trim());
//    }
//
//    public String getLocalNavigationIconToc()
//    {
//        return props.getProperty(PROP_LOCAL_NAVIGATION_ICON_TOC, "").trim();
//    }
//    
//    public void setLocalNavigationIconToc(String filename)
//    {
//        props.setProperty(PROP_LOCAL_NAVIGATION_ICON_TOC, (filename == null) ? "" : filename.trim());
//    }

    public String getBreadcrumbsBgCSS()
    {
        String css = props.getProperty(PROP_BREADCRUMBS_BG, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setBreadcrumbsBgCSS(String css)
    {
        props.setProperty(PROP_BREADCRUMBS_BG, (css == null) ? "" : css);
    }

    public String getBreadcrumbsBgImage()
    {
        return props.getProperty(PROP_BREADCRUMBS_BGIMAGE, "").trim();
    }
    
    public void setBreadcrumbsBgImage(String filename)
    {
        props.setProperty(PROP_BREADCRUMBS_BGIMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getBreadcrumbsBgRepeat()
    {
        return props.getProperty(PROP_BREADCRUMBS_BGREPEAT, "no-repeat").trim();
    }
    
    public void setBreadcrumbsBgRepeat(String value)
    {
        props.setProperty(PROP_BREADCRUMBS_BGREPEAT, (value == null) ? "no-repeat" : value.trim());
    }

    public String getBreadcrumbsLinkCSS()
    {
        String css = props.getProperty(PROP_BREADCRUMBS_LINK_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setBreadcrumbsLinkCSS(String css)
    {
        props.setProperty(PROP_BREADCRUMBS_LINK_FONT, (css == null) ? "" : css);
    }

    public String getBreadcrumbsLastCSS()
    {
        String css = props.getProperty(PROP_BREADCRUMBS_LAST_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setBreadcrumbsLastCSS(String css)
    {
        props.setProperty(PROP_BREADCRUMBS_LAST_FONT, (css == null) ? "" : css);
    }

    public String getBreadcrumbsHoverCSS()
    {
        String css = props.getProperty(PROP_BREADCRUMBS_HOVER_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setBreadcrumbsHoverCSS(String css)
    {
        props.setProperty(PROP_BREADCRUMBS_HOVER_FONT, (css == null) ? "" : css);
    }

    public String getBreadcrumbsSeparatorCSS()
    {
        String css = props.getProperty(PROP_BREADCRUMBS_SEP_FONT, "").trim();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        return css;
    }
    
    public void setBreadcrumbsSeparatorCSS(String css)
    {
        props.setProperty(PROP_BREADCRUMBS_SEP_FONT, (css == null) ? "" : css);
    }

    public String getBreadcrumbsMarginTop()
    {
        return props.getProperty(PROP_BREADCRUMBS_MARGIN_TOP, "0px").trim();
    }
    
    public void setBreadcrumbsMarginTop(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_MARGIN_TOP, cssLength(top));
    }

    public Integer getBreadcrumbsMarginTopInteger()
    {
        String str = getBreadcrumbsMarginTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsMarginBottom()
    {
        return props.getProperty(PROP_BREADCRUMBS_MARGIN_BOTTOM, "0px").trim();
    }
    
    public void setBreadcrumbsMarginBottom(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_MARGIN_BOTTOM, cssLength(top));
    }

    public Integer getBreadcrumbsMarginBottomInteger()
    {
        String str = getBreadcrumbsMarginBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsMarginLeft()
    {
        return props.getProperty(PROP_BREADCRUMBS_MARGIN_LEFT, "0px").trim();
    }
    
    public void setBreadcrumbsMarginLeft(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_MARGIN_LEFT, cssLength(top));
    }

    public Integer getBreadcrumbsMarginLeftInteger()
    {
        String str = getBreadcrumbsMarginLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsMarginRight()
    {
        return props.getProperty(PROP_BREADCRUMBS_MARGIN_RIGHT, "0px").trim();
    }
    
    public void setBreadcrumbsMarginRight(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_MARGIN_RIGHT, cssLength(top));
    }

    public Integer getBreadcrumbsMarginRightInteger()
    {
        String str = getBreadcrumbsMarginRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsPaddingTop()
    {
        return props.getProperty(PROP_BREADCRUMBS_PADDING_TOP, "0px").trim();
    }
    
    public void setBreadcrumbsPaddingTop(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_PADDING_TOP, cssLength(top));
    }

    public Integer getBreadcrumbsPaddingTopInteger()
    {
        String str = getBreadcrumbsPaddingTop();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsPaddingBottom()
    {
        return props.getProperty(PROP_BREADCRUMBS_PADDING_BOTTOM, "0px").trim();
    }
    
    public void setBreadcrumbsPaddingBottom(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_PADDING_BOTTOM, cssLength(top));
    }

    public Integer getBreadcrumbsPaddingBottomInteger()
    {
        String str = getBreadcrumbsPaddingBottom();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsPaddingLeft()
    {
        return props.getProperty(PROP_BREADCRUMBS_PADDING_LEFT, "0px").trim();
    }
    
    public void setBreadcrumbsPaddingLeft(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_PADDING_LEFT, cssLength(top));
    }

    public Integer getBreadcrumbsPaddingLeftInteger()
    {
        String str = getBreadcrumbsPaddingLeft();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsPaddingRight()
    {
        return props.getProperty(PROP_BREADCRUMBS_PADDING_RIGHT, "0px").trim();
    }
    
    public void setBreadcrumbsPaddingRight(String top)
    {
        props.setProperty(PROP_BREADCRUMBS_PADDING_RIGHT, cssLength(top));
    }

    public Integer getBreadcrumbsPaddingRightInteger()
    {
        String str = getBreadcrumbsPaddingRight();
        return new Integer(extractLengthValue(str));  // remove px, pt, em, ...
    }

    public String getBreadcrumbsSeparatorImage()
    {
        return props.getProperty(PROP_BREADCRUMBS_SEPARATOR_IMAGE, "").trim();
    }
    
    public void setBreadcrumbsSeparatorImage(String filename)
    {
        props.setProperty(PROP_BREADCRUMBS_SEPARATOR_IMAGE, (filename == null) ? "" : filename.trim());
    }

    public String getBreadcrumbsSeparatorImageWidth()
    {
        return props.getProperty(PROP_BREADCRUMBS_SEPARATOR_IMAGE_WIDTH, "15px").trim();
    }
    
    public void setBreadcrumbsSeparatorImageWidth(String width)
    {
        props.setProperty(PROP_BREADCRUMBS_SEPARATOR_IMAGE_WIDTH, cssLength(width));
    }

    public Integer getBreadcrumbsSeparatorImageWidthInteger()
    {
        String wstr = getBreadcrumbsSeparatorImageWidth();
        return new Integer(extractLengthValue(wstr));  // remove px, pt, em, ...
    }


    //********************************************************************
    //*********************   Helper functions   *************************
    //********************************************************************
    
    private String cssLength(String len) 
    {
        return ((len == null) || len.equals("")) ? "0px" : len;
    }
    
    private String cssLengthOrEmpty(String len) 
    {
        return (len == null) ? "" : len;
    }
    
    private String extractLengthValue(String str)
    {
        return (str.length() < 2) ? "0" : str.substring(0, str.length() - 2);  // remove px, pt, em, ...
    }
    
    private Integer integerOrNullFromLength(String len)
    {
        return (len == null || len.equals("")) ? null : new Integer(extractLengthValue(len));  // remove px, pt, em, ...
    }
}
