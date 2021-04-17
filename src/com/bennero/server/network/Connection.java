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
import com.bennero.common.networking.NetworkUtils;
import com.bennero.common.messages.ConnectionRequestReplyDataPositions;
import com.bennero.common.messages.MessageType;
import com.bennero.server.SynchronizedConnection;
import com.bennero.server.Version;
import com.bennero.server.event.*;
import com.bennero.server.message.*;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.isVersionCompatible;
import static com.bennero.common.networking.NetworkUtils.writeStringToMessage;
import static com.bennero.server.Version.*;
import static com.bennero.server.message.ConnectionRequestMessage.processConnectionRequestMessageData;

/**
 * The Connection class is responsible for handling an active connection to the server. Each connection is handled
 * individually on its own thread so that multiple connections can run concurrently. A connection does not mean that
 * a hardware monitor editor has taken control of the hardware monitor however, as the hardware monitor is first
 * required to accept the control and may refuse it (and therefor end the connection) if the request states and
 * incompatible version or if the monitor is already in use.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class Connection implements Runnable
{
    // Tag for logging
    private static final String TAG = Connection.class.getSimpleName();

    private SynchronizedConnection connection;
    private SocketChannel socketChannel;
    private EventHandler connectedEvent;
    private EventHandler disconnectedEvent;
    private EventHandler<PageSetupEvent> pageMessageReceived;
    private EventHandler<SensorSetupEvent> sensorMessageReceived;
    private EventHandler<SensorDataEvent> sensorDataMessageReceived;
    private EventHandler<RemovePageEvent> removePageMessageReceived;
    private EventHandler<RemoveSensorEvent> removeSensorMessageReceived;

    private Boolean connected;
    private volatile String clientHostname;

    public Connection(SynchronizedConnection connection,
                      SocketChannel socketChannel,
                      EventHandler connectedEvent,
                      EventHandler disconnectedEvent,
                      EventHandler<PageSetupEvent> pageMessageReceived,
                      EventHandler<SensorSetupEvent> sensorMessageReceived,
                      EventHandler<RemovePageEvent> removePageMessageReceived,
                      EventHandler<SensorDataEvent> sensorDataMessageReceived,
                      EventHandler<RemoveSensorEvent> removeSensorMessageReceived)
    {
        this.connection = connection;
        this.socketChannel = socketChannel;
        this.connectedEvent = connectedEvent;
        this.disconnectedEvent = disconnectedEvent;
        this.pageMessageReceived = pageMessageReceived;
        this.sensorMessageReceived = sensorMessageReceived;
        this.removePageMessageReceived = removePageMessageReceived;
        this.sensorDataMessageReceived = sensorDataMessageReceived;
        this.removeSensorMessageReceived = removeSensorMessageReceived;
        connected = false;

        clientHostname = "Not specified";
    }

    public void setConnectionAlive(boolean state)
    {
        this.connected = state;
    }

    public boolean isConnectionActive()
    {
        return this.connected;
    }

    public String getClientHostname()
    {
        return this.clientHostname;
    }

    private void setClientHostname(String clientHostname)
    {
        this.clientHostname = clientHostname;
    }

    public byte[] getAddress()
    {
        return this.socketChannel.socket().getInetAddress().getAddress();
    }

    @Override
    public void run()
    {
        try
        {
            InputStream is = socketChannel.socket().getInputStream();

            byte[] bytes;
            while (socketChannel.isOpen())
            {
                bytes = new byte[MESSAGE_NUM_BYTES];
                is.read(bytes, 0, MESSAGE_NUM_BYTES);
                readMessage(bytes);
            }

            Logger.log(LogLevel.INFO, TAG, "Connection has ended with '" + connection.getClientHostname() +
                    "'/" + NetworkUtils.ip4AddressToString(connection.getAddress()));
            socketChannel.close();
        }
        catch (Exception e)
        {
            Logger.log(LogLevel.ERROR, TAG, "Unexpected end of connection with '" +
                    connection.getClientHostname() + "'/" + NetworkUtils.ip4AddressToString(connection.getAddress()));
            e.printStackTrace();
        }
        finally
        {
            connection.setConnectionAlive(false);
            disconnectedEvent.handle(new Event(null));
        }
    }

    private void readMessage(byte[] bytes)
    {
        switch (bytes[MESSAGE_TYPE_POS])
        {
            case MessageType.DATA:
                System.out.println("Received hardware data message");
                sensorDataMessageReceived.handle(new SensorDataEvent(SensorDataMessage.processSensorDataMessage(bytes)));
                break;
            case MessageType.PAGE_SETUP:
                System.out.println("Received page setup message");
                pageMessageReceived.handle(new PageSetupEvent(PageSetupMessage.readPageSetupMessage(bytes)));
                break;
            case MessageType.SENSOR_SETUP:
                System.out.println("Received sensor setup message");
                sensorMessageReceived.handle(new SensorSetupEvent(SensorSetupMessage.processSensorSetupMessage(bytes)));
                break;
            case MessageType.REMOVE_PAGE:
                System.out.println("Received remove page message");
                removePageMessageReceived.handle(new RemovePageEvent(RemovePageMessage.processRemovePageMessage(bytes)));
                break;
            case MessageType.REMOVE_SENSOR:
                System.out.println("Received remove sensor message");
                removeSensorMessageReceived.handle(new RemoveSensorEvent(RemoveSensorMessage.processRemoveSensorMessage(bytes)));
                break;
            case MessageType.CONNECTION_REQUEST_MESSAGE:
                ConnectionRequestMessage message = processConnectionRequestMessageData(bytes);

                // We now know the clients hostname so store this information
                setClientHostname(message.getHostname());

                // Announce connection request
                System.out.println("Received connection request message from '" + message.getHostname() + "' v(" +
                        message.getMajorVersion() + "." + message.getMinorVersion() + "." + message.getPatchVersion() +
                        ")");

                // Is the version compatible
                boolean versionMismatch = isVersionCompatible(VERSION_MAJOR, VERSION_MINOR, message.getMajorVersion(),
                        message.getMinorVersion()) != NetworkUtils.Compatibility.COMPATIBLE;
                boolean currentlyInUse = connection.isConnectionActive() && !message.isForceConnection();

                // Should we accept the connection or not
                final boolean acceptConnection = !versionMismatch && !currentlyInUse;

                if (acceptConnection)
                {
                    sendConnectionRequestReplyMessage(true, false, false);

                    Logger.log(LogLevel.INFO, TAG, "Accepted connection request message from '" + message.
                            getHostname() + "' v(" + message.getMajorVersion() + "." + message.getMinorVersion() + "." +
                            message.getPatchVersion() + ")");
                    connection.setConnection(this);
                }
                else
                {
                    if (versionMismatch)
                    {
                        sendConnectionRequestReplyMessage(false, true, currentlyInUse);

                        Logger.log(LogLevel.WARNING, TAG, "Rejected connection request message from '" +
                                message.getHostname() + "' v(" + message.getMajorVersion() + "." + message.
                                getMinorVersion() + "." + message.getPatchVersion() + ") because the client version " +
                                "is not compatible with the monitor version (" + Version.getVersionString() + ")");
                    }
                    else if (currentlyInUse)
                    {
                        sendConnectionRequestReplyMessage(false, false, true,
                                connection.getClientHostname());

                        Logger.log(LogLevel.WARNING, TAG, "Rejected connection request message from '" +
                                message.getHostname() + "' v(" + message.getMajorVersion() + "." + message.
                                getMinorVersion() + "." + message.getPatchVersion() + ") because the monitor is " +
                                "currently in use by '" + connection.getClientHostname() + "'");
                    }

                    // Try to close the socket channel, ending the connection with the client and making the threads
                    // life come to an end
                    try
                    {
                        socketChannel.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                System.out.println("Received Unrecognised Message Type: " + (int) bytes[MESSAGE_TYPE_POS]);
                break;
        }
    }

    // Returns true if the connection message sent allows connectivity, else false
    private void sendConnectionRequestReplyMessage(boolean acceptConnection,
                                                   boolean versionMismatch,
                                                   boolean currentlyInUse,
                                                   final String hostname)
    {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.CONNECTION_REQUEST_RESPONSE_MESSAGE;
        message[ConnectionRequestReplyDataPositions.MAJOR_VERSION_POS] = VERSION_MAJOR;
        message[ConnectionRequestReplyDataPositions.MINOR_VERSION_POS] = VERSION_MINOR;
        message[ConnectionRequestReplyDataPositions.PATCH_VERSION_POS] = VERSION_PATCH;
        message[ConnectionRequestReplyDataPositions.CONNECTION_ACCEPTED] = acceptConnection ? (byte) 0x01 : 0x00;
        message[ConnectionRequestReplyDataPositions.VERSION_MISMATCH] = versionMismatch ? (byte) 0x01 : 0x00;
        message[ConnectionRequestReplyDataPositions.CURRENTLY_IN_USE] = currentlyInUse ? (byte) 0x01 : 0x00;

        if (hostname != null)
        {
            writeStringToMessage(message, ConnectionRequestReplyDataPositions.CURRENT_CLIENT_HOSTNAME, hostname,
                    NAME_STRING_NUM_BYTES);
        }

        try
        {
            socketChannel.socket().getOutputStream().write(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent connection request reply message");
            socketChannel.socket().getOutputStream().flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (acceptConnection)
        {
            connectedEvent.handle(null);
            setConnectionAlive(true);
        }
    }

    private void sendConnectionRequestReplyMessage(boolean acceptConnection,
                                                   boolean versionMismatch,
                                                   boolean currentlyInUse)
    {
        sendConnectionRequestReplyMessage(acceptConnection, versionMismatch, currentlyInUse, null);
    }
}
