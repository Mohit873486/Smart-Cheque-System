package com.chequeprint.service;

import com.chequeprint.model.Cheque;
import java.util.List;

/**
 * Exception thrown when batch printing completes with one or more failures.
 * Contains both successfully printed cheques and failure error details.
 */
public class BatchPrintException extends Exception {
    private final List<Cheque> successes;
    private final List<String> failures;

    public BatchPrintException(String message, List<Cheque> successes, List<String> failures) {
        super(message);
        this.successes = successes;
        this.failures = failures;
    }

    public List<Cheque> getSuccesses() {
        return successes;
    }

    public List<String> getFailures() {
        return failures;
    }
}
