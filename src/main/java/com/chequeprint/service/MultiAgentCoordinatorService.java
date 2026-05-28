package com.chequeprint.service;

import com.chequeprint.model.Cheque;

import java.util.LinkedHashMap;
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
                toRows(result.getCheques()),
                result.getCheques().size());
    }

    private List<Map<String, Object>> toRows(List<Cheque> cheques) {
        if (cheques == null || cheques.isEmpty()) {
            return List.of();
        }

        return cheques.stream()
                .map(this::toRow)
                .toList();
    }

    private Map<String, Object> toRow(Cheque cheque) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cheque_no", cheque.getChequeNo());
        row.put("payee_name", cheque.getPayeeName());
        row.put("amount", cheque.getAmount());
        row.put("bank_id", cheque.getBankId());
        row.put("issue_date", cheque.getIssueDate());
        row.put("status", cheque.getStatus());
        return row;
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

        // Build rows from suggested cheques (high amount and upcoming) so UI can show
        // them
        List<Map<String, Object>> rows = List.of();
        java.util.List<com.chequeprint.model.Cheque> cheques = new java.util.ArrayList<>();

        // collect cheques matching high amount threshold
        if (!suggestions.getHighAmountTransactions().isEmpty() || !suggestions.getUpcomingPayments().isEmpty()) {
            var all = new com.chequeprint.service.ChequeService().getAll();
            // match high amount messages by amount and date where possible
            for (var insight : suggestions.getHighAmountTransactions()) {
                // insight message format: "High amount cheque for NAME: AMOUNT on DATE"
                String msg = insight.getMessage();
                try {
                    String[] parts = msg.split(":");
                    if (parts.length >= 2) {
                        String amountPart = parts[1].trim().split(" ")[0];
                        java.math.BigDecimal amt = new java.math.BigDecimal(amountPart.replaceAll("[^0-9.]", ""));
                        all.stream().filter(c -> c.getAmount() != null && c.getAmount().compareTo(amt) == 0)
                                .findFirst().ifPresent(cheques::add);
                    }
                } catch (Exception ignored) {
                }
            }

            // upcoming payments - match by date
            for (var insight : suggestions.getUpcomingPayments()) {
                String msg = insight.getMessage();
                try {
                    // expects "Upcoming cheque for NAME on YYYY-MM-DD amount AMOUNT"
                    String[] parts = msg.split(" on ");
                    if (parts.length >= 2) {
                        String[] dateParts = parts[1].trim().split(" ");
                        java.time.LocalDate d = java.time.LocalDate.parse(dateParts[0]);
                        all.stream().filter(c -> c.getIssueDate() != null && d.equals(c.getIssueDate()))
                                .forEach(cheques::add);
                    }
                } catch (Exception ignored) {
                }
            }

            if (!cheques.isEmpty()) {
                rows = cheques.stream().map(this::toRow).toList();
            }
        }

        int count = rows.isEmpty() ? suggestions.allInsights().size() : rows.size();

        return new MultiAgentResult(
                true,
                "Smart Suggestion Agent",
                message,
                "",
                rows,
                count);
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
