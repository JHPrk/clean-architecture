package com.food.ordering.system.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.outbox.OutboxStatus;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class KafkaMessageHelper {

  private final ObjectMapper objectMapper;

  public <T> T getOrderEventPayload(String payload, Class<T> outputType) {
    try {
      return objectMapper.readValue(payload, outputType);
    } catch (JsonProcessingException e) {
      log.error("Could not read {} object!", outputType.getName(), e);
      throw new OrderDomainException(
          MessageFormat.format("Could not read {0} object!", outputType.getName()), e);
    }
  }

  public <T, U> BiConsumer<SendResult<String, T>, Throwable> getKafkaCallback(
      String responseTopicName, T avroModel, U outboxMessage,
      BiConsumer<U, OutboxStatus> outboxCallback,
      String orderId,
      String avroModelName) {
    return (result, ex) -> {
      if (ex != null) {
        log.error("Error while sending {} with message {} and outbox type: {} to topic {}",
            avroModelName, avroModel.toString(), outboxMessage.getClass().getName(),
            responseTopicName, ex);
        outboxCallback.accept(outboxMessage, OutboxStatus.FAILED);
        return;
      }
      RecordMetadata metadata = result.getRecordMetadata();
      log.info(
          "Received successful response from kafka for order id: {} Topic: {} Partition: {} Offset: {} Timestamp: {}",
          orderId,
          metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp());
      outboxCallback.accept(outboxMessage, OutboxStatus.COMPLETED);
    };
  }
}
