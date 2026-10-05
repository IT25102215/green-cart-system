package com.greencart.designpattern.member6support.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** Concrete Subject: notifies all support observers after a customer submission. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupportRequestSubject {
    private final List<SupportRequestObserver> observers;

    public void notifyObservers(SupportRequestEvent event) {
        for (SupportRequestObserver observer : observers) {
            try {
                observer.update(event);
            } catch (RuntimeException ex) {
                // The support request is already stored; secondary notification failures are isolated.
                log.warn("Support observer {} failed: {}",
                        observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }
}
