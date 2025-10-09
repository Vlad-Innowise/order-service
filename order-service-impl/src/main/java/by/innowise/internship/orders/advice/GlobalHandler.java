package by.innowise.internship.orders.advice;

import by.innowise.common.library.exception.ApplicationException;
import by.innowise.common.library.exception.dto.ComplexExceptionDto;
import by.innowise.common.library.exception.dto.SimpleExceptionDto;
import by.innowise.common.library.exception.dto.StructuredExceptionDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class GlobalHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleExceptionDto> handle(Exception e) {
        log.error("Internal server error", e);
        return ResponseEntity.internalServerError()
                             .body(new SimpleExceptionDto(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                          e.getMessage()));
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<SimpleExceptionDto> handle(ApplicationException e) {
        log.error("Application error: ", e);
        return ResponseEntity.status(e.getHttpStatus())
                             .body(new SimpleExceptionDto(e.getHttpStatus().value(),
                                                          e.getMessage()));
    }

    /**
     * Occurred when some constraints were violated inside {@code @RequestBody}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ComplexExceptionDto> handle(MethodArgumentNotValidException e) {
        log.error("Invalid user input: {}", e.getMessage());
        List<StructuredExceptionDto> errors =
                e.getBindingResult().getFieldErrors().stream()
                 .map(er -> new StructuredExceptionDto(
                         er.getField(),
                         er.getDefaultMessage()))
                 .toList();
        return ResponseEntity.badRequest()
                             .body(new ComplexExceptionDto(HttpStatus.BAD_REQUEST.value(), errors));
    }

    /**
     * Occurred when there is:
     * invalid JSON;
     * empty {@code @RequestBody};
     * type mismatch between JSON and {@code model}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SimpleExceptionDto> handle(HttpMessageNotReadableException e) {
        Throwable rootCause = Optional.ofNullable(e.getRootCause()).orElse(e);
        log.error("Failed to read a request body: {}", rootCause.getMessage(), rootCause);
        String message = Optional.ofNullable(rootCause.getMessage())
                                 .orElse("Incorrect JSON or invalid request body!");
        return ResponseEntity.badRequest()
                             .body(new SimpleExceptionDto(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * Occurred when a received value from the client cannot be casted to the expected type of the method parameter.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<SimpleExceptionDto> handle(MethodArgumentTypeMismatchException e) {
        String message = e.getMessage();
        log.error("Incorrect argument type: {}", message, e);
        return ResponseEntity.badRequest()
                             .body(new SimpleExceptionDto(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * Occurred when constraints were violated on method parameter or by invoking a validator.validate()
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ComplexExceptionDto> handle(ConstraintViolationException e) {
        List<StructuredExceptionDto> errors =
                e.getConstraintViolations()
                 .stream()
                 .map(cv ->
                              new StructuredExceptionDto(cv.getPropertyPath().toString(),
                                                         cv.getMessage())
                 ).toList();
        log.error("Invalid data: {}", errors, e);
        return ResponseEntity.badRequest()
                             .body(new ComplexExceptionDto(HttpStatus.BAD_REQUEST.value(), errors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<SimpleExceptionDto> handle(MissingServletRequestParameterException e) {
        String message = String.format("Missing required request parameter: {%s}", e.getParameterName());
        log.error(message, e);
        return ResponseEntity.badRequest()
                             .body(new SimpleExceptionDto(HttpStatus.BAD_REQUEST.value(), message));
    }

}
