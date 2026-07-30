package com.chequeprint.util;

/**
 * @deprecated Relocated to {@link com.chequeprint.state.AppState}. Use com.chequeprint.state.AppState directly.
 */
@Deprecated
public final class AppState {

    private AppState() {
    }

    public static com.chequeprint.state.AppState getInstance() {
        return com.chequeprint.state.AppState.getInstance();
    }
}
