package com.bennero.server.pages;

import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.networking.ConnectionAttemptStatus;
import com.bennero.common.networking.NetworkUtils;
import com.bennero.server.event.NetworkConnectionEntryEvent;
import com.bennero.server.network.ConnectionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class NetworkConnectionEntryPage extends BorderPane {
    // Class name used in logging
    private static final String CLASS_NAME = NetworkConnectionEntryPage.class.getSimpleName();
    final EventHandler backButtonEvent;
    final EventHandler<ConnectionEvent> connectingEvent;
    final EventHandler connectedEvent;
    final EventHandler<NetworkConnectionEntryEvent> failedConnectionEvent;
    private final String networkDevice;
    private final String networkSsid;
    private final String previousConnectionError;
    private final boolean showPreviousConnectionError;
    private boolean showPassword;

    public NetworkConnectionEntryPage(final String networkDevice,
                                      final String networkSsid,
                                      final EventHandler backButtonEvent,
                                      final EventHandler<ConnectionEvent> connectingEvent,
                                      final EventHandler connectedEvent,
                                      final EventHandler<NetworkConnectionEntryEvent> failedConnectionEvent) {
        this.networkDevice = networkDevice;
        this.networkSsid = networkSsid;
        this.showPassword = false;
        this.showPreviousConnectionError = false;
        this.previousConnectionError = null;
        this.backButtonEvent = backButtonEvent;
        this.connectingEvent = connectingEvent;
        this.connectedEvent = connectedEvent;
        this.failedConnectionEvent = failedConnectionEvent;
        init();
    }

    public NetworkConnectionEntryPage(final String networkDevice,
                                      final String networkSsid,
                                      final String connectionErrorMessage,
                                      final EventHandler backButtonEvent,
                                      final EventHandler<ConnectionEvent> connectingEvent,
                                      final EventHandler connectedEvent,
                                      final EventHandler<NetworkConnectionEntryEvent> failedConnectionEvent) {
        this.networkDevice = networkDevice;
        this.networkSsid = networkSsid;
        this.previousConnectionError = connectionErrorMessage;
        this.showPreviousConnectionError = true;
        this.showPassword = false;
        this.backButtonEvent = backButtonEvent;
        this.connectingEvent = connectingEvent;
        this.connectedEvent = connectedEvent;
        this.failedConnectionEvent = failedConnectionEvent;
        init();
    }

    private void init() {
        BorderPane enterPasswordPane = new BorderPane();
        BorderPane passwordFooterPane = new BorderPane();

        Button backButton = new Button("Back");
        backButton.setId("hw-default-button");
        passwordFooterPane.setLeft(backButton);
        backButton.setOnAction(actionEvent ->
        {
            backButtonEvent.handle(null);
        });

        Button enterPasswordButton = new Button("Connect");
        enterPasswordButton.setId("hw-default-button");
        passwordFooterPane.setRight(enterPasswordButton);
        enterPasswordPane.setBottom(passwordFooterPane);

        VBox slide = new VBox();
        slide.setId("hw-welcome-page-pane");
        slide.setSpacing(5.0);

        Label infoLabel = new Label("Connect to " + networkSsid);
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
            final String PASSWORD = showPassword ? passwordTextField.getText() : passwordField.getText();

            connectingEvent.handle(new ConnectionEvent(networkSsid));

            try {
                final ConnectionAttemptStatus connectionAttemptStatus = NetworkUtils.connectToWifi(networkDevice,
                        networkSsid,
                        PASSWORD);

                switch (connectionAttemptStatus) {
                    case SUCCESS:
                        connectedEvent.handle(null);
                        break;
                    case OS_NOT_SUPPORTED:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Operating system not supported to connect to networks"));
                        break;
                    case FAILED_TO_WRITE_NETWORK_DATA_FILE:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Failed to write to the network data file"));
                        break;
                    case NETWORK_DATA_FILE_NOT_FOUND:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Failed to locate the network data file"));
                        break;
                    case FAILED_TO_RECONFIGURE_NETWORK:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Failed to reconfigure the network"));
                        break;
                    case FAILED_TO_CONNECT:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Failed to connect"));
                        break;
                    case INCORRECT_PASSWORD:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Password entered was incorrect"));
                        break;
                    case PASSWORD_REQUIRED:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "This network requires a password"));
                        break;
                    case UNKNOWN:
                    default:
                        failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkDevice, networkSsid,
                                "Unknown failure"));
                        break;
                }
            } catch (Exception e) {
                Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to connect to network " + networkSsid);
                Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
                failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkSsid,
                        "Unsupported operating system"));
            }
        });

        Button togglePasswordView = new Button("V");
        togglePasswordView.setId("hw-default-button");
        togglePasswordView.setOnAction(actionEvent ->
        {
            showPassword = !showPassword;

            if (showPassword) {
                passwordTextField.setText(passwordField.getText());
                passwordPane.getChildren().remove(passwordField);
                passwordPane.getChildren().add(passwordTextField);
            } else {
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

        // Shows the previous connection error if it was specified
        if (showPreviousConnectionError) {
            Label connectionFailedLabel = new Label("Connection Failed: " + previousConnectionError);
            connectionFailedLabel.setId("hw-network-connection-failed-label");
            slide.getChildren().add(connectionFailedLabel);
        }

        StackPane.setAlignment(slide, Pos.CENTER);

        super.getChildren().clear();
        enterPasswordPane.setCenter(slide);
        super.setCenter(enterPasswordPane);
    }
}
