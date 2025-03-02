package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RestaurantApprovalOutboxCleanerScheduler implements OutboxScheduler {

  private final ApprovalOutboxHelper approvalOutboxHelper;

  public RestaurantApprovalOutboxCleanerScheduler(ApprovalOutboxHelper approvalOutboxHelper) {
    this.approvalOutboxHelper = approvalOutboxHelper;
  }

  @Override
  @Scheduled(cron = "@midnight")
  public void processOutboxMessage() {
    Optional<List<OrderApprovalOutboxMessage>> outboxMessageResponse = approvalOutboxHelper.getOrderApprovalOutboxMessageByOutboxStatusAndSagaStatus(
        OutboxStatus.COMPLETED, SagaStatus.SUCCEEDED, SagaStatus.FAILED, SagaStatus.COMPENSATED);

    if (outboxMessageResponse.isPresent()) {
      List<OrderApprovalOutboxMessage> outboxMessages = outboxMessageResponse.get();
      log.info("Received {} OrderApprovalOutboxMessage for clean-up. The payloads: {}",
          outboxMessages.size(),
          outboxMessages.stream().map(OrderApprovalOutboxMessage::getPayload)
              .collect(Collectors.joining("\n")));
      approvalOutboxHelper.deleteApprovalOutboxMessageByOutboxStatusAndSagaStatus(
          OutboxStatus.COMPLETED,
          SagaStatus.SUCCEEDED,
          SagaStatus.FAILED,
          SagaStatus.COMPENSATED);
      log.info("{} OrderApprovalOutboxMessage deleted!", outboxMessages.size());
    }

  }
}
