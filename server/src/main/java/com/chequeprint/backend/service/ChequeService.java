package com.chequeprint.backend.service;

import com.chequeprint.backend.dto.ChequeResponse;
import com.chequeprint.backend.dto.ChequeSearchRequest;
import com.chequeprint.backend.dto.ChequeStatsResponse;
import com.chequeprint.backend.dto.PageResponse;
import com.chequeprint.backend.entity.Cheque;
import com.chequeprint.backend.mapper.ChequeMapper;
import com.chequeprint.backend.repository.ChequeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChequeService {

    private final ChequeRepository chequeRepository;
    private final ChequeMapper chequeMapper;

    public ChequeService(ChequeRepository chequeRepository, ChequeMapper chequeMapper) {
        this.chequeRepository = chequeRepository;
        this.chequeMapper = chequeMapper;
    }

    // BACKWARD COMPATIBILITY
    public List<Cheque> getAllCheques() { return chequeRepository.findAll(); }
    public Optional<Cheque> getChequeById(int id) { return chequeRepository.findById(id); }

    @Transactional
    public Cheque createCheque(Cheque cheque) { return chequeRepository.save(cheque); }

    @Transactional
    public Cheque updateCheque(int id, Cheque cheque) {
        if (!chequeRepository.existsById(id)) throw new IllegalArgumentException("Cheque not found: " + id);
        cheque.setId(id);
        return chequeRepository.save(cheque);
    }

    @Transactional
    public void deleteCheque(int id) {
        if (!chequeRepository.existsById(id)) throw new IllegalArgumentException("Cheque not found: " + id);
        chequeRepository.deleteById(id);
    }

    public boolean existsByChequeNo(String chequeNo, int excludeId) {
        return chequeRepository.existsByChequeNo(chequeNo, excludeId);
    }

    public List<Cheque> searchCheques(String query) { return chequeRepository.searchByQuery(query); }

    // STATS
    public ChequeStatsResponse getDashboardStats() {
        Map<String, Object> rawStats = chequeRepository.getChequeStatsNative();
        ChequeStatsResponse stats = new ChequeStatsResponse();
        if (rawStats != null) {
            stats.setTotalCheques(getLong(rawStats, "totalCheques"));
            stats.setPendingCount(getLong(rawStats, "pendingCount"));
            stats.setApprovedCount(getLong(rawStats, "approvedCount"));
            stats.setPrintedCount(getLong(rawStats, "printedCount"));
            stats.setRejectedCount(getLong(rawStats, "rejectedCount"));
            stats.setDraftCount(getLong(rawStats, "draftCount"));
            stats.setCancelledCount(getLong(rawStats, "cancelledCount"));
            stats.setBouncedCount(getLong(rawStats, "bouncedCount"));
            stats.setClearedCount(getLong(rawStats, "clearedCount"));
            stats.setDepositedCount(getLong(rawStats, "depositedCount"));
            stats.setTotalAmountAll(getBigDecimal(rawStats, "totalAmountAll"));
            stats.setTotalAmountThisMonth(getBigDecimal(rawStats, "totalAmountThisMonth"));
            stats.setTotalAmountThisWeek(getBigDecimal(rawStats, "totalAmountThisWeek"));
            stats.setTotalAmountToday(getBigDecimal(rawStats, "totalAmountToday"));
            stats.setPendingAmount(getBigDecimal(rawStats, "pendingAmount"));
            stats.setApprovedAmount(getBigDecimal(rawStats, "approvedAmount"));
            stats.setPrintedAmount(getBigDecimal(rawStats, "printedAmount"));
            stats.setChequesCreatedToday(getLong(rawStats, "chequesCreatedToday"));
            stats.setChequesCreatedThisWeek(getLong(rawStats, "chequesCreatedThisWeek"));
            stats.setChequesCreatedThisMonth(getLong(rawStats, "chequesCreatedThisMonth"));
            stats.setOverduePendingCount(getLong(rawStats, "overduePendingCount"));
            stats.setReadyToPrintCount(getLong(rawStats, "readyToPrintCount"));
        }
        stats.setStatusBreakdown(getStatusBreakdownMap());
        stats.setMonthlyTrend(getMonthlyTrendMap());
        stats.setBankBreakdown(getBankBreakdownMap());
        stats.setDailyTrend(getDailyTrendMap());
        return stats;
    }

    private Map<String, Long> getStatusBreakdownMap() {
        List<Map<String, Object>> rows = chequeRepository.getStatusBreakdown();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("status")), ((Number) row.get("count")).longValue());
        }
        return result;
    }

    private Map<String, BigDecimal> getMonthlyTrendMap() {
        List<Map<String, Object>> rows = chequeRepository.getMonthlyTrend();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("month")), getBigDecimal(row, "total"));
        }
        return result;
    }

    private Map<String, Long> getBankBreakdownMap() {
        List<Map<String, Object>> rows = chequeRepository.getBankBreakdown();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("bankName")), ((Number) row.get("count")).longValue());
        }
        return result;
    }

    private Map<String, Long> getDailyTrendMap() {
        List<Map<String, Object>> rows = chequeRepository.getDailyTrend();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("day")), ((Number) row.get("count")).longValue());
        }
        return result;
    }

    // SEARCH & PAGINATION
    public PageResponse<ChequeResponse> searchCheques(ChequeSearchRequest request) {
        Sort sort = request.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Cheque.Status statusEnum = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try { statusEnum = Cheque.Status.valueOf(request.getStatus()); } catch (IllegalArgumentException e) {}
        }

        Page<Cheque> page;
        if (request.getSearchText() != null && request.getSearchText().length() >= 3) {
            page = chequeRepository.fullTextSearch(request.getSearchText(), request.getStatus(),
                    request.getBankId(), request.getDateFrom(), request.getDateTo(), pageable);
        } else {
            page = chequeRepository.searchCheques(statusEnum, request.getBankId(), request.getAccountId(),
                    request.getDateFrom(), request.getDateTo(), request.getMinAmount(), request.getMaxAmount(),
                    request.getActive(), request.getSearchText(), pageable);
        }

        List<ChequeResponse> responses = page.getContent().stream().map(chequeMapper::toResponse).collect(Collectors.toList());
        return new PageResponse<>(responses, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious());
    }

    public PageResponse<ChequeResponse> getAllCheques(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Cheque> result = chequeRepository.findAll(pageable);
        List<ChequeResponse> responses = result.getContent().stream().map(chequeMapper::toResponse).collect(Collectors.toList());
        return new PageResponse<>(responses, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast(), result.hasNext(), result.hasPrevious());
    }

    public ChequeResponse getChequeResponseById(int id) {
        return chequeRepository.findById(id).map(chequeMapper::toResponse).orElse(null);
    }

    public List<ChequeResponse> getRecentCheques(int limit) {
        return chequeRepository.findRecentCheques(PageRequest.of(0, limit, Sort.by("createdAt").descending()))
                .stream().map(chequeMapper::toResponse).collect(Collectors.toList());
    }

    public List<ChequeResponse> getOverduePending() {
        return chequeRepository.findOverduePending(LocalDate.now().minusDays(7))
                .stream().map(chequeMapper::toResponse).collect(Collectors.toList());
    }

    public List<ChequeResponse> getReadyToPrint(int limit) {
        return chequeRepository.findReadyToPrint(PageRequest.of(0, limit))
                .stream().map(chequeMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ChequeResponse saveCheque(Cheque cheque) {
        return chequeMapper.toResponse(chequeRepository.save(cheque));
    }

    @Transactional
    public ChequeResponse updateStatus(int id, Cheque.Status newStatus) {
        return chequeRepository.findById(id).map(cheque -> {
            cheque.setStatus(newStatus);
            return chequeMapper.toResponse(chequeRepository.save(cheque));
        }).orElse(null);
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? 0L : ((Number) val).longValue();
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }
}
