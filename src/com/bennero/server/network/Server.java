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
import com.bennero.common.networking.AddressInformation;
import com.bennero.server.SynchronizedConnection;
import com.bennero.server.event.*;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static com.bennero.common.Constants.PORT;

/**
 * Server class handles all connections with hardware monitor editors. It is designed to be able to handle multiple
 * connections however it will only accept/allow one hardware monitor editor to actually control the device. The other
 * connections may come in the form of connection requests that the server can choose to allow or reject depending on
 * the current circumstances e.g. if the server is already being controlled or if the editor is out of date then the
 * server can reject the connection request with a response that contains this rejection reason.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class Server implements Runnable {
    // Class name used in logging
    private static final String CLASS_NAME = Server.class.getName();

    private AddressInformation siteLocalAddressInformation;
    private EventHandler connectedEvent;
    private EventHandler disconnectedEvent;
    private EventHandler<PageSetupEvent> pageMessageReceived;
    private EventHandler<SensorSetupEvent> sensorMessageReceived;
    private EventHandler<SensorDataEvent> sensorDataMessageReceived;
    private EventHandler<RemovePageEvent> removePageMessageReceived;
    private EventHandler<RemoveSensorEvent> removeSensorMessageReceived;
    private EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived;

    private SynchronizedConnection activeConnection;

    private BroadcastReplier broadcastReplier;
    private Thread broadcastReplyThread;

    private HeartbeatSender heartbeatSender;
    private Thread heartbeatSenderThread;

    private boolean acceptConnections;

    public Server(AddressInformation siteLocalAddressInformation,
                  EventHandler connectedEvent,
                  EventHandler disconnectedEvent,
                  EventHandler<PageSetupEvent> pageMessageReceived,
                  EventHandler<SensorSetupEvent> sensorMessageReceived,
                  EventHandler<RemovePageEvent> removePageMessageReceived,
                  EventHandler<SensorDataEvent> sensorDataMessageReceived,
                  EventHandler<RemoveSensorEvent> removeSensorMessageReceived,
                  EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived) {
        this.siteLocalAddressInformation = siteLocalAddressInformation;
        this.connectedEvent = connectedEvent;
        this.disconnectedEvent = disconnectedEvent;
        this.pageMessageReceived = pageMessageReceived;
        this.sensorMessageReceived = sensorMessageReceived;
        this.removePageMessageReceived = removePageMessageReceived;
        this.sensorDataMessageReceived = sensorDataMessageReceived;
        this.removeSensorMessageReceived = removeSensorMessageReceived;
        this.sensorTransformationMessageReceived = sensorTransformationMessageReceived;
        activeConnection = new SynchronizedConnection();
        acceptConnections = true;
    }

    public void disconnectActiveConnection() throws InterruptedException {
        activeConnection.stop();
    }

    @Override
    public void run() {
        broadcastReplier = new BroadcastReplier(siteLocalAddressInformation);
        broadcastReplyThread = new Thread(broadcastReplier);
        broadcastReplyThread.start();

        heartbeatSender = new HeartbeatSender(activeConnection);
        heartbeatSenderThread = new Thread(heartbeatSender);
        heartbeatSenderThread.start();

        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT));

            while (acceptConnections) {
                SocketChannel socketChannel = serverSocketChannel.accept();

                // Connected event we should re-route the events to the caller of server
                // Disconnect event we should remove the connection from the list (the thread will die automatically)
                Connection connection = new Connection(activeConnection, socketChannel, connectedEvent,
                        disconnectedEvent, pageMessageReceived, sensorMessageReceived, removePageMessageReceived,
                        sensorDataMessageReceived, removeSensorMessageReceived, sensorTransformationMessageReceived);

                Thread thread = new Thread(connection);
                thread.start();
            }
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Server connection ended unexpectedly");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());

            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (IOException ex) {
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to close the server socket channel");
                    Logger.log(LogLevel.DEBUG, CLASS_NAME, ex.getMessage());
                }
            }
            disconnectedEvent.handle(new Event(null));
        }
    }
}
