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

    @Autowired
    public ChequeService(ChequeRepository repository) {
        this.repository = repository;
    }

    public List<Cheque> getAllCheques() {
        return repository.findAll();
    }

    public Optional<Cheque> getChequeById(int id) {
        return repository.findById(id);
    }

    public Cheque createCheque(Cheque cheque) {
        return repository.save(cheque);
    }

    public Cheque updateCheque(int id, Cheque updatedCheque) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setChequeNo(updatedCheque.getChequeNo());
                    existing.setPayeeName(updatedCheque.getPayeeName());
                    existing.setAmount(updatedCheque.getAmount());
                    existing.setAmountWords(updatedCheque.getAmountWords());
                    existing.setBankId(updatedCheque.getBankId());
                    existing.setIssueDate(updatedCheque.getIssueDate());
                    existing.setStatus(updatedCheque.getStatus());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found with ID: " + id));
    }

    public void deleteCheque(int id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Cheque not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    public boolean existsByChequeNo(String chequeNo, int excludeId) {
        return repository.existsByChequeNoAndIdNot(chequeNo, excludeId);
    }

    public Optional<Cheque> findByChequeNo(String chequeNo) {
        return repository.findByChequeNo(chequeNo);
    }
}
