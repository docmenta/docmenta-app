/*
 * DocmaCharEntity.java
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

package org.docma.app;

import java.util.*;
import org.docma.coreapi.DocException;
import org.docma.plugin.CharEntity;

/**
 *
 * @author MP
 */
public class DocmaCharEntity implements CharEntity
{
    public static final DocmaCharEntity[] DEFAULT_ENTITIES = {
        new DocmaCharEntity("&nbsp;", "&#160;", true, "no-break space"),
        new DocmaCharEntity("&amp;", "&#38;", true, "ampersand"),
        new DocmaCharEntity("&quot;", "&#34;", true, "quotation mark"),
        // finance
        new DocmaCharEntity("&cent;", "&#162;", true, "cent sign"),
        new DocmaCharEntity("&euro;", "&#8364;", true, "euro sign"),
        new DocmaCharEntity("&pound;", "&#163;", true, "pound sign"),
        new DocmaCharEntity("&yen;", "&#165;", true, "yen sign"),
        // signs
        new DocmaCharEntity("&copy;", "&#169;", true, "copyright sign"),
        new DocmaCharEntity("&reg;", "&#174;", true, "registered sign"),
        new DocmaCharEntity("&trade;", "&#8482;", true, "trade mark sign"),
        new DocmaCharEntity("&permil;", "&#8240;", true, "per mille sign"),
        new DocmaCharEntity("&micro;", "&#181;", true, "micro sign"),
        new DocmaCharEntity("&middot;", "&#183;", true, "middle dot"),
        new DocmaCharEntity("&bull;", "&#8226;", true, "bullet"),
        new DocmaCharEntity("&hellip;", "&#8230;", true, "three dot leader"),
        new DocmaCharEntity("&prime;", "&#8242;", true, "minutes / feet"),
        new DocmaCharEntity("&Prime;", "&#8243;", true, "seconds / inches"),
        new DocmaCharEntity("&sect;", "&#167;", true, "section sign"),
        new DocmaCharEntity("&para;", "&#182;", true, "paragraph sign"),
        new DocmaCharEntity("&szlig;", "&#223;", true, "sharp s / ess-zed"),
        // quotations
        new DocmaCharEntity("&lsaquo;", "&#8249;", true, "single left-pointing angle quotation mark"),
        new DocmaCharEntity("&rsaquo;", "&#8250;", true, "single right-pointing angle quotation mark"),
        new DocmaCharEntity("&laquo;", "&#171;", true, "left pointing guillemet"),
        new DocmaCharEntity("&raquo;", "&#187;", true, "right pointing guillemet"),
        new DocmaCharEntity("&lsquo;", "&#8216;", true, "left single quotation mark"),
        new DocmaCharEntity("&rsquo;", "&#8217;", true, "right single quotation mark"),
        new DocmaCharEntity("&ldquo;", "&#8220;", true, "left double quotation mark"),
        new DocmaCharEntity("&rdquo;", "&#8221;", true, "right double quotation mark"),
        new DocmaCharEntity("&sbquo;", "&#8218;", true, "single low-9 quotation mark"),
        new DocmaCharEntity("&bdquo;", "&#8222;", true, "double low-9 quotation mark"),
        // math / logical / symbols
        new DocmaCharEntity("&lt;", "&#60;", true, "less-than sign"),
        new DocmaCharEntity("&gt;", "&#62;", true, "greater-than sign"),
        new DocmaCharEntity("&le;", "&#8804;", true, "less-than or equal to"),
        new DocmaCharEntity("&ge;", "&#8805;", true, "greater-than or equal to"),
        new DocmaCharEntity("&ndash;", "&#8211;", true, "en dash"),
        new DocmaCharEntity("&mdash;", "&#8212;", true, "em dash"),
        new DocmaCharEntity("&macr;", "&#175;", true, "macron"),
        new DocmaCharEntity("&oline;", "&#8254;", true, "overline"),
        new DocmaCharEntity("&curren;", "&#164;", true, "currency sign"),
        new DocmaCharEntity("&brvbar;", "&#166;", true, "broken bar"),
        new DocmaCharEntity("&uml;", "&#168;", true, "diaeresis"),
        new DocmaCharEntity("&iexcl;", "&#161;", true, "inverted exclamation mark"),
        new DocmaCharEntity("&iquest;", "&#191;", true, "turned question mark"),
        new DocmaCharEntity("&circ;", "&#710;", true, "circumflex accent"),
        new DocmaCharEntity("&tilde;", "&#732;", true, "small tilde"),
        new DocmaCharEntity("&deg;", "&#176;", true, "degree sign"),
        new DocmaCharEntity("&minus;", "&#8722;", true, "minus sign"),
        new DocmaCharEntity("&plusmn;", "&#177;", true, "plus-minus sign"),
        new DocmaCharEntity("&divide;", "&#247;", true, "division sign"),
        new DocmaCharEntity("&frasl;", "&#8260;", true, "fraction slash"),
        new DocmaCharEntity("&times;", "&#215;", true, "multiplication sign"),
        new DocmaCharEntity("&sup1;", "&#185;", true, "superscript one"),
        new DocmaCharEntity("&sup2;", "&#178;", true, "superscript two"),
        new DocmaCharEntity("&sup3;", "&#179;", true, "superscript three"),
        new DocmaCharEntity("&frac14;", "&#188;", true, "fraction one quarter"),
        new DocmaCharEntity("&frac12;", "&#189;", true, "fraction one half"),
        new DocmaCharEntity("&frac34;", "&#190;", true, "fraction three quarters"),
        new DocmaCharEntity("&fnof;", "&#402;", true, "function / florin"),
        new DocmaCharEntity("&int;", "&#8747;", true, "integral"),
        new DocmaCharEntity("&sum;", "&#8721;", true, "n-ary sumation"),
        new DocmaCharEntity("&infin;", "&#8734;", true, "infinity"),
        new DocmaCharEntity("&radic;", "&#8730;", true, "square root"),
        new DocmaCharEntity("&sim;", "&#8764;", false, "similar to"),
        new DocmaCharEntity("&cong;", "&#8773;", false, "approximately equal to"),
        new DocmaCharEntity("&asymp;", "&#8776;", true, "almost equal to"),
        new DocmaCharEntity("&ne;", "&#8800;", true, "not equal to"),
        new DocmaCharEntity("&equiv;", "&#8801;", true, "identical to"),
        new DocmaCharEntity("&isin;", "&#8712;", false, "element of"),
        new DocmaCharEntity("&notin;", "&#8713;", false, "not an element of"),
        new DocmaCharEntity("&ni;", "&#8715;", false, "contains as member"),
        new DocmaCharEntity("&prod;", "&#8719;", true, "n-ary product"),
        new DocmaCharEntity("&and;", "&#8743;", false, "logical and"),
        new DocmaCharEntity("&or;", "&#8744;", false, "logical or"),
        new DocmaCharEntity("&not;", "&#172;", true, "not sign"),
        new DocmaCharEntity("&cap;", "&#8745;", true, "intersection"),
        new DocmaCharEntity("&cup;", "&#8746;", false, "union"),
        new DocmaCharEntity("&part;", "&#8706;", true, "partial differential"),
        new DocmaCharEntity("&forall;", "&#8704;", false, "for all"),
        new DocmaCharEntity("&exist;", "&#8707;", false, "there exists"),
        new DocmaCharEntity("&empty;", "&#8709;", false, "diameter"),
        new DocmaCharEntity("&nabla;", "&#8711;", false, "backward difference"),
        new DocmaCharEntity("&lowast;", "&#8727;", false, "asterisk operator"),
        new DocmaCharEntity("&prop;", "&#8733;", false, "proportional to"),
        new DocmaCharEntity("&ang;", "&#8736;", false, "angle"),
        new DocmaCharEntity("&acute;", "&#180;", true, "acute accent"),
        new DocmaCharEntity("&cedil;", "&#184;", true, "cedilla"),
        new DocmaCharEntity("&ordf;", "&#170;", true, "feminine ordinal indicator"),
        new DocmaCharEntity("&ordm;", "&#186;", true, "masculine ordinal indicator"),
        new DocmaCharEntity("&dagger;", "&#8224;", true, "dagger"),
        new DocmaCharEntity("&Dagger;", "&#8225;", true, "double dagger"),
        // alphabetical special chars
        new DocmaCharEntity("&Agrave;", "&#192;", true, "A - grave"),
        new DocmaCharEntity("&Aacute;", "&#193;", true, "A - acute"),
        new DocmaCharEntity("&Acirc;", "&#194;", true, "A - circumflex"),
        new DocmaCharEntity("&Atilde;", "&#195;", true, "A - tilde"),
        new DocmaCharEntity("&Auml;", "&#196;", true, "A - diaeresis"),
        new DocmaCharEntity("&Aring;", "&#197;", true, "A - ring above"),
        new DocmaCharEntity("&AElig;", "&#198;", true, "ligature AE"),
        new DocmaCharEntity("&Ccedil;", "&#199;", true, "C - cedilla"),
        new DocmaCharEntity("&Egrave;", "&#200;", true, "E - grave"),
        new DocmaCharEntity("&Eacute;", "&#201;", true, "E - acute"),
        new DocmaCharEntity("&Ecirc;", "&#202;", true, "E - circumflex"),
        new DocmaCharEntity("&Euml;", "&#203;", true, "E - diaeresis"),
        new DocmaCharEntity("&Igrave;", "&#204;", true, "I - grave"),
        new DocmaCharEntity("&Iacute;", "&#205;", true, "I - acute"),
        new DocmaCharEntity("&Icirc;", "&#206;", true, "I - circumflex"),
        new DocmaCharEntity("&Iuml;", "&#207;", true, "I - diaeresis"),
        new DocmaCharEntity("&ETH;", "&#208;", true, "ETH"),
        new DocmaCharEntity("&Ntilde;", "&#209;", true, "N - tilde"),
        new DocmaCharEntity("&Ograve;", "&#210;", true, "O - grave"),
        new DocmaCharEntity("&Oacute;", "&#211;", true, "O - acute"),
        new DocmaCharEntity("&Ocirc;", "&#212;", true, "O - circumflex"),
        new DocmaCharEntity("&Otilde;", "&#213;", true, "O - tilde"),
        new DocmaCharEntity("&Ouml;", "&#214;", true, "O - diaeresis"),
        new DocmaCharEntity("&Oslash;", "&#216;", true, "O - slash"),
        new DocmaCharEntity("&OElig;", "&#338;", true, "ligature OE"),
        new DocmaCharEntity("&Scaron;", "&#352;", true, "S - caron"),
        new DocmaCharEntity("&Ugrave;", "&#217;", true, "U - grave"),
        new DocmaCharEntity("&Uacute;", "&#218;", true, "U - acute"),
        new DocmaCharEntity("&Ucirc;", "&#219;", true, "U - circumflex"),
        new DocmaCharEntity("&Uuml;", "&#220;", true, "U - diaeresis"),
        new DocmaCharEntity("&Yacute;", "&#221;", true, "Y - acute"),
        new DocmaCharEntity("&Yuml;", "&#376;", true, "Y - diaeresis"),
        new DocmaCharEntity("&THORN;", "&#222;", true, "THORN"),
        new DocmaCharEntity("&agrave;", "&#224;", true, "a - grave"),
        new DocmaCharEntity("&aacute;", "&#225;", true, "a - acute"),
        new DocmaCharEntity("&acirc;", "&#226;", true, "a - circumflex"),
        new DocmaCharEntity("&atilde;", "&#227;", true, "a - tilde"),
        new DocmaCharEntity("&auml;", "&#228;", true, "a - diaeresis"),
        new DocmaCharEntity("&aring;", "&#229;", true, "a - ring above"),
        new DocmaCharEntity("&aelig;", "&#230;", true, "ligature ae"),
        new DocmaCharEntity("&ccedil;", "&#231;", true, "c - cedilla"),
        new DocmaCharEntity("&egrave;", "&#232;", true, "e - grave"),
        new DocmaCharEntity("&eacute;", "&#233;", true, "e - acute"),
        new DocmaCharEntity("&ecirc;", "&#234;", true, "e - circumflex"),
        new DocmaCharEntity("&euml;", "&#235;", true, "e - diaeresis"),
        new DocmaCharEntity("&igrave;", "&#236;", true, "i - grave"),
        new DocmaCharEntity("&iacute;", "&#237;", true, "i - acute"),
        new DocmaCharEntity("&icirc;", "&#238;", true, "i - circumflex"),
        new DocmaCharEntity("&iuml;", "&#239;", true, "i - diaeresis"),
        new DocmaCharEntity("&eth;", "&#240;", true, "eth"),
        new DocmaCharEntity("&ntilde;", "&#241;", true, "n - tilde"),
        new DocmaCharEntity("&ograve;", "&#242;", true, "o - grave"),
        new DocmaCharEntity("&oacute;", "&#243;", true, "o - acute"),
        new DocmaCharEntity("&ocirc;", "&#244;", true, "o - circumflex"),
        new DocmaCharEntity("&otilde;", "&#245;", true, "o - tilde"),
        new DocmaCharEntity("&ouml;", "&#246;", true, "o - diaeresis"),
        new DocmaCharEntity("&oslash;", "&#248;", true, "o slash"),
        new DocmaCharEntity("&oelig;", "&#339;", true, "ligature oe"),
        new DocmaCharEntity("&scaron;", "&#353;", true, "s - caron"),
        new DocmaCharEntity("&ugrave;", "&#249;", true, "u - grave"),
        new DocmaCharEntity("&uacute;", "&#250;", true, "u - acute"),
        new DocmaCharEntity("&ucirc;", "&#251;", true, "u - circumflex"),
        new DocmaCharEntity("&uuml;", "&#252;", true, "u - diaeresis"),
        new DocmaCharEntity("&yacute;", "&#253;", true, "y - acute"),
        new DocmaCharEntity("&thorn;", "&#254;", true, "thorn"),
        new DocmaCharEntity("&yuml;", "&#255;", true, "y - diaeresis"),
        new DocmaCharEntity("&Alpha;", "&#913;", true, "Alpha"),
        new DocmaCharEntity("&Beta;", "&#914;", true, "Beta"),
        new DocmaCharEntity("&Gamma;", "&#915;", true, "Gamma"),
        new DocmaCharEntity("&Delta;", "&#916;", true, "Delta"),
        new DocmaCharEntity("&Epsilon;", "&#917;", true, "Epsilon"),
        new DocmaCharEntity("&Zeta;", "&#918;", true, "Zeta"),
        new DocmaCharEntity("&Eta;", "&#919;", true, "Eta"),
        new DocmaCharEntity("&Theta;", "&#920;", true, "Theta"),
        new DocmaCharEntity("&Iota;", "&#921;", true, "Iota"),
        new DocmaCharEntity("&Kappa;", "&#922;", true, "Kappa"),
        new DocmaCharEntity("&Lambda;", "&#923;", true, "Lambda"),
        new DocmaCharEntity("&Mu;", "&#924;", true, "Mu"),
        new DocmaCharEntity("&Nu;", "&#925;", true, "Nu"),
        new DocmaCharEntity("&Xi;", "&#926;", true, "Xi"),
        new DocmaCharEntity("&Omicron;", "&#927;", true, "Omicron"),
        new DocmaCharEntity("&Pi;", "&#928;", true, "Pi"),
        new DocmaCharEntity("&Rho;", "&#929;", true, "Rho"),
        new DocmaCharEntity("&Sigma;", "&#931;", true, "Sigma"),
        new DocmaCharEntity("&Tau;", "&#932;", true, "Tau"),
        new DocmaCharEntity("&Upsilon;", "&#933;", true, "Upsilon"),
        new DocmaCharEntity("&Phi;", "&#934;", true, "Phi"),
        new DocmaCharEntity("&Chi;", "&#935;", true, "Chi"),
        new DocmaCharEntity("&Psi;", "&#936;", true, "Psi"),
        new DocmaCharEntity("&Omega;", "&#937;", true, "Omega"),
        new DocmaCharEntity("&alpha;", "&#945;", true, "alpha"),
        new DocmaCharEntity("&beta;", "&#946;", true, "beta"),
        new DocmaCharEntity("&gamma;", "&#947;", true, "gamma"),
        new DocmaCharEntity("&delta;", "&#948;", true, "delta"),
        new DocmaCharEntity("&epsilon;", "&#949;", true, "epsilon"),
        new DocmaCharEntity("&zeta;", "&#950;", true, "zeta"),
        new DocmaCharEntity("&eta;", "&#951;", true, "eta"),
        new DocmaCharEntity("&theta;", "&#952;", true, "theta"),
        new DocmaCharEntity("&iota;", "&#953;", true, "iota"),
        new DocmaCharEntity("&kappa;", "&#954;", true, "kappa"),
        new DocmaCharEntity("&lambda;", "&#955;", true, "lambda"),
        new DocmaCharEntity("&mu;", "&#956;", true, "mu"),
        new DocmaCharEntity("&nu;", "&#957;", true, "nu"),
        new DocmaCharEntity("&xi;", "&#958;", true, "xi"),
        new DocmaCharEntity("&omicron;", "&#959;", true, "omicron"),
        new DocmaCharEntity("&pi;", "&#960;", true, "pi"),
        new DocmaCharEntity("&rho;", "&#961;", true, "rho"),
        new DocmaCharEntity("&sigmaf;", "&#962;", true, "final sigma"),
        new DocmaCharEntity("&sigma;", "&#963;", true, "sigma"),
        new DocmaCharEntity("&tau;", "&#964;", true, "tau"),
        new DocmaCharEntity("&upsilon;", "&#965;", true, "upsilon"),
        new DocmaCharEntity("&phi;", "&#966;", true, "phi"),
        new DocmaCharEntity("&chi;", "&#967;", true, "chi"),
        new DocmaCharEntity("&psi;", "&#968;", true, "psi"),
        new DocmaCharEntity("&omega;", "&#969;", true, "omega"),
        // more symbols
        new DocmaCharEntity("&alefsym;", "&#8501;", false, "alef symbol"),
        new DocmaCharEntity("&piv;", "&#982;", false, "pi symbol"),
        new DocmaCharEntity("&real;", "&#8476;", false, "real part symbol"),
        new DocmaCharEntity("&thetasym;", "&#977;", false, "theta symbol"),
        new DocmaCharEntity("&upsih;", "&#978;", false, "upsilon - hook symbol"),
        new DocmaCharEntity("&weierp;", "&#8472;", false, "Weierstrass p"),
        new DocmaCharEntity("&image;", "&#8465;", false, "imaginary part"),
        // arrows
        new DocmaCharEntity("&larr;", "&#8592;", true, "leftwards arrow"),
        new DocmaCharEntity("&uarr;", "&#8593;", true, "upwards arrow"),
        new DocmaCharEntity("&rarr;", "&#8594;", true, "rightwards arrow"),
        new DocmaCharEntity("&darr;", "&#8595;", true, "downwards arrow"),
        new DocmaCharEntity("&harr;", "&#8596;", true, "left right arrow"),
        new DocmaCharEntity("&crarr;", "&#8629;", false, "carriage return"),
        new DocmaCharEntity("&lArr;", "&#8656;", false, "leftwards double arrow"),
        new DocmaCharEntity("&uArr;", "&#8657;", false, "upwards double arrow"),
        new DocmaCharEntity("&rArr;", "&#8658;", false, "rightwards double arrow"),
        new DocmaCharEntity("&dArr;", "&#8659;", false, "downwards double arrow"),
        new DocmaCharEntity("&hArr;", "&#8660;", false, "left right double arrow"),
        new DocmaCharEntity("&there4;", "&#8756;", false, "therefore"),
        new DocmaCharEntity("&sub;", "&#8834;", false, "subset of"),
        new DocmaCharEntity("&sup;", "&#8835;", false, "superset of"),
        new DocmaCharEntity("&nsub;", "&#8836;", false, "not a subset of"),
        new DocmaCharEntity("&sube;", "&#8838;", false, "subset of or equal to"),
        new DocmaCharEntity("&supe;", "&#8839;", false, "superset of or equal to"),
        new DocmaCharEntity("&oplus;", "&#8853;", false, "circled plus"),
        new DocmaCharEntity("&otimes;", "&#8855;", false, "circled times"),
        new DocmaCharEntity("&perp;", "&#8869;", false, "perpendicular"),
        new DocmaCharEntity("&sdot;", "&#8901;", false, "dot operator"),
        new DocmaCharEntity("&lceil;", "&#8968;", false, "left ceiling"),
        new DocmaCharEntity("&rceil;", "&#8969;", false, "right ceiling"),
        new DocmaCharEntity("&lfloor;", "&#8970;", false, "left floor"),
        new DocmaCharEntity("&rfloor;", "&#8971;", false, "right floor"),
        new DocmaCharEntity("&lang;", "&#9001;", false, "left-pointing angle bracket"),
        new DocmaCharEntity("&rang;", "&#9002;", false, "right-pointing angle bracket"),
        new DocmaCharEntity("&loz;", "&#9674;", true, "lozenge"),
        new DocmaCharEntity("&spades;", "&#9824;", true, "black spade suit"),
        new DocmaCharEntity("&clubs;", "&#9827;", true, "black club suit"),
        new DocmaCharEntity("&hearts;", "&#9829;", true, "black heart suit"),
        new DocmaCharEntity("&diams;", "&#9830;", true, "black diamond suit"),
        new DocmaCharEntity("&ensp;", "&#8194;", false, "en space"),
        new DocmaCharEntity("&emsp;", "&#8195;", false, "em space"),
        new DocmaCharEntity("&thinsp;", "&#8201;", false, "thin space"),
        new DocmaCharEntity("&zwnj;", "&#8204;", false, "zero width non-joiner"),
        new DocmaCharEntity("&zwj;", "&#8205;", false, "zero width joiner"),
        new DocmaCharEntity("&lrm;", "&#8206;", false, "left-to-right mark"),
        new DocmaCharEntity("&rlm;", "&#8207;", false, "right-to-left mark"),
        new DocmaCharEntity("&shy;", "&#173;", false, "soft hyphen"),
    };
    
