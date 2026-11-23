package org.spring.divas.order.feature;

import com.zaxxer.hikari.HikariDataSource;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("toxiproxy")
public abstract class AbstractToxiproxyIntegrationTest {

    private static final int PROXY_PORT = 8666;
    private static final Network NETWORK = Network.newNetwork();

    protected static final PostgreSQLContainer POSTGRES
            = new PostgreSQLContainer("postgres:17-alpine")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres");

    protected static final ToxiproxyContainer TOXIPROXY =
            new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.12.0")
            .withNetwork(NETWORK);

    protected static final Proxy POSTGRES_PROXY = startContainersAndCreateProxy();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/%s?socketTimeout=1".formatted(
                TOXIPROXY.getHost(),
                TOXIPROXY.getMappedPort(PROXY_PORT),
                POSTGRES.getDatabaseName()));
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterEach
    void resetProxyAndPool() throws Exception {
        for (var toxic : POSTGRES_PROXY.toxics().getAll()) {
            toxic.remove();
        }
        evictConnections();
    }

    protected void evictConnections() throws SQLException {
        dataSource.unwrap(HikariDataSource.class)
                .getHikariPoolMXBean()
                .softEvictConnections();
    }

    private static Proxy startContainersAndCreateProxy() {
        Startables.deepStart(Stream.of(POSTGRES, TOXIPROXY)).join();
        try {
            return new ToxiproxyClient(TOXIPROXY.getHost(), TOXIPROXY.getControlPort())
                    .createProxy("postgres", "0.0.0.0:" + PROXY_PORT, "postgres:5432");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create PostgreSQL proxy", exception);
        }
    }
}
