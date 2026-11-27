
package com.LB.loadbalancer.stategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.LB.loadbalancer.strategy.LeastResponseTimeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.LB.model.Server;
import com.LB.model.ServerMetrics;

class LeastResponseTimeStrategyTest {

    private LeastResponseTimeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LeastResponseTimeStrategy();
    }

    private Server createServer(String url, long responseTime) {
        Server server = mock(Server.class);
        ServerMetrics metrics = mock(ServerMetrics.class);

        when(server.getUrl()).thenReturn(url);
        when(server.getMetrics()).thenReturn(metrics);
        when(metrics.getAverageResponseTimeMs()).thenReturn(responseTime);

        return server;
    }

    @Test
    void shouldReturnOnlyAvailableServer() {
        Server server = createServer("http://server-1", 50);

        Server selected = strategy.selectServer(
                List.of(server),
                List.of()
        );

        assertSame(server, selected);
    }

    @Test
    void shouldSelectServerWithLowestResponseTime() {
        Server slow = createServer("http://slow", 300);
        Server medium = createServer("http://medium", 150);
        Server fast = createServer("http://fast", 40);

        Server selected = strategy.selectServer(
                List.of(slow, medium, fast),
                List.of()
        );

        assertSame(fast, selected);
    }

    @Test
    void shouldRandomlySelectAmongServersWithEqualMinimumResponseTime() {
        Server fast1 = createServer("http://fast-1", 20);
        Server fast2 = createServer("http://fast-2", 20);
        Server slow = createServer("http://slow", 100);

        List<Server> servers = List.of(fast1, fast2, slow);

        Server selected = strategy.selectServer(servers, List.of());

        assertTrue(
                selected == fast1 || selected == fast2,
                "Should select one of the servers with minimum response time"
        );
    }

    @Test
    void shouldIgnoreExcludedServers() {
        Server fast = createServer("http://fast", 10);
        Server slow = createServer("http://slow", 100);

        Server selected = strategy.selectServer(
                List.of(fast, slow),
                List.of("http://fast")
        );

        assertSame(slow, selected);
    }

    @Test
    void shouldThrowWhenNoServersAreAvailable() {
        assertThrows(
                RuntimeException.class,
                () -> strategy.selectServer(List.of(), List.of())
        );
    }

    @Test
    void shouldThrowWhenAllServersAreExcluded() {
        Server server1 = createServer("http://server-1", 10);
        Server server2 = createServer("http://server-2", 20);

        assertThrows(
                RuntimeException.class,
                () -> strategy.selectServer(
                        List.of(server1, server2),
                        List.of("http://server-1", "http://server-2")
                )
        );
    }


    @Test
    void shouldNotCrashWhenLatencyChangesConcurrently() throws InterruptedException {
        Server server1 = mock(Server.class);
        Server server2 = mock(Server.class);

        ServerMetrics metrics1 = mock(ServerMetrics.class);
        ServerMetrics metrics2 = mock(ServerMetrics.class);

        java.util.concurrent.atomic.AtomicLong latency1 =
                new java.util.concurrent.atomic.AtomicLong(10);

        java.util.concurrent.atomic.AtomicLong latency2 =
                new java.util.concurrent.atomic.AtomicLong(20);

        when(server1.getUrl()).thenReturn("http://server-1");
        when(server2.getUrl()).thenReturn("http://server-2");

        when(server1.getMetrics()).thenReturn(metrics1);
        when(server2.getMetrics()).thenReturn(metrics2);

        when(metrics1.getAverageResponseTimeMs())
                .thenAnswer(invocation -> latency1.get());

        when(metrics2.getAverageResponseTimeMs())
                .thenAnswer(invocation -> latency2.get());

        AtomicBoolean running = new AtomicBoolean(true);

        Thread updater = new Thread(() -> {
            long value = 1;

            while (running.get()) {
                latency1.set(value);
                latency2.set(value + 1);
                value = (value % 100) + 1;
            }
        });

        updater.start();

        try {
            for (int i = 0; i < 10_000; i++) {
                Server selected = strategy.selectServer(
                        List.of(server1, server2),
                        List.of()
                );

                assertNotNull(selected);
                assertTrue(selected == server1 || selected == server2);
            }
        } finally {
            running.set(false);
            updater.join();
        }
    }

}
