package com.food.ordering.system.order.service.domain.entity;

import java.util.List;
import java.util.UUID;

import com.food.ordering.system.domain.entity.AggregateRoot;
import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.valueobject.OrderItemId;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import com.food.ordering.system.order.service.domain.valueobject.TrackingId;

public class Order extends AggregateRoot<OrderId> {
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final StreetAddress deliveryAddress;
    private final Money price;
    private final List<OrderItem> items;
    
    private TrackingId trackingId;
    private OrderStatus orderStatus;
    private List<String> failureMessages;

    public void initializeOrder(){
        setId(new OrderId(UUID.randomUUID()));
        trackingId = new TrackingId(UUID.randomUUID());
        orderStatus = OrderStatus.PENDING;
        initializeOrderItems();
    }

    public void validateOrder() {
        validateInitialOrder();
        validateTotalPrice();
        validateItemsPrice();
    }

    private void validateInitialOrder() {
        if (orderStatus != null || getId() != null){
            throw new OrderDomainException("Order is not in correct state for initialization!");
        }
    }

    private void validateTotalPrice() {
        if (price == null || !price.isGreaterThanZero()){
            throw new OrderDomainException("Total price must be greater than zero!");
        }
    }

    private void validateItemsPrice() {
        Money orderItemsTotal = items.stream().map(orderItem -> {
            validateItemPrice(orderItem);
            return orderItem.getSubTotal();
        }).reduce(Money.ZERO, Money::add);

        if(price.equals(orderItemsTotal)){
            throw new OrderDomainException("Total Price : " + price.getAmount() + " is not euqal to Order items total: " + orderItemsTotal.getAmount() + "!");
        }
    }

    private void validateItemPrice(OrderItem orderItem) {
        if(!orderItem.isPriceValid()){
            throw new OrderDomainException("Order item price : " + orderItem.getPrice().getAmount()
                + " is not valid for product " + orderItem.getProduct().getId().getValue());
        }

    }

    private void initializeOrderItems(){
        long itemID = 1;
        for (OrderItem orderItem : items){
            orderItem.initializeOrderItem(super.getId(), new OrderItemId(itemID++));
        }
    }
  

    private Order(Builder builder) {
        super.setId(builder.orderId);
        this.customerId = builder.customerId;
        this.restaurantId = builder.restaurantId;
        this.deliveryAddress = builder.deliveryAddress;
        this.price = builder.price;
        this.items = builder.items;
        this.trackingId = builder.trackingId;
        this.orderStatus = builder.orderStatus;
        this.failureMessages = builder.failureMessages;
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    public RestaurantId getRestaurantId() {
        return restaurantId;
    }
    public StreetAddress getDeliveryAddress() {
        return deliveryAddress;
    }
    public Money getPrice() {
        return price;
    }
    public List<OrderItem> getItems() {
        return items;
    }
    public TrackingId getTrackingId() {
        return trackingId;
    }
    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
    public List<String> getFailureMessages() {
        return failureMessages;
    }
    
    public static class Builder {
        private OrderId orderId;
        private CustomerId customerId;
        private RestaurantId restaurantId;
        private StreetAddress deliveryAddress;
        private Money price;
        private List<OrderItem> items;
        private TrackingId trackingId;
        private OrderStatus orderStatus;
        private List<String> failureMessages;

        private Builder(){}
        
        public Builder orderId(OrderId val) {
            orderId = val;
            return this;
        }
        
        public Builder customerId(CustomerId val) {
            customerId = val;
            return this;
        }
        
        public Builder restaurantId(RestaurantId val) {
            restaurantId = val;
            return this;
        }

        public Builder deliveryAddress(StreetAddress val) {
            deliveryAddress = val;
            return this;
        }

        public Builder price(Money val) {
            price = val;
            return this;
        }

        public Builder items(List<OrderItem> val) {
            items = val;
            return this;
        }

        public Builder trackingId(TrackingId val) {
            this.trackingId = val;
            return this;
        }

        public Builder orderStatus(OrderStatus val) {
            this.orderStatus = val;
            return this;
        }
        
        public Builder failureMessages(List<String> val) {
            this.failureMessages = val;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }

}
