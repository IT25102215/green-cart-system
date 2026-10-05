package com.greencart.designpattern.member6support.observer;

import com.greencart.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Concrete Observer: forwards the new support request to the configured support email/log. */
@Component
@RequiredArgsConstructor
public class SupportMailObserver implements SupportRequestObserver {
    private final MailService mailService;

    @Override
    public void update(SupportRequestEvent event) {
        mailService.notifySupport(
                event.customerName() == null ? "Green Cart customer" : event.customerName(),
                event.customerEmail() == null ? "unknown@greencart.local" : event.customerEmail(),
                "[" + event.requestType() + "] " + event.subject(),
                event.message()
        );
    }
}
