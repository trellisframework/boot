package net.trellisframework.core.log;

import java.util.Optional;

@FunctionalInterface
public interface LogContextProvider {
    Optional<LogContext> resolve();
}
