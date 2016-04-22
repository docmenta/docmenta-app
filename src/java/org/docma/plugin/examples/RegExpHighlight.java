/*
 * RegExpHighlight.java
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

package org.docma.plugin.examples;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.docma.util.*;
import org.docma.plugin.*;

/**
 *
 * @author MP
 */
public class RegExpHighlight implements AutoFormat
{
    private static final String ARG_CFG = "cfg";
    private static final String ARG_DECODE = "decode";
    private static final String ARG_SKIPARGS = "skipargs";

    private static final String STATE_START = "START";
    private static final int DEFAULT_PRIORITY = 1;
    private static final Pattern STATENAME_PATTERN = Pattern.compile("[A-Za-z_][0-9A-Za-z_]+");
    private static final int MAX_SUCCESSIVE_STATE_FORWARDINGS = 16;
    
    private ExportContext exportCtx;
    private Map<String, HighlightConfig> cfgMap;


    public String getShortInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

    public void initialize(ExportContext ctx)
    {
        exportCtx = ctx;
        cfgMap = new HashMap();
    }

    public void finished()
    {
        exportCtx = null;
        cfgMap = null;
    }

    public void transform(TransformationContext ctx) throws Exception
    {
        Writer out = ctx.getWriter();
        ctx.setStyleRecursion(false);

        // Arguments:
        // cfg=<FILE_ALIAS>
        // decode=[true|false],  default: true
        // skipargs=[true|false],  default: true
        Map<String, String> args = new HashMap<String, String>(43);
        for (int i=0; i < ctx.getArgumentCount(); i++) {
            readArg(ctx.getArgument(i, ""), args);
        }

        String file_alias = args.get(ARG_CFG);
        if (file_alias == null) {
            out.write(ctx.getOuterString());  // do nothing; return original content
            logError("Missing Auto-Format paramter '" + ARG_CFG + "'.");
            return;
        }
        String decode_str = args.get(ARG_DECODE);
        boolean decode_entities = (decode_str == null) || decode_str.equalsIgnoreCase("true");
        String skip_str = args.get(ARG_SKIPARGS);
        boolean skip_args = (skip_str == null) || skip_str.equalsIgnoreCase("true");

        // List<RegExpEntry> regexplist = (List<RegExpEntry>) cfgMap.get(file_alias);
        HighlightConfig highlightConf = cfgMap.get(file_alias);
        if (highlightConf == null) {
            highlightConf = readHighlightConfig(file_alias);
            cfgMap.put(file_alias, highlightConf);
        }

        // Write opening tag of original outer element
        String elem_name = ctx.getTagName();
        out.append("<").append(elem_name);
        Map<String, String> attribs = ctx.getTagAttributes();
        Iterator<String> it = attribs.keySet().iterator();
        while (it.hasNext()) {
            String attname = it.next();
            String attval = attribs.get(attname).replace("\"", "&#34;");
            out.append(" ").append(attname).append("=\"").append(attval).append("\"");
        }
        out.append(">");

        // Apply syntax highlighting to the inner content
        String input = ctx.getInnerString();
        int copy_pos = 0;
        if (skip_args) {
            copy_pos = skipArgs(input);
            if (copy_pos > 0) {  // args exist
                out.append(input, 0, copy_pos);
            }
        }
        StringBuilder buf = new StringBuilder(1024);
        List<Integer> decode_positions = new ArrayList<Integer>();
        List<String> replaced = new ArrayList<String>();
        Stack<String> enter_stack = new Stack<String>();
        String state_name = highlightConf.getInitialStateName();
        XMLParser parser = new XMLParser(input, copy_pos);
        int eventType;
        do {
            // Read input into buf until first non-<br/> element.
            // <br/> elements are decoded to \n characters to allow detection
            // of newline in regular expressions using \n.
            // Apply regular expression highlighting to buf and write result to out.
            // Then continue reading input until next non-<br/> element and so on.
            eventType = parser.next();
            if ((eventType == XMLParser.START_ELEMENT) ||
                (eventType == XMLParser.END_ELEMENT)) {
                int tag_start = parser.getStartOffset();
                int tag_end = parser.getEndOffset();
                copyChars(input, copy_pos, tag_start, buf, decode_entities, decode_positions, replaced);
                String tag_name = parser.getElementName();
                if (tag_name.equalsIgnoreCase("br")) {
                    // Decode <br/> to \n
                    decode_positions.add(buf.length());
                    replaced.add("<br />");
                    buf.append("\n");
                } else {
                    // Start of an non-<br/> element.
                    // Apply highlighting to buf and write to out
                    if (buf.length() > 0) {
                        state_name = highlight(buf, out, state_name, highlightConf, enter_stack, decode_positions, replaced);
                        // clear buf and decoding-lists for next highlighting
                        buf.setLength(0);
                        decode_positions.clear();
                        replaced.clear();
                    }
                    // write non-<br/> tag
                    out.append(input, tag_start, tag_end);
                }
                copy_pos = tag_end;
            }
        } while (eventType != XMLParser.FINISHED);

        // Highlight remaining characters
        if (copy_pos < input.length()) {
            copyChars(input, copy_pos, input.length(), buf, decode_entities, decode_positions, replaced);
        }
        if (buf.length() > 0) {
            highlight(buf, out, state_name, highlightConf, enter_stack, decode_positions, replaced);
        }

        // Write closing tag of original outer element
        out.append("</").append(elem_name).append(">");
    }

