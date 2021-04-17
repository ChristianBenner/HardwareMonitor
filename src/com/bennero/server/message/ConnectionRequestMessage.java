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

import com.bennero.common.messages.ConnectionRequestDataPositions;

import static com.bennero.common.Constants.IP4_ADDRESS_NUM_BYTES;
import static com.bennero.common.Constants.NAME_STRING_NUM_BYTES;
import static com.bennero.common.networking.NetworkUtils.readBytes;
import static com.bennero.common.networking.NetworkUtils.readString;

/**
 * ConnectionRequestMessage stores the data associated to a connection request sent by a client. This includes the
 * version of the client, its IP4 address (so that the server can communicate with it on its approval/rejection of
 * connection), and its hostname.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ConnectionRequestMessage
{
    private final byte majorVersion;
    private final byte minorVersion;
    private final byte patchVersion;
    private final boolean forceConnection;
    private final byte[] ip4Address;
    private final String hostname;

    private ConnectionRequestMessage(byte majorVersion,
                                     byte minorVersion,
                                     byte patchVersion,
                                     boolean forceConnection,
                                     byte[] ip4Address,
                                     String hostname)
    {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        this.forceConnection = forceConnection;
        this.ip4Address = ip4Address;
        this.hostname = hostname;
    }

    public static ConnectionRequestMessage processConnectionRequestMessageData(byte[] bytes)
    {
        final int majorVersion = bytes[ConnectionRequestDataPositions.MAJOR_VERSION_POS] & 0xFF;
        final int minorVersion = bytes[ConnectionRequestDataPositions.MINOR_VERSION_POS] & 0xFF;
        final int patchVersion = bytes[ConnectionRequestDataPositions.PATCH_VERSION_POS] & 0xFF;
        final boolean forceConnection = bytes[ConnectionRequestDataPositions.FORCE_CONNECT] == 0x01;
        final byte[] ip4Address = readBytes(bytes, ConnectionRequestDataPositions.IP4_ADDRESS_POS,
                IP4_ADDRESS_NUM_BYTES);
        final String hostname = readString(bytes, ConnectionRequestDataPositions.HOSTNAME_POS, NAME_STRING_NUM_BYTES);

        return new ConnectionRequestMessage((byte) majorVersion, (byte) minorVersion, (byte) patchVersion, forceConnection,
                ip4Address, hostname);
    }

    public byte getMajorVersion()
    {
        return majorVersion;
    }

    public byte getMinorVersion()
    {
        return minorVersion;
    }

    public byte getPatchVersion()
    {
        return patchVersion;
    }

    public boolean isForceConnection()
    {
        return forceConnection;
    }

    public byte[] getIp4Address()
    {
        return ip4Address;
    }

    public String getHostname()
    {
        return hostname;
    }
}
