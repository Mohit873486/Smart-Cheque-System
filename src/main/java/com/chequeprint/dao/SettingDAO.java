package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.User;
import com.chequeprint.dao.SettingDAO;
import com.chequeprint.model.Settings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SettingDAO {
  public void saveSettings(Settings s) throws SQLException {
    String sql = "UPDATE settings SET app_name=?, currency=?, date_format=?, language=?, cheque_prefix=?, invoice_prefix=?, theme=? WHERE id=1";

    try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {

      ps.setString(1, s.getAppName());
      ps.setString(2, s.getCurrency());
      ps.setString(3, s.getDateFormat());
      ps.setString(4, s.getLanguage());
      ps.setString(5, s.getChequePrefix());
      ps.setString(6, s.getInvoicePrefix());
      ps.setString(7, s.getTheme());

      ps.executeUpdate();
    }
  }
}
