package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.Cheque;
import com.chequeprint.backend.repository.ChequeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cheques/stats")
public class ChequeStatsController {

    private final ChequeRepository chequeRepo;

    @Autowired
    public ChequeStatsController(ChequeRepository chequeRepo) {
        this.chequeRepo = chequeRepo;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", chequeRepo.count());
        stats.put("pending", chequeRepo.countByStatus(Cheque.Status.Pending));
        stats.put("printed", chequeRepo.countByStatus(Cheque.Status.Printed));
        stats.put("today", chequeRepo.countByIssueDate(LocalDate.now()));
        stats.put("thisMonthAmount", chequeRepo.sumThisMonth() != null ? chequeRepo.sumThisMonth() : BigDecimal.ZERO);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/by-date")
    public ResponseEntity<Map<String, Long>> getCountByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<Object[]> results = chequeRepo.countByDateRange(start, end);
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            map.put(date.toString(), count);
        }
        return ResponseEntity.ok(map);
    }
}