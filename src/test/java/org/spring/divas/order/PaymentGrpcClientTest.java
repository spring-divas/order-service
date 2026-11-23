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

import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    @DisplayName("processRealtimePayment handles null userId, email and items gracefully")
    void testProcessRealtimePaymentWithNullFieldsAndNullItems() throws Exception {
        Order order = Order.builder()
                .id(201L)
                .userId(null)
                .email(null)
                .items(null)
                .build();
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePayment(order, false);
        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PAID);
    }

    @Test
    @DisplayName("processRealtimePayment handles empty items list")
    void testProcessRealtimePaymentWithEmptyItems() throws Exception {
        Order order = Order.builder()
                .id(202L)
                .userId(5L)
                .email("user@divas.ua")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePayment(order, false);
        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PAID);
    }

    @Test
    @DisplayName("sendOrderCancellation with null paymentId and reason uses default values")
    void testSendOrderCancellationWithNulls() throws Exception {
        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.sendOrderCancellation(203L, null, null);

        PaymentStreamResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_CANCELLED);
    }

    @Test
    @DisplayName("sendOrderCancellation completes exceptionally when server reports error")
    void testSendOrderCancellationError() {
        testServerService.setCancelError(true);

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.sendOrderCancellation(204L, 999L, "Cancel order");

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @DisplayName("processRealtimePayment completes exceptionally when server returns direct error")
    void testProcessRealtimePaymentDirectError() {
        Order order = createSampleOrder(206L);
        testServerService.setImmediateError(Status.INTERNAL.withDescription("Direct internal error"));

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePayment(order, false);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(io.grpc.StatusRuntimeException.class);
    }

    @Test
    @DisplayName("Retry Policy: non-UNAVAILABLE error is not retried and fails immediately")
    void testProcessRealtimePaymentWithRetryNonUnavailable() {
        Order order = createSampleOrder(207L);
        testServerService.setImmediateError(Status.INVALID_ARGUMENT.withDescription("Invalid payload"));

        CompletableFuture<PaymentStreamResponse> future =
                paymentGrpcClient.processRealtimePaymentWithRetry(order, false);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(io.grpc.StatusRuntimeException.class)
                .satisfies(ex -> {
                    io.grpc.StatusRuntimeException sre = (io.grpc.StatusRuntimeException) ex.getCause();
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                });

        assertThat(testServerService.getStreamOpenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test isUnavailableError helper branch coverage")
    void testIsUnavailableErrorBranches() {
        Boolean res1 = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "isUnavailableError",
                new io.grpc.StatusRuntimeException(Status.UNAVAILABLE));
        assertThat(res1).isTrue();

        Boolean res2 = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "isUnavailableError",
                new io.grpc.StatusRuntimeException(Status.NOT_FOUND));
        assertThat(res2).isFalse();

        Boolean res3 = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "isUnavailableError",
                new RuntimeException(new io.grpc.StatusRuntimeException(Status.UNAVAILABLE)));
        assertThat(res3).isTrue();

        Boolean res4 = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "isUnavailableError",
                new RuntimeException(new io.grpc.StatusRuntimeException(Status.PERMISSION_DENIED)));
        assertThat(res4).isFalse();

        Boolean res5 = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "isUnavailableError",
                new RuntimeException("no cause"));
        assertThat(res5).isFalse();

        Boolean res6 = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "isUnavailableError",
                new RuntimeException(new IllegalArgumentException("other cause")));
        assertThat(res6).isFalse();
    }

    @Test
    @DisplayName("Test forwardResult helper branch coverage")
    void testForwardResultBranches() throws Exception {
        CompletableFuture<PaymentStreamResponse> targetSuccess = new CompletableFuture<>();
        PaymentStreamResponse sampleResponse = PaymentStreamResponse.newBuilder()
                .setStatus(org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PAID)
                .build();

        ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "forwardResult", targetSuccess, sampleResponse, null);
        assertThat(targetSuccess.get(1, TimeUnit.SECONDS)).isEqualTo(sampleResponse);

        CompletableFuture<PaymentStreamResponse> targetFailure = new CompletableFuture<>();
        Throwable sampleError = new RuntimeException("Test error");

        ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "forwardResult", targetFailure, null, sampleError);
        assertThat(targetFailure).isCompletedExceptionally();
    }

    @Test
    @DisplayName("Test calculateTotal helper branch coverage")
    void testCalculateTotalBranches() {
        Order nullItemsOrder = Order.builder().items(null).build();
        BigDecimal totalNull = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "calculateTotal", nullItemsOrder);
        assertThat(totalNull).isEqualTo(BigDecimal.ZERO);

        Order emptyItemsOrder = Order.builder().items(new ArrayList<>()).build();
        BigDecimal totalEmpty = ReflectionTestUtils.invokeMethod(
                paymentGrpcClient, "calculateTotal", emptyItemsOrder);
        assertThat(totalEmpty).isEqualTo(BigDecimal.ZERO);
    }

    /**
     * Test mock for PaymentService gRPC bidirectional stream server.
     */
    private static class TestPaymentServerServiceImpl
            extends PaymentServiceGrpc.PaymentServiceImplBase {

        private final AtomicInteger streamOpenCount = new AtomicInteger(0);
        private volatile int failuresBeforeSuccess = 0;
        private volatile Status immediateError;
        private volatile boolean sendUnknownStatus;
        private volatile boolean cancelError;

        void setFailuresBeforeSuccess(int count) {
            this.failuresBeforeSuccess = count;
        }

        void setImmediateError(Status status) {
            this.immediateError = status;
        }

        void setSendUnknownStatus(boolean sendUnknownStatus) {
            this.sendUnknownStatus = sendUnknownStatus;
        }

        void setCancelError(boolean cancelError) {
            this.cancelError = cancelError;
        }

        int getStreamOpenCount() {
            return streamOpenCount.get();
        }

        @Override
        public StreamObserver<PaymentStreamRequest> processPaymentStream(
                StreamObserver<PaymentStreamResponse> responseObserver) {

            int currentAttempt = streamOpenCount.incrementAndGet();

            if (immediateError != null) {
                responseObserver.onError(immediateError.asRuntimeException());
                return new StreamObserver<>() {
                    @Override
                    public void onNext(PaymentStreamRequest value) {
                        // empty
                    }

                    @Override
                    public void onError(Throwable t) {
                        // empty
                    }

                    @Override
                    public void onCompleted() {
                        // empty
                    }
                };
            }

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

                        if (sendUnknownStatus) {
                            PaymentStreamResponse unknown = PaymentStreamResponse.newBuilder()
                                    .setStatus(org.spring.divas.payment.v1.PaymentStatus.UNRECOGNIZED)
                                    .setMessage("Unknown status")
                                    .build();
                            responseObserver.onNext(unknown);
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
                        if (cancelError) {
                            responseObserver.onError(Status.INTERNAL
                                    .withDescription("Cancellation error")
                                    .asRuntimeException());
                            return;
                        }
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
                    // Test in-process mock server ignores client stream errors
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
