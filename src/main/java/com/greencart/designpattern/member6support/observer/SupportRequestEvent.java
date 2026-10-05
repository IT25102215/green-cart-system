package com.greencart.designpattern.member6support.observer;

/** Event generated when a customer submits feedback, a complaint, or an inquiry. */
public record SupportRequestEvent(
        String requestType,
        Long entityId,
        String customerName,
        String customerEmail,
        String subject,
        String message
) { }
