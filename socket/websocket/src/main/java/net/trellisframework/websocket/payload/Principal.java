package net.trellisframework.websocket.payload;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.trellisframework.context.payload.Principle;

@Data
@RequiredArgsConstructor
public final class Principal implements java.security.Principal {

    private final String name;

    public static Principal of(String name) {
        return new Principal(name);
    }

    public static Principal of(Principle principle) {
        return new Principal(principle.getUsername());
    }

}
