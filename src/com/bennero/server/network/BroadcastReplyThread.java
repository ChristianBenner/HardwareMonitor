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

import com.bennero.common.Constants;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.BroadcastAnnouncementDataPositions;
import com.bennero.common.messages.BroadcastReplyDataPositions;
import com.bennero.common.messages.MessageType;
import com.bennero.common.networking.AddressInformation;
import com.bennero.server.message.BroadcastMessage;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.*;
import static com.bennero.server.Version.*;

/**
 * BroadcastReplyThread is a thread that runs concurrent to the Hardware Monitor user interface. It is designed to reply
 * to any broadcast messages sent by Hardware Monitor editors on the same network, therefor it listens to the broadcast
 * address. A specific random key is verified to ensure that the broadcast message came from a hardware monitor editor
 * and not another unrelated device on the network.
 *
 * Once a verified hardware monitor editor broadcast message has been received, the thread will attempt to send a
 * response back using the provided IP address. That response will contain some useful data for the hardware monitor
 * editor such as the version of the hardware monitor, hostname, MAC address and the IP4 address (therefor the
 * hardware monitor editor knows which device has replied and how to request connection to it). The broadcast reply
 * also contains a specific random key so that the editor can verify it is receiving communication from a hardware
 * monitor and not an unrelated device on the network.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
class BroadcastReplyThread implements Runnable
{
    // Class name used in logging
    private static final String CLASS_NAME = BroadcastReplyThread.class.getName();

    private AddressInformation siteLocalAddressInformation;

    public BroadcastReplyThread(AddressInformation siteLocalAddressInformation)
    {
        this.siteLocalAddressInformation = siteLocalAddressInformation;
    }

    @Override
    public void run()
    {
        DatagramChannel datagramChannel = null;

        try
        {
            datagramChannel = DatagramChannel.open();
            datagramChannel.socket().bind(new InetSocketAddress(BROADCAST_RECEIVE_PORT));
            while (true)
            {
                ByteBuffer buf = ByteBuffer.allocate(MESSAGE_NUM_BYTES);
                buf.clear();
                datagramChannel.receive(buf);

                byte[] bytes = buf.array();
                if (bytes[MESSAGE_TYPE_POS] == MessageType.BROADCAST_MESSAGE)
                {
                    BroadcastMessage broadcastMessage = BroadcastMessage.processBroadcastMessageData(bytes);
                    if (broadcastMessage.isVerifiedBroadcastMessage())
                    {
                        writeBroadcastReplyMessage(broadcastMessage.getIp4Address());
                    }
                }
            }
        }
        catch (IOException e)
        {
            Logger.log(LogLevel.ERROR, CLASS_NAME,
                    "Failed to open datagram channel to receive broadcast messages");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
        finally
        {
            if (datagramChannel != null && datagramChannel.isOpen())
            {
                try
                {
                    datagramChannel.close();
                }
                catch (IOException e)
                {
                    Logger.log(LogLevel.ERROR, CLASS_NAME,
                            "Failed to close broadcast message receiver datagram channel");
                    Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
                }
            }
        }
    }

    private void writeBroadcastReplyMessage(byte[] ip4Address)
    {
        // Create a socket, and send a broadcast reply message
        try
        {
            Socket socket = new Socket(InetAddress.getByAddress(ip4Address), Constants.BROADCAST_REPLY_PORT);
            PrintStream broadcastReplyWriter = new PrintStream(socket.getOutputStream(), true);

            // Write broadcast reply message
            byte[] replyMessage = new byte[MESSAGE_NUM_BYTES];
            replyMessage[MESSAGE_TYPE_POS] = MessageType.BROADCAST_REPLY_MESSAGE;
            writeToMessage(replyMessage, BroadcastAnnouncementDataPositions.HW_SYSTEM_IDENTIFIER_POS, HW_MONITOR_SYSTEM_UNIQUE_CONNECTION_ID);
            replyMessage[BroadcastReplyDataPositions.MAJOR_VERSION_POS] = VERSION_MAJOR;
            replyMessage[BroadcastReplyDataPositions.MINOR_VERSION_POS] = VERSION_MINOR;
            replyMessage[BroadcastReplyDataPositions.PATCH_VERSION_POS] = VERSION_PATCH;
            writeBytesToMessage(replyMessage, BroadcastReplyDataPositions.MAC_ADDRESS_POS, siteLocalAddressInformation.getMacAddress(), MAC_ADDRESS_NUM_BYTES);
            writeBytesToMessage(replyMessage, BroadcastReplyDataPositions.IP4_ADDRESS_POS, siteLocalAddressInformation.getIp4Address(), IP4_ADDRESS_NUM_BYTES);
            writeStringToMessage(replyMessage, BroadcastReplyDataPositions.HOSTNAME_POS, siteLocalAddressInformation.getHostname(), NAME_STRING_NUM_BYTES);

            broadcastReplyWriter.write(replyMessage, 0, MESSAGE_NUM_BYTES);

            Logger.log(LogLevel.DEBUG, CLASS_NAME, "Sent broadcast acknowledgement to editor: " +
                    InetAddress.getByAddress(ip4Address));
        }
        catch (IOException e)
        {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to send broadcast reply message");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
    }
}