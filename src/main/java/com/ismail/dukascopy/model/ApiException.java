package com.ismail.dukascopy.model;

/**
 * API Exception
 * 
 * @author ismail
 * @since 20220617
 */
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 3788669840036201041L;

    private ApiError apiError = null;

    /**
     * Instantiates a new binance api exception.
     */
    public ApiException() {
        super();
    }

    public ApiException(ApiError apiError) {
        super(apiError.getErrorCode() + "-" + apiError.getErrorMsg());

        this.apiError = apiError;
    }

    public ApiException(int pErrorCode, String pErrorMsg) {
        super(pErrorCode + "-" + pErrorMsg);
    }

    /**
     * Instantiates a new binance api exception.
     *
     * @param message the message
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * Instantiates a new binance api exception.
     *
     * @param cause the cause
     */
    public ApiException(Throwable cause) {
        super(cause);
    }

    /**
     * Instantiates a new binance api exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiError getErrorCode() {
        return apiError;
    }

}