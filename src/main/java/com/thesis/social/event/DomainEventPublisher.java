package com.thesis.social.event;

import java.util.Map;

public interface DomainEventPublisher {

    void publish(String eventType, Map<String, Object> payload);
}
