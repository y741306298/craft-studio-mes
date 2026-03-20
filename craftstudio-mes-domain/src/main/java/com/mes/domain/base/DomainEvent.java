package com.mes.domain.base;

import lombok.Getter;

import java.util.Date;
import java.util.UUID;

@Getter
public class DomainEvent {
    private final String eventId;
    private final Date occurredTime;

    public DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredTime = new Date();
    }
}
