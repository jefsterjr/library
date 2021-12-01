package org.jdev.library.controller;

import lombok.extern.slf4j.Slf4j;
import org.jdev.library.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.util.NoSuchElementException;

@Slf4j
public abstract class CommonsController {

    @ExceptionHandler(Exception.class)
    public @ResponseBody
    ResponseEntity<ErrorResponse> handleException(Exception ex) {
        if (ex instanceof NoSuchElementException) return handleNoSuchElementException((NoSuchElementException) ex);
        return handleDefaultException(ex);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST) /* 400 */
    private @ResponseBody
    ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException ex) {
        log.warn(ex.toString());
        return new ResponseEntity<>(new ErrorResponse(ex.toString(), ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpServerErrorException.InternalServerError.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR) /* 400 */
    private @ResponseBody
    ResponseEntity<ErrorResponse> handleDefaultException(Exception ex) {
        log.error(ex.toString());
        return new ResponseEntity<>(new ErrorResponse(ex.toString(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
