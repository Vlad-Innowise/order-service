package by.innowise.internship.orders.exception;

import by.innowise.common.library.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApplicationException {

    public OrderNotFoundException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

}
