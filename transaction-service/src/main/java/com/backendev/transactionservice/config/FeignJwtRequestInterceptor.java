package com.backendev.transactionservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignJwtRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        // Simply forward the Authorization header from the incoming request
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();

            // Get the Authorization header from the incoming request
            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null) {
                // Forward it to the outgoing Feign request
                requestTemplate.header("Authorization", authorizationHeader);
            }
        }
    }
}
