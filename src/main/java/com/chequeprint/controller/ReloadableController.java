package com.chequeprint.controller;

/**
 * Interface implemented by FXML view controllers that support hot-reloading
 * state snapshots and clean resource cleanup.
 */
public interface ReloadableController {

    /**
     * Called once when the view page is loaded/navigated to.
     * Use with a boolean guard (if (alreadyLoaded) return;) to avoid redundant API calls.
     */
    default void onPageLoad() {
    }

    /**
     * Called before the controller/view is hot-reloaded.
     * Returns an object representing the transient UI/data state (e.g. search query, selected item ID, active tab).
     */
    default Object saveState() {
        return null;
    }

    /**
     * Called after the view is hot-reloaded and controller is re-bound.
     * Restores state captured prior to reload.
     *
     * @param state State snapshot returned from saveState()
     */
    default void restoreState(Object state) {
    }

    /**
     * Clean up listeners, timers, event filters, or background tasks
     * before old controller instance is discarded to prevent memory leaks.
     */
    default void cleanup() {
    }
}
