package smsServer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrMask {

    private StrMask() {

    }

    // variable name for applying the mask to (disabled if null)
    protected String maskVar = null;
    // start position (within value) for applying the mask (disabled if 0, applied right to left if < 0)
    protected int maskStart = 0;
    // length of masked area (disabled if <= 0)
    protected int maskLen = 0;
    // pattern of a single mask config string
    private static String pattern = "\\((\\w+),(-?\\d+)(,\\d*)?\\)";
    private static Pattern p = Pattern.compile(pattern);
    // pattern of multiple mask configs
    private static String patterns = "(\\([^\\)]+\\))";
    private static Pattern ps = Pattern.compile(patterns);


    /*
     * parse config string for a mask
     */
    public static StrMask parseConfig(String s) {
        if (s == null) {
            return null;
        }
        StrMask mask = new StrMask();
        Matcher m = p.matcher(s);
        int i = 0;
        if (m.matches()) {
            try {
                int cnt = m.groupCount();
                if (cnt > i++) {
                    mask.maskVar = m.group(1);
                }
                if (cnt > i++) {
                    mask.maskStart = Integer.parseInt(m.group(2));
                }
                if (cnt > i++) {
                    String str = m.group(3);
                    if (str == null) {
                        //mask.indexEnd = 0;
                        mask.maskLen = 0;
                    }
                    else {
                        int ii = m.group(3).indexOf(',')+1;
                        str = str.substring(ii);
                        mask.maskLen = Integer.parseInt(str);
                    }
                }
            } catch (NumberFormatException ex) {
                if (i == 3) {
                    // this is ok, the third parameter is optional
                    mask.maskLen = 0;
                }
                else {
                    // failure is an option?
                    return null;
                }
            }
        }
        return mask;
    }


    /*
     * parse config line for a list of masks into an array of masks
     */
    public static StrMask[] parseMaskConfig(String s) {
        ArrayList<StrMask> masks = new ArrayList<StrMask>();
        if (s != null) {
            Matcher m = ps.matcher(s);
            while (m.find()) {
                String msk = m.group();
                //System.err.println("pattern:"+msk);
                StrMask cnf = parseConfig(msk);
                if (cnf != null) {
                    masks.add(cnf);
                }
            }
        }
        return masks.toArray(new StrMask[masks.size()]);
    }


    /*
     * apply mask once to the given string
     */
    private String applyMaskOnce(String s) {

        if (maskVar == null || maskStart == 0) {
            return s;
        }
        String key = maskVar+"=";
        int i = s.indexOf(key);
        if (i < 0) {
            // key not found
            return s;
        }
        i += key.length();
        int j = s.length();
        if (maskStart < 0) {
            // apply from right to left
            j += (maskStart+1);
            if (j < i) {
                j = i;
            }
            if (maskLen > 0) {
                i = (j - maskLen < i ? i : j - maskLen);
            }
        }
        else if (maskStart > 0){
            i += (maskStart-1);
            if (i > j) {
                i = j;
            }
            if (maskLen > 0) {
                j = (i + maskLen < j ? i + maskLen : j);
            }
        }

        StringBuilder t = new StringBuilder(s.substring(0, i));
        while (i++ < j) {
            t.append('*');
        }
        return t.append(s.substring(j)).toString();
    }


    /*
     * apply mask repeatedly to the given string
     */
    public String applyMaskRepeatedly(String s) {
        String key = maskVar+"=";

        // fail early
        if (!s.contains(key)) {
            return s;
        }

        StringBuilder t = new StringBuilder(s.length());

        while (s.contains(key)){
            int i = s.indexOf(key);    // start of parameter
            int j = 0;                 // end of parameter
            if (i > -1) {
                j = s.indexOf('&', i);
                if (j <= 0) {
                    j = s.length();
                }
                /* i..j is the area to be processed */

                t.append(s.substring(0, i)).append(applyMaskOnce(s.substring(i, j)));
                s = s.substring(j);
            }
        }
        return t.append(s).toString();
    }


    /*
     * apply an array of masks to the given string repeatedly
     */
    public static String applyMasks(String s, StrMask[] masks) {
        if (masks != null) {
            for (StrMask mask : masks) {
                s = mask.applyMaskRepeatedly(s);
            }
        }
        return s;
    }

}
