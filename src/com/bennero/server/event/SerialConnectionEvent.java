package com.bennero.server.event;

import javafx.event.Event;

public class SerialConnectionEvent extends Event {
    private boolean connected;
    private String error;

    public SerialConnectionEvent(boolean connected, String error) {
        super(connected, null, null);
        this.connected = connected;
        this.error = error;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getError() {
        return error;
    }
}
