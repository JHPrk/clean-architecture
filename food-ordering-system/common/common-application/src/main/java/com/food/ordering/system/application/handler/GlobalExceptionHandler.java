package com.food.ordering.system.application.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ResponseBody
  @ExceptionHandler({Exception.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorDTO handleException(Exception exception) {
    log.error(exception.getMessage(), exception);
    return ErrorDTO.builder()
        .code(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .message("Unexpected Error!") // For Security Reason
        .build();
  }

  @ResponseBody
  @ExceptionHandler({ValidationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleException(ValidationException validationException) {
    ErrorDTO errorDTO;
    if (validationException instanceof ConstraintViolationException) {
      String violations = extractViolationsFromException(
          (ConstraintViolationException) validationException);
      log.error(violations, validationException);
      errorDTO = ErrorDTO.builder()
          .code(HttpStatus.BAD_REQUEST.getReasonPhrase())
          .message(violations)
          .build();
    } else {
      log.error(validationException.getMessage(), validationException);
      errorDTO = ErrorDTO.builder()
          .code(HttpStatus.BAD_REQUEST.getReasonPhrase())
          .message(validationException.getMessage()) // For Security Reason
          .build();
    }
    return errorDTO;
  }

  private String extractViolationsFromException(ConstraintViolationException validationException) {
    return validationException.getConstraintViolations()
        .stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining("--"));
  }
}
