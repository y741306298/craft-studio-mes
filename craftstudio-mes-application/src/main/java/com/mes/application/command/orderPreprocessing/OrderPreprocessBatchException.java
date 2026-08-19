package com.mes.application.command.orderPreprocessing;

import java.util.List;

/**
 * Signals that one or more order items in a preprocessing batch failed.
 *
 * <p>The queue uses this signal to retry the batch before it persists a permanent failure state.</p>
 */
public class OrderPreprocessBatchException extends RuntimeException {

    private final List<String> failedOrderItemIds;
    private final List<String> failures;

    public OrderPreprocessBatchException(List<String> failedOrderItemIds, List<String> failures) {
        super(String.join("; ", failures));
        this.failedOrderItemIds = List.copyOf(failedOrderItemIds);
        this.failures = List.copyOf(failures);
    }

    public List<String> getFailedOrderItemIds() {
        return failedOrderItemIds;
    }

    public List<String> getFailures() {
        return failures;
    }
}
