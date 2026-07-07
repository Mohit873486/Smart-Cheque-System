package com.chequeprint;

import com.chequeprint.config.AppConfig;
import org.junit.jupiter.api.Test;
import java.sql.*;

public class DBTest {
    @Test
    public void testCheques() throws Exception {
        System.out.println("--- DB TEST CHEQUES ---");
        try (Connection conn = AppConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, cheque_no, payee_name, status FROM cheques")) {
            while (rs.next()) {
                System.out.printf("CHQ: id=%d, cheque_no=%s, payee=%s, status=%s%n",
                    rs.getInt("id"), rs.getString("cheque_no"), rs.getString("payee_name"), rs.getString("status"));
            }
        }
        System.out.println("-----------------------");
    }
}
