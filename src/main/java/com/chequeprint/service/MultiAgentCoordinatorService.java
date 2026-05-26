package com.chequeprint.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MultiAgentCoordinatorService {

    private ChequeAiActionExecutor actionAgent;
    private AiSqlDatabaseService dataAgent;
    private ChequeReminderService reminderAgent;
    private SmartSuggestionService suggestionAgent;

    public MultiAgentCoordinatorService() {
    }

    MultiAgentCoordinatorService(
            ChequeAiActionExecutor actionAgent,
            AiSqlDatabaseService dataAgent,
            ChequeReminderService reminderAgent,
            SmartSuggestionService suggestionAgent) {
        this.actionAgent = actionAgent;
        this.dataAgent = dataAgent;
        this.reminderAgent = reminderAgent;
        this.suggestionAgent = suggestionAgent;
    }

    public MultiAgentResult handle(String userInput) throws Exception {
        String text = userInput == null ? "" : userInput.trim();
        if (text.isBlank()) {
            return MultiAgentResult.failure("Chat Agent", "Please enter a command.");
        }

        Intent intent = detectIntent(text);
        return switch (intent) {
            case REMINDER -> runReminderAgent();
            case SUGGESTION -> runSuggestionAgent();
            case DATA -> runDataAgent(text);
            case ACTION -> runActionAgent(text);
        };
    }

    private MultiAgentResult runActionAgent(String userInput) throws Exception {
        ChequeAiActionExecutor.AiActionResult result = getActionAgent().handleUserInput(userInput);
        return new MultiAgentResult(
                result.isSuccess(),
                "Action Agent",
                result.getMessage(),
                result.getAiJson(),
                List.of(),
                result.getCheques().size());
    }

    private MultiAgentResult runDataAgent(String userInput) throws Exception {
        AiSqlDatabaseService.SqlResult result = getDataAgent().askDatabase(userInput);
        return new MultiAgentResult(
                true,
                "Data Agent",
                "Database query returned " + result.getRows().size() + " rows.",
                result.getSql(),
                result.getRows(),
                result.getRows().size());
    }

    private MultiAgentResult runReminderAgent() throws Exception {
        ChequeReminderService agent = getReminderAgent();
        var cheques = agent.getUpcomingChequesWithinDays(2);
        String message = agent.buildReminderMessage(cheques);
        if (message.isBlank()) {
            message = "No cheques due within 2 days.";
        }
        return new MultiAgentResult(
                true,
                "Reminder Agent",
                message,
                "",
                List.of(),
                cheques.size());
    }

    private MultiAgentResult runSuggestionAgent() throws Exception {
        SmartSuggestionService agent = getSuggestionAgent();
        SmartSuggestionService.SmartSuggestions suggestions = agent.analyze();
        String message = agent.buildSuggestionMessage();
        return new MultiAgentResult(
                true,
                "Smart Suggestion Agent",
                message,
                "",
                List.of(),
                suggestions.allInsights().size());
    }

    private Intent detectIntent(String userInput) {
        String text = userInput.toLowerCase(Locale.ROOT);

        if (containsAny(text, "reminder", "remind", "due", "upcoming within 2 days")) {
            return Intent.REMINDER;
        }
        if (containsAny(text, "suggest", "suggestion", "insight", "analyze", "analyse", "frequent", "high amount")) {
            return Intent.SUGGESTION;
        }
        if (containsAny(text, "sql", "database", "query", "records", "rows", "above", "below")) {
            return Intent.DATA;
        }
        if (text.startsWith("show ") && containsAny(text, "all", "history", "pending")) {
            return Intent.DATA;
        }

        return Intent.ACTION;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private ChequeAiActionExecutor getActionAgent() {
        if (actionAgent == null) {
            actionAgent = new ChequeAiActionExecutor();
        }
        return actionAgent;
    }

    private AiSqlDatabaseService getDataAgent() {
        if (dataAgent == null) {
            dataAgent = new AiSqlDatabaseService();
        }
        return dataAgent;
    }

    private ChequeReminderService getReminderAgent() {
        if (reminderAgent == null) {
            reminderAgent = new ChequeReminderService();
        }
        return reminderAgent;
    }

    private SmartSuggestionService getSuggestionAgent() {
        if (suggestionAgent == null) {
            suggestionAgent = new SmartSuggestionService();
        }
        return suggestionAgent;
    }

    private enum Intent {
        ACTION,
        DATA,
        REMINDER,
        SUGGESTION
    }

    public static class MultiAgentResult {
        private final boolean success;
        private final String agent;
        private final String message;
        private final String rawOutput;
        private final List<Map<String, Object>> rows;
        private final int count;

        private MultiAgentResult(
                boolean success,
                String agent,
                String message,
                String rawOutput,
                List<Map<String, Object>> rows,
                int count) {
            this.success = success;
            this.agent = agent;
            this.message = message;
            this.rawOutput = rawOutput;
            this.rows = rows == null ? List.of() : rows;
            this.count = count;
        }

        public static MultiAgentResult failure(String agent, String message) {
            return new MultiAgentResult(false, agent, message, "", List.of(), 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getAgent() {
            return agent;
        }

        public String getMessage() {
            return message;
        }

        public String getRawOutput() {
            return rawOutput;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public int getCount() {
            return count;
        }
    }
}
