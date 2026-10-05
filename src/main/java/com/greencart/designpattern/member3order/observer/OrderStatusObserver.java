package com.greencart.designpattern.member3order.observer;

/** Observer contract for order lifecycle changes. */
public interface OrderStatusObserver {
    void update(OrderStatusEvent event);
}
