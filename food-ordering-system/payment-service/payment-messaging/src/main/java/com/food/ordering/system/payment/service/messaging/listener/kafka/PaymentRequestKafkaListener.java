package com.food.ordering.system.payment.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.PaymentOrderStatus;
import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.payment.service.domain.exception.PaymentApplicationServiceException;
import com.food.ordering.system.payment.service.domain.exception.PaymentNotFoundException;
import com.food.ordering.system.payment.service.domain.ports.input.message.listener.PaymentRequestMessageListener;
import com.food.ordering.system.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentRequestKafkaListener implements KafkaConsumer<PaymentRequestAvroModel> {

  private final PaymentRequestMessageListener paymentRequestMessageListener;
  private final PaymentMessagingDataMapper paymentMessagingDataMapper;

  @Override
  @KafkaListener(id = "${kafka-consumer-config.payment-consumer-group-id}",
      topics = "${payment-service.payment-request-topic-name}")
  public void receive(@Payload List<PaymentRequestAvroModel> messages,
      @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
      @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
      @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
    log.info("{} number of payment requests received with keys:{}, partitions:{} and offsets: {}",
        messages.size(),
        keys.toString(),
        partitions.toString(),
        offsets.toString());

    messages.forEach(paymentRequestAvroModel -> {
      try {
        if (PaymentOrderStatus.PENDING == paymentRequestAvroModel.getPaymentOrderStatus()) {
          log.info("Processing payment for order id: {}", paymentRequestAvroModel.getOrderId());
          paymentRequestMessageListener.completePayment(paymentMessagingDataMapper
              .paymentRequestAvroModelToPaymentRequest(paymentRequestAvroModel));
        } else if (PaymentOrderStatus.CANCELLED
            == paymentRequestAvroModel.getPaymentOrderStatus()) {
          log.info("Cancelling payment for order id: {}", paymentRequestAvroModel.getOrderId());
          paymentRequestMessageListener.cancelPayment(paymentMessagingDataMapper
              .paymentRequestAvroModelToPaymentRequest(paymentRequestAvroModel));
        }
      } catch (DataAccessException e) {
        SQLException sqlException = (SQLException) e.getRootCause();
        if (sqlException.getSQLState() != null && PSQLState.UNIQUE_VIOLATION.getState()
            .equals(sqlException.getSQLState())) {
          // Unique 키 제약사항에 걸렸을 경우 아무런 처리를 하지 않음. (왜냐하면 한번 더 처리해봤자 똑같은 에러가 날 뿐임)
          log.error(
              "Caught Unique constraint exception with sql state: {} in PaymentRequestKafkaListener for order id: {}",
              sqlException.getSQLState(), paymentRequestAvroModel.getOrderId());
        } else {
          throw new PaymentApplicationServiceException(
              MessageFormat.format(
                  "Throwing DataAccessException in PaymentRequestKafkaListener: {0}",
                  e.getMessage()), e);
        }
      } catch (PaymentNotFoundException e) {
        // 결제정보가 존재하지 않아서 처리가 불가능한 경우는 재시도해도 의미가 없음.
        log.error("No Payment found for order id: {}", paymentRequestAvroModel.getOrderId(), e);
      }
    });

  }
}
