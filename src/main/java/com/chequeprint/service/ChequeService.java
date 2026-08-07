package com.chequeprint.service;

import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.PageRequest;
import com.chequeprint.model.PageResult;
import com.chequeprint.util.NumberToWordsConverter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ChequeService {

    private static final java.util.concurrent.atomic.AtomicLong SEQUENCE = new java.util.concurrent.atomic.AtomicLong(0);
    private final ChequeDAO dao = new ChequeDAO();

    // ═══════════════════════════════════════════════════════════════════════
    // 1. PAGINATED READS
    // ═══════════════════════════════════════════════════════════════════════

    public PageResult<Cheque> getAll(PageRequest pageRequest) {
        return dao.findAll(pageRequest);
    }

    /**
     * Legacy compatibility. Loads up to 1000 items.
     * @deprecated Use {@link #getAll(PageRequest)} for production.
     */
    @Deprecated
    public List<Cheque> getAll() throws SQLException {
        return dao.findAll(PageRequest.of(0, 1000)).getContent();
    }

    public Cheque getById(int id) throws SQLException {
        return dao.findById(id).orElse(null);
    }

    public PageResult<Cheque> search(String query, PageRequest pageRequest) {
        return dao.search(query, pageRequest);
    }

    /**
     * @deprecated Use paginated {@link #search(String, PageRequest)}.
     */
    @Deprecated
    public List<Cheque> search(String query) throws SQLException {
        return dao.search(query, PageRequest.of(0, 1000)).getContent();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. WRITE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    public boolean save(Cheque c) throws SQLException {
        return createCheque(c);
    }

    public boolean createCheque(Cheque c) throws SQLException {
        if (c.getChequeNo() == null || c.getChequeNo().isBlank()) {
            c.setChequeNo(generateChequeNo());
            while (dao.existsByChequeNo(c.getChequeNo(), c.getId())) {
                c.setChequeNo(generateChequeNo());
            }
        }
        validateCheque(c);
        c.setAmountWords(convertAmountToWords(c.getAmount()));
        return dao.insert(c).isOk();
    }

    public boolean update(Cheque c) throws SQLException {
        validateCheque(c);
        c.setAmountWords(convertAmountToWords(c.getAmount()));
        return dao.update(c).isOk();
    }

    public boolean delete(int id) throws SQLException {
        return dao.delete(id).isOk();
    }

    public boolean markPrinted(int id) throws SQLException {
        return dao.updateStatus(id, Cheque.Status.Printed).isOk();
    }

    public boolean setStatus(Cheque cheque, Cheque.Status status) throws SQLException {
        if (cheque == null || status == null) return false;
        return dao.updateStatus(cheque.getId(), status).isOk();
    }

    public boolean approveCheque(int id) throws SQLException {
        return dao.updateStatus(id, Cheque.Status.Approved).isOk();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. STATS (Server-side via dedicated endpoint)
    // ═══════════════════════════════════════════════════════════════════════

    public ChequeDAO.ChequeStats fetchStats() {
        var response = dao.fetchStats();
        return response.isOk() && response.getData() != null
            ? response.getData()
            : new ChequeDAO.ChequeStats(0, 0, 0, 0, 0.0);
    }

    @Deprecated
    public int getTotalCheques() throws SQLException { return (int) fetchStats().total(); }

    @Deprecated
    public int getPrintedCheques() throws SQLException { return (int) fetchStats().printed(); }

    @Deprecated
    public int getPendingCheques() throws SQLException { return (int) fetchStats().pending(); }

    @Deprecated
    public int getTodayCheques() throws SQLException { return (int) fetchStats().today(); }

    @Deprecated
    public double getMonthlyAmount() throws SQLException { return fetchStats().monthlySum(); }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. VALIDATION & HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    public void validateCheque(Cheque c) throws SQLException {
        if (c.getPayeeName() == null || c.getPayeeName().isBlank())
            throw new IllegalArgumentException("Payee name is required.");
        if (c.getPayeeName().length() > 150)
            throw new IllegalArgumentException("Payee name cannot exceed 150 characters.");
        if (!c.getPayeeName().matches("^[a-zA-Z0-9 .'-]+$"))
            throw new IllegalArgumentException("Payee name contains invalid characters.");

        if (c.getAmount() == null || c.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero.");
        if (c.getAmount().compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new IllegalArgumentException("Amount exceeds maximum allowed limit.");

        if (c.getIssueDate() == null) {
            throw new IllegalArgumentException("Issue date is required.");
        }
        LocalDate today = LocalDate.now();
        if (c.getIssueDate().isBefore(today.minusDays(90))) {
            throw new IllegalArgumentException("Cheque date cannot be older than 90 days (stale cheque).");
        }
        if (c.getIssueDate().isAfter(today.plusDays(180))) {
            throw new IllegalArgumentException("Cheque date cannot be more than 180 days in the future.");
        }

        if (c.getChequeNo() != null && !c.getChequeNo().isBlank()) {
            if (dao.existsByChequeNo(c.getChequeNo(), c.getId())) {
                throw new IllegalArgumentException("Cheque number '" + c.getChequeNo() + "' already exists.");
            }
        }
    }

    public Cheque validateChequeData(Cheque cheque) {
        if (cheque == null) {
            throw new IllegalStateException("Cheque data is null. Cannot proceed with printing.");
        }
        if (cheque.getPayeeName() == null || cheque.getPayeeName().isBlank()) {
            throw new IllegalStateException("Payee Name is required for printing.");
        }
        if (cheque.getAmount() == null || cheque.getAmount().doubleValue() <= 0) {
            throw new IllegalStateException("Cheque Amount must be greater than zero.");
        }
        if (cheque.getIssueDate() == null) {
            throw new IllegalStateException("Issue Date is required for printing.");
        }
        return cheque;
    }

    public String convertAmountToWords(BigDecimal amount) {
        if (amount == null) return "";
        return NumberToWordsConverter.convert(amount);
    }

    private String generateChequeNo() {
        String date = java.time.LocalDate.now().toString().replace("-", "");
        long seq = SEQUENCE.incrementAndGet();
        return String.format("CHQ-%s-%06d", date, seq % 999999);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. DASHBOARD AGGREGATIONS (Client-side on provided list)
    // ═══════════════════════════════════════════════════════════════════════

    public BigDecimal calculatePortfolioSum(List<Cheque> cheques) {
        if (cheques == null || cheques.isEmpty()) return BigDecimal.ZERO;
        return cheques.stream()
            .map(Cheque::getAmount)
            .filter(amt -> amt != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public double calculatePendingRatio(List<Cheque> cheques) {
        if (cheques == null || cheques.isEmpty()) return 0.0;
        long pendingCount = cheques.stream().filter(c -> c.getStatus() == Cheque.Status.Pending).count();
        return (pendingCount * 100.0) / cheques.size();
    }

    public double calculatePrintedRatio(List<Cheque> cheques) {
        if (cheques == null || cheques.isEmpty()) return 0.0;
        long printedCount = cheques.stream().filter(c -> c.getStatus() == Cheque.Status.Printed).count();
        return (printedCount * 100.0) / cheques.size();
    }
}