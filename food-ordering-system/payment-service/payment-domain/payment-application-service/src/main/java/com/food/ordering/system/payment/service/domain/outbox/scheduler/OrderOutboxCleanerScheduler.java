package com.food.ordering.system.payment.service.domain.outbox.scheduler;

import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class OrderOutboxCleanerScheduler implements OutboxScheduler {

  private final OrderOutboxHelper orderOutboxHelper;

  @Override
  @Transactional
  @Scheduled(cron = "@midnight")
  public void processOutboxMessage() {
    Optional<List<OrderOutboxMessage>> outboxMessageResponse = orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(
        OutboxStatus.COMPLETED);

    if (outboxMessageResponse.isPresent() && outboxMessageResponse.get().size() > 0) {
      List<OrderOutboxMessage> orderOutboxMessages = outboxMessageResponse.get();
      log.info("Received {} OrderOutboxMessage for clean-up!",
          orderOutboxMessages.size());
      orderOutboxHelper.deleteOrderOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);
      log.info("Deleted {} OrderOutboxMessage!", orderOutboxMessages.size());
    }
  }
}
