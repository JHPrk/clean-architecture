package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalRequestAvroModel;
import com.food.ordering.system.kafka.producer.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PayOrderKafkaMessagePublisher implements
    DomainEventPublisher<OrderPaidEvent> {

  private final OrderMessagingDataMapper orderMessagingDataMapper;
  private final OrderServiceConfigData orderServiceConfigData;
  private final KafkaProducer<String, RestaurantApprovalRequestAvroModel> kafkaProducer;
  private final KafkaMessageHelper orderKafkaMessageHelper;

  @Override
  public void publish(OrderPaidEvent domainEvent) {
    String orderId = domainEvent.getOrder().getId().getValue().toString();

    try {
      RestaurantApprovalRequestAvroModel restaurantApprovalRequestAvroModel = orderMessagingDataMapper
          .orderPaidEventToRestaurantApprovalRequestAvroModel(domainEvent);

      kafkaProducer.send(orderServiceConfigData.getRestaurantApprovalRequestTopicName(),
          orderId,
          restaurantApprovalRequestAvroModel,
          orderKafkaMessageHelper.getKafkaCallback(
              orderServiceConfigData.getRestaurantApprovalRequestTopicName(),
              restaurantApprovalRequestAvroModel, orderId, "RestaurantApprovalRequestAvroModel")
      );

      log.info("RestaurantApprovalRequestAvroModel sent to Kafka for order id: {}",
          restaurantApprovalRequestAvroModel.getOrderId());
    } catch (Exception e) {
      log.error(
          "Error while sending RestaurantApprovalRequestAvroModel message to kafka with order id: {}, error: {}",
          orderId, e.getMessage());
    }
  }
}
