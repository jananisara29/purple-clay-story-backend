package com.purpleclay.jewelry.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LogContext {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_EMAIL = "userEmail";
    public static final String ENDPOINT   = "endpoint";

    public static void set(String key, String value) {
        if (value != null) MDC.put(key, value);
    }

    public static void setRequestId() {
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));
    }

    public static void clear() {
        MDC.clear();
    }

    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }
}
