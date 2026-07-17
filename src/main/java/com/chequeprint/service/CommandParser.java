package com.chequeprint.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CommandParser for the AI Assistant.
 * Parses natural language commands using Regex and extracts structured data.
 */
public class CommandParser {

    // Regex patterns for various commands
    private static final Pattern ADD_CHEQUE_PATTERN = Pattern.compile("(?i)^add\\s+cheque\\s+(?:for\\s+)?([a-zA-Z]+(?:\\s+[a-zA-Z]+)*)\\s+(\\d+(?:\\.\\d+)?)\\s+(.*)$");
    private static final Pattern SHOW_ALL_PATTERN = Pattern.compile("(?i)^show\\s+all\\s+cheques$");
    private static final Pattern SHOW_PENDING_PATTERN = Pattern.compile("(?i)^show\\s+pending\\s+cheques$");
    private static final Pattern DELETE_CHEQUE_PATTERN = Pattern.compile("(?i)^delete\\s+cheque\\s+(\\d+)$");

    public enum CommandAction {
        ADD_CHEQUE,
        SHOW_ALL_CHEQUES,
        SHOW_PENDING_CHEQUES,
        DELETE_CHEQUE,
        UNKNOWN
    }

    public static class ParsedCommand {
        private final CommandAction action;
        private final String message;
        private final Map<String, Object> data;

        public ParsedCommand(CommandAction action, String message, Map<String, Object> data) {
            this.action = action;
            this.message = message;
            this.data = data != null ? data : new HashMap<>();
        }

        public CommandAction getAction() { return action; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }

        @Override
        public String toString() {
            return "{\n" +
                   "  \"action\": \"" + action + "\",\n" +
                   "  \"message\": \"" + message + "\",\n" +
                   "  \"data\": " + data + "\n" +
                   "}";
        }
    }

    /**
     * Parses the user input string and returns a structured ParsedCommand.
     * 
     * @param input the raw text typed by the user
     * @return a structured ParsedCommand containing action, message, and extracted data
     */
    public ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParsedCommand(CommandAction.UNKNOWN, "Input cannot be empty", null);
        }

        String trimmed = input.trim();

        // 1. Parse Add Cheque
        Matcher addMatcher = ADD_CHEQUE_PATTERN.matcher(trimmed);
        if (addMatcher.matches()) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", addMatcher.group(1));
            data.put("amount", Double.parseDouble(addMatcher.group(2)));
            data.put("date", addMatcher.group(3).trim());
            return new ParsedCommand(CommandAction.ADD_CHEQUE, "Add cheque command parsed successfully", data);
        }

        // 2. Parse Show All Cheques
        Matcher showAllMatcher = SHOW_ALL_PATTERN.matcher(trimmed);
        if (showAllMatcher.matches()) {
            return new ParsedCommand(CommandAction.SHOW_ALL_CHEQUES, "Show all cheques command parsed", null);
        }

        // 3. Parse Show Pending Cheques
        Matcher showPendingMatcher = SHOW_PENDING_PATTERN.matcher(trimmed);
        if (showPendingMatcher.matches()) {
            return new ParsedCommand(CommandAction.SHOW_PENDING_CHEQUES, "Show pending cheques command parsed", null);
        }

        // 4. Parse Delete Cheque
        Matcher deleteMatcher = DELETE_CHEQUE_PATTERN.matcher(trimmed);
        if (deleteMatcher.matches()) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", Integer.parseInt(deleteMatcher.group(1)));
            return new ParsedCommand(CommandAction.DELETE_CHEQUE, "Delete cheque command parsed successfully", data);
        }

        // Handle Invalid / Unknown input gracefully
        return new ParsedCommand(CommandAction.UNKNOWN, "Command not recognized. Please try again.", null);
    }
}
