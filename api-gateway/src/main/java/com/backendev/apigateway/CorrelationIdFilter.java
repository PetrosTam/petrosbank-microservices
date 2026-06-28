package com.backendev.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest requestWithCorrelationId = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange exchangeWithCorrelationId = exchange.mutate()
                .request(requestWithCorrelationId)
                .build();

        String finalCorrelationId = correlationId;

        exchangeWithCorrelationId.getResponse().beforeCommit(() -> {
            exchangeWithCorrelationId.getResponse()
                    .getHeaders()
                    .set(CORRELATION_ID_HEADER, finalCorrelationId);
            return Mono.empty();
        });

        MDC.put(CORRELATION_ID_MDC_KEY, finalCorrelationId);

        log.info(
                "Gateway request received. correlationId={}, method={}, path={}",
                finalCorrelationId,
                requestWithCorrelationId.getMethod(),
                requestWithCorrelationId.getURI().getPath()
        );

        return chain.filter(exchangeWithCorrelationId)
                .doFinally(signalType -> MDC.remove(CORRELATION_ID_MDC_KEY));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}