package com.chequeprint.backend.controller;

import com.chequeprint.backend.dto.ApiResponse;
import com.chequeprint.backend.dto.ChequeResponse;
import com.chequeprint.backend.dto.ChequeSearchRequest;
import com.chequeprint.backend.dto.ChequeStatsResponse;
import com.chequeprint.backend.dto.PageResponse;
import com.chequeprint.backend.entity.Cheque;
import com.chequeprint.backend.service.ChequeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cheques")
public class ChequeV1Controller {

    private final ChequeService chequeService;

    public ChequeV1Controller(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<ChequeStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats loaded", chequeService.getDashboardStats()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<PageResponse<ChequeResponse>>> getAllCheques(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(chequeService.getAllCheques(page, size, sortBy, sortDir)));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<PageResponse<ChequeResponse>>> searchCheques(
            @Valid @RequestBody ChequeSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Search results", chequeService.searchCheques(request)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<PageResponse<ChequeResponse>>> searchChequesGet(
            @RequestParam(required = false) String status, @RequestParam(required = false) Integer bankId,
            @RequestParam(required = false) String searchText, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        ChequeSearchRequest request = new ChequeSearchRequest();
        request.setStatus(status); request.setBankId(bankId); request.setSearchText(searchText);
        request.setPage(page); request.setSize(size); request.setSortBy(sortBy); request.setSortDir(sortDir);
        return ResponseEntity.ok(ApiResponse.success("Search results", chequeService.searchCheques(request)));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<List<ChequeResponse>>> getRecentCheques(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(chequeService.getRecentCheques(limit)));
    }

    @GetMapping("/overdue-pending")
    @PreAuthorize("hasAnyRole('Admin', 'Manager')")
    public ResponseEntity<ApiResponse<List<ChequeResponse>>> getOverduePending() {
        return ResponseEntity.ok(ApiResponse.success(chequeService.getOverduePending()));
    }

    @GetMapping("/ready-to-print")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator')")
    public ResponseEntity<ApiResponse<List<ChequeResponse>>> getReadyToPrint(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(chequeService.getReadyToPrint(limit)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<ChequeResponse>> getChequeById(@PathVariable int id) {
        ChequeResponse cheque = chequeService.getChequeResponseById(id);
        if (cheque == null) return ResponseEntity.status(404).body(ApiResponse.notFound("Cheque not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(cheque));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator', 'User')")
    public ResponseEntity<ApiResponse<ChequeResponse>> createCheque(@Valid @RequestBody Cheque cheque) {
        return ResponseEntity.status(201).body(ApiResponse.created(chequeService.saveCheque(cheque)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Operator')")
    public ResponseEntity<ApiResponse<ChequeResponse>> updateCheque(
            @PathVariable int id, @Valid @RequestBody Cheque cheque) {
        cheque.setId(id);
        return ResponseEntity.ok(ApiResponse.success("Cheque updated", chequeService.saveCheque(cheque)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Manager')")
    public ResponseEntity<ApiResponse<ChequeResponse>> updateStatus(
            @PathVariable int id, @RequestParam String status) {
        Cheque.Status newStatus;
        try { newStatus = Cheque.Status.valueOf(status); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status: " + status)); }
        ChequeResponse updated = chequeService.updateStatus(id, newStatus);
        if (updated == null) return ResponseEntity.status(404).body(ApiResponse.notFound("Cheque not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<Void>> deleteCheque(@PathVariable int id) {
        chequeService.deleteCheque(id);
        return ResponseEntity.ok(ApiResponse.success("Cheque deleted", null));
    }
}
