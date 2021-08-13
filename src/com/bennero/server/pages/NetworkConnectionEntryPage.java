package com.bennero.server.pages;

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

public class NetworkConnectionEntryPage extends BorderPane
{
    private final String networkSsid;
    private final String previousConnectionError;
    private final boolean showPreviousConnectionError;

    final EventHandler backButtonEvent;
    final EventHandler<ConnectionEvent> connectingEvent;
    final EventHandler connectedEvent;
    final EventHandler<NetworkConnectionEntryEvent> failedConnectionEvent;

    private boolean showPassword;

    public NetworkConnectionEntryPage(final String networkSsid,
                                      final EventHandler backButtonEvent,
                                      final EventHandler<ConnectionEvent> connectingEvent,
                                      final EventHandler connectedEvent,
                                      final EventHandler<NetworkConnectionEntryEvent> failedConnectionEvent)
    {
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

    public NetworkConnectionEntryPage(final String networkSsid,
                                      final String connectionErrorMessage,
                                      final EventHandler backButtonEvent,
                                      final EventHandler<ConnectionEvent> connectingEvent,
                                      final EventHandler connectedEvent,
                                      final EventHandler<NetworkConnectionEntryEvent> failedConnectionEvent)
    {
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

    private void init()
    {
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

            try
            {
                if (NetworkUtils.connectToWifi(networkSsid, PASSWORD))
                {
                    connectedEvent.handle(null);
                }
                else
                {
                    failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkSsid,
                            "No connection or incorrect password"));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                failedConnectionEvent.handle(new NetworkConnectionEntryEvent(networkSsid,
                        "Unsupported operating system"));
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

        // Shows the previous connection error if it was specified
        if(showPreviousConnectionError)
        {
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
