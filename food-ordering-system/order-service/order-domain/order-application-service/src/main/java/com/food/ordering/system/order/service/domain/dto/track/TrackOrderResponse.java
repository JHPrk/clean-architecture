package com.food.ordering.system.order.service.domain.dto.track;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public final class TrackOrderResponse {

  private final @NotNull UUID orderTrackingId;
  private final @NotNull OrderStatus orderStatus;
  private final List<String> failureMessages;
}
