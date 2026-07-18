package com.chequeprint.service;

import java.util.logging.Logger;

/**
 * Service responsible for detecting local application commands
 * from user input before falling back to the external AI API.
 * This ensures clean architecture by keeping business logic parsing
 * separate from the Controller.
 */
public class CommandDetectionService {

    private static final Logger LOGGER = Logger.getLogger(CommandDetectionService.class.getName());

    public enum CommandType {
        CREATE_CHEQUE,
        SHOW_PENDING_CHEQUES,
        PRINT_CHEQUE,
        AUTO_FILL_CHEQUE,
        AI_FALLBACK // No matching command found, use AI
    }

    /**
     * Analyzes the user input and determines if it matches a predefined local command.
     * 
     * @param input the raw text typed by the user
     * @return the resolved CommandType
     */
    public CommandType detectCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return CommandType.AI_FALLBACK;
        }

        // Normalize string for simpler matching
        String normalized = input.toLowerCase().trim();

        if (normalized.contains("create cheque") || normalized.contains("add cheque") || normalized.contains("new cheque")) {
            return CommandType.CREATE_CHEQUE;
        } else if (normalized.contains("show pending") || normalized.contains("pending cheques")) {
            return CommandType.SHOW_PENDING_CHEQUES;
        } else if (normalized.contains("print cheque") || normalized.contains("print the cheque")) {
            return CommandType.PRINT_CHEQUE;
        } else if (normalized.startsWith("auto-fill data:")) {
            return CommandType.AUTO_FILL_CHEQUE;
        }

        return CommandType.AI_FALLBACK;
    }

    /**
     * Executes the local business logic associated with the detected command.
     * In a full implementation, this class would take dependencies (like DAO or other services)
     * via its constructor to interact with the database or printer.
     * 
     * @param command the detected CommandType to execute
     * @return the text response to show in the UI
     */
    public String executeCommand(CommandType command) {
        LOGGER.info("Executing local command: " + command);
        
        switch (command) {
            case CREATE_CHEQUE:
                // TODO: Integrate with ChequeCreationService / DAO here
                return "✅ Opening cheque creation wizard... Please fill out the required details.";
                
            case SHOW_PENDING_CHEQUES:
                // TODO: Fetch results from Database using ChequeRepository
                return "📋 Here are your pending cheques from the database:\n\n- Cheque #00123: ₹5,000 (Pending)\n- Cheque #00124: ₹12,000 (Pending)";
                
            case PRINT_CHEQUE:
                // TODO: Trigger PrintService API
                return "🖨️ Preparing cheque for printing. Sending document to the default printer...";
                
            default:
                throw new IllegalArgumentException("Unknown local command executed: " + command);
        }
    }
}
