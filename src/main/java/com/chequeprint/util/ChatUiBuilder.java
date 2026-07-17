package com.chequeprint.util;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ChatUiBuilder {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    
    private final VBox chatContainer;
    private final VBox resultPanel;

    public ChatUiBuilder(VBox chatContainer, VBox resultPanel) {
        this.chatContainer = chatContainer;
        this.resultPanel = resultPanel;
    }

    public void addUserBubble(String text) {
        Platform.runLater(() -> {
            VBox bubble = createBubble(text, true);
            Label time = createTimeLabel(true);

            StackPane avatar = createAvatar("U", true);

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_RIGHT);
            row.setPadding(new Insets(2, 0, 2, 0));
            row.setMaxWidth(Double.MAX_VALUE);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox msgCol = new VBox(4);
            msgCol.setAlignment(Pos.TOP_RIGHT);
            msgCol.setMaxWidth(460);
            msgCol.getChildren().addAll(bubble, time);

            row.getChildren().addAll(spacer, msgCol, avatar);

            animateBubbleIn(row);
            chatContainer.getChildren().add(row);
        });
    }

    public void addAiBubble(String text) {
        Platform.runLater(() -> {
            StackPane avatar = createAvatar("✦", false);
            VBox bubble = createBubble(text, false);
            Label time = createTimeLabel(false);

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));
            row.setMaxWidth(Double.MAX_VALUE);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox msgCol = new VBox(4);
            msgCol.setAlignment(Pos.TOP_LEFT);
            msgCol.setMaxWidth(460);
            msgCol.getChildren().addAll(bubble, time);

            row.getChildren().addAll(avatar, msgCol, spacer);

            animateBubbleIn(row);
            chatContainer.getChildren().add(row);
        });
    }

    public void addAiSuccessBubble(String text) {
        Platform.runLater(() -> {
            StackPane avatar = createAvatar("✦", false);
            VBox bubble = createBubble(text, false);
            bubble.getStyleClass().add("chat-bubble-success");
            if (bubble.getChildren().get(0) instanceof Label lbl) {
                lbl.setStyle("-fx-text-fill: #166534; -fx-font-size: 13.5px; -fx-font-weight: bold;");
            }
            Label time = createTimeLabel(false);

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));
            
            VBox msgCol = new VBox(4, bubble, time);
            msgCol.setAlignment(Pos.TOP_LEFT);
            msgCol.setMaxWidth(460);
            row.getChildren().addAll(avatar, msgCol);
            
            animateBubbleIn(row);
            chatContainer.getChildren().add(row);
        });
    }

    public void addAiErrorBubble(String text) {
        Platform.runLater(() -> {
            StackPane avatar = createAvatar("✦", false);
            VBox bubble = createBubble(text, false);
            bubble.getStyleClass().add("chat-bubble-error");
            if (bubble.getChildren().get(0) instanceof Label lbl) {
                lbl.setStyle("-fx-text-fill: #991b1b; -fx-font-size: 13.5px; -fx-font-weight: bold;");
            }
            Label time = createTimeLabel(false);

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));
            
            VBox msgCol = new VBox(4, bubble, time);
            msgCol.setAlignment(Pos.TOP_LEFT);
            msgCol.setMaxWidth(460);
            row.getChildren().addAll(avatar, msgCol);
            
            animateBubbleIn(row);
            chatContainer.getChildren().add(row);
        });
    }
    
    public void addUserImageBubble(File file) {
        Platform.runLater(() -> {
            VBox bubble = new VBox(4);
            bubble.getStyleClass().add("chat-bubble-user");
            bubble.setMaxWidth(460);
            
            try {
                Image img = new Image(file.toURI().toString());
                ImageView imgView = new ImageView(img);
                imgView.setPreserveRatio(true);
                imgView.setFitWidth(200);
                imgView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                
                Label lbl = new Label("📷 Uploaded Image: " + file.getName());
                lbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px;");
                
                bubble.getChildren().addAll(imgView, lbl);
            } catch (Exception ex) {
                Label err = new Label("📷 [Image: " + file.getName() + "]");
                err.setStyle("-fx-text-fill: #ffffff;");
                bubble.getChildren().add(err);
            }

            Label time = createTimeLabel(true);
            StackPane avatar = createAvatar("U", true);

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_RIGHT);
            row.setPadding(new Insets(2, 0, 2, 0));
            row.setMaxWidth(Double.MAX_VALUE);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox msgCol = new VBox(4);
            msgCol.setAlignment(Pos.TOP_RIGHT);
            msgCol.setMaxWidth(460);
            msgCol.getChildren().addAll(bubble, time);

            row.getChildren().addAll(spacer, msgCol, avatar);

            animateBubbleIn(row);
            chatContainer.getChildren().add(row);
        });
    }

    public void addSingleChequeCard(Map<String, Object> cheque) {
        Platform.runLater(() -> {
            resultPanel.getChildren().clear();

            VBox card = new VBox(12);
            card.getStyleClass().add("cheque-card-large");
            card.setMaxWidth(Double.MAX_VALUE);
            card.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label("Cheque Details: " + cheque.get("chequeNo"));
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #1e293b;");
            
            Label payee = new Label("👤 Payee: " + cheque.get("payeeName"));
            payee.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
            Label amount = new Label("💰 Amount: ₹" + cheque.get("amount"));
            amount.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
            Label date = new Label("📅 Date: " + cheque.get("issueDate"));
            date.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
            Label status = new Label("📌 Status: " + cheque.get("status"));
            status.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569; -fx-font-weight: bold;");
            
            card.getChildren().addAll(title, payee, amount, date, status);
            resultPanel.getChildren().add(card);
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    public void addTableBubble(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;

        Platform.runLater(() -> {
            resultPanel.getChildren().clear();

            VBox tableCard = new VBox(0);
            tableCard.getStyleClass().add("chat-table-card");
            tableCard.setMaxWidth(Double.MAX_VALUE);

            HBox headerRow = new HBox(0);
            headerRow.getStyleClass().add("chat-table-header");
            for (String key : rows.get(0).keySet()) {
                Label hdr = new Label(formatColumnTitle(key));
                hdr.getStyleClass().add("chat-table-header-cell");
                hdr.setMinWidth(90);
                hdr.setMaxWidth(160);
                HBox.setHgrow(hdr, Priority.ALWAYS);
                headerRow.getChildren().add(hdr);
            }
            tableCard.getChildren().add(headerRow);

            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> rowData = rows.get(i);
                HBox dataRow = new HBox(0);
                dataRow.getStyleClass().add(i % 2 == 0 ? "chat-table-row-even" : "chat-table-row-odd");
                for (String key : rowData.keySet()) {
                    Object val = rowData.get(key);
                    Label cell = new Label(val == null ? "" : val.toString());
                    cell.getStyleClass().add("chat-table-cell");
                    cell.setMinWidth(90);
                    cell.setMaxWidth(160);
                    HBox.setHgrow(cell, Priority.ALWAYS);
                    dataRow.getChildren().add(cell);
                }
                tableCard.getChildren().add(dataRow);
            }

            resultPanel.getChildren().add(tableCard);
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), tableCard);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    private StackPane createAvatar(String text, boolean isUser) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add(isUser ? "chat-avatar-user" : "chat-avatar-ai");
        Label avatarIcon = new Label(text);
        avatarIcon.getStyleClass().add(isUser ? "chat-avatar-letter" : "chat-avatar-icon");
        avatar.getChildren().add(avatarIcon);
        avatar.setMinSize(34, 34);
        avatar.setMaxSize(34, 34);
        return avatar;
    }

    private VBox createBubble(String text, boolean isUser) {
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add(isUser ? "chat-bubble-user" : "chat-bubble-ai");
        bubble.setMaxWidth(460);

        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(420);
        content.getStyleClass().add(isUser ? "chat-text-user" : "chat-text-ai");
        content.setStyle(isUser
                ? "-fx-text-fill: #ffffff; -fx-font-size: 13.5px;"
                : "-fx-text-fill: #1e293b; -fx-font-size: 13.5px;");

        bubble.getChildren().add(content);
        return bubble;
    }

    private Label createTimeLabel(boolean isUser) {
        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-time");
        if (isUser) {
            time.setAlignment(Pos.CENTER_RIGHT);
            time.setMaxWidth(Double.MAX_VALUE);
            time.setStyle("-fx-alignment: center-right;");
        }
        return time;
    }

    private void animateBubbleIn(HBox row) {
        row.setOpacity(0);
        row.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(350), row);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(350), row);
        translate.setFromY(20);
        translate.setToY(0);

        ParallelTransition pt = new ParallelTransition(fade, translate);
        pt.play();
    }
    
    private String formatColumnTitle(String key) {
        if (key == null || key.isBlank()) return "";
        String[] words = key.replace('_', ' ').split("\\s+");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!title.isEmpty()) title.append(' ');
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) title.append(word.substring(1));
        }
        return title.toString();
    }
}
