package com.food.ordering.system.restaurant.service.domain.dto;

import com.food.ordering.system.domain.valueobject.RestaurantOrderStatus;
import com.food.ordering.system.restaurant.service.domain.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RestaurantApprovalRequest {

  private String id;
  private String sagaId;
  private String restaurantId;
  private String orderId;
  private RestaurantOrderStatus restaurantOrderStatus;
  private List<Product> products;
  private BigDecimal price;
  private Instant createdAt;
}