    private HighlightConfig readHighlightConfig(String file_alias) throws IOException
    {
        return readHighlightConfig(file_alias, new HashMap<String, String>(), new ArrayList<String>());
    }
    
    private HighlightConfig readHighlightConfig(String file_alias, 
                                                Map<String, String> definesMap,
                                                List<String> importedConfigs) throws IOException
    {
        importedConfigs.add(file_alias);  // add alias to the list to avoid cricular import of the same configuration
        
        HighlightConfig highlightConf = new HighlightConfig();
        // Read content of node with alias file_alias
        String cfg_str = exportCtx.getContentStringByAlias(file_alias);
        if (cfg_str == null) {
            // out.write(ctx.getOuterString());  // do nothing; return original content
            logError("Could not find node with alias '" + file_alias + "'.");
            return highlightConf;  // return empty configuration
        }

        // Parse configuration. Create one RegExpEntry instance for each line:
        final String IMPORT_PREFIX = "@import(";
        final String ENTER_PREFIX = "enter(";
        final String RETURN_PREFIX = "return(";
        BufferedReader in = new BufferedReader(new StringReader(cfg_str));
        String currentStateName = STATE_START;
        int currentPriority = DEFAULT_PRIORITY;
        String firstDefinedState = null;
        RegExpEntry currentRule = null;
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) continue;    // ignore empty lines
            if (line.startsWith("#")) continue;  // ignore comment lines
            if (line.startsWith("%")) {
                currentRule = null;  // clear
                if (definesMap == null) {
                    definesMap = new HashMap<String, String>();
                }
                readDefinedConstant(line, definesMap);
            } else if (line.startsWith(IMPORT_PREFIX)) {
                currentRule = null;  // clear
                int p = line.indexOf(')');
                if (p > 0) {
                    String imp_alias = line.substring(IMPORT_PREFIX.length(), p).trim();
                    if ((! imp_alias.equals("")) && (! importedConfigs.contains(imp_alias))) {
                        HighlightConfig hc = readHighlightConfig(imp_alias, definesMap, importedConfigs);
                        for (State st : hc.getStates()) {
                            highlightConf.addState(st);
                        }
                    }
                } else {
                    logError("Invalid import command: " + line);
                }
            } else if (line.startsWith("::")) {
                currentRule = null;  // clear
                String state_name = line.substring(2).trim();
                String forward_name = null;
                int priority = 1;
                int p = state_name.indexOf("->");
                if (p > 0) {
                    forward_name = state_name.substring(p + 2).trim();
                    if (! validStateName(forward_name)) {
                        logError("Invalid state name in configuration with alias '" + file_alias + 
                                 "': " + forward_name);
                        forward_name = null;
                    }
                    state_name = state_name.substring(0, p).trim();
                }
                int p1 = state_name.indexOf('(');
                int p2 = state_name.indexOf(')');
                if ((p1 > 0) && (p2 > p1)) {  // state parameters are supplied in round brackets
                    String state_params = state_name.substring(p1 + 1, p2).trim().toLowerCase();
                    state_name = state_name.substring(0, p1).trim();
                    Map<String, String> params = parseStateParams(state_params);
                    String prio_str = params.get("priority");
                    if (prio_str != null) {
                        try {
                            priority = Integer.parseInt(prio_str);
                        } catch (Exception ex) {
                            logError("Invalid priority in configuration with alias '" + file_alias + 
                                     "': " + prio_str);
                        }
                    }
                }
                if (! validStateName(state_name)) {
                    logError("Invalid state name in configuration with alias '" + file_alias + 
                             "': " + state_name);
                    return highlightConf;  // severe error; abort parsing
                }
                
                currentStateName = state_name;
                currentPriority = priority;
                if (firstDefinedState == null) {
                    firstDefinedState = state_name;
                }
                State state = highlightConf.getState(state_name);
                if (state != null) {  
                    // Definitions already exist for this state.
                    // Handle cases where existing definitions have same, lower 
                    // and higher priority.
                    if (priority == state.priority) {
                        if (forward_name != null) {
                            if (state.getRules().size() == 0) {
                                state.forwardName = forward_name;
                            } else {
                                logWarning("Defining state forwarding and regular expression rules with same priority: " + 
                                           "Ignoring state forwarding from '" + state_name + "' to '" + forward_name + "'.");
                            }
                        }
                    }
                    // If priority > state.priority then delete the existing rules
                    // (because of lower priority compared to the rules defined in this section). 
                    if (priority > state.priority) {
                        state.getRules().clear();
                        state.priority = priority;
                        state.forwardName = forward_name;
                    }
                    // If priority < state.priority then nothing needs to be done
                    // here, i.e. the existing rules are kept and rules defined
                    // in this section will be ignored (see below).
                } else {
                    // This is the first definition for this state.
                    state = new State(state_name);
                    state.priority = priority;
                    state.forwardName = forward_name;
                    highlightConf.addState(state);
                }
            } else if (line.startsWith("->")) {
                String next_state = line.substring(2).trim();
                if (next_state.startsWith(ENTER_PREFIX)) {
                    int endpos = next_state.lastIndexOf(')');
                    if (endpos > 0) {
                        currentRule.nextIsEnter = true;
                        next_state = next_state.substring(ENTER_PREFIX.length(), endpos).trim();
                        int sep_pos = next_state.indexOf(',');
                        if (sep_pos > 0) {
                            String enter_param = next_state.substring(sep_pos + 1).trim();
                            next_state = next_state.substring(0, sep_pos).trim();
                            int colon_pos = enter_param.indexOf(':');
                            if ((colon_pos > 0) && enter_param.substring(0, colon_pos).trim().equalsIgnoreCase("returnstate")) {
                                String ret_state = enter_param.substring(colon_pos + 1).trim();
                                if (validStateName(ret_state)) {
                                    currentRule.returnFromEnterState = ret_state;
                                } else {
                                    logInvalidStateName(file_alias, ret_state);
                                }
                            }
                        }
                    }
                } else if (next_state.startsWith(RETURN_PREFIX)) {
                    currentRule.nextIsReturn = true;
                    if (! next_state.substring(RETURN_PREFIX.length()).trim().equals(")")) {
                        logError("Invalid return statement in configuration with alias '" + file_alias + 
                                 "': " + next_state);
                    }
                }
                if (currentRule.nextIsReturn) {
                    currentRule.nextState = null;
                } else {
                    if (validStateName(next_state)) {
                        if (currentRule != null) {
                            currentRule.nextState = next_state;
                        } else {
                            logError("Invalid position of operator -> in configuration with alias '" + 
                                     file_alias + "'. Ignoring line: " + line);
                        }
                    } else {
                        logInvalidStateName(file_alias, next_state);
                    }
                }
            } else {
                RegExpEntry rule = readRule(line, in, definesMap, file_alias);
                if (rule == null) {
                    continue;
                }
                State currentState = highlightConf.getState(currentStateName);
                if (currentState == null) {
                    // No state definition exists for this state. This can only
                    // occur for the default state "START", i.e. regular expression
                    // rules are defined without defining a state name using
                    // the :: operator.
                    currentState = new State(currentStateName);
                    highlightConf.addState(currentState);
                    if (firstDefinedState == null) {  // default state "START" is first defined state
                        firstDefinedState = currentStateName;
                    }
                }
                // Ignore rule if it has lower priority than existing rules
                if (currentPriority >= currentState.priority) {
                    currentState.getRules().add(rule);
                }
                // Set the field currentRule. This is required for the -> operator, 
                // which may follow in the next line.
                currentRule = rule;
            }
        }
        
        if (firstDefinedState == null) {  // This should never occur
            highlightConf.setInitialStateName(STATE_START);
            logError("Internal error: Initial state is null.");
        } else {
            highlightConf.setInitialStateName(firstDefinedState);
        }
        
        if (highlightConf.isEmpty()) {
            logWarning("Highlight configuration file with alias '" + file_alias + "' is empty.");
        }
        return highlightConf;
    }
    
    private void logInvalidStateName(String file_alias, String state_name)
    {
        logError("Invalid state name in configuration with alias '" + file_alias + 
                 "': " + state_name);
    }
    
    private void readDefinedConstant(String line, Map<String, String> definesMap)
    {
        int pos_end = line.indexOf('%', 1);
        if (pos_end < 2) {
            logError("Invalid definition: " + line);
            return;
        }
        String def_name = line.substring(0, pos_end + 1);
        int pos = line.indexOf(':', pos_end);
        if (pos < 0) {
            logError("Missing ':' in line: " + line);
            return;
        }
        String def_expr = line.substring(pos + 1).trim();

        // Replace defines within defines
        Iterator<Map.Entry<String, String>> it = definesMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> def = it.next();
            def_expr = def_expr.replace(def.getKey(), def.getValue());
        }

        // Enclose regular expression with non-capturing group brackets (?: ... )
        def_expr = "(?:" + def_expr + ")";
        if (definesMap.containsKey(def_name)) {
            logWarning("Constant " + def_name + " already exists. Replacing value " + 
                       definesMap.get(def_name) + " by " + def_expr);
        }
        definesMap.put(def_name, def_expr);
    }
    
    private RegExpEntry readRule(String line, BufferedReader in, Map<String, String> definesMap, String file_alias)
    throws IOException
    {
        String ids = "";
        int colon_pos;
        // Read lines up to the next colon
        do {
            colon_pos = line.indexOf(':');
            if (colon_pos < 0) {
                ids += " " + line + " ";
                do {  // read next line; ignore comment lines
                    line = in.readLine();
                    if (line == null) {
                        logError("Unexpected end of configuration with alias '" +
                                 file_alias + "' near '" + ids + "'. Character ':' expected.");
                        return null;
                    }
                    line = line.trim();
                } while (line.equals("") || line.startsWith("#"));  // ignore empty and comment lines
                // Now line contains the next non-empty non-comment line.
            }
        } while (colon_pos < 0);
        ids += line.substring(0, colon_pos).trim();
        ids = ids.trim();
        if (ids.length() == 0) {
            logWarning("Missing style ids in configuration with alias '" + file_alias + "' near '" + line + "'.");
            return null;  // ignore line and continue with next line
        }
        String regexp = line.substring(colon_pos + 1).trim();
        if (regexp.length() == 0) {
            logWarning("Missing regular expression in configuration with alias '" + file_alias + "' near '" + line + "'.");
            return null;  // ignore line and continue with next line
        }
        if (definesMap != null) {
            Iterator<Map.Entry<String, String>> it = definesMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> def = it.next();
                regexp = regexp.replace(def.getKey(), def.getValue());
            }
        }

        RegExpEntry entry = new RegExpEntry();
        entry.styleIDs = ids.split("\\s*[,\\s]\\s*");
        entry.pattern = Pattern.compile(regexp, Pattern.DOTALL | Pattern.MULTILINE);
        return entry;
    }
    
    private boolean validStateName(String name)
    {
        return STATENAME_PATTERN.matcher(name).matches();
    }
    
    private Map<String, String> parseStateParams(String params)
    {
        Map<String, String> res = new HashMap<String, String>();
        if (params.equals("")) {
            return res;
        }
        String[] arr = params.split("[, ]");
        for (String par : arr) {
            int col_pos = par.indexOf(':');
            if (col_pos >= 0) {
                String name = par.substring(0, col_pos).trim();
                String val = par.substring(col_pos + 1).trim();
                if ((name.length() > 0) && (val.length() > 0)) {
                    res.put(name, val);
                }
            } else {
                logWarning("Ignoring invalid state parameter in highlight configuration: " + par);
            }
        }
        return res;
    }
    
    private void logError(String msg)
    {
        exportCtx.logError(this.getClass().getName() + ": " + msg);
    }

    private void logWarning(String msg)
    {
        exportCtx.logWarning(this.getClass().getName() + ": " + msg);
    }

    private void readArg(String arg, Map<String, String> argMap)
    {
        if ((arg == null) || (arg.length() == 0)) {
            return;
        }
        int p = arg.indexOf('=');
        if (p < 0) argMap.put(arg, "true");
        else {
            argMap.put(arg.substring(0, p), arg.substring(p + 1));
        }
    }

    private void copyChars(String input,
                           int start,
                           int end,
                           StringBuilder buf,
                           boolean decode_entities,
                           List<Integer> decode_positions,
                           List<String> decode_strings)
    {
        if (! decode_entities) {
            buf.append(input, start, end);
            return;
        }

        // Decode character entities
        int pos = start;
        while (pos < end) {
            char ch = input.charAt(pos);
            if (ch == '&') {  // start of a character entity
                int entity_end = input.indexOf(';', pos + 1);
                if ((entity_end > 0) && (entity_end < end)) {
                    ++entity_end;  // position after ;
                    String entity_str = input.substring(pos, entity_end);
                    String decoded = exportCtx.decodeCharEntities(entity_str);
                    if (decoded.length() == 1) {  // decoding was successful
                        decode_positions.add(buf.length());
                        decode_strings.add(entity_str);
                        buf.append(decoded);
                        pos = entity_end;
                        continue;
                    }
                }
            }

            // If no character entity or character entity could not be decoded
            buf.append(ch);
            ++pos;  // continue with next char
        }
    }

    private String highlight(StringBuilder input,
                             Appendable output,
                             String initial_state,
                             HighlightConfig highlight_conf,
                             Stack<String> enter_stack,
                             List<Integer> decode_positions,
                             List<String> decode_strings) throws Exception
    {
        String state_name = initial_state;
        Map<String, State> preparedStates = new HashMap<String, State>();

        // Search next match
        int len = input.length();
        int copy_pos = 0;
        while (copy_pos < len) {
            // Get next non-forwarding state
            State state;
            int forward_cnt = 0;
            while (true) {  // Loop to handle state forwarding
                state = preparedStates.get(state_name);
                if (state == null) {
                    state = highlight_conf.getState(state_name);
                    if (state == null) {  // undefined state; should not occur
                        state = new State(state_name);  // dummy state with no rules (to avoid exception)
                        logError("Undefined highlight state: " + state_name);
                    }
                    // Prepare rules: create matcher object for the given input
                    for (RegExpEntry entry : state.getRules()) {
                        entry.matcher = entry.pattern.matcher(input);
                        entry.matchedPosition = -2;
                    }
                    preparedStates.put(state_name, state);
                }
                if (state.forwardName != null) {
                    forward_cnt++;
                    if (forward_cnt > MAX_SUCCESSIVE_STATE_FORWARDINGS) {
                        logError("Reached maximum state forwarding count. Current state: '" + 
                                 state_name + "'. Forwarding state: '" + state.forwardName + "'.");
                        break;
                    }
                    state_name = state.forwardName;
                    continue;
                }
                break;
            }
            // Get prepared rules
            List<RegExpEntry> regexplist = state.getRules(); 
            
            // Find nearest match
            int next_pos = -1;   // The lowest match position.
            int found_idx = -1;  // The index of the rule with the lowest match position.
            for (int i=0; i < regexplist.size(); i++) {
                RegExpEntry entry = regexplist.get(i);
                if (entry.matchedPosition != -1) {  // -1 means: pattern was not found in the complete input
                    // Three cases:
                    // If entry.matchedPosition == -2 then, this pattern
                    // was not matched yet (else-part of the if-statement is executed).
                    // If 0 <= entry.matchedPosition < copy_pos, then the
                    // pattern was already matched by a previous loop of the 
                    // outer while loop, but the matched position is before the
                    // current parse position, i.e. another matching needs to be 
                    // done starting from the current position. 
                    // If entry.matchedPosition >= copy_pos then the pattern was 
                    // already matched by a previous loop of the outer while loop
                    // and the matched position is located after the current parse 
                    // position. Therefore another matching is not required, but
                    // the match position of the previous loop can be used.
                    if (entry.matchedPosition >= copy_pos) {
                        // Set next_pos to the lowest match position found.
                        if ((next_pos < 0) || (entry.matchedPosition < next_pos)) {
                            next_pos = entry.matchedPosition;
                            found_idx = i;
                        }
                    } else {
                        if (entry.matcher.find(copy_pos)) {
                            entry.matchedPosition = entry.matcher.start();
                            // Set next_pos to the lowest match position found.
                            if ((next_pos < 0) || (entry.matchedPosition < next_pos)) {
                                next_pos = entry.matchedPosition;
                                found_idx = i;
                            }
                        } else {
                            // The pattern was not found in the complete input.
                            entry.matchedPosition = -1;  
                        }
                    }
                }
            }

            if (found_idx >= 0) {  // match was found
                RegExpEntry entry = regexplist.get(found_idx);
                // output.append(input, copy_pos, entry.matcher.start());
                encodeEntitiesAndBr(input, copy_pos, entry.matcher.start(),
                                    decode_positions, decode_strings, output);
                Range r = calcRange(input, entry.matcher, 0, entry.styleIDs,
                                    decode_positions, decode_strings);
                output.append(r.toString());
                copy_pos = r.endPos;
                if (entry.nextIsReturn) {
                    if (enter_stack.isEmpty()) {
                        logWarning("Reached return() statement without enter() statement.");
                    } else {
                        state_name = enter_stack.pop();
                    }
                } else {
                    if (entry.nextIsEnter) {
                        if (entry.returnFromEnterState != null) {
                            enter_stack.push(entry.returnFromEnterState);
                        } else {
                            enter_stack.push(state_name);
                        }
                    }
                    if (entry.nextState != null) {
                        state_name = entry.nextState;
                    }
                }
            } else {
                break;
            }
        }
        if (copy_pos < len) {
            // output.append(input, copy_pos, len);
            encodeEntitiesAndBr(input, copy_pos, len, decode_positions, decode_strings, output);
        }
        
        return state_name;  // return final state
    }


    private Range calcRange(StringBuilder input,
                            Matcher match,
                            int startgroup,
                            String[] styleIDs,
                            List<Integer> decode_positions,
                            List<String> decode_strings) throws Exception
    {
        int maxgroup = styleIDs.length - 1; // Math.min(match.groupCount(), styleIDs.length - 1);
        if (maxgroup > match.groupCount()) {
            maxgroup = match.groupCount();
            logWarning("No regular expression group found for style '" + styleIDs[styleIDs.length - 1] + "'.");
        }
        Range range = new Range();
        range.firstGroup = startgroup;
        range.lastGroup = startgroup;
        range.styleID = styleIDs[startgroup];
        range.startPos = match.start(startgroup);
        range.endPos = match.end(startgroup);

        // search all inner groups
        int copy_pos = range.startPos;
        int next_grp = startgroup + 1;
        while (next_grp <= maxgroup) {
            int next_start = match.start(next_grp);

            if (next_start >= 0) {  // group matched
                if (next_start >= range.endPos) break;

                // next group is inner group
                String prevstr = encodeEntitiesAndBr(input, copy_pos, next_start,
                                                     decode_positions, decode_strings, null);
                copy_pos = next_start;
                if (prevstr.length() > 0) range.subparts.add(prevstr);
                Range inner_range = calcRange(input, match, next_grp, styleIDs,
                                              decode_positions, decode_strings);
                range.lastGroup = next_grp;
                range.subparts.add(inner_range);
                copy_pos = inner_range.endPos;
                next_grp = inner_range.lastGroup + 1;
            } else {
                // group did not match anything; continue with next group
                ++next_grp;
            }
        }
        String str = encodeEntitiesAndBr(input, copy_pos, range.endPos,
                                         decode_positions, decode_strings, null);
        if (str.length() > 0) range.subparts.add(str);
        return range;
    }

    private String encodeEntitiesAndBr(StringBuilder input,
                                       int start,
                                       int end,
                                       List<Integer> decode_positions,
                                       List<String> decode_strings,
                                       Appendable output) throws Exception
    {
        int sz = decode_positions.size();
        int decode_start_idx = -1;
        int decode_end_idx = -1;
        for (int i=0; i < sz; i++) {
            int char_pos = decode_positions.get(i);
            if (char_pos < start) continue;
            if (char_pos >= end) break;
            if (decode_start_idx == -1) decode_start_idx = i;
            decode_end_idx = i;
        }

        if (decode_start_idx == -1) {  // nothing to encode
            if (output == null) {
                return input.substring(start, end);
            } else {
                output.append(input, start, end);
                return null;  // return null if result is returned in output
            }
        }

        Appendable buf = (output != null) ? output : new StringBuilder((end - start) + 128);
        int copy_pos = start;
        for (int i=decode_start_idx; i <= decode_end_idx; i++) {
            int decode_pos = decode_positions.get(i);
            buf.append(input, copy_pos, decode_pos);
            buf.append(decode_strings.get(i));
            copy_pos = decode_pos + 1;
        }
        if (copy_pos < end) {
            buf.append(input, copy_pos, end);
        }
        // Return null if result is returned in output:
        return (output != null) ? null : buf.toString();
    }


    private int skipArgs(String input)
    {
        // Skip whitespace
        int len = input.length();
        int pos = 0;
        while (pos < len) {
            if (! Character.isWhitespace(input.charAt(pos))) break;
            ++pos;
        }
        final String ARGS_PATTERN = "[args:";
        if (input.regionMatches(true, pos, ARGS_PATTERN, 0, ARGS_PATTERN.length())) {
            int endPos = input.indexOf(']', pos);
            if (endPos > 0) {  // line is a valid arguments line
                return endPos + 1;  // return position after arguments line
            }
        }
        return 0;   // no valid arguments line; do not skip
    }


    private static class Range
    {
        int startPos;
        int endPos;
        String styleID;
        List subparts = new ArrayList();
        int firstGroup = -1;
        int lastGroup = -1;

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            boolean has_style = (styleID.length() > 0);
            if (has_style) sb.append("<span class=\"").append(styleID).append("\">");
            for (int i=0; i < subparts.size(); i++) {
                Object obj = subparts.get(i);
                sb.append(obj.toString());
            }
            if (has_style) sb.append("</span>");
            return sb.toString();
        }
    }

    private static class RegExpEntry
    {
        Pattern pattern;
        String[] styleIDs;
        String nextState = null;
        boolean nextIsEnter = false;
        boolean nextIsReturn = false;
        String returnFromEnterState = null;   // explicit return state (optional)
        
        Matcher matcher;
        int matchedPosition;
    }

    private static class State
    {
        int priority = DEFAULT_PRIORITY;
        String name;
        String forwardName = null;
        List<RegExpEntry> rules = new ArrayList<RegExpEntry>();
        
        State(String name) 
        {
            this.name = name;
        }
        
        List<RegExpEntry> getRules()
        {
            return rules;
        }
    }

    private static class HighlightConfig
    {
        private Map<String, State> stateMap = new HashMap<String, State>();
        private String initialStateName = null;
        
        State getState(String name) 
        {
            return stateMap.get(name);
        }
        
        Collection<State> getStates()
        {
            return stateMap.values();
        }
        
        void addState(State state)
        {
            State old_state = getState(state.name);
            if ((old_state == null) || (old_state.priority < state.priority)) {
                stateMap.put(state.name, state);
            } else if (old_state.priority == state.priority) {
                // Same priority: merge with existing rules
                old_state.rules.addAll(state.rules);
            }
            // else if old_state.priority > state.priority then ignore state rules
        }
        
        String getInitialStateName()
        {
            return initialStateName;
        }
        
        void setInitialStateName(String name)
        {
            initialStateName = name;
        }
        
        boolean isEmpty()
        {
            return stateMap.isEmpty();
        }
        
        //        String[] invalidStateTransitions()
        //        {
        //            Iterator<State> it = stateMap.values().iterator();
        //            while (it.hasNext()) {
        //                State st = it.next();
        //                if (st.forwardName != null) {
        //                    
        //                }
        //            }
        //        }
    }
}
