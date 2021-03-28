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

import com.bennero.logging.LogLevel;
import com.bennero.logging.Logger;
import com.bennero.networking.NetworkUtils;
import com.bennero.osspecific.OSUtils;
import com.bennero.osspecific.RaspberryPiScreenUtils;
import com.bennero.messages.MessageType;
import com.bennero.server.SynchronizedConnection;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.bennero.common.Constants.*;
import static com.bennero.networking.NetworkUtils.writeToMessage;

/**
 * HeartbeatSender sub-system manages sending heartbeat messages to the currently connected hardware monitor editor so
 * that it is aware the monitor is still alive
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
class HeartbeatSender implements Runnable
{
    // Tag for logging
    private static final String TAG = HeartbeatSender.class.getSimpleName();

    private static int SCREEN_TIME_OUT_SECONDS_RASPBERRY_PI_OS = 20;

    private SynchronizedConnection connection;
    private PrintStream socketWriter;
    private Socket socket;
    private boolean connectionLostCounterEnabled;
    private int secondsConnectionLost;

    public HeartbeatSender(SynchronizedConnection connection)
    {
        this.connection = connection;
        socket = new Socket();
        secondsConnectionLost = 0;
        connectionLostCounterEnabled = false;
    }

    private void connect()
    {
        if (connection.isConnectionActive())
        {
            System.out.println("Attempting Connection: " + NetworkUtils.ip4AddressToString(connection.getAddress()));

            // This means that the IP4 and MAC address have just been discovered, so we can start with a direct
            // connection attempt
            try
            {
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(connection.getAddress()), HEARTBEAT_PORT), 5000);
                socketWriter = new PrintStream(socket.getOutputStream(), true);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            // If the socket is not connected to anything, or the address of the socket does not match the current
            // connection.
            if (!socket.isConnected())
            {
                connect();
            }
            else if (!NetworkUtils.doAddressesMatch(socket.getInetAddress().getAddress(), connection.getAddress()))
            {
                // Disconnect current connection
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                connect();
            }

            if (connection.isConnectionActive())
            {
                // Write broadcast reply message
                byte[] replyMessage = new byte[MESSAGE_NUM_BYTES];
                replyMessage[MESSAGE_TYPE_POS] = MessageType.HEARTBEAT_MESSAGE;
                writeToMessage(replyMessage, HW_HEARTBEAT_VALIDATION_NUMBER_POS, HW_HEARTBEAT_VALIDATION_NUMBER);

                socketWriter.write(replyMessage, 0, MESSAGE_NUM_BYTES);
                Logger.log(LogLevel.DEBUG, TAG, "Sent heartbeat message");
                socketWriter.flush();

                secondsConnectionLost = 0;
                connectionLostCounterEnabled = true;

                // If we are on a Raspberry Pi OS and the screen has been turned off, turn it back on
                if (OSUtils.getOperatingSystem() == OSUtils.OperatingSystem.RASPBERRY_PI &&
                        !RaspberryPiScreenUtils.isDisplayEnabled())
                {
                    RaspberryPiScreenUtils.setDisplayEnabled(true);
                }
            }
            else
            {
                if (connectionLostCounterEnabled)
                {
                    System.out.println("Seconds since connection lost: " + secondsConnectionLost);
                    secondsConnectionLost++;

                    // If we are on a Raspberry Pi OS and the screen is on, turn it off
                    if (secondsConnectionLost > SCREEN_TIME_OUT_SECONDS_RASPBERRY_PI_OS && OSUtils.getOperatingSystem() ==
                            OSUtils.OperatingSystem.RASPBERRY_PI && RaspberryPiScreenUtils.isDisplayEnabled())
                    {
                        RaspberryPiScreenUtils.setDisplayEnabled(false);
                    }
                }
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}