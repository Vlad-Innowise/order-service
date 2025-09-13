package by.innowise.internship.orders.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderStatus {

    PENDING,
    FINISHED;

    @JsonCreator
    public static OrderStatus fromString(String value) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status provided: %s".formatted(value));
    }

}
