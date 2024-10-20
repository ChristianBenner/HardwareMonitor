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

package com.bennero.server;

import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.TransitionType;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.FileDataPositions;
import com.bennero.common.networking.AddressInformation;
import com.bennero.common.networking.DiscoveredNetworkList;
import com.bennero.common.networking.NetworkUtils;
import com.bennero.common.osspecific.OSUtils;
import com.bennero.server.event.*;
import com.bennero.server.network.Server;
import com.bennero.server.pages.*;
import com.bennero.server.serial.SerialListener;
import com.bennero.server.ui.DisconnectButton;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

/**
 * Application class that controls all of the hardware monitor subsystems
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class ApplicationCore extends Application {
    enum CommunicationMode {
        Serial,
        Network,
    }

    public static final int WINDOW_WIDTH_PX = 800;
    public static final int WINDOW_HEIGHT_PX = 480;
    private static final String CLASS_NAME = ApplicationCore.class.getSimpleName();
    private HashMap<Byte, Sensor> sensorMap = new HashMap<>();
    private StackPane mainPane;

    private Thread serverThread;
    private Thread pageRollerThread;
    private Server server;
    private PageRoller pageRoller;
    private CommunicationMode connectionMode;

    private DisconnectButton disconnectButton;

    private void displayNetworkConnectionEntryPage(final String networkDevice,
                                                   final String networkSsid,
                                                   final String previousConnectionError) {
        mainPane.getChildren().clear();
        mainPane.getChildren().add(new NetworkConnectionEntryPage(networkDevice, networkSsid, previousConnectionError,
                event -> // Back button selected event
                {
                    displayNetworkSelectionPage();
                },
                event -> // Connecting event
                {
                    displayConnectingPage(event.getNetworkSSID());
                },
                event -> // Connected event
                {
                    runServer();
                },
                event -> // Failed to connect event
                {
                    displayNetworkConnectionEntryPage(event.getNetworkDevice(), event.getNetworkSsid(),
                            event.getPreviousConnectionError());
                }));
    }

    private void displayNetworkConnectionEntryPage(final String networkDevice,
                                                   final String networkSsid) {
        mainPane.getChildren().clear();
        mainPane.getChildren().add(new NetworkConnectionEntryPage(networkDevice, networkSsid,
                event -> // Back button selected event
                {
                    displayNetworkSelectionPage();
                },
                event -> // Connecting event
                {
                    displayConnectingPage(event.getNetworkSSID());
                },
                event -> // Connected event
                {
                    runServer();
                },
                event -> // Failed to connect event
                {
                    displayNetworkConnectionEntryPage(event.getNetworkDevice(), event.getNetworkSsid(),
                            event.getPreviousConnectionError());
                }));
    }

    private void displayDiscoveringNetworksPage() {
        mainPane.getChildren().clear();
        mainPane.getChildren().add(new InformationPage("Discovering Networks"));
    }

    private void displayNetworkErrorPage(String infoString) {
        mainPane.getChildren().clear();
        mainPane.getChildren().add(new InformationButtonPage("Error Discovering Networks", infoString,
                false, "Back", event ->
        {
            if (!NetworkUtils.isConnected()) {
                Logger.log(LogLevel.WARNING, CLASS_NAME, "Not connected to a network, opening connection page");
                displayNetworkSelectionPage();
            } else {
                // Go back to the waiting for connection page
                displayWaitingForConnectionPage();
            }
        }));
    }

    private void displayNetworkSelectionPage() {
        displayDiscoveringNetworksPage();

        // Discover networks on another thread to prevent locking up
        Runnable runnable = () ->
        {
            try {
                DiscoveredNetworkList discoveredNetworks = NetworkUtils.getWirelessNetworks();
                if(discoveredNetworks.size() == 0) {
                        Platform.runLater(() -> displayNetworkErrorPage("Failed to find any wireless networks"));
                } else {
                    Platform.runLater(() ->
                    {
                        // If an error occurred retrieving the networks, display it
                        if (discoveredNetworks.hasErrorOccurred()) {
                            displayNetworkErrorPage(discoveredNetworks.getErrorMessage());
                        } else {
                            mainPane.getChildren().clear();
                            mainPane.getChildren().add(new NetworkSelectionPane(discoveredNetworks, networkConnectionEntryEvent ->
                            {
                                // User has selected an SSID on the network list page, so display the network connection entry page
                                displayNetworkConnectionEntryPage(networkConnectionEntryEvent.getNetworkDevice(),
                                        networkConnectionEntryEvent.getNetworkSsid());
                            }));
                        }
                    });
                }
            } catch (Exception e) {
                Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to retrieve wireless networks");
                Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());

                Platform.runLater(() ->
                {
                    displayNetworkErrorPage("Unknown failure");
                });
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void displayConnectedPage() {
        mainPane.getChildren().clear();

        StackPane waitingPage = new StackPane();
        waitingPage.setId("standard-pane");
        VBox slide = new VBox();
        slide.setSpacing(5.0);
        Label titleLabel = new Label("Connected");
        slide.setId("hw-welcome-page-pane");
        titleLabel.setId("hw-welcome-page-title");
        slide.setAlignment(Pos.CENTER);
        slide.getChildren().add(titleLabel);

        Label infoLabel = new Label("Now create some pages in the editor");
        infoLabel.setId("hw-welcome-page-subtitle");
        slide.getChildren().add(infoLabel);

        StackPane.setAlignment(slide, Pos.CENTER);
        waitingPage.getChildren().add(slide);

        mainPane.getChildren().add(waitingPage);

        // Change network button in the top left corner that should disappear with a few seconds of no mouse movement
        disconnectButton = new DisconnectButton(actionEvent -> onNetDisconnect());
        disconnectButton.setVisible(false);
        mainPane.getChildren().add(disconnectButton);
        StackPane.setMargin(disconnectButton, new Insets(5, 5, 5, 5));
        StackPane.setAlignment(disconnectButton, Pos.TOP_LEFT);
    }

    public void displayConnectingPage(String ssid) {
        mainPane.getChildren().clear();
        InformationPage informationPage = new InformationPage("Connecting", ssid);
        mainPane.getChildren().add(informationPage);
    }

    public void displayWaitingForConnectionPage() {
        mainPane.getChildren().clear();
        try {
            if (NetworkUtils.isNetworkChangeSupported()) {
                InformationButtonPage informationPage = new InformationButtonPage("Waiting on Connection",
                        "My Hostname: " + InetAddress.getLocalHost().getHostName(), "Change Network",
                        (EventHandler<Event>) event -> displayNetworkSelectionPage());
                mainPane.getChildren().add(informationPage);
            } else {
                Logger.log(LogLevel.DEBUG, CLASS_NAME,
                        "Network change option not available on this device");
                InformationPage informationPage = new InformationPage("Waiting on Connection",
                        "My Hostname: " + InetAddress.getLocalHost().getHostName());
                mainPane.getChildren().add(informationPage);
            }
        } catch (UnknownHostException e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to determine hostname of this device");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());

            InformationPage informationPage = new InformationPage("Failed to determine hostname");
            mainPane.getChildren().add(informationPage);
        }
    }

    public void displayPage(CustomisableSensorPage customisableSensorPage, CustomisableSensorPage currentCustomisableSensorPage) {
        Logger.log(LogLevel.DEBUG, CLASS_NAME, "Display Page: " + customisableSensorPage.getTitle());

        // If we are trying to add a page before its evening finished transitioning away from itself, remove it and
        // re-add it. It may cause no page to show but this is a fault in the way the user has configured it
        if (mainPane.getChildren().contains(customisableSensorPage)) {
            if (customisableSensorPage.getTransitionControl() != null &&
                    customisableSensorPage.getTransitionControl().getStatus() == Animation.Status.RUNNING) {
                customisableSensorPage.setTransitionControl(null);
                customisableSensorPage.getTransitionControl().stop();
            }
        } else {
            mainPane.getChildren().add(customisableSensorPage);
        }

        // Change network button in the top left corner that should disappear with a few seconds of no mouse movement
        disconnectButton = new DisconnectButton(actionEvent -> onNetDisconnect());
        disconnectButton.setVisible(false);
        mainPane.getChildren().add(disconnectButton);
        StackPane.setMargin(disconnectButton, new Insets(5, 5, 5, 5));
        StackPane.setAlignment(disconnectButton, Pos.TOP_LEFT);

        if (currentCustomisableSensorPage != null) {
            if (customisableSensorPage.getTransitionType() == TransitionType.CUT) {
                mainPane.getChildren().remove(currentCustomisableSensorPage);
            } else {
                Transition transition = TransitionType.getTransition(customisableSensorPage.getTransitionType(), customisableSensorPage.getTransitionTime(),
                        mainPane, customisableSensorPage);
                customisableSensorPage.setTransitionControl(transition);
                transition.setOnFinished(actionEvent1 ->
                {
                    if (!mainPane.getChildren().isEmpty()) {
                        mainPane.getChildren().remove(currentCustomisableSensorPage);
                    }
                });
                transition.play();
            }
        }
    }

    public void removePage(CustomisableSensorPage customisableSensorPage) {
        mainPane.getChildren().remove(customisableSensorPage);
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        System.exit(0);
    }

    private void onConnect() {
        Platform.runLater(() -> displayConnectedPage());
    }

    private void onSerialDisconnect(SerialDisconnectionEvent disconnectionEvent) {
        if (connectionMode != CommunicationMode.Serial) {
            return;
        }

        Platform.runLater(() -> {
            pageRoller.removeAllPages();
            sensorMap.clear();

            String text = disconnectionEvent.isExpected() ? null : "Last session disconnected: " + disconnectionEvent.getReason();
            displaySerialAwaitingConnectionPage(text);
        });
    }

    private void onNetDisconnect() {
        if (connectionMode != CommunicationMode.Network) {
            return;
        }

        Platform.runLater(() -> {
            pageRoller.removeAllPages();
            sensorMap.clear();

            try {
                server.disconnectActiveConnection();
            } catch (InterruptedException e) {
                Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to disconnect active connection");
                Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
            }
            displayWaitingForConnectionPage();
        });
    }

    private void processPageMessageEvent(PageSetupEvent pageMessageEvent) {
        PageData pdRcv = pageMessageEvent.getPageData();
        Logger.logf(LogLevel.DEBUG, CLASS_NAME, "Received new page: [ID: %d], [TITLE: %s]", pdRcv.getUniqueId(), pdRcv.getTitle());

        Platform.runLater(() -> {
            if (!pageRoller.exists(pdRcv.getUniqueId())) {
                CustomisableSensorPage pgRcv = new CustomisableSensorPage(pdRcv);
                pageRoller.addPage(pgRcv);
            } else {
                pageRoller.updatePage(pdRcv);
            }
        });
    }

    private void processSensorMessageEvent(SensorSetupEvent sensorMessageEvent) {
        Sensor sensor = sensorMessageEvent.getSensor();
        Logger.logf(LogLevel.DEBUG, CLASS_NAME, "Received new sensor: [ID: %d], [TITLE: %s]", sensor.getUniqueId(), sensor.getTitle());

        Platform.runLater(() ->
        {
            sensorMap.put(sensor.getUniqueId(), sensor);
            pageRoller.addSensor(sensorMessageEvent.getPageId(), sensor);
        });
    }

    private void processRemovePageEvent(RemovePageEvent removePageEvent) {
        Logger.logf(LogLevel.DEBUG, CLASS_NAME, "Received request to remove page: [ID: %d]", removePageEvent.getPageId());
        Platform.runLater(() ->
        {
            pageRoller.removePage(removePageEvent.getPageId());
        });
    }

    private void processSensorTransformationEvent(SensorTransformationEvent event) {
        Logger.logf(LogLevel.DEBUG, CLASS_NAME, "Received sensor transformation request: [ID: %d], [PAGE: %d]", event.getSensorId(), event.getPageId());
        Platform.runLater(() -> pageRoller.transformSensor(event.getSensorId(), event.getPageId(), event.getRow(),
                event.getColumn(), event.getRowSpan(), event.getColumnSpan()));
    }

    private void processSensorDataEvent(SensorDataEvent sensorDataEvent) {
        Platform.runLater(() ->
        {
            byte key = sensorDataEvent.getSensorId();
            if(sensorMap.containsKey(key)) {
                sensorMap.get(key).setValue(sensorDataEvent.getValue());
            }
        });
    }

    private void processRemoveSensorEvent(RemoveSensorEvent removeSensorEvent) {
        Logger.logf(LogLevel.DEBUG, CLASS_NAME, "Received remove sensor request: [ID: %d], [PAGE: %d]", removeSensorEvent.getSensorId(), removeSensorEvent.getPageId());

        Platform.runLater(() -> pageRoller.removeSensor(removeSensorEvent.getSensorId(),
                removeSensorEvent.getPageId()));
    }

    private void processFileTransferEvent(FileTransferEvent fileTransferEvent) {
        Logger.logf(LogLevel.DEBUG, CLASS_NAME, "Received file: [Name: %s], [NumBytes: %d]", fileTransferEvent.getFileName(), fileTransferEvent.getFileBytes().length);

        // Save file to device
        String dir;
        switch (fileTransferEvent.getType()) {
            case FileDataPositions.TYPE_IMAGE:
                dir = OSUtils.getBackgroundImageDirectory() + "_in";
                break;
            case FileDataPositions.TYPE_SOFTWARE_UPDATE:
            default:
                dir = OSUtils.getApplicationDataDirectory() + "_in";
                break;
        }

        File directory = new File(dir);
        if(!directory.exists()) {
            directory.mkdirs();
        }

        String filePath = dir + File.separator + fileTransferEvent.getFileName();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(fileTransferEvent.getFileBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runServer() {
        try {
            // Start server and stuff now
            AddressInformation siteLocalAddress = NetworkUtils.getMyIpAddress();
            displayWaitingForConnectionPage();
            pageRoller = new PageRoller(this);
            pageRollerThread = new Thread(pageRoller);
            pageRollerThread.start();

            server = new Server(siteLocalAddress,
                    connectEvent -> onConnect(),
                    disconnectEvent -> onNetDisconnect(),
                    pageMessageEvent -> processPageMessageEvent(pageMessageEvent),
                    sensorMessageEvent -> processSensorMessageEvent(sensorMessageEvent),
                    removePageEvent -> processRemovePageEvent(removePageEvent),
                    sensorDataEvent -> processSensorDataEvent(sensorDataEvent),
                    removeSensorEvent -> processRemoveSensorEvent(removeSensorEvent),
                    sensorTransformationEvent -> processSensorTransformationEvent(sensorTransformationEvent));

            serverThread = new Thread(server);
            serverThread.start();
        } catch (Exception e) {
            Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to run server");
            Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
        }
    }

    public void displaySerialAwaitingConnectionPage(String string) {
        mainPane.getChildren().clear();
        InformationPage informationPage = new InformationPage("Awaiting USB Connection", string);
        mainPane.getChildren().add(informationPage);
    }

    @Override
    public void start(Stage stage) {
        boolean debugTerminal = false;
        boolean windowed = true;
        String serialPort = "";

        // Process parameters
        List<String> parameterList = super.getParameters().getRaw();
        for (int i = 0; i < parameterList.size(); i++) {
            switch (parameterList.get(i).toLowerCase()) {
                case "-l":
                case "--log":
                    // Shows the debug terminal
                    debugTerminal = true;
                    Logger.setLogLevel(LogLevel.DEBUG);
                    break;
                case "-d":
                case "--debug":
                    Logger.setLogLevel(LogLevel.DEBUG);
                    break;
                case "-w":
                case "--windowed":
                    windowed = true;
                    break;
                case "-f":
                case "--fullscreen":
                    windowed = false;
                    break;
                case "--serial-port":
                    if (parameterList.size() > i + 1 && !parameterList.get(i + 1).startsWith("--")) {
                        connectionMode = CommunicationMode.Serial;
                        serialPort = parameterList.get(i + 1);
                        i++;
                    } else {
                        // Err no serial port defined
                        Logger.log(LogLevel.ERROR, CLASS_NAME, "Serial port flag set but no port provided");
                    }
                    break;
                default:
                    connectionMode = CommunicationMode.Network;
                    break;
            }
        }

        mainPane = new StackPane();
        mainPane.setId("standard-pane");

        Scene uiScene;
        if (debugTerminal) {
            // Create the terminal overlay first thing so it can show all information
            TerminalOverlay terminalOverlay = new TerminalOverlay();

            StackPane root = new StackPane();
            root.getChildren().add(mainPane);
            root.getChildren().add(terminalOverlay);

            uiScene = new Scene(root, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX);
        } else {
            uiScene = new Scene(mainPane, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX);
        }

        stage.setTitle("Hardware Monitor " + Version.getVersionString());
        uiScene.getStylesheets().add("stylesheet.css");
        uiScene.setOnKeyPressed(keyEvent -> {
            if(disconnectButton != null && keyEvent.getCode() == KeyCode.ESCAPE) {
                disconnectButton.setVisible(!disconnectButton.isVisible());
            }
        });
        stage.setScene(uiScene);

        if (!windowed) {
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);
        }

        // Print some information relevant to this machine
        Logger.log(LogLevel.INFO, CLASS_NAME, "Running Hardware Monitor Server");
        Logger.log(LogLevel.INFO, CLASS_NAME, "Version: " + Version.getVersionString());
        Logger.log(LogLevel.INFO, CLASS_NAME, "OperatingSystem: " + OSUtils.getOperatingSystemString());

        Logger.log(LogLevel.INFO, CLASS_NAME, "TEST1");
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        Logger.log(LogLevel.INFO, CLASS_NAME, "TEST2");
        switch (connectionMode) {
            case Serial:
                Logger.log(LogLevel.INFO, CLASS_NAME, "TEST3");
                SerialListener serialListener = new SerialListener(
                        serialPort,
                        disconnectEvent -> onSerialDisconnect(disconnectEvent),
                        pageMessageEvent -> processPageMessageEvent(pageMessageEvent),
                        sensorMessageEvent -> processSensorMessageEvent(sensorMessageEvent),
                        removePageEvent -> processRemovePageEvent(removePageEvent),
                        sensorDataEvent -> processSensorDataEvent(sensorDataEvent),
                        removeSensorEvent -> processRemoveSensorEvent(removeSensorEvent),
                        sensorTransformationEvent -> processSensorTransformationEvent(sensorTransformationEvent),
                        fileTransferEvent -> processFileTransferEvent(fileTransferEvent));
                displaySerialAwaitingConnectionPage(null);
                Logger.log(LogLevel.INFO, CLASS_NAME, "TEST4");
                pageRoller = new PageRoller(this);
                pageRollerThread = new Thread(pageRoller);
                pageRollerThread.start();
                Logger.log(LogLevel.INFO, CLASS_NAME, "TEST5");
                serialListener.connect(event -> {
                    if(!event.isConnected()) {
                        displaySerialAwaitingConnectionPage(event.getError());
                    } else {
                        displayConnectedPage();
                    }
                });
                Logger.log(LogLevel.INFO, CLASS_NAME, "TEST6");
                break;
            case Network:
                if (!NetworkUtils.isConnected()) {
                    Logger.log(LogLevel.WARNING, CLASS_NAME, "Not connected to a network, opening connection page");
                    displayNetworkSelectionPage();
                } else {
                    runServer();
                }
                break;
        }
        Logger.log(LogLevel.INFO, CLASS_NAME, "TEST7");
        stage.show();
    }

}
