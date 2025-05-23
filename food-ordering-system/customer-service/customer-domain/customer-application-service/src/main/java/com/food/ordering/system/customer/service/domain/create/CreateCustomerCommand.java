package com.food.ordering.system.customer.service.domain.create;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateCustomerCommand {

  @NotNull
  private final UUID customerId;
  @NotNull
  private final String username;
  @NotNull
  private final String firstName;
  @NotNull
  private final String lastName;
}
