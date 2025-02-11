package com.telus.api.credit.sync.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlers.class);

    @ExceptionHandler(value = CreditException.class)
    public ResponseEntity<ErrorResponse> handleCreditException(CreditException ce) {
        LOGGER.error("{}: Credit Exception", ExceptionConstants.STACKDRIVER_METRIC, ce);
        return ExceptionHelper.createErrorResponse(ce);
    }
}
