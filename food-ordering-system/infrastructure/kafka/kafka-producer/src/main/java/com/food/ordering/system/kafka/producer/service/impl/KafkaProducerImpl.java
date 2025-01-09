package com.food.ordering.system.kafka.producer.service.impl;

import com.food.ordering.system.kafka.producer.exception.KafkaProducerException;
import com.food.ordering.system.kafka.producer.service.KafkaProducer;
import jakarta.annotation.PreDestroy;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
@AllArgsConstructor
public class KafkaProducerImpl<K extends Serializable, V extends SpecificRecordBase> implements
    KafkaProducer<K, V> {

  private final KafkaTemplate<K, V> kafkaTemplate;

  @Override
  public void send(String topicName, K key, V message,
      BiConsumer<SendResult<K, V>, Throwable> callback) {
    log.info("Sending message={} to topic={}", message, topicName);
    try {
      CompletableFuture<SendResult<K, V>> kafkaResultFuture = kafkaTemplate.send(topicName, key,
          message);
      kafkaResultFuture.whenComplete(callback);

    } catch (KafkaException e) {
      log.error("Error on kafka producer with key: {}, message: {} and exception: {}", key, message,
          e.getMessage());
      throw new KafkaProducerException(MessageFormat.format(
          "Error on kafka producer with key: {0}, message: {1} and exception: {2}", key, message,
          e.getMessage()));
    }
  }

  @PreDestroy
  public void close() {
    if (!ObjectUtils.isEmpty(kafkaTemplate)) {
      log.info("Closing kafka producer!");
      kafkaTemplate.destroy();
    }
  }
}
