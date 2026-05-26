package com.chequeprint.service;

import com.chequeprint.model.Cheque;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class ChequeAiActionExecutor {

    private final OpenAiChequeAssistantService aiService;
    private final ChequeService chequeService;
    private final PrintService printService;
    private final ChequeReminderService reminderService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChequeAiActionExecutor() {
        this(new OpenAiChequeAssistantService(), new ChequeService(), new PrintService(), new ChequeReminderService());
    }

    ChequeAiActionExecutor(
            OpenAiChequeAssistantService aiService,
            ChequeService chequeService,
            PrintService printService,
            ChequeReminderService reminderService) {
        this.aiService = aiService;
        this.chequeService = chequeService;
        this.printService = printService;
        this.reminderService = reminderService;
    }

    public AiActionResult handleUserInput(String userInput) throws Exception {
        String json = aiService.runAgent(userInput);
        ChequeAiCommand command = parseCommand(json);
        return execute(command, json);
    }

    public ChequeAiCommand parseCommand(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return new ChequeAiCommand();
        }
        return mapper.readValue(json, ChequeAiCommand.class);
    }

    public AiActionResult execute(ChequeAiCommand command) throws Exception {
        return execute(command, mapper.writeValueAsString(command));
    }

    private AiActionResult execute(ChequeAiCommand command, String aiJson) throws Exception {
        if (command == null || command.getAction().isBlank()) {
            return AiActionResult.failure("No action found.", aiJson);
        }

        return switch (command.getAction().trim().toUpperCase(Locale.ROOT)) {
            case "ADD_CHEQUE" -> addCheque(command, aiJson);
            case "PRINT_CHEQUE" -> printCheque(command, aiJson);
            case "SHOW_PENDING_CHEQUES" -> showPendingCheques(aiJson);
            case "SHOW_HISTORY" -> showHistory(aiJson);
            case "SEARCH_CHEQUE" -> searchCheque(command, aiJson);
            case "REMINDER_CHECK" -> reminderCheck(aiJson);
            default -> AiActionResult.failure("Unsupported action: " + command.getAction(), aiJson);
        };
    }

    private AiActionResult addCheque(ChequeAiCommand command, String aiJson) throws Exception {
        String name = command.getData().getName().trim();
        BigDecimal amount = parseAmount(command.getData().getAmount());
        LocalDate date = parseDateOrToday(command.getData().getDate());

        if (name.isBlank()) {
            return AiActionResult.failure("Payee name is required to add a cheque.", aiJson);
        }
        if (amount == null) {
            return AiActionResult.failure("Amount is required to add a cheque.", aiJson);
        }

        Cheque cheque = new Cheque(null, name, amount, 1, date);
        chequeService.save(cheque);
        return AiActionResult.success("Cheque added: " + cheque.getChequeNo(), aiJson, List.of(cheque));
    }

    private AiActionResult printCheque(ChequeAiCommand command, String aiJson) throws Exception {
        Cheque cheque = findBestMatch(command);
        if (cheque == null) {
            return AiActionResult.failure("No matching cheque found to print.", aiJson);
        }

        boolean printed = printService.previewCheque(cheque);
        return printed
                ? AiActionResult.success("Cheque printed: " + cheque.getChequeNo(), aiJson, List.of(cheque))
                : AiActionResult.failure("Cheque print was cancelled.", aiJson);
    }

    private AiActionResult showPendingCheques(String aiJson) throws Exception {
        List<Cheque> cheques = chequeService.getAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft || c.getStatus() == Cheque.Status.Pending)
                .toList();
        return AiActionResult.success("Pending cheques: " + cheques.size(), aiJson, cheques);
    }

    private AiActionResult showHistory(String aiJson) throws Exception {
        List<Cheque> cheques = chequeService.getAll();
        return AiActionResult.success("Cheque history loaded: " + cheques.size(), aiJson, cheques);
    }

    private AiActionResult searchCheque(ChequeAiCommand command, String aiJson) throws Exception {
        String query = command.getData().getQuery().trim();
        String name = !command.getData().getName().isBlank()
                ? command.getData().getName().trim().toLowerCase(Locale.ROOT)
                : query.toLowerCase(Locale.ROOT);
        BigDecimal commandAmount = parseAmount(command.getData().getAmount());
        final BigDecimal amount = commandAmount != null ? commandAmount : parseAmount(query);
        final String searchName = name;

        List<Cheque> cheques = chequeService.getAll().stream()
                .filter(c -> {
                    boolean nameMatches = !searchName.isBlank()
                            && c.getPayeeName() != null
                            && c.getPayeeName().toLowerCase(Locale.ROOT).contains(searchName);
                    boolean amountMatches = amount != null
                            && c.getAmount() != null
                            && c.getAmount().compareTo(amount) == 0;
                    return nameMatches || amountMatches;
                })
                .toList();

        return AiActionResult.success("Matching cheques: " + cheques.size(), aiJson, cheques);
    }

    private AiActionResult reminderCheck(String aiJson) throws Exception {
        List<Cheque> cheques = reminderService.getUpcomingChequesWithinDays(2);
        String message = reminderService.buildReminderMessage(cheques);
        if (message.isBlank()) {
            message = "No cheques due within 2 days.";
        }
        return AiActionResult.success(message, aiJson, cheques);
    }

    private Cheque findBestMatch(ChequeAiCommand command) throws Exception {
        String query = command.getData().getQuery().trim();
        String name = !command.getData().getName().isBlank()
                ? command.getData().getName().trim().toLowerCase(Locale.ROOT)
                : query.toLowerCase(Locale.ROOT);
        BigDecimal commandAmount = parseAmount(command.getData().getAmount());
        final BigDecimal amount = commandAmount != null ? commandAmount : parseAmount(query);
        final String searchName = name;
        LocalDate date = parseDate(command.getData().getDate());

        return chequeService.getAll().stream()
                .filter(c -> searchName.isBlank()
                        || (c.getPayeeName() != null && c.getPayeeName().toLowerCase(Locale.ROOT).contains(searchName)))
                .filter(c -> amount == null || (c.getAmount() != null && c.getAmount().compareTo(amount) == 0))
                .filter(c -> date == null || date.equals(c.getIssueDate()))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll("[^0-9.\\-]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDateOrToday(String raw) {
        LocalDate parsed = parseDate(raw);
        return parsed != null ? parsed : LocalDate.now();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);
        if ("today".equals(value)) {
            return LocalDate.now();
        }
        if ("tomorrow".equals(value)) {
            return LocalDate.now().plusDays(1);
        }
        if ("yesterday".equals(value)) {
            return LocalDate.now().minusDays(1);
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static class AiActionResult {
        private final boolean success;
        private final String message;
        private final String aiJson;
        private final List<Cheque> cheques;

        private AiActionResult(boolean success, String message, String aiJson, List<Cheque> cheques) {
            this.success = success;
            this.message = message;
            this.aiJson = aiJson;
            this.cheques = cheques == null ? List.of() : cheques;
        }

        public static AiActionResult success(String message, String aiJson, List<Cheque> cheques) {
            return new AiActionResult(true, message, aiJson, cheques);
        }

        public static AiActionResult failure(String message, String aiJson) {
            return new AiActionResult(false, message, aiJson, List.of());
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getAiJson() {
            return aiJson;
        }

        public List<Cheque> getCheques() {
            return cheques;
        }
    }
}
