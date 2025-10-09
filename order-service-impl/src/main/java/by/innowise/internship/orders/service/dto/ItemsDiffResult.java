package by.innowise.internship.orders.service.dto;

import java.util.Set;

public record ItemsDiffResult(
        Set<Long> toAdd,
        Set<Long> toUpdate,
        Set<Long> toRemove
) {
}
