package org.spring.divas.order;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderRepository;
import org.spring.divas.order.feature.order.OrderStatus;
import org.spring.divas.order.feature.orderitem.OrderItem;
import org.spring.divas.order.feature.payment.PaymentGrpcClient;
import org.spring.divas.payment.v1.OrderCancellation;
import org.spring.divas.payment.v1.OrderPaymentInfo;
import org.spring.divas.payment.v1.PaymentConfirmation;
import org.spring.divas.payment.v1.PaymentServiceGrpc;
import org.spring.divas.payment.v1.PaymentStreamRequest;
import org.spring.divas.payment.v1.PaymentStreamResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGrpcClientTest {

    private String serverName;
    private Server inProcessServer;
    private ManagedChannel inProcessChannel;

    private TestPaymentServerServiceImpl testServerService;
    private PaymentGrpcClient paymentGrpcClient;

    @Mock
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() throws IOException {
        serverName = InProcessServerBuilder.generateName();
        testServerService = new TestPaymentServerServiceImpl();

        inProcessServer = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(testServerService)
                .build()
                .start();

        inProcessChannel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        PaymentServiceGrpc.PaymentServiceStub asyncStub = PaymentServiceGrpc.newStub(inProcessChannel);
        paymentGrpcClient = new PaymentGrpcClient(asyncStub, orderRepository);
    }

    @AfterEach
    void tearDown() {
        if (inProcessChannel != null) {
            inProcessChannel.shutdownNow();
        }
        if (inProcessServer != null) {
            inProcessServer.shutdownNow();
        }
    }

    private Order createSampleOrder(Long orderId) {
        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .firstName("Taras")
                .lastName("Shevchenko")
                .email("taras@divas.ua")
                .venueId(10L)
                .tableId(2L)
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .dishId(5L)
                .name("Borscht")
                .quantity(2)
                .price(new BigDecimal("15.50"))
                .build();

        order.getItems().add(item);
        return order;
    }

    @Test
    @DisplayName("Bidirectional Stream: successful payment lifecycle (steps 1-6)")
    void testProcessRealtimePaymentSuccess() throws Exception {
        Order order = createSampleOrder(101L);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePayment(order, false);

        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, atLeastOnce()).save(order);
    }

    @Test
    @DisplayName("Bidirectional Stream: payment failure scenario (FAILED)")
    void testProcessRealtimePaymentFailure() throws Exception {
        Order order = createSampleOrder(102L);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePayment(order, true);

        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_FAILED);
        assertThat(response.hasFailureReason()).isTrue();
        assertThat(response.getFailureReason()).contains("Insufficient funds");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(orderRepository, atLeastOnce()).save(order);
    }

    @Test
    @DisplayName("Bidirectional Stream: sending order cancellation (step 7 CANCELLED)")
    void testSendOrderCancellation() throws Exception {
        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.sendOrderCancellation(103L, 999L, "Customer cancelled");

        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_CANCELLED);
        assertThat(response.getOrderId()).isEqualTo(103L);
    }

    @Test
    @DisplayName("Retry Policy: stream recovers after 2 UNAVAILABLE failures (3 attempts total)")
    void testProcessRealtimePaymentWithRetrySuccessAfterTransientUnavailable() throws Exception {
        Order order = createSampleOrder(104L);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // First 2 attempts end with UNAVAILABLE, 3rd attempt succeeds
        testServerService.setFailuresBeforeSuccess(2);

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePaymentWithRetry(order, false);

        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(testServerService.getStreamOpenCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Retry Policy: retry exhaustion after 3 attempts on persistent UNAVAILABLE")
    void testProcessRealtimePaymentWithRetryExhaustionOnPersistentUnavailable() {
        Order order = createSampleOrder(105L);

        // Server persistently unavailable
        testServerService.setFailuresBeforeSuccess(10);

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePaymentWithRetry(order, false);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(io.grpc.StatusRuntimeException.class)
                .satisfies(ex -> {
                    io.grpc.StatusRuntimeException sre =
                            (io.grpc.StatusRuntimeException) ex.getCause();
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
                });

        assertThat(testServerService.getStreamOpenCount()).isEqualTo(3);
    }

    /**
     * Test mock for PaymentService gRPC bidirectional stream server.
     */
    private static class TestPaymentServerServiceImpl
            extends PaymentServiceGrpc.PaymentServiceImplBase {

        private final AtomicInteger streamOpenCount = new AtomicInteger(0);
        private volatile int failuresBeforeSuccess = 0;

        void setFailuresBeforeSuccess(int count) {
            this.failuresBeforeSuccess = count;
        }

        int getStreamOpenCount() {
            return streamOpenCount.get();
        }

        @Override
        public StreamObserver<PaymentStreamRequest> processPaymentStream(
            StreamObserver<PaymentStreamResponse> responseObserver) {

            int currentAttempt = streamOpenCount.incrementAndGet();

            return new StreamObserver<>() {
                private Long orderId;
                private boolean simulateFailure;

                @Override
                public void onNext(PaymentStreamRequest request) {
                    if (request.hasOrderInfo()) {
                        // Check if server is currently simulating unavailable
                        if (currentAttempt <= failuresBeforeSuccess) {
                            responseObserver.onError(Status.UNAVAILABLE
                                    .withDescription("Payment server temporarily unavailable")
                                    .asRuntimeException());
                            return;
                        }

                        OrderPaymentInfo info = request.getOrderInfo();
                        this.orderId = info.getOrderId();
                        this.simulateFailure = info.getSimulateFailure();

                        // Step 3: send PENDING
                        PaymentStreamResponse pending = PaymentStreamResponse.newBuilder()
                                .setPaymentId(501L)
                                .setOrderId(orderId)
                                .setStatus(org.spring.divas.payment.v1.PaymentStatus
                                        .PAYMENT_STATUS_PENDING)
                                .setMessage("Payment created, awaiting confirmation.")
                                .setAttemptNumber(currentAttempt)
                                .setRequiresConfirmation(true)
                                .build();
                        responseObserver.onNext(pending);

                    } else if (request.hasConfirmation()) {
                        PaymentConfirmation confirmation = request.getConfirmation();
                        if (simulateFailure) {
                            // Step 5: send FAILED
                            PaymentStreamResponse failed = PaymentStreamResponse.newBuilder()
                                    .setPaymentId(confirmation.getPaymentId())
                                    .setOrderId(orderId)
                                    .setStatus(org.spring.divas.payment.v1.PaymentStatus
                                            .PAYMENT_STATUS_FAILED)
                                    .setFailureReason("Insufficient funds on balance")
                                    .setMessage("Payment declined")
                                    .setAttemptNumber(currentAttempt)
                                    .build();
                            responseObserver.onNext(failed);
                        } else {
                            // Step 5: send PAID
                            PaymentStreamResponse paid = PaymentStreamResponse.newBuilder()
                                    .setPaymentId(confirmation.getPaymentId())
                                    .setOrderId(orderId)
                                    .setStatus(org.spring.divas.payment.v1.PaymentStatus
                                            .PAYMENT_STATUS_PAID)
                                    .setMessage("Payment successfully confirmed")
                                    .setAttemptNumber(currentAttempt)
                                    .build();
                            responseObserver.onNext(paid);
                        }
                    } else if (request.hasCancellation()) {
                        OrderCancellation cancel = request.getCancellation();
                        PaymentStreamResponse cancelled = PaymentStreamResponse.newBuilder()
                                .setPaymentId(cancel.getPaymentId())
                                .setOrderId(cancel.getOrderId())
                                .setStatus(org.spring.divas.payment.v1.PaymentStatus
                                        .PAYMENT_STATUS_CANCELLED)
                                .setMessage("Payment cancelled")
                                .setAttemptNumber(currentAttempt)
                                .build();
                        responseObserver.onNext(cancelled);
                    }
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
