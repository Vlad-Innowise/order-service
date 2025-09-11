package by.innowise.internship.orders.exception;

import by.innowise.common.library.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ItemNotFoundException extends ApplicationException {

    public ItemNotFoundException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

}
