package com.greencart.designpattern.member6support.observer;

/** Observer contract for customer support submissions. */
public interface SupportRequestObserver {
    void update(SupportRequestEvent event);
}
