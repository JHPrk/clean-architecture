package com.food.ordering.system.order.service.domain;

import static com.food.ordering.system.domain.DomainConstants.UTC;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.dto.message.RestaurantApprovalResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.scheduler.approval.ApprovalOutboxHelper;
import com.food.ordering.system.order.service.domain.outbox.scheduler.payment.PaymentOutboxHelper;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.SagaStep;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class OrderApprovalSaga implements
    SagaStep<RestaurantApprovalResponse> {

  private final OrderDomainService orderDomainService;
  private final OrderSagaHelper orderSagaHelper;
  private final ApprovalOutboxHelper approvalOutboxHelper;
  private final PaymentOutboxHelper paymentOutboxHelper;
  private final OrderDataMapper orderDataMapper;

  @Override
  @Transactional
  public void process(RestaurantApprovalResponse restaurantApprovalResponse) {

    Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageResponse =
        approvalOutboxHelper.getOrderApprovalOutboxMessageBySagaIdAndSagaStatus(
            UUID.fromString(restaurantApprovalResponse.getSagaId()),
            SagaStatus.PROCESSING
        );

    if (orderApprovalOutboxMessageResponse.isEmpty()) {
      log.info("An outbox message with saga id: {} is already processed!",
          restaurantApprovalResponse.getSagaId());
      return;
    }

    OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageResponse.get();

    Order order = approvalOrder(restaurantApprovalResponse);

    SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());

    approvalOutboxHelper.save(getUpdatedApprovalOutboxMessage(orderApprovalOutboxMessage,
        order.getOrderStatus(), sagaStatus));

    paymentOutboxHelper.save(getUpdatedPaymentOutboxMessage(restaurantApprovalResponse.getSagaId(),
        order.getOrderStatus(), sagaStatus));
    log.info("Order with id :{} is approved", order.getId().getValue());
  }

  private OrderPaymentOutboxMessage getUpdatedPaymentOutboxMessage(String sagaId,
      OrderStatus orderStatus, SagaStatus sagaStatus) {
    Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
        UUID.fromString(sagaId), SagaStatus.PROCESSING);

    if (orderPaymentOutboxMessageResponse.isEmpty()) {
      throw new OrderDomainException(MessageFormat.format(
          "Payment outbox message could not be found in {0} status!",
          SagaStatus.PROCESSING.name()));
    }

    OrderPaymentOutboxMessage orderPaymentOutboxMessage = orderPaymentOutboxMessageResponse.get();
    orderPaymentOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(UTC)));
    orderPaymentOutboxMessage.setOrderStatus(orderStatus);
    orderPaymentOutboxMessage.setSagaStatus(sagaStatus);
    return orderPaymentOutboxMessage;
  }

  private OrderApprovalOutboxMessage getUpdatedApprovalOutboxMessage(
      OrderApprovalOutboxMessage orderApprovalOutboxMessage, OrderStatus orderStatus,
      SagaStatus sagaStatus) {
    orderApprovalOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(UTC)));
    orderApprovalOutboxMessage.setOrderStatus(orderStatus);
    orderApprovalOutboxMessage.setSagaStatus(sagaStatus);
    return orderApprovalOutboxMessage;
  }

  private Order approvalOrder(RestaurantApprovalResponse restaurantApprovalResponse) {
    log.info("Approving order with id: {}", restaurantApprovalResponse.getOrderId());
    Order order = orderSagaHelper.findOrder(restaurantApprovalResponse.getOrderId());
    orderDomainService.approveOrder(order);
    orderSagaHelper.saveOrder(order);
    return order;
  }

  @Override
  @Transactional
  public void rollback(RestaurantApprovalResponse restaurantApprovalResponse) {

    Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageResponse = approvalOutboxHelper.getOrderApprovalOutboxMessageBySagaIdAndSagaStatus(
        UUID.fromString(restaurantApprovalResponse.getSagaId()),
        SagaStatus.PROCESSING
    );

    if (orderApprovalOutboxMessageResponse.isEmpty()) {
      log.info("An outbox message with saga id: {} is already roll backed!",
          restaurantApprovalResponse.getSagaId());
      return;
    }

    OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageResponse.get();

    OrderCancelledEvent orderCancelledEvent = rollbackOrder(restaurantApprovalResponse);

    SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(
        orderCancelledEvent.getOrder().getOrderStatus());

    approvalOutboxHelper.save(getUpdatedApprovalOutboxMessage(orderApprovalOutboxMessage,
        orderCancelledEvent.getOrder().getOrderStatus(), sagaStatus));

    paymentOutboxHelper.savePaymentOutboxMessage(
        orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(orderCancelledEvent),
        orderCancelledEvent.getOrder().getOrderStatus(),
        sagaStatus,
        OutboxStatus.STARTED,
        UUID.fromString(restaurantApprovalResponse.getSagaId())
    );

    log.info("Order with id: {} is cancelling",
        orderCancelledEvent.getOrder().getId().getValue());
  }

  private OrderCancelledEvent rollbackOrder(RestaurantApprovalResponse restaurantApprovalResponse) {
    log.info("Cancelling order with id: {}", restaurantApprovalResponse.getOrderId());
    Order order = orderSagaHelper.findOrder(restaurantApprovalResponse.getOrderId());
    OrderCancelledEvent orderCancelledEvent = orderDomainService.cancelOrderPayment(order,
        restaurantApprovalResponse.getFailureMessages());
    orderSagaHelper.saveOrder(order);
    return orderCancelledEvent;
  }
}
