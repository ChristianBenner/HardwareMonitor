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

import com.bennero.networking.NetworkUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    private BorderPane sensorOverview;
    private ListView<String> ssidListView;
    private Button refreshButton;
    private Button selectButton;
    private boolean showPassword;
    private EventHandler<ConnectionEvent> connectingEvent;
    private EventHandler connectedEvent;
    private EventHandler<ConnectionEvent> failedConnectionEvent;
    public NetworkSelectionPane(EventHandler<ConnectionEvent> connectingEvent,
                                EventHandler connectedEvent,
                                EventHandler<ConnectionEvent> failedConnectionEvent)
    {
        this.connectingEvent = connectingEvent;
        this.connectedEvent = connectedEvent;
        this.failedConnectionEvent = failedConnectionEvent;

        super.setPadding(new Insets(10));
        showPassword = false;

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
        refreshButton.setOnAction(actionEvent ->
        {
            ssidListView.getItems().clear();
            try
            {
                ssidListView.getItems().addAll(NetworkUtils.getWirelessNetworks());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        selectButton = new Button("Connect");
        selectButton.setId("hw-default-button");
        footerPane.setRight(selectButton);

        sensorOverview.setTop(titleAndHardwareCollection);
        sensorOverview.setCenter(ssidListView);
        sensorOverview.setBottom(footerPane);

        selectButton.setOnMouseClicked(mouseEvent ->
        {
            if (ssidListView.getSelectionModel().getSelectedItem() != null)
            {
                BorderPane enterPasswordPane = new BorderPane();
                BorderPane passwordFooterPane = new BorderPane();

                Button backButton = new Button("Back");
                backButton.setId("hw-default-button");
                passwordFooterPane.setLeft(backButton);
                backButton.setOnAction(actionEvent ->
                {
                    getChildren().clear();
                    setCenter(sensorOverview);
                });

                Button enterPasswordButton = new Button("Connect");
                enterPasswordButton.setId("hw-default-button");
                passwordFooterPane.setRight(enterPasswordButton);
                enterPasswordPane.setBottom(passwordFooterPane);

                VBox slide = new VBox();
                slide.setId("hw-welcome-page-pane");
                slide.setSpacing(5.0);

                Label infoLabel = new Label("Connect to " + ssidListView.getSelectionModel().getSelectedItem());
                infoLabel.setId("hw-welcome-page-subtitle");

                HBox passwordEntryBox = new HBox();
                passwordEntryBox.setAlignment(Pos.CENTER);
                passwordEntryBox.setSpacing(5.0);

                Label passwordLabel = new Label("Password: ");
                passwordLabel.setId("hw-welcome-page-subtitle");

                HBox passwordPane = new HBox();
                passwordPane.setAlignment(Pos.CENTER);

                PasswordField passwordField = new PasswordField();
                passwordField.setId("hw-text-field");

                TextField passwordTextField = new TextField();
                passwordTextField.setId("hw-text-field");

                enterPasswordButton.setOnAction(actionEvent ->
                {
                    final String SELECTED_SSID = ssidListView.getSelectionModel().getSelectedItem();
                    final String PASSWORD = showPassword ? passwordTextField.getText() : passwordField.getText();

                    connectingEvent.handle(new ConnectionEvent(SELECTED_SSID));

                    try
                    {
                        if (NetworkUtils.connectToWifi(SELECTED_SSID, PASSWORD))
                        {
                            connectedEvent.handle(null);
                        }
                        else
                        {
                            failedConnectionEvent.handle(new ConnectionEvent(SELECTED_SSID));
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        failedConnectionEvent.handle(new ConnectionEvent(SELECTED_SSID));
                    }
                });

                Button togglePasswordView = new Button("V");
                togglePasswordView.setId("hw-default-button");
                togglePasswordView.setOnAction(actionEvent ->
                {
                    showPassword = !showPassword;

                    if (showPassword)
                    {
                        passwordTextField.setText(passwordField.getText());
                        passwordPane.getChildren().remove(passwordField);
                        passwordPane.getChildren().add(passwordTextField);
                    }
                    else
                    {
                        passwordField.setText(passwordTextField.getText());
                        passwordPane.getChildren().remove(passwordTextField);
                        passwordPane.getChildren().add(passwordField);
                    }

                });

                passwordEntryBox.getChildren().add(passwordLabel);

                passwordPane.getChildren().add(passwordField);
                passwordEntryBox.getChildren().add(passwordPane);

                passwordEntryBox.getChildren().add(togglePasswordView);

                slide.setAlignment(Pos.CENTER);
                slide.getChildren().add(infoLabel);
                slide.getChildren().addAll(passwordEntryBox);
                StackPane.setAlignment(slide, Pos.CENTER);

                super.getChildren().clear();
                enterPasswordPane.setCenter(slide);
                super.setCenter(enterPasswordPane);

                // todo: Write code to set rpi network interface
            }
        });

        try
        {
            ssidListView.getItems().addAll(NetworkUtils.getWirelessNetworks());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        super.setCenter(sensorOverview);

        super.setId("standard-pane");
        title.setId("pane-title");
    }

    public class ConnectionEvent extends Event
    {
        private String networkSSID;

        public ConnectionEvent(String networkSSID)
        {
            super(networkSSID, null, null);
            this.networkSSID = networkSSID;
        }

        public String getNetworkSSID()
        {
            return networkSSID;
        }
    }
}
