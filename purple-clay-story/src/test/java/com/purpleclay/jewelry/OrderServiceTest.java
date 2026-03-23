package com.purpleclay.jewelry;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.model.dto.CartDTOs;
import com.purpleclay.jewelry.model.entity.*;
import com.purpleclay.jewelry.model.enums.OrderStatus;
import com.purpleclay.jewelry.model.enums.Role;
import com.purpleclay.jewelry.repository.OrderRepository;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.repository.UserRepository;
import com.purpleclay.jewelry.service.CartService;
import com.purpleclay.jewelry.service.OrderService;
import com.purpleclay.jewelry.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;
    @Mock private ProductService productService;

    @InjectMocks private OrderService orderService;

    private User mockUser;
    private Product mockProduct;
    private CartDTOs.Cart cartWithItems;
    private CartDTOs.PlaceOrderRequest placeOrderRequest;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).name("Earrings").build();
        mockUser = User.builder().id(1L).name("Janani").email("test@test.com").role(Role.CUSTOMER).build();
        mockProduct = Product.builder()
            .id(1L).name("Gold Studs").price(new BigDecimal("599.00"))
            .stockQuantity(10).active(true).category(category).build();

        CartDTOs.CartItem item = new CartDTOs.CartItem(
            1L, "Gold Studs", null, new BigDecimal("599.00"), 2,
            "gold", "round", "stud", null
        );
        cartWithItems = new CartDTOs.Cart(1L, List.of(item), new BigDecimal("1198.00"));
        placeOrderRequest = new CartDTOs.PlaceOrderRequest("123, Anna Nagar, Chennai", null);
    }

    @Test
    void placeOrder_success_createsOrderAndClearsCart() {
        when(cartService.getCart(1L)).thenReturn(cartWithItems);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        doNothing().when(productService).decrementStock(anyLong(), anyInt());

        Order savedOrder = Order.builder()
            .id(1L).orderNumber("PCS-123").status(OrderStatus.PENDING)
            .user(mockUser).totalAmount(new BigDecimal("1198.00"))
            .shippingAddress("123, Anna Nagar, Chennai")
            .orderItems(new ArrayList<>()).build();

        OrderItem oi = OrderItem.builder().product(mockProduct).quantity(2)
            .unitPrice(new BigDecimal("599.00")).order(savedOrder).build();
        savedOrder.getOrderItems().add(oi);

        when(orderRepository.save(any())).thenReturn(savedOrder);

        CartDTOs.OrderResponse response = orderService.placeOrder(1L, placeOrderRequest);

        assertNotNull(response);
        assertEquals("PCS-123", response.orderNumber());
        assertEquals("PENDING", response.status());
        verify(cartService).clearCart(1L);
        verify(productService).decrementStock(1L, 2);
    }

    @Test
    void placeOrder_emptyCart_throwsBadRequest() {
        when(cartService.getCart(1L)).thenReturn(CartDTOs.Cart.empty(1L));
        assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, placeOrderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_missingAddress_throwsBadRequest() {
        when(cartService.getCart(1L)).thenReturn(cartWithItems);
        assertThrows(BadRequestException.class,
            () -> orderService.placeOrder(1L, new CartDTOs.PlaceOrderRequest("", null)));
    }

    @Test
    void placeOrder_insufficientStock_throwsBadRequest() {
        when(cartService.getCart(1L)).thenReturn(cartWithItems);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        mockProduct.setStockQuantity(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, placeOrderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_wrongUser_throwsBadRequest() {
        User otherUser = User.builder().id(99L).build();
        Order order = Order.builder().id(1L).user(otherUser).orderItems(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThrows(BadRequestException.class, () -> orderService.getOrderById(1L, 1L));
    }
}
