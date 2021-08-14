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

package com.bennero.server.pages;

import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.networking.DiscoveredNetwork;
import com.bennero.common.networking.DiscoveredNetworkList;
import com.bennero.common.networking.NetworkUtils;
import com.bennero.server.ApplicationCore;
import com.bennero.server.event.NetworkConnectionEntryEvent;
import com.bennero.server.event.PageSetupEvent;
import com.bennero.server.message.PageSetupMessage;
import com.bennero.server.network.ConnectionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/**
 * Network selection pane is a class that will present a list of available networks. This is so that the user of the
 * hardware monitor does not have to exit the application in order to connect to a Wi-Fi network (as the hardware
 * monitor in many cases may be used without a OS GUI, connecting to Wi-Fi may be quite difficult for some users)
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class NetworkSelectionPane extends BorderPane
{
    private static final String CLASS_NAME = NetworkSelectionPane.class.getName();

    private DiscoveredNetworkList discoveredNetworks;
    private final EventHandler<NetworkConnectionEntryEvent> networkConnectionEntryEventHandler;
    private BorderPane sensorOverview;
    private ListView<DiscoveredNetwork> ssidListView;
    private Button refreshButton;
    private Button selectButton;

    public NetworkSelectionPane(final DiscoveredNetworkList discoveredNetworks,
                                final EventHandler<NetworkConnectionEntryEvent> networkConnectionEntryEventHandler)
    {
        this.discoveredNetworks = discoveredNetworks;
        this.networkConnectionEntryEventHandler = networkConnectionEntryEventHandler;

        super.setPadding(new Insets(10));

        sensorOverview = new BorderPane();
        ssidListView = new ListView<>();

        BorderPane titleAndHardwareCollection = new BorderPane();
        Label title = new Label("Select Network");
        titleAndHardwareCollection.setTop(title);

        BorderPane.setAlignment(title, Pos.CENTER);

        BorderPane footerPane = new BorderPane();

        refreshButton = new Button("Refresh");
        refreshButton.setId("hw-default-button");
        footerPane.setLeft(refreshButton);

        // Refresh button clears the list and then re-adds the found wireless networks
        refreshButton.setOnAction(actionEvent -> refreshNetworkList());

        selectButton = new Button("Connect");
        selectButton.setId("hw-default-button");
        footerPane.setRight(selectButton);

        sensorOverview.setTop(titleAndHardwareCollection);
        sensorOverview.setCenter(ssidListView);
        sensorOverview.setBottom(footerPane);

        selectButton.setOnMouseClicked(mouseEvent -> connectToNetwork(ssidListView.getSelectionModel().getSelectedItem()));
        addNetworksToList(discoveredNetworks);

        super.setCenter(sensorOverview);

        super.setId("standard-pane");
        title.setId("pane-title");
    }

    private void addNetworksToList(final DiscoveredNetworkList discoveredNetworks)
    {
        // If there are multiple devices, include the device name in the string
        boolean includeDeviceName = discoveredNetworks.getNumberOfDevices() > 1;

        for(int i = 0; i < discoveredNetworks.size(); i++)
        {
            discoveredNetworks.get(i).setIncludeDeviceStr(includeDeviceName);
            ssidListView.getItems().add(discoveredNetworks.get(i));
        }
    }

    private void connectToNetwork(final DiscoveredNetwork discoveredNetwork)
    {
        if (discoveredNetwork != null && discoveredNetwork.getNetworkDevice() != null &&
                discoveredNetwork.getNetworkSsid() != null && !discoveredNetwork.getNetworkDevice().isEmpty() &&
                !discoveredNetwork.getNetworkSsid().isEmpty())
        {
            networkConnectionEntryEventHandler.handle(new NetworkConnectionEntryEvent(discoveredNetwork));
        }
        else
        {
            Logger.log(LogLevel.ERROR, CLASS_NAME,
                    "Failed to connect to network due to invalid or empty selection from the network list");
        }
    }

    private void refreshNetworkList()
    {
        discoveredNetworks.clear();
        ssidListView.getItems().clear();
        try
        {
            discoveredNetworks.addAll(NetworkUtils.getWirelessNetworks());
            addNetworksToList(discoveredNetworks);
        }
        catch (Exception e)
        {
            Logger.log(LogLevel.ERROR, CLASS_NAME,
                    "Failed to refresh network list due to error in obtaining the available wireless networks");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
    }
}
