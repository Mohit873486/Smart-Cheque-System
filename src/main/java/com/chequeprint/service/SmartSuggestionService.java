package com.chequeprint.service;

import com.chequeprint.model.Cheque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SmartSuggestionService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");

    private final ChequeService chequeService;

    public SmartSuggestionService() {
        this(new ChequeService());
    }

    SmartSuggestionService(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    public SmartSuggestions analyze() throws Exception {
        List<Cheque> cheques = chequeService.getAll();

        return new SmartSuggestions(
                findHighAmountTransactions(cheques),
                findFrequentUsers(cheques),
                findUpcomingPayments(cheques));
    }

    public String buildSuggestionMessage() throws Exception {
        SmartSuggestions suggestions = analyze();
        StringBuilder message = new StringBuilder("Smart cheque suggestions:");

        if (suggestions.getHighAmountTransactions().isEmpty()
                && suggestions.getFrequentUsers().isEmpty()
                && suggestions.getUpcomingPayments().isEmpty()) {
            return "No smart suggestions found.";
        }

        if (!suggestions.getHighAmountTransactions().isEmpty()) {
            message.append("\n\nHigh amount transactions:");
            suggestions.getHighAmountTransactions().forEach(insight ->
                    message.append("\n- ").append(insight.getMessage()));
        }

        if (!suggestions.getFrequentUsers().isEmpty()) {
            message.append("\n\nFrequent users:");
            suggestions.getFrequentUsers().forEach(insight ->
                    message.append("\n- ").append(insight.getMessage()));
        }

        if (!suggestions.getUpcomingPayments().isEmpty()) {
            message.append("\n\nUpcoming payments:");
            suggestions.getUpcomingPayments().forEach(insight ->
                    message.append("\n- ").append(insight.getMessage()));
        }

        return message.toString();
    }

    private List<SuggestionInsight> findHighAmountTransactions(List<Cheque> cheques) {
        return cheques.stream()
                .filter(c -> c.getAmount() != null && c.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0)
                .sorted(Comparator.comparing(Cheque::getAmount, Comparator.reverseOrder()))
                .limit(5)
                .map(c -> new SuggestionInsight(
                        "HIGH_AMOUNT",
                        "High amount cheque for " + nullToDash(c.getPayeeName())
                                + ": " + c.getAmount().toPlainString()
                                + " on " + nullToDash(c.getIssueDate())))
                .toList();
    }

    private List<SuggestionInsight> findFrequentUsers(List<Cheque> cheques) {
        Map<String, Long> counts = cheques.stream()
                .filter(c -> c.getPayeeName() != null && !c.getPayeeName().isBlank())
                .collect(Collectors.groupingBy(
                        c -> c.getPayeeName().trim(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new SuggestionInsight(
                        "FREQUENT_USER",
                        entry.getKey() + " appears in " + entry.getValue() + " cheques."))
                .toList();
    }

    private List<SuggestionInsight> findUpcomingPayments(List<Cheque> cheques) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        List<SuggestionInsight> insights = new ArrayList<>();

        cheques.stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft || c.getStatus() == Cheque.Status.Pending)
                .filter(c -> c.getIssueDate() != null)
                .filter(c -> !c.getIssueDate().isBefore(today) && !c.getIssueDate().isAfter(nextWeek))
                .sorted(Comparator.comparing(Cheque::getIssueDate))
                .limit(5)
                .forEach(c -> insights.add(new SuggestionInsight(
                        "UPCOMING_PAYMENT",
                        "Upcoming cheque for " + nullToDash(c.getPayeeName())
                                + " on " + c.getIssueDate()
                                + " amount " + (c.getAmount() != null ? c.getAmount().toPlainString() : "0.00"))));

        return insights;
    }

    private String nullToDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    public static class SmartSuggestions {
        private final List<SuggestionInsight> highAmountTransactions;
        private final List<SuggestionInsight> frequentUsers;
        private final List<SuggestionInsight> upcomingPayments;

        public SmartSuggestions(
                List<SuggestionInsight> highAmountTransactions,
                List<SuggestionInsight> frequentUsers,
                List<SuggestionInsight> upcomingPayments) {
            this.highAmountTransactions = highAmountTransactions == null ? List.of() : highAmountTransactions;
            this.frequentUsers = frequentUsers == null ? List.of() : frequentUsers;
            this.upcomingPayments = upcomingPayments == null ? List.of() : upcomingPayments;
        }

        public List<SuggestionInsight> getHighAmountTransactions() {
            return highAmountTransactions;
        }

        public List<SuggestionInsight> getFrequentUsers() {
            return frequentUsers;
        }

        public List<SuggestionInsight> getUpcomingPayments() {
            return upcomingPayments;
        }

        public List<SuggestionInsight> allInsights() {
            List<SuggestionInsight> all = new ArrayList<>();
            all.addAll(highAmountTransactions);
            all.addAll(frequentUsers);
            all.addAll(upcomingPayments);
            return all;
        }
    }

    public static class SuggestionInsight {
        private final String type;
        private final String message;

        public SuggestionInsight(String type, String message) {
            this.type = type == null ? "" : type.toUpperCase(Locale.ROOT);
            this.message = message == null ? "" : message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}
