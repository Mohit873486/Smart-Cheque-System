package com.chequeprint.service;

import com.chequeprint.model.Cheque;

import java.time.LocalDate;
import java.util.List;

public class ChequeReminderService {

    private final ChequeService chequeService;

    public ChequeReminderService() {
        this(new ChequeService());
    }

    ChequeReminderService(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    public List<Cheque> getUpcomingChequesWithinDays(int days) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(Math.max(0, days));

        return chequeService.getAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft || c.getStatus() == Cheque.Status.Pending)
                .filter(c -> c.getIssueDate() != null)
                .filter(c -> !c.getIssueDate().isBefore(today) && !c.getIssueDate().isAfter(maxDate))
                .toList();
    }

    public String buildReminderMessage(List<Cheque> cheques) {
        if (cheques == null || cheques.isEmpty()) {
            return "";
        }

        StringBuilder message = new StringBuilder("Upcoming cheque reminders:\n");
        for (Cheque cheque : cheques) {
            message.append("\n")
                    .append(cheque.getIssueDate())
                    .append(" - ")
                    .append(nullToDash(cheque.getPayeeName()))
                    .append(" - ")
                    .append(cheque.getAmount() != null ? cheque.getAmount().toPlainString() : "0.00")
                    .append(" (")
                    .append(nullToDash(cheque.getChequeNo()))
                    .append(")");
        }
        return message.toString();
    }

    public String checkUpcomingReminderMessage() throws Exception {
        return buildReminderMessage(getUpcomingChequesWithinDays(2));
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
