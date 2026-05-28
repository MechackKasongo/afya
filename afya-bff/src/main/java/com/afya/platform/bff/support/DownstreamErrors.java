package com.afya.platform.bff.support;

import org.springframework.web.client.ResourceAccessException;

public final class DownstreamErrors {

    private DownstreamErrors() {
    }

    public static boolean isUnreachable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