    private String symbolic = "";
    private String numeric = "";
    private boolean selectable = true;
    private String description = "";

    public DocmaCharEntity()
    {
    }
    
    public DocmaCharEntity(String sym, String num, boolean sel, String desc)
    {
        this.symbolic = sym;
        this.numeric = num;
        this.selectable = sel;
        this.description = desc;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNumeric() {
        return numeric;
    }

    public int getNumericValue() {
        String num_str = getNumeric();
        int start_pos = num_str.startsWith("&#") ? 2 : 0;
        int end_pos = num_str.endsWith(";") ? numeric.length() - 1 : numeric.length();
        try {
            num_str = num_str.substring(start_pos, end_pos);
            return Integer.parseInt(num_str);
        } catch (Exception ex) {
            return -1;
        }
    }

    public void setNumeric(String numeric) {
        this.numeric = numeric;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public String getSymbolic() {
        return symbolic;
    }

    public void setSymbolic(String symbolic) {
        this.symbolic = symbolic;
    }

    public String toString()
    {
        return "['" + getSymbolic() + "','" + getNumeric() +
               "'," + isSelectable() + ",'" + getDescription().replace('\'', ' ') + "']";
    }

    public static String saveToString(DocmaCharEntity[] entities)
    {
        if (entities.length == 0) return "";
        StringBuilder sb = new StringBuilder(entities.length * 100);
        sb.append(entities[0].toString());
        for (int i=1; i < entities.length; i++) {
            sb.append(",").append(entities[i].toString());
        }
        return sb.toString();
    }

    public static DocmaCharEntity[] loadFromString(String input) throws DocException
    {
        ArrayList list = new ArrayList(500);
        int searchpos = 0;
        while (searchpos < input.length()) {
            searchpos = input.indexOf("['", searchpos);
            if (searchpos < 0) break;
            int pos1 = input.indexOf("','", searchpos);
            if (pos1 < 0) throw new DocException("Could not parse character entity string.");
            String symbolic = input.substring(searchpos + 2, pos1);
            pos1 += 3;
            int pos2 = input.indexOf("',", pos1);
            if (pos2 < 0) throw new DocException("Could not parse character entity string.");
            String numeric = input.substring(pos1, pos2);
            pos2 += 2;
            int pos3 = input.indexOf(",'", pos2);
            if (pos3 < 0) throw new DocException("Could not parse character entity string.");
            String sel = input.substring(pos2, pos3).trim().toLowerCase();
            boolean is_sel = sel.equals("true");
            if (! (is_sel || sel.equals("false"))) {
                throw new DocException("Could not parse character entity string.");
            }
            pos3 += 2;
            int pos4 = input.indexOf("']", pos3);
            if (pos4 < 0) throw new DocException("Could not parse character entity string.");
            String desc = input.substring(pos3, pos4);
            DocmaCharEntity ce = new DocmaCharEntity(symbolic, numeric, is_sel, desc);
            list.add(ce);
            searchpos = pos4 + 2;
        }
        DocmaCharEntity[] arr = new DocmaCharEntity[list.size()];
        return (DocmaCharEntity[]) list.toArray(arr);
    }

}
