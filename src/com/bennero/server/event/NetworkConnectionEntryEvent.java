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

package com.bennero.server.event;

import com.bennero.common.networking.DiscoveredNetwork;
import javafx.event.Event;

/**
 * NetworkConnectionEntryEvent creates an event that is used to begin the network connection entry process upon the
 * selection of a network SSID
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see Event
 * @since 1.0
 */
public class NetworkConnectionEntryEvent extends Event {
    private final String networkDevice;
    private final String networkSsid;
    private final String previousConnectionError;
    private final boolean showPreviousConnectionError;

    public NetworkConnectionEntryEvent(final DiscoveredNetwork discoveredNetwork) {
        super(discoveredNetwork.getNetworkSsid(), null, null);
        this.networkDevice = discoveredNetwork.getNetworkDevice();
        this.networkSsid = discoveredNetwork.getNetworkSsid();
        this.previousConnectionError = null;
        this.showPreviousConnectionError = false;
    }

    public NetworkConnectionEntryEvent(final String networkDevice, final String networkSsid) {
        super(networkSsid, null, null);
        this.networkDevice = networkDevice;
        this.networkSsid = networkSsid;
        this.previousConnectionError = null;
        this.showPreviousConnectionError = false;
    }

    public NetworkConnectionEntryEvent(final DiscoveredNetwork discoveredNetwork, final String connectionErrorMessage) {
        super(discoveredNetwork.getNetworkSsid(), null, null);
        this.networkDevice = discoveredNetwork.getNetworkDevice();
        this.networkSsid = discoveredNetwork.getNetworkSsid();
        this.previousConnectionError = connectionErrorMessage;
        this.showPreviousConnectionError = true;
    }

    public NetworkConnectionEntryEvent(final String networkDevice,
                                       final String networkSsid,
                                       final String connectionErrorMessage) {
        super(networkSsid, null, null);
        this.networkDevice = networkDevice;
        this.networkSsid = networkSsid;
        this.previousConnectionError = connectionErrorMessage;
        this.showPreviousConnectionError = true;
    }

    public String getNetworkDevice() {
        return networkDevice;
    }

    public String getNetworkSsid() {
        return networkSsid;
    }

    public String getPreviousConnectionError() {
        return previousConnectionError;
    }

    public boolean hasPreviousConnectionError() {
        return showPreviousConnectionError;
    }
}