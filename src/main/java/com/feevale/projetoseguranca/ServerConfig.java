package com.feevale.projetoseguranca;

public enum ServerConfig {
    PORT(8084),
    MAX_CLIENTS(10),
    HOST("localhost");

    private final Object value;

    ServerConfig(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
