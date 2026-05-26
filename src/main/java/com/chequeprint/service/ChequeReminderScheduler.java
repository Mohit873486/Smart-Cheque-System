package com.chequeprint.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChequeReminderScheduler {

    private final ChequeReminderService reminderService;
    private ScheduledExecutorService scheduler;

    public ChequeReminderScheduler() {
        this(new ChequeReminderService());
    }

    ChequeReminderScheduler(ChequeReminderService reminderService) {
        this.reminderService = reminderService;
    }

    public synchronized void startDaily() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cheque-reminder-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(
                this::checkAndShowReminder,
                initialDelaySeconds(),
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }

    public void checkNow() {
        checkAndShowReminder();
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void checkAndShowReminder() {
        try {
            String message = reminderService.checkUpcomingReminderMessage();
            if (message == null || message.isBlank()) {
                return;
            }

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
                alert.setTitle("Cheque Reminder");
                alert.setHeaderText("Cheques due within 2 days");
                alert.show();
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private long initialDelaySeconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1);
        }
        return Math.max(1, Duration.between(now, nextRun).getSeconds());
    }
}
