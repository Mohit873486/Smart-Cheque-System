package com.chequeprint.controller;

import com.chequeprint.model.BankAccount;

/**
 * Owns account-oriented helpers for the bank screen.
 */
public class BankAccountController extends BankSettingsController {

    protected String formatAccountTemplateLabel(BankAccount account) {
        if (account == null) {
            return "Select a Bank Account";
        }
        String bankName = account.getBankName() != null && !account.getBankName().isBlank()
                ? account.getBankName()
                : "Bank Account";
        String accNo = account.getAccountNumber() != null ? account.getAccountNumber().trim() : "";
        String last4 = accNo.length() >= 4 ? accNo.substring(accNo.length() - 4) : accNo;
        return bankName + (last4.isEmpty() ? "" : " (... " + last4 + ")");
    }
}
