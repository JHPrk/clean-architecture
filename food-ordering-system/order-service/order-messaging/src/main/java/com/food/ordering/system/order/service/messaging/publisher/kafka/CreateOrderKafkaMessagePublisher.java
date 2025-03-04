package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.kafka.producer.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class CreateOrderKafkaMessagePublisher implements
    DomainEventPublisher<OrderCreatedEvent> {

  private final OrderMessagingDataMapper orderMessagingDataMapper;
  private final OrderServiceConfigData orderServiceConfigData;
  private final KafkaProducer<String, PaymentRequestAvroModel> kafkaProducer;
  private final KafkaMessageHelper orderKafkaMessageHelper;

  @Override
  public void publish(OrderCreatedEvent domainEvent) {
    String orderId = domainEvent.getOrder().getId().getValue().toString();
    log.info("Received OrderCreatedEvent for order id : {}", orderId);

    try {
      PaymentRequestAvroModel paymentRequestAvroModel = orderMessagingDataMapper
          .orderCreatedEventToPaymentRequestAvroModel(domainEvent);

      kafkaProducer.send(orderServiceConfigData.getPaymentRequestTopicName(), orderId,
          paymentRequestAvroModel,
          orderKafkaMessageHelper.getKafkaCallback(
              orderServiceConfigData.getPaymentResponseTopicName(),
              paymentRequestAvroModel,
              orderId,
              "PaymentRequestAvroModel"));

      log.info("PaymentRequestAvroModel sent to Kafka for order id: {}",
          paymentRequestAvroModel.getOrderId());
    } catch (Exception e) {
      log.error(
          "Error while sending PaymentRequestAvroModel message to kafka with order id: {}, error: {}",
          orderId, e.getMessage());
    }
  }
}
