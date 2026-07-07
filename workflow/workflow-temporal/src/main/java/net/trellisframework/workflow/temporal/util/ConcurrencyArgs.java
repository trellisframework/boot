package net.trellisframework.workflow.temporal.util;

import java.util.ArrayList;
import java.util.List;

public final class ConcurrencyArgs {

    public static final String IDEMPOTENCY_MARKER = "__IDEMPOTENCY__";

    private ConcurrencyArgs() {
    }

    public static List<Object> withKey(List<Object> workArgs, String key) {
        if (key == null || key.isBlank())
            return workArgs;
        List<Object> out = new ArrayList<>(workArgs);
        out.add(IDEMPOTENCY_MARKER);
        out.add(key);
        return out;
    }

    public static String keyOf(List<Object> workArgs) {
        int n = workArgs.size();
        if (n >= 2 && IDEMPOTENCY_MARKER.equals(workArgs.get(n - 2)))
            return (String) workArgs.get(n - 1);
        return null;
    }

    public static List<Object> strip(List<Object> workArgs) {
        int n = workArgs.size();
        if (n >= 2 && IDEMPOTENCY_MARKER.equals(workArgs.get(n - 2)))
            return new ArrayList<>(workArgs.subList(0, n - 2));
        return workArgs;
    }
}
