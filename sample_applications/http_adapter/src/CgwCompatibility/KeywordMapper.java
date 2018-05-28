/*
 * Opaali (Telia Operator Service Platform) sample code
 *
 * Copyright(C) 2018 Telia Company
 *
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 *
 * Author: jlasanen
 *
 */

package CgwCompatibility;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smsServer.Mapping;

/*
 * takes a CgwMessage, maps its keyword to a template Url
 * and expands the macros based on the message content
 */
public class KeywordMapper {

    // initialize with default URL only
    public KeywordMapper(String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    // initialize with keyword->templateUrl mappings
    public KeywordMapper(HashMap<String,String> keyMap) {
        this.keyMap = keyMap;
    }

    // initialize with keyword->templateUrl mappings and a default URL
    public KeywordMapper(HashMap<String,String> keyMap, String defaultUrl) {
        this.keyMap = keyMap;
        this.defaultUrl = defaultUrl;
    }

    // initialize with keyword->templateUrl mappings and a default URL
    public KeywordMapper(Mapping mapping, String defaultUrl) {
        this.mapping = mapping;
        this.defaultUrl = defaultUrl;
    }

    /*
     * return URL template based on (serviceNumber, keyword) pair
     */
    public String mapKeyword(String serviceNumber, String keyword) {
        String urlTemplate = null;
        if (mapping != null) {
            urlTemplate = mapping.mapToUrl(serviceNumber, keyword);
            if (urlTemplate != null) {
                return urlTemplate;
            }
            else {
                return defaultUrl;
            }
        }
        else if (keyMap != null) {
            return mapKeyword(keyword);
        }
        else if (defaultUrl != null) {
            return defaultUrl;
        }
        return null;
    }

    /*
     * return URL template based on keyword only
     */
    public String mapKeyword(String keyword) {
        if (keyMap != null) {
            return keyMap.getOrDefault(keyword, defaultUrl);
        }
        else if (mapping != null) {
            return mapping.mapToUrl(null, keyword);
        }
        return defaultUrl;
    }

    /*
     * expand macros in URL template with given values
     */
    public static String tmplExpand(String templateUrl, CgwMessage message) {
        return fillTemplate(templateUrl, message);
    }


    // = end of public part ===============================================

    // mappings
    private String defaultUrl = null;
    private HashMap<String,String> keyMap = null;
    private Mapping mapping = null;


    // parameter names
    private static final String UDH_PARAM = "UDH";
    private static final String MSISDN_PARAM = "MSISDN";
    private static final String M_PARAM = "M";
    private static final String FROM_PARAM = "FROM";
    private static final String MSG_PARAM = "MSG";
    private static final String RECIPIENT_PARAM = "RECIPIENT";
    private static final String R_PARAM = "R";
    private static final String TO_PARAM = "TO";

    /*
     * supported template variables
     */
    private static final int PATTERN_COUNT = 4;
    //static final String PATTERN_VAR_PREFIX = "\\$\\((|\\[";
    private static final int PATTERN_NTH_WORD_INDEX = 0;
    private static final String PATTERN_NTH_WORD = "\\G\\$\\((\\d+)\\)";    // $(number)
    private static final int PATTERN_ALL_WORDS_INDEX = 1;
    private static final String PATTERN_ALL_WORDS = "\\G\\$\\(\\*\\)";      // $(*)
    private static final int PATTERN_VAR_INDEX = 2;
    private static final String PATTERN_VAR = "\\G\\$\\(([^)]+)\\)";        // $(variable)
    private static final int PATTERN_REST_OF_WORDS_INDEX = 3;
    private static final String PATTERN_REST_OF_WORDS = "\\G\\$\\[\\*\\]";  // $[*]

    private static final Pattern[] p;


    private KeywordMapper() {

    }


    static {
        // initialize patterns
        p = new Pattern[PATTERN_COUNT];
        p[PATTERN_NTH_WORD_INDEX] = Pattern.compile(PATTERN_NTH_WORD);
        p[PATTERN_ALL_WORDS_INDEX] = Pattern.compile(PATTERN_ALL_WORDS);
        p[PATTERN_REST_OF_WORDS_INDEX] = Pattern.compile(PATTERN_REST_OF_WORDS);
        p[PATTERN_VAR_INDEX] = Pattern.compile(PATTERN_VAR);
    }

    /*
     * substitute variables with their values in the given template string and
     * return as String
     */
    private static String fillTemplate(String template, CgwMessage message) {
        String target = "";
        int pos = 0;
        int word = 1;

        if (template == null || message == null) {
            // fail fast
            return null;
        }

        // initialize matchers
        Matcher m[] = new Matcher[PATTERN_COUNT];
        for (int i = 0; i < p.length; i++) {
            m[i] = p[i].matcher(template);
        }


        // go through the template string and substitute known variables with values
        char c = '\0';    // current char
        char lk = '\0';   // lookahead char
        if (pos < template.length()) {
            c = template.charAt(pos);
        }
        while (pos + 1 < template.length()) {
            lk = template.charAt(pos + 1);
            if (c == '$' && (lk == '(' || lk == '[')) {
                // potential start of variable
                boolean found = false;

                if (lk == '[' && pos + 2 < template.length() && template.charAt(pos+2) == ']') {
                    // treat $[] as a special case, same as $(number) where number increases
                    target += message.getWord(++word);
                    // skip to end of variable
                    pos += 3;
                    if (pos < template.length()) {
                        c = template.charAt(pos);
                    }
                    found = true;
                }
                else if (lk == '[' && pos + 2 < template.length() && template.toUpperCase().startsWith("DEF:", pos+2)) {
                    int end = template.indexOf(']', pos+2);
                    String text = (end > pos+2+4 ? template.substring(pos+6, end) : "");
                    // treat $[DEF:text] as a special case, same as $(number) where number increases 
                    // (and insert text if macro expands to empty)
                    String w = message.getWord(++word);
                    target += (w.length() > 0 ? w : text);
                    // skip to end of variable
                    pos = end+1;
                    if (pos < template.length()) {
                        c = template.charAt(pos);
                    }
                    found = true;
                }
                else {

                    /*
                     * $\(M\)|$\(MSISDN\)|$\(FROM\) $\(R\)|$\(RECIPIENT\)|$(\TO\)
                     * $(\d+) $(\*) $\(\*(.)\) $\[\] $\[DEF(.+)\] $\[\*\] $\[\*(.)\]
                     * $\(.*\)
                     */
                    for (int i = 0; i < p.length; i++) {
                        if (m[i].find(pos)) {
                            target += template.substring(pos, m[i].start());
                            if (i == PATTERN_NTH_WORD_INDEX) {
                                int d = Integer.parseInt(m[i].group(1));
                                target += message.getWord(d);
                            } else if (i == PATTERN_ALL_WORDS_INDEX) {
                                target += message.getWord(1);
                                for (int j = 2; j <= message.getWordCount(); j++) {
                                    target += ('+' + message.getWord(j));
                                }
                            } else if (i == PATTERN_REST_OF_WORDS_INDEX) {
                            for (int j = word+1; j <= message.getWordCount(); j++) {
                                target += ((j == word+1 ? "" : '+') + message.getWord(j));
                                }
                            } else if (i == PATTERN_VAR_INDEX) {
                                // assume a variable
                                String ds = m[i].group(1);

                                if (M_PARAM.equalsIgnoreCase(ds)
                                    || MSISDN_PARAM.equalsIgnoreCase(ds)
                                    || FROM_PARAM.equalsIgnoreCase(ds)) {
                                    String s = message.getFrom();
                                    if (s.startsWith("+358")) {
                                        // for CGW compatibility
                                        s = "0"+s.substring(4);
                                    }
                                    target += s;
                                } else if (R_PARAM.equalsIgnoreCase(ds)
                                    || RECIPIENT_PARAM.equalsIgnoreCase(ds)
                                    || TO_PARAM.equalsIgnoreCase(ds)) {
                                    String s = message.getTo();
                                    if (s.startsWith("+358")) {
                                        // for CGW compatibility
                                        s = "0"+s.substring(4);
                                    }
                                    target += s;
                                } else if (UDH_PARAM.equalsIgnoreCase(ds)) {
                                    target += message.getUdh();
                                } else if (MSG_PARAM.equalsIgnoreCase(ds)) {
                                    target += message.getMsg();
                                } else {
                                    // ignore unknown param
                                }
                            }
                            // skip to end of variable
                            pos = m[i].end();
                            if (pos < template.length()) {
                                c = template.charAt(pos);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // next char
                        //target += c;
                        c = lk;
                        pos++;
                    }
                }
            }
            else if (c == '$' && lk == '$' ) {
                // treat this as escape
                target += c;
                c = '\0';
                pos++;
            }
            else if (c == '\0') {
                // skip nul
                c = lk;
                pos++;
            }
            else {
                // next char
                target += c;
                c = lk;
                pos++;
            }
        }
        if (pos < template.length()) {
            // copy last char
            c = template.charAt(pos);
            target += c;
        }

        return target;
    }

}
