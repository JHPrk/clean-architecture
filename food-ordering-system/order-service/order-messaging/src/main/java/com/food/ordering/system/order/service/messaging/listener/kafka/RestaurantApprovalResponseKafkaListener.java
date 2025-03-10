package com.food.ordering.system.order.service.messaging.listener.kafka;

import static com.food.ordering.system.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMETER;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.OrderApprovalStatus;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalResponseAvroModel;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.ports.input.message.listener.restaurantapproval.RestaurantApprovalResponseMessageListener;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class RestaurantApprovalResponseKafkaListener implements
    KafkaConsumer<RestaurantApprovalResponseAvroModel> {

  private final RestaurantApprovalResponseMessageListener restaurantApprovalResponseMessageListener;
  private final OrderMessagingDataMapper orderMessagingDataMapper;

  @Override
  @KafkaListener(id = "${kafka-consumer-config.restaurant-approval-consumer-group-id}",
      topics = "${order-service.restaurant-approval-response-topic-name}")
  public void receive(@Payload List<RestaurantApprovalResponseAvroModel> messages,
      @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
      @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
      @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
    log.info(
        "{} number of restaurant approval responses received with keys {}, partitions {}, offsets {}",
        messages.size(), keys.toString(), partitions.toString(), offsets.toString());

    messages.forEach(restaurantApprovalResponseAvroModel -> {
      try {
        if (OrderApprovalStatus.APPROVED
            == restaurantApprovalResponseAvroModel.getOrderApprovalStatus()) {
          log.info("Processing approved order for order id: {}",
              restaurantApprovalResponseAvroModel.getOrderId());
          restaurantApprovalResponseMessageListener.orderApproved(
              orderMessagingDataMapper.approvalResponseAvroModelToApprovalResponse(
                  restaurantApprovalResponseAvroModel));
        } else if (OrderApprovalStatus.REJECTED
            == restaurantApprovalResponseAvroModel.getOrderApprovalStatus()) {
          log.info("Processing rejected order for order id: {} with failure messages: {}",
              restaurantApprovalResponseAvroModel.getOrderId(),
              String.join(FAILURE_MESSAGE_DELIMETER,
                  restaurantApprovalResponseAvroModel.getFailureMessages()));
          restaurantApprovalResponseMessageListener.orderRejected(
              orderMessagingDataMapper.approvalResponseAvroModelToApprovalResponse(
                  restaurantApprovalResponseAvroModel));
        }
      } catch (
          OptimisticLockingFailureException e) {
        // 낙관적 잠금으로 인해 실패한 건은 재시도 하지 않아도 됨. 왜냐하면 동일 처리를 동시에 해야지만 발생하기 때문에 이런 케이스는 이미 다른 스레드가 처리를 완료한 경우임.
        // 따라서 Exception 을 발생시키지 않게 하여 카프카 프로듀서가 메시지를 재발행하는 것을 방지함
        log.error(
            "Caught Optimistic locking exception in RestaurantApprovalResponseKafkaListener for order id: {}",
            restaurantApprovalResponseAvroModel.getOrderId());
      } catch (
          OrderNotFoundException e) {
        // 주문이 존재하지 않아서 처리가 불가능한 경우는 재시도해도 의미가 없음
        log.error("No order found for order id: {}",
            restaurantApprovalResponseAvroModel.getOrderId());
      }
    });
  }
}
