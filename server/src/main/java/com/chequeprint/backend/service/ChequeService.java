package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.Cheque;
import com.chequeprint.backend.repository.ChequeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChequeService {

    private final ChequeRepository repository;
    private final AuditLogService auditLogService;

    @Autowired
    public ChequeService(ChequeRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<Cheque> getAllCheques() {
        return repository.findAll();
    }

    public Optional<Cheque> getChequeById(int id) {
        return repository.findById(id);
    }

    public Cheque createCheque(Cheque cheque) {
        Cheque saved = repository.save(cheque);
        auditLogService.record(null, "cheques", saved.getId(), "INSERT", "Created cheque: " + saved.getChequeNo());
        return saved;
    }

    public Cheque updateCheque(int id, Cheque updatedCheque) {
        return repository.findById(id)
                .map(existing -> {
                    String oldStatus = existing.getStatus() != null ? existing.getStatus().name() : "";
                    String newStatus = updatedCheque.getStatus() != null ? updatedCheque.getStatus().name() : "";

                    existing.setChequeNo(updatedCheque.getChequeNo());
                    existing.setPayeeName(updatedCheque.getPayeeName());
                    existing.setAmount(updatedCheque.getAmount());
                    existing.setAmountWords(updatedCheque.getAmountWords());
                    existing.setBankId(updatedCheque.getBankId());
                    existing.setIssueDate(updatedCheque.getIssueDate());
                    existing.setStatus(updatedCheque.getStatus());

                    Cheque saved = repository.save(existing);

                    if (!oldStatus.equals(newStatus)) {
                        auditLogService.record(null, "cheques", saved.getId(), newStatus,
                                "Status changed from " + oldStatus + " to " + newStatus);
                    } else {
                        auditLogService.record(null, "cheques", saved.getId(), "UPDATE", "Updated cheque details.");
                    }

                    return saved;
                })
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found with ID: " + id));
    }

    public void deleteCheque(int id) {
        Optional<Cheque> existing = repository.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Cheque not found with ID: " + id);
        }
        repository.deleteById(id);
        auditLogService.record(null, "cheques", id, "DELETE", "Deleted cheque: " + existing.get().getChequeNo());
    }

    public boolean existsByChequeNo(String chequeNo, int excludeId) {
        return repository.existsByChequeNoAndIdNot(chequeNo, excludeId);
    }

    public Optional<Cheque> findByChequeNo(String chequeNo) {
        return repository.findByChequeNo(chequeNo);
    }

    public List<Cheque> searchCheques(String query) {
        return repository.searchCheques(query);
    }
}
