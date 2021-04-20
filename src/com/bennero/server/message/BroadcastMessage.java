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

package com.bennero.server.message;

import com.bennero.common.messages.BroadcastAnnouncementDataPositions;
import com.bennero.common.networking.NetworkUtils;

import static com.bennero.common.Constants.HW_EDITOR_SYSTEM_UNIQUE_CONNECTION_ID;
import static com.bennero.common.Constants.IP4_ADDRESS_NUM_BYTES;
import static com.bennero.common.networking.NetworkUtils.readBytes;
import static com.bennero.common.networking.NetworkUtils.readLong;

/**
 * BroadcastMessage stores data from a received broadcast message such as the IP4 address that it came from. A
 * broadcast message should be sent out by a client that is looking for Hardware Monitor servers to connect to. It
 * contains the IP4 address of the broadcasting device so that the server can send a response back to it (approving or
 * denying). A BroadcastMessage is to be sent on a broadcast address so that all Hardware Monitor servers can see it.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class BroadcastMessage
{
    private final boolean verifiedBroadcastMessage;
    private final byte[] ip4Address;

    private BroadcastMessage(boolean verifiedBroadcastMessage, byte[] ip4Address)
    {
        this.verifiedBroadcastMessage = verifiedBroadcastMessage;
        this.ip4Address = ip4Address;
    }

    public static BroadcastMessage processBroadcastMessageData(byte[] bytes)
    {
        final long hwEditorSystemUniqueConnectionId =
                readLong(bytes, BroadcastAnnouncementDataPositions.HW_SYSTEM_IDENTIFIER_POS);

        // Ensures that the message came from a hardware monitor editor and not a random device on the network
        if (hwEditorSystemUniqueConnectionId == HW_EDITOR_SYSTEM_UNIQUE_CONNECTION_ID)
        {
            byte[] ip4Address = readBytes(bytes, BroadcastAnnouncementDataPositions.IP4_ADDRESS_POS,
                    IP4_ADDRESS_NUM_BYTES);
            System.out.println("Received a broadcast announcement message from: " + NetworkUtils.ip4AddressToString(ip4Address));
            return new BroadcastMessage(true, ip4Address);
        }

        return new BroadcastMessage(false, null);
    }

    public boolean isVerifiedBroadcastMessage()
    {
        return verifiedBroadcastMessage;
    }

    public byte[] getIp4Address()
    {
        return ip4Address;
    }
}
