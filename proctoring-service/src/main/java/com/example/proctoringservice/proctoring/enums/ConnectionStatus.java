package com.example.proctoringservice.proctoring.enums;

/** Websocket/transport state of the candidate's client. */
public enum ConnectionStatus {
    CONNECTED,
    UNSTABLE,
    RECONNECTING,
    DISCONNECTED
}
