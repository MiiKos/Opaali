/*
 * Opaali (Telia Operator Service Platform) sample code
 * 
 * Copyright(C) 2017 Telia Company
 * 
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 * 
 * Author: jlasanen
 * 
 */


package OpaaliAPI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Template {

    public Template(String[] template) {
        this.template = template;
    }

    /*
     * replace ${variables} in template with values provided in vars
     * notice that variable values may be variables themselves 
     * so you may need to expand templates recursively
     */
    public Template expand(HashMap <String,String> vars) {
        String[] tmpl = tmplExpand(template, vars);
        //System.err.println(Arrays.toString(tmpl));
        return new Template(tmpl);
    }
    
    /*
     * return template as a single string
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return Arrays.toString(template);
    }

    /*
     * return template as a single string
     * @see java.lang.Object#toString()
     */
    public String[] toStrings() {
        return template;
    }

    
    public String compact() {
        return Arrays.toString(template).replaceAll("\\s", "");
    }
    
    // = end of public part =============================================================

    private String[] template = null;
    
    /*
     * perform actual template expansion
     */
    private static String[] tmplExpand(String[] tmpl, HashMap<String, String> vars) {
        int a = 0;
        int b = 0;
        String[] target = new String[tmpl.length];
        String pattern="\\$\\{[_a-zA-Z0-9-]+\\}";
        Pattern p = Pattern.compile(pattern);
        for (int line = 0; line < tmpl.length; line++) {
            StringBuilder targetStr = new StringBuilder(); 
            a = 0;
            target[line] = "";
            Matcher m = p.matcher(tmpl[line]);
            while (m.find()) {
                b = m.start();
                targetStr.append(tmpl[line].substring(a, b));
                targetStr.append(vars.get(tmpl[line].substring(m.start()+2, m.end()-1)));
                a = m.end();
            }
            targetStr.append(tmpl[line].substring(a));
            target[line] = targetStr.toString();
        }
        return target;
        
    }
    
}
