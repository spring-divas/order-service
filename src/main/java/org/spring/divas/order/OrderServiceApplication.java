package org.spring.divas.order;

import org.spring.divas.payment.v1.PaymentServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ImportGrpcClients(target = "payment", types = {
        PaymentServiceGrpc.PaymentServiceBlockingStub.class,
        PaymentServiceGrpc.PaymentServiceStub.class
})
public class OrderServiceApplication {

    /**
     * Starts the Order Service application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
