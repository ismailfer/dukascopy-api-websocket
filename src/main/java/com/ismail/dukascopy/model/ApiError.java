package com.ismail.dukascopy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents an API error
 * 
 * @author ismail
 * @since 20220617
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiError {
    /**
     * Error code.
     */
    private int errorCode;

    /**
     * Error message.
     */
    private String errorMsg;

    /**
     * Error Detail.
     */
    private String errorDetail;

}
