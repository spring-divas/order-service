package org.spring.divas.order;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderRepository;
import org.spring.divas.order.feature.order.OrderStatus;
import org.spring.divas.order.feature.orderitem.OrderItem;
import org.spring.divas.payment.v1.PaymentConfirmation;
import org.spring.divas.payment.v1.PaymentServiceGrpc;
import org.spring.divas.payment.v1.PaymentStatus;
import org.spring.divas.payment.v1.PaymentStreamRequest;
import org.spring.divas.payment.v1.PaymentStreamResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtAuthenticationConfig.class)
class OrderControllerGrpcIntegrationTest {

    private static Server grpcServer;
    private static int grpcPort;

    private static final AtomicBoolean cancellationReceived = new AtomicBoolean(false);
    private static final AtomicBoolean paymentProcessed = new AtomicBoolean(false);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeAll
    static void startGrpcServer() throws IOException {
        grpcServer = ServerBuilder.forPort(0)
                .addService(new PaymentServiceGrpc.PaymentServiceImplBase() {
                    @Override
                    public StreamObserver<PaymentStreamRequest> processPaymentStream(
                            StreamObserver<PaymentStreamResponse> responseObserver) {
                        return new StreamObserver<>() {
                            private Long orderId;
                            private boolean simulateFailure;

                            @Override
                            public void onNext(PaymentStreamRequest request) {
                                if (request.hasOrderInfo()) {
                                    this.orderId = request.getOrderInfo().getOrderId();
                                    this.simulateFailure =
                                            request.getOrderInfo().getSimulateFailure();

                                    // Step 3: Payment returns PENDING
                                    responseObserver.onNext(PaymentStreamResponse.newBuilder()
                                            .setPaymentId(777L)
                                            .setOrderId(orderId)
                                            .setStatus(PaymentStatus.PAYMENT_STATUS_PENDING)
                                            .setMessage("Payment created")
                                            .setAttemptNumber(1)
                                            .setRequiresConfirmation(true)
                                            .build());

                                } else if (request.hasConfirmation()) {
                                    paymentProcessed.set(true);
                                    PaymentConfirmation confirm = request.getConfirmation();

                                    if (simulateFailure) {
                                        // Step 5: return FAILED
                                        responseObserver.onNext(PaymentStreamResponse.newBuilder()
                                                .setPaymentId(confirm.getPaymentId())
                                                .setOrderId(orderId)
                                                .setStatus(PaymentStatus.PAYMENT_STATUS_FAILED)
                                                .setFailureReason("Insufficient funds on balance")
                                                .setMessage("Payment declined")
                                                .build());
                                    } else {
                                        // Step 5: return PAID
                                        responseObserver.onNext(PaymentStreamResponse.newBuilder()
                                                .setPaymentId(confirm.getPaymentId())
                                                .setOrderId(orderId)
                                                .setStatus(PaymentStatus.PAYMENT_STATUS_PAID)
                                                .setMessage("Payment successfully confirmed")
                                                .build());
                                    }

                                } else if (request.hasCancellation()) {
                                    cancellationReceived.set(true);
                                    responseObserver.onNext(PaymentStreamResponse.newBuilder()
                                            .setPaymentId(777L)
                                            .setOrderId(request.getCancellation().getOrderId())
                                            .setStatus(PaymentStatus.PAYMENT_STATUS_CANCELLED)
                                            .setMessage("Payment cancelled")
                                            .build());
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                // Errors are not expected in test mock gRPC server
                            }

                            @Override
                            public void onCompleted() {
                                responseObserver.onCompleted();
                            }
                        };
                    }
                })
                .build()
                .start();

        grpcPort = grpcServer.getPort();
    }

    @AfterAll
    static void stopGrpcServer() {
        if (grpcServer != null) {
            grpcServer.shutdownNow();
        }
    }

    @DynamicPropertySource
    static void configureGrpcTarget(DynamicPropertyRegistry registry) {
        registry.add("PAYMENT_GRPC_TARGET", () -> "static://localhost:" + grpcPort);
    }

    @BeforeEach
    void setUp() {
        cancellationReceived.set(false);
        paymentProcessed.set(false);
    }

    private Order createAndSaveOrder() {
        Order order = Order.builder()
                .userId(1L)
                .firstName("Ivan")
                .lastName("Franko")
                .email("franko@divas.ua")
                .venueId(5L)
                .tableId(3L)
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .dishId(10L)
                .name("Varenyky")
                .quantity(2)
                .price(new BigDecimal("20.00"))
                .build();

        order.getItems().add(item);
        return orderRepository.save(order);
    }

    @Test
    @DisplayName("HTTP POST /order/{id}/pay -> gRPC payment -> order status becomes PAID")
    void testHttpPayOrderSuccessFlow() throws Exception {
        Order order = createAndSaveOrder();

        mockMvc.perform(post("/order/{id}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("PAID"));

        assertThat(paymentProcessed.get()).isTrue();

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("HTTP POST /order/{id}/pay?simulateFailure=true -> FAILED via gRPC -> PAYMENT_FAILED")
    void testHttpPayOrderFailureFlow() throws Exception {
        Order order = createAndSaveOrder();

        mockMvc.perform(post("/order/{id}/pay", order.getId())
                        .param("simulateFailure", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        assertThat(paymentProcessed.get()).isTrue();

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("HTTP PATCH /order/{id}/cancel -> cancellation via gRPC -> status CANCELLED")
    void testHttpCancelOrderFlow() throws Exception {
        Order order = createAndSaveOrder();

        mockMvc.perform(patch("/order/{id}/cancel", order.getId())
                        .param("reason", "Customer cancelled the order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(cancellationReceived.get()).isTrue();

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
