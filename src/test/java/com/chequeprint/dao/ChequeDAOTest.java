package com.chequeprint.dao;

import com.chequeprint.util.ChequeApiClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ChequeDAOTest {

    @Test
    void existsByChequeNoReturnsFalseWhenRemoteServiceIsUnavailable() throws SQLException, NoSuchFieldException, IllegalAccessException {
        ChequeDAO dao = new ChequeDAO();

        Field clientField = ChequeDAO.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(dao, new ChequeApiClient() {
            @Override
            public boolean existsByChequeNo(String chequeNo, int excludeId) {
                throw new RuntimeException("Connection refused");
            }
        });

        assertFalse(dao.existsByChequeNo("CHQ-123", 0));
    }
}
