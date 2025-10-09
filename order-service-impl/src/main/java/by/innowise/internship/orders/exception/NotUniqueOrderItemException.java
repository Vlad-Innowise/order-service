package by.innowise.internship.orders.exception;

import by.innowise.common.library.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class NotUniqueOrderItemException extends ApplicationException {

    public NotUniqueOrderItemException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
