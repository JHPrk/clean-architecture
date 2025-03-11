package com.food.ordering.system.payment.service.domain.event;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import java.time.ZonedDateTime;

public class PaymentCancelledEvent extends PaymentEvent {

  private final DomainEventPublisher<PaymentCancelledEvent> paymentCancelledEventDomainEventPublisher;

  public PaymentCancelledEvent(Payment payment, ZonedDateTime createdAt,
      DomainEventPublisher<PaymentCancelledEvent> paymentCancelledEventDomainEventPublisher) {
    super(payment, createdAt, null);
    this.paymentCancelledEventDomainEventPublisher = paymentCancelledEventDomainEventPublisher;
  }

}
