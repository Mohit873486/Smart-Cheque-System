package com.chequeprint.backend.mapper;

import com.chequeprint.backend.dto.ChequeResponse;
import com.chequeprint.backend.entity.Cheque;
import com.chequeprint.backend.entity.BankTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChequeMapper {

    public ChequeResponse toResponse(Cheque cheque) {
        if (cheque == null) return null;

        ChequeResponse dto = new ChequeResponse();
        dto.setId(cheque.getId());
        dto.setChequeNo(cheque.getChequeNo());
        dto.setPayeeName(cheque.getPayeeName());
        dto.setAmount(cheque.getAmount());
        dto.setAmountWords(cheque.getAmountWords());
        dto.setBankId(cheque.getBankId());
        dto.setAccountId(cheque.getAccountId());
        dto.setIssueDate(cheque.getIssueDate());
        dto.setStatus(cheque.getStatus() != null ? cheque.getStatus().name() : null);
        dto.setActive(cheque.isActive());
        dto.setLastPrinter(cheque.getLastPrinter());
        dto.setLastPrintResult(cheque.getLastPrintResult());
        dto.setPrintedAt(cheque.getPrintedAt());
        dto.setCreatedAt(cheque.getCreatedAt());
        dto.setUpdatedAt(cheque.getUpdatedAt());

        BankTemplate bank = cheque.getBankTemplate();
        if (bank != null) {
            dto.setBankName(bank.getBankName());
            dto.setBankCode(bank.getBankCode());
        }

        String status = dto.getStatus();
        dto.setCanEdit("Draft".equals(status) || "Pending".equals(status));
        dto.setCanDelete("Draft".equals(status) || "Pending".equals(status) || "Rejected".equals(status));
        dto.setCanApprove("Pending".equals(status));
        dto.setCanReject("Pending".equals(status));
        dto.setCanPrint("Approved".equals(status) || "Printed".equals(status));

        return dto;
    }

    public List<ChequeResponse> toResponseList(List<Cheque> cheques) {
        if (cheques == null) return List.of();
        return cheques.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
