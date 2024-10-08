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
import com.bennero.common.messages.BroadcastAnnouncementMessage;
import com.bennero.common.messages.BroadcastReplyMessage;
import com.bennero.common.messages.Message;
import com.bennero.common.messages.MessageType;
import com.bennero.common.networking.AddressInformation;
import com.bennero.server.Identity;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import static com.bennero.common.Constants.*;
import static com.bennero.common.messages.MessageUtils.*;
import static com.bennero.server.Version.*;

/**
 * BroadcastReplier is a thread that runs concurrent to the Hardware Monitor user interface. It is designed to reply
 * to any broadcast messages sent by Hardware Monitor editors on the same network, therefor it listens to the broadcast
 * address. A specific random key is verified to ensure that the broadcast message came from a hardware monitor editor
 * and not another unrelated device on the network.
 * <p>
 * Once a verified hardware monitor editor broadcast message has been received, the thread will attempt to send a
 * response back using the provided IP address. That response will contain some useful data for the hardware monitor
 * editor such as the version of the hardware monitor, hostname, MAC address and the IP4 address (therefor the
 * hardware monitor editor knows which device has replied and how to request connection to it). The broadcast reply
 * also contains a specific random key so that the editor can verify it is receiving communication from a hardware
 * monitor and not an unrelated device on the network.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
class BroadcastReplier implements Runnable {
    // Class name used in logging
    private static final String CLASS_NAME = BroadcastReplier.class.getSimpleName();

    private AddressInformation siteLocalAddressInformation;
    private boolean reply;
    private DatagramChannel datagramChannel;

    public BroadcastReplier(AddressInformation siteLocalAddressInformation) {
        this.siteLocalAddressInformation = siteLocalAddressInformation;
        reply = true;
    }

    public void stop() {
        reply = false;
        try {
            datagramChannel.socket().close();
            datagramChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.socket().bind(new InetSocketAddress(BROADCAST_RECEIVE_PORT));

            while (reply) {
                ByteBuffer buf = ByteBuffer.allocate(Message.NUM_BYTES);
                buf.clear();
                datagramChannel.receive(buf);

                byte[] bytes = buf.array();

                if (Message.getType(bytes) == MessageType.BROADCAST) {
                    BroadcastAnnouncementMessage in = new BroadcastAnnouncementMessage(bytes);
                    if (in.getSystemIdentifier() == HW_EDITOR_SYSTEM_UNIQUE_CONNECTION_ID) {
                        writeBroadcastReplyMessage(in.getIp4Address());
                    }
                }
            }
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME,
                    "Failed to open datagram channel to receive broadcast messages");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        } finally {
            if (datagramChannel != null && datagramChannel.isOpen()) {
                try {
                    datagramChannel.close();
                } catch (IOException e) {
                    Logger.log(LogLevel.ERROR, CLASS_NAME,
                            "Failed to close broadcast message receiver datagram channel");
                    Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
                }
            }
        }
    }

    private void writeBroadcastReplyMessage(byte[] ip4Address) {
        // Create a socket, and send a broadcast reply message
        try {
            Socket socket = new Socket(InetAddress.getByAddress(ip4Address), Constants.BROADCAST_REPLY_PORT);
            PrintStream broadcastReplyWriter = new PrintStream(socket.getOutputStream(), true);

            BroadcastReplyMessage out = new BroadcastReplyMessage(Identity.getMyUuid(), true,
                    HW_MONITOR_SYSTEM_UNIQUE_CONNECTION_ID, VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH,
                    siteLocalAddressInformation.getMacAddress(), siteLocalAddressInformation.getIp4Address(),
                    siteLocalAddressInformation.getHostname());
            broadcastReplyWriter.write(out.write(), 0, Message.NUM_BYTES);

            Logger.log(LogLevel.DEBUG, CLASS_NAME, "Sent broadcast acknowledgement to editor: " +
                    InetAddress.getByAddress(ip4Address));
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to send broadcast reply message");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
    }
}