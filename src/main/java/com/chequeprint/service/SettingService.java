package com.chequeprint.service;

import com.chequeprint.dao.SettingDAO;
import com.chequeprint.model.Settings;

import java.sql.SQLException;

/**
 * Service class for Settings operations, serving as the business logic layer
 * between Controllers and SettingDAO.
 */
public class SettingService {

    private final SettingDAO dao = new SettingDAO();

    public Settings getSettings() throws SQLException {
        return dao.getSettings();
    }

    public void saveSettings(Settings s) throws SQLException {
        if (s.getAppName() == null || s.getAppName().isBlank()) {
            throw new IllegalArgumentException("Application name is required.");
        }
        dao.saveSettings(s);
    }
}
