package com.food.ordering.system.payment.service.domain;


import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.payment.service.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.exception.PaymentApplicationServiceException;
import com.food.ordering.system.payment.service.domain.mapper.PaymentDataMapper;
import com.food.ordering.system.payment.service.domain.ports.output.repository.CreditEntryRepository;
import com.food.ordering.system.payment.service.domain.ports.output.repository.CreditHistoryRepository;
import com.food.ordering.system.payment.service.domain.ports.output.repository.PaymentRepository;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentRequestHelper {

  private final PaymentDomainService paymentDomainService;
  private final PaymentDataMapper paymentDataMapper;
  private final PaymentRepository paymentRepository;
  private final CreditEntryRepository creditEntryRepository;
  private final CreditHistoryRepository creditHistoryRepository;

  @Transactional
  public void persistPayment(PaymentRequest paymentRequest) {
    log.info("Received payment complete event for order id: {}", paymentRequest.getOrderId());
    Payment payment = paymentDataMapper.paymentRequestModelToPayment(paymentRequest);
    CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
    List<CreditHistory> creditHistories = getCreditHistory(payment.getCustomerId());
    List<String> failureMessages = new ArrayList<>();
    PaymentEvent paymentEvent = paymentDomainService.validateAndInitiatePayment(payment,
        creditEntry, creditHistories, failureMessages);
    paymentRepository.save(payment);
    if (failureMessages.isEmpty()) {
      creditEntryRepository.save(creditEntry);
      creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
    }
  }

  @Transactional
  public void persistCancelPayment(PaymentRequest paymentRequest) {
    log.info("Received payment cancel event for order id: {}", paymentRequest.getOrderId());
    Optional<Payment> paymentResponse = paymentRepository.findByOrderId(
        UUID.fromString(paymentRequest.getOrderId()));
    if (paymentResponse.isEmpty()) {
      log.error("Payment with order id: {} could not be found!", paymentRequest.getOrderId());
      throw new PaymentApplicationServiceException(
          MessageFormat.format("Payment with order id: {0} could not be found!",
              paymentRequest.getOrderId()));
    }
    Payment payment = paymentResponse.get();
    CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
    List<CreditHistory> creditHistories = getCreditHistory(payment.getCustomerId());
    List<String> failureMessages = new ArrayList<>();
    PaymentEvent paymentEvent = paymentDomainService.validateAndCancelPayment(payment, creditEntry,
        creditHistories, failureMessages);
    persistDbObjects(payment, creditEntry, creditHistories, failureMessages);
  }

  private CreditEntry getCreditEntry(CustomerId customerId) {
    Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
    if (creditEntry.isEmpty()) {
      log.error("Could not find credit entry for customer: {}", customerId.getValue());
      throw new PaymentApplicationServiceException(
          MessageFormat.format("Could not find credit entry for customer: {0}",
              customerId.getValue()));
    }
    return creditEntry.get();
  }

  private List<CreditHistory> getCreditHistory(CustomerId customerId) {
    Optional<List<CreditHistory>> creditHistories = creditHistoryRepository.findByCustomerId(
        customerId);
    if (creditHistories.isEmpty()) {
      log.error("Could not find credit history for customer: {}", customerId.getValue());
      throw new PaymentApplicationServiceException(
          MessageFormat.format("Could not find credit history for customer: {0}",
              customerId.getValue()));
    }
    return creditHistories.get();
  }

  private void persistDbObjects(Payment payment, CreditEntry creditEntry,
      List<CreditHistory> creditHistories, List<String> failureMessages) {
    paymentRepository.save(payment);
    if (failureMessages.isEmpty()) {
      creditEntryRepository.save(creditEntry);
      creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
    }
  }
}
