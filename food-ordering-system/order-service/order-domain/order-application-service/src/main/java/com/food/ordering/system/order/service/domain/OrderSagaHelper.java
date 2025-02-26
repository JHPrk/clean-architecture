package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.saga.SagaStatus;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderSagaHelper {

  private final OrderRepository orderRepository;

  public OrderSagaHelper(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public Order findOrder(String orderId) {
    Optional<Order> orderResponse = orderRepository.findById(new OrderId(UUID.fromString(orderId)));
    if (orderResponse.isEmpty()) {
      log.error("Order with id: {} could not be found!", orderId);
      throw new OrderNotFoundException(
          MessageFormat.format("Order with id: {0} could not be found!", orderId));
    }
    return orderResponse.get();
  }

  public void saveOrder(Order order) {
    orderRepository.save(order);
  }

  public SagaStatus orderStatusToSagaStatus(OrderStatus orderStatus) {
    switch (orderStatus) {
      case PAID -> {
        return SagaStatus.PROCESSING;
      }
      case APPROVED -> {
        return SagaStatus.SUCCEEDED;
      }
      case CANCELLING -> {
        return SagaStatus.COMPENSATING;
      }
      case CANCELLED -> {
        return SagaStatus.COMPENSATED;
      }
      default -> {
        return SagaStatus.STARTED;
      }
    }
  }
}
