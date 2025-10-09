package by.innowise.internship.orders.exception;

import by.innowise.common.library.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class OrderModificationDeniedException extends ApplicationException {

    public OrderModificationDeniedException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

}
