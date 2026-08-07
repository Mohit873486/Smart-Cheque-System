package com.chequeprint.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ChequeStatsResponse {
    private long totalCheques;
    private long pendingCount;
    private long approvedCount;
    private long printedCount;
    private long rejectedCount;
    private long draftCount;
    private long cancelledCount;
    private long bouncedCount;
    private long clearedCount;
    private long depositedCount;
    private BigDecimal totalAmountAll;
    private BigDecimal totalAmountThisMonth;
    private BigDecimal totalAmountThisWeek;
    private BigDecimal totalAmountToday;
    private BigDecimal pendingAmount;
    private BigDecimal approvedAmount;
    private BigDecimal printedAmount;
    private Map<String, Long> statusBreakdown;
    private Map<String, BigDecimal> monthlyTrend;
    private Map<String, Long> bankBreakdown;
    private Map<String, Long> dailyTrend;
    private long chequesCreatedToday;
    private long chequesCreatedThisWeek;
    private long chequesCreatedThisMonth;
    private long averageProcessingHours;
    private long overduePendingCount;
    private long readyToPrintCount;

    public ChequeStatsResponse() {}

    public long getTotalCheques() { return totalCheques; }
    public void setTotalCheques(long totalCheques) { this.totalCheques = totalCheques; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public long getApprovedCount() { return approvedCount; }
    public void setApprovedCount(long approvedCount) { this.approvedCount = approvedCount; }
    public long getPrintedCount() { return printedCount; }
    public void setPrintedCount(long printedCount) { this.printedCount = printedCount; }
    public long getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(long rejectedCount) { this.rejectedCount = rejectedCount; }
    public long getDraftCount() { return draftCount; }
    public void setDraftCount(long draftCount) { this.draftCount = draftCount; }
    public long getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(long cancelledCount) { this.cancelledCount = cancelledCount; }
    public long getBouncedCount() { return bouncedCount; }
    public void setBouncedCount(long bouncedCount) { this.bouncedCount = bouncedCount; }
    public long getClearedCount() { return clearedCount; }
    public void setClearedCount(long clearedCount) { this.clearedCount = clearedCount; }
    public long getDepositedCount() { return depositedCount; }
    public void setDepositedCount(long depositedCount) { this.depositedCount = depositedCount; }
    public BigDecimal getTotalAmountAll() { return totalAmountAll; }
    public void setTotalAmountAll(BigDecimal totalAmountAll) { this.totalAmountAll = totalAmountAll; }
    public BigDecimal getTotalAmountThisMonth() { return totalAmountThisMonth; }
    public void setTotalAmountThisMonth(BigDecimal totalAmountThisMonth) { this.totalAmountThisMonth = totalAmountThisMonth; }
    public BigDecimal getTotalAmountThisWeek() { return totalAmountThisWeek; }
    public void setTotalAmountThisWeek(BigDecimal totalAmountThisWeek) { this.totalAmountThisWeek = totalAmountThisWeek; }
    public BigDecimal getTotalAmountToday() { return totalAmountToday; }
    public void setTotalAmountToday(BigDecimal totalAmountToday) { this.totalAmountToday = totalAmountToday; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public BigDecimal getPrintedAmount() { return printedAmount; }
    public void setPrintedAmount(BigDecimal printedAmount) { this.printedAmount = printedAmount; }
    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, Long> statusBreakdown) { this.statusBreakdown = statusBreakdown; }
    public Map<String, BigDecimal> getMonthlyTrend() { return monthlyTrend; }
    public void setMonthlyTrend(Map<String, BigDecimal> monthlyTrend) { this.monthlyTrend = monthlyTrend; }
    public Map<String, Long> getBankBreakdown() { return bankBreakdown; }
    public void setBankBreakdown(Map<String, Long> bankBreakdown) { this.bankBreakdown = bankBreakdown; }
    public Map<String, Long> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(Map<String, Long> dailyTrend) { this.dailyTrend = dailyTrend; }
    public long getChequesCreatedToday() { return chequesCreatedToday; }
    public void setChequesCreatedToday(long chequesCreatedToday) { this.chequesCreatedToday = chequesCreatedToday; }
    public long getChequesCreatedThisWeek() { return chequesCreatedThisWeek; }
    public void setChequesCreatedThisWeek(long chequesCreatedThisWeek) { this.chequesCreatedThisWeek = chequesCreatedThisWeek; }
    public long getChequesCreatedThisMonth() { return chequesCreatedThisMonth; }
    public void setChequesCreatedThisMonth(long chequesCreatedThisMonth) { this.chequesCreatedThisMonth = chequesCreatedThisMonth; }
    public long getAverageProcessingHours() { return averageProcessingHours; }
    public void setAverageProcessingHours(long averageProcessingHours) { this.averageProcessingHours = averageProcessingHours; }
    public long getOverduePendingCount() { return overduePendingCount; }
    public void setOverduePendingCount(long overduePendingCount) { this.overduePendingCount = overduePendingCount; }
    public long getReadyToPrintCount() { return readyToPrintCount; }
    public void setReadyToPrintCount(long readyToPrintCount) { this.readyToPrintCount = readyToPrintCount; }
}
