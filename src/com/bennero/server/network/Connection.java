/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Additional terms included with this license are to:
 * - Preserve legal notices and author attributions such as this one. Do not remove the original author license notices
 *   from the program
 * - Preserve the donation button and its link to the original authors donation page (christianbenner35@gmail.com)
 * - Only break the terms if given permission from the original author christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.server.network;

import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.*;
import com.bennero.common.networking.NetworkUtils;
import com.bennero.server.Identity;
import com.bennero.server.SynchronizedConnection;
import com.bennero.server.Version;
import com.bennero.server.event.*;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;

import static com.bennero.common.messages.MessageUtils.isVersionCompatible;
import static com.bennero.server.Version.*;

/**
 * The Connection class is responsible for handling an active connection to the server. Each connection is handled
 * individually on its own thread so that multiple connections can run concurrently. A connection does not mean that
 * a hardware monitor editor has taken control of the hardware monitor however, as the hardware monitor is first
 * required to accept the control and may refuse it (and therefor end the connection) if the request states and
 * incompatible version or if the monitor is already in use.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class Connection implements Runnable {
    // Tag for logging
    private static final String CLASS_NAME = Connection.class.getSimpleName();

    private SynchronizedConnection connection;
    private SocketChannel socketChannel;
    private EventHandler connectedEvent;
    private EventHandler disconnectedEvent;
    private EventHandler<PageSetupEvent> pageMessageReceived;
    private EventHandler<SensorSetupEvent> sensorMessageReceived;
    private EventHandler<SensorDataEvent> sensorDataMessageReceived;
    private EventHandler<RemovePageEvent> removePageMessageReceived;
    private EventHandler<RemoveSensorEvent> removeSensorMessageReceived;
    private EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived;

    private Boolean connected;
    private volatile String clientHostname;
    private boolean stop;

    public Connection(SynchronizedConnection connection,
                      SocketChannel socketChannel,
                      EventHandler connectedEvent,
                      EventHandler disconnectedEvent,
                      EventHandler<PageSetupEvent> pageMessageReceived,
                      EventHandler<SensorSetupEvent> sensorMessageReceived,
                      EventHandler<RemovePageEvent> removePageMessageReceived,
                      EventHandler<SensorDataEvent> sensorDataMessageReceived,
                      EventHandler<RemoveSensorEvent> removeSensorMessageReceived,
                      EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived) {
        this.connection = connection;
        this.socketChannel = socketChannel;
        this.connectedEvent = connectedEvent;
        this.disconnectedEvent = disconnectedEvent;
        this.pageMessageReceived = pageMessageReceived;
        this.sensorMessageReceived = sensorMessageReceived;
        this.removePageMessageReceived = removePageMessageReceived;
        this.sensorDataMessageReceived = sensorDataMessageReceived;
        this.removeSensorMessageReceived = removeSensorMessageReceived;
        this.sensorTransformationMessageReceived = sensorTransformationMessageReceived;
        connected = false;
        stop = false;

        clientHostname = "Not specified";
    }

    public void setConnectionAlive(boolean state) {
        this.connected = state;
    }

    public boolean isConnectionActive() {
        return this.connected;
    }

    public String getClientHostname() {
        return this.clientHostname;
    }

    private void setClientHostname(String clientHostname) {
        this.clientHostname = clientHostname;
    }

    public byte[] getAddress() {
        return this.socketChannel.socket().getInetAddress().getAddress();
    }

    public boolean isStopped() {
        return stop;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        try {
            InputStream is = socketChannel.socket().getInputStream();

            byte[] bytes;
            while (!stop && socketChannel.isOpen()) {
                bytes = new byte[Message.NUM_BYTES];
                is.read(bytes, 0, Message.NUM_BYTES);
                readMessage(bytes);
            }

            Logger.log(LogLevel.INFO, CLASS_NAME, "Connection has ended with '" + connection.getClientHostname() +
                    "'/" + NetworkUtils.ip4AddressToString(connection.getAddress()));
            socketChannel.close();
        } catch (Exception e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Unexpected end of connection with '" +
                    connection.getClientHostname() + "'/" + NetworkUtils.ip4AddressToString(connection.getAddress()));
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        } finally {
            connection.setConnectionAlive(false);
            disconnectedEvent.handle(new Event(null));
        }
    }

    private void readMessage(byte[] bytes) {
        switch (Message.getType(bytes)) {
            case MessageType.SENSOR_UPDATE:
                sensorDataMessageReceived.handle(new SensorDataEvent(new SensorUpdateMessage(bytes)));
                break;
            case MessageType.PAGE_CREATE:
                pageMessageReceived.handle(new PageSetupEvent(new PageCreateMessage(bytes)));
                break;
            case MessageType.SENSOR_CREATE:
                sensorMessageReceived.handle(new SensorSetupEvent(new SensorCreateMessage(bytes)));
                break;
            case MessageType.PAGE_REMOVE:
                removePageMessageReceived.handle(new RemovePageEvent(new PageRemoveMessage(bytes)));
                break;
            case MessageType.SENSOR_REMOVE:
                removeSensorMessageReceived.handle(new RemoveSensorEvent(new SensorRemoveMessage(bytes)));
                break;
            case MessageType.SENSOR_TRANSFORM:
                sensorTransformationMessageReceived.handle(new SensorTransformationEvent(new SensorTransformationMessage(bytes)));
                break;
            case MessageType.CONNECTION_REQUEST:
                handleConnectionRequest(new ConnectionRequestMessage(bytes));
                break;
            case MessageType.DISCONNECT:
                Logger.log(LogLevel.DEBUG, CLASS_NAME, "Received disconnect message");
                handleDisconnect();
                break;
        }
    }

    private void handleConnectionRequest(ConnectionRequestMessage message) {
        // We now know the client hostname so store this information
        setClientHostname(message.getHostname());

        // Announce connection request
        Logger.log(LogLevel.INFO, CLASS_NAME, "Received connection request message from '" +
                message.getHostname() + "' v(" + message.getVersionMajor() + "." + message.getVersionMinor() + "." +
                message.getVersionPatch() + ")");

        // Is the version compatible
        boolean versionMismatch = isVersionCompatible(VERSION_MAJOR, VERSION_MINOR, message.getVersionMajor(),
                message.getVersionMinor()) != MessageUtils.Compatibility.COMPATIBLE;
        boolean currentlyInUse = connection.isConnectionActive() && !message.isForceConnection();

        // Should we accept the connection or not
        final boolean acceptConnection = !versionMismatch && !currentlyInUse;

        if (acceptConnection) {
            sendConnectionRequestReplyMessage(true, false, false);

            Logger.log(LogLevel.INFO, CLASS_NAME, "Accepted connection request message from '" + message.
                    getHostname() + "' v(" + message.getVersionMajor() + "." + message.getVersionMinor() + "." +
                    message.getVersionPatch() + ")");
            connection.setConnection(this);
        } else {
            if (versionMismatch) {
                sendConnectionRequestReplyMessage(false, true, currentlyInUse);

                Logger.log(LogLevel.WARNING, CLASS_NAME, "Rejected connection request message from '" +
                        message.getHostname() + "' v(" + message.getVersionMajor() + "." + message.
                        getVersionMinor() + "." + message.getVersionPatch() + ") because the client version " +
                        "is not compatible with the monitor version (" + Version.getVersionString() + ")");
            } else if (currentlyInUse) {
                sendConnectionRequestReplyMessage(false, false, true,
                        connection.getClientHostname());

                Logger.log(LogLevel.WARNING, CLASS_NAME, "Rejected connection request message from '" +
                        message.getHostname() + "' v(" + message.getVersionMajor() + "." + message.
                        getVersionMinor() + "." + message.getVersionPatch() + ") because the monitor is " +
                        "currently in use by '" + connection.getClientHostname() + "'");
            }

            // Try to close the socket channel, ending the connection with the client and making the threads
            // life come to an end
            try {
                socketChannel.close();
            } catch (IOException e) {
                Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to close connections socket channel");
                Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
            }
        }
    }

    private void handleDisconnect() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to send connections socket channel");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
    }

    // Returns true if the connection message sent allows connectivity, else false
    private void sendConnectionRequestReplyMessage(boolean acceptConnection,
                                                   boolean versionMismatch,
                                                   boolean currentlyInUse,
                                                   final String hostname) {
        ConnectionRequestResponseMessage out = new ConnectionRequestResponseMessage(Identity.getMyUuid(), true,
                VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, acceptConnection, versionMismatch, currentlyInUse,
                hostname == null ? "" : hostname);

        try {
            socketChannel.socket().getOutputStream().write(out.write(), 0, Message.NUM_BYTES);
            Logger.log(LogLevel.DEBUG, CLASS_NAME, "Sent connection request reply message");
            socketChannel.socket().getOutputStream().flush();

            if (acceptConnection) {
                connectedEvent.handle(null);
                setConnectionAlive(true);
            }
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to sent connection request reply message");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
    }

    private void sendConnectionRequestReplyMessage(boolean acceptConnection,
                                                   boolean versionMismatch,
                                                   boolean currentlyInUse) {
        sendConnectionRequestReplyMessage(acceptConnection, versionMismatch, currentlyInUse, null);
    }
}
