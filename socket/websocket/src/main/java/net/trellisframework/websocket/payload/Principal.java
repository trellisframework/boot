package net.trellisframework.websocket.payload;

import net.trellisframework.context.payload.Principle;

public final class Principal implements java.security.Principal {

    private final String name;

    @Override
    public String getName() {
        return name;
    }

    public Principal(String name) {
        this.name = name;
    }

    public static Principal of(String name) {
        return new Principal(name);
    }

    public static Principal of(Principle principle) {
        return new Principal(principle.getUsername());
    }

}
