package org.spring.divas.order.feature.dish.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.dish.v1.DishChange;
import org.spring.divas.dish.v1.DishStreamServiceGrpc;
import org.spring.divas.order.feature.dish.DishRequestDto;
import org.spring.divas.order.feature.dish.DishService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.spring.divas.order.feature.dish.grpc.DishChangeFixture.borscht;
import static org.spring.divas.order.feature.dish.grpc.DishChangeFixture.idOf;
import static org.spring.divas.order.feature.dish.grpc.DishChangeFixture.nameOf;
import static org.spring.divas.order.feature.dish.grpc.DishChangeFixture.priceOf;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DishStreamGrpcClientTest {
    private Server server;
    private ManagedChannel channel;
    private TestDishStreamService streamService;

    @Mock
    private DishService dishService;

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        streamService = new TestDishStreamService();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(streamService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void savesDishReceivedFromStream() {
        DishChange change = borscht();
        streamService.setChange(change);
        DishStreamGrpcClient client = client();
        client.subscribeOnStartup();
        assertDishMatches(capturedDish(), change);
    }

    @Test
    void reconnectsAfterUnavailableServer() {
        streamService.setFailFirstSubscription(true);
        DishChange change = borscht();
        streamService.setChange(change);
        DishStreamGrpcClient client = client();
        client.subscribeOnStartup();
        client.reconnect();
        assertThat(streamService.subscriptionCount()).isEqualTo(2);
        assertDishMatches(capturedDish(), change);
    }

    private void assertDishMatches(DishRequestDto dish, DishChange change) {
        assertThat(dish.getDishId()).isEqualTo(idOf(change));
        assertThat(dish.getName()).isEqualTo(nameOf(change));
        assertThat(dish.getPrice()).isEqualByComparingTo(priceOf(change));
    }

    private DishStreamGrpcClient client() {
        return new DishStreamGrpcClient(
                DishStreamServiceGrpc.newStub(channel),
                dishService,
                new DishProtoMapper()
        );
    }

    private DishRequestDto capturedDish() {
        ArgumentCaptor<DishRequestDto> captor = ArgumentCaptor.forClass(DishRequestDto.class);
        verify(dishService).upsert(captor.capture());
        return captor.getValue();
    }
}
