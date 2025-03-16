package com.food.ordering.system.payment.service.domain.outbox.scheduler;

import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import com.food.ordering.system.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class OrderOutboxScheduler implements OutboxScheduler {

  private final OrderOutboxHelper orderOutboxHelper;
  private final PaymentResponseMessagePublisher paymentResponseMessagePublisher;

  @Override
  @Transactional
  @Scheduled(fixedDelayString = "${payment-service.outbox-scheduler-fixed-rate}",
      initialDelayString = "${payment-service.outbox-scheduler-initial-delay}")
  public void processOutboxMessage() {
    Optional<List<OrderOutboxMessage>> outboxMessageResponse = orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(
        OutboxStatus.STARTED);
    if (outboxMessageResponse.isPresent() && outboxMessageResponse.get().size() > 0) {
      List<OrderOutboxMessage> orderOutboxMessages = outboxMessageResponse.get();
      log.info("Received {} OrderOutboxMessage with ids: {}, sending to message bus!",
          orderOutboxMessages.size(),
          orderOutboxMessages.stream().map(outboxMessage -> outboxMessage.getId().toString())
              .collect(Collectors.joining(","))
      );
      orderOutboxMessages.forEach(outboxMessage -> {
        paymentResponseMessagePublisher.publish(outboxMessage,
            orderOutboxHelper::updateOutboxMessage);
      });
      log.info("{} OrderOutboxMessage sent to message bus!", orderOutboxMessages.size());
    }
  }
}
