package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.kafka.producer.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.PaymentRequestMessagePublisher;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.food.ordering.system.outbox.OutboxStatus;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class OrderPaymentEventKafkaPublisher implements PaymentRequestMessagePublisher {

  private final OrderMessagingDataMapper orderMessagingDataMapper;
  private final KafkaProducer<String, PaymentRequestAvroModel> kafkaProducer;
  private final OrderServiceConfigData orderServiceConfigData;
  private final KafkaMessageHelper kafkaMessageHelper;

  @Override
  public void publish(OrderPaymentOutboxMessage orderPaymentOutboxMessage,
      BiConsumer<OrderPaymentOutboxMessage, OutboxStatus> outboxCallback) {
    OrderPaymentEventPayload orderPaymentEventPayload = kafkaMessageHelper.getOrderEventPayload(
        orderPaymentOutboxMessage.getPayload(), OrderPaymentEventPayload.class);

    String sagaId = orderPaymentOutboxMessage.getSagaId().toString();
    log.info("Received OrderPaymentOutboxMessage for order id: {} and saga id: {}",
        orderPaymentEventPayload.getOrderId(), sagaId);

    try {
      PaymentRequestAvroModel paymentRequestAvroModel = orderMessagingDataMapper.orderPaymentEventPayloadToPaymentRequestAvroModel(
          sagaId, orderPaymentEventPayload);

      kafkaProducer.send(orderServiceConfigData.getPaymentRequestTopicName(), sagaId,
          paymentRequestAvroModel,
          kafkaMessageHelper.getKafkaCallback(orderServiceConfigData.getPaymentRequestTopicName(),
              paymentRequestAvroModel,
              orderPaymentOutboxMessage,
              outboxCallback,
              orderPaymentEventPayload.getOrderId(),
              "PaymentRequestAvroModel"));

      log.info("OrderPaymentEventPayload sent to Kafka for order id: {} and saga id: {}",
          orderPaymentEventPayload.getOrderId(), sagaId);
    } catch (Exception e) {
      log.error(
          "Error while sending OrderPaymentEventPayload to kafka with order id: {} and saga id: {}, error: {}",
          orderPaymentEventPayload.getOrderId(), sagaId, e.getMessage());

    }
  }
}
