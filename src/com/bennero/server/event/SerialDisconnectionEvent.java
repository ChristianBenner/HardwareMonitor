package com.bennero.server.event;

import javafx.event.Event;

public class SerialDisconnectionEvent extends Event {
    private boolean expected;
    private String reason;

    public SerialDisconnectionEvent(boolean expected, String reason) {
        super(expected, null, null);
        this.expected = expected;
        this.reason = reason;
    }

    public boolean isExpected() {
        return expected;
    }

    public String getReason() {
        return reason;
    }
}
