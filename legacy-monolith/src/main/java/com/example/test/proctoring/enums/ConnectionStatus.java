package com.example.test.proctoring.enums;

/** Websocket/transport state of the candidate's client. */
public enum ConnectionStatus {
    CONNECTED,
    UNSTABLE,
    RECONNECTING,
    DISCONNECTED
}
