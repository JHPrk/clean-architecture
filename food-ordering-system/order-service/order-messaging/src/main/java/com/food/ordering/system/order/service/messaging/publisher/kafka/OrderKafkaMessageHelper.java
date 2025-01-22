package com.food.ordering.system.order.service.messaging.publisher.kafka;

import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderKafkaMessageHelper {

  public <T> BiConsumer<SendResult<String, T>, Throwable> getKafkaCallback(
      String responseTopicName, T requestAvroModel, String orderId, String requestAvroModelName) {
    return (result, ex) -> {
      if (ex != null) {
        log.error("Error while sending {} message {} to topic {}",
            requestAvroModelName, requestAvroModel.toString(), responseTopicName, ex);
        return;
      }
      RecordMetadata metadata = result.getRecordMetadata();
      log.info(
          "Received successful response from kafka for order id: {} Topic: {} Partition: {} Offset: {} Timestamp: {}",
          orderId,
          metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp());
    };
  }
}
