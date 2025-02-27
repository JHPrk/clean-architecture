package com.food.ordering.system.order.service.domain.outbox.scheduler.payment;

import static com.food.ordering.system.saga.order.SagaConstants.ORDER_SAGA_NAME;

import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.repository.PaymentOutboxRepository;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class PaymentOutboxHelper {

  private final PaymentOutboxRepository paymentOutboxRepository;

  public PaymentOutboxHelper(PaymentOutboxRepository paymentOutboxRepository) {
    this.paymentOutboxRepository = paymentOutboxRepository;
  }

  @Transactional(readOnly = true)
  public Optional<List<OrderPaymentOutboxMessage>> getPaymentOutboxMessageByOutboxStatusAndSagaStatus(
      OutboxStatus outboxStatus,
      SagaStatus... sagaStatus) {
    return paymentOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(
        ORDER_SAGA_NAME, outboxStatus, sagaStatus);
  }

  @Transactional(readOnly = true)
  public Optional<OrderPaymentOutboxMessage> getPaymentOutboxMessageBySagaIdAndSagaStatus(
      UUID sagaId, SagaStatus... sagaStatus) {
    return paymentOutboxRepository.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, sagaId,
        sagaStatus);
  }

  @Transactional
  public void save(OrderPaymentOutboxMessage orderPaymentOutboxMessage) {
    OrderPaymentOutboxMessage response = paymentOutboxRepository.save(orderPaymentOutboxMessage);
    if (Objects.isNull(response)) {
      log.error("Could not save OrderPaymentOutboxMessage with order id: {}",
          orderPaymentOutboxMessage.getId());
      throw new OrderDomainException(
          MessageFormat.format("Could not save OrderPaymentOutboxMessage with order id: {0}",
              orderPaymentOutboxMessage.getId()));
    }
    log.info("OrderPaymentOutboxMessage saved with outbox id: {}",
        orderPaymentOutboxMessage.getId());
  }

  @Transactional
  public void deletePaymentOutboxMessagesByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
      SagaStatus... sagaStatus) {
    paymentOutboxRepository.deleteByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME, outboxStatus,
        sagaStatus);
  }
}
