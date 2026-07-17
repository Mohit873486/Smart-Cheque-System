package com.chequeprint.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ChatHistoryService {

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".chequeprint";
    private static final String HISTORY_FILE = APP_DIR + File.separator + "chat_history.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatHistoryService() {
        File dir = new File(APP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public List<Map<String, String>> loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(file, new TypeReference<List<Map<String, String>>>() {});
        } catch (IOException e) {
            System.err.println("Failed to load chat history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void saveMessage(String role, String text) {
        if (text == null || text.isBlank()) return;
        List<Map<String, String>> history = loadHistory();
        
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role); // "USER" or "AI"
        msg.put("text", text);
        
        history.add(msg);
        
        // Keep only last 100 messages
        if (history.size() > 100) {
            history = history.subList(history.size() - 100, history.size());
        }
        
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(HISTORY_FILE), history);
        } catch (IOException e) {
            System.err.println("Failed to save chat history: " + e.getMessage());
        }
    }
    
    public void clearHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}
