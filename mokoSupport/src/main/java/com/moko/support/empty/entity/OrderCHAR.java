package com.moko.support.empty.entity;

import java.io.Serializable;
import java.util.UUID;

public enum OrderCHAR implements Serializable {
    CHAR_PARAMS(UUID.fromString("0000AA00-0000-1000-8000-00805F9B34FB")),
    ;

    private UUID uuid;

    OrderCHAR(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
