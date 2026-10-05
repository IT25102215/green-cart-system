package com.greencart.designpattern.member3order.observer;

import com.greencart.entity.OrderStatus;

/** Event passed from the order Subject to every order Observer. */
public record OrderStatusEvent(
        Long orderId,
        String customerName,
        String customerEmail,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String actorEmail,
        String note
) { }
