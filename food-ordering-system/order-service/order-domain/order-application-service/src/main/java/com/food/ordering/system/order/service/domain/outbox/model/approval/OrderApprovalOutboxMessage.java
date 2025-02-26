package com.food.ordering.system.order.service.domain.outbox.model.approval;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class OrderApprovalOutboxMessage {

  private UUID id;
  private UUID sagaId;
  private ZonedDateTime createdAt;
  @Setter
  private ZonedDateTime processedAt;
  private String type;
  private String payload;
  @Setter
  private SagaStatus sagaStatus;
  @Setter
  private OrderStatus orderStatus;
  @Setter
  private OutboxStatus outboxStatus;
  private int version;
}
