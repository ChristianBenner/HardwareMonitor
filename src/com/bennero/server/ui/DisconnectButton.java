package com.bennero.server.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class DisconnectButton extends Button {
    public DisconnectButton(EventHandler<ActionEvent> eventHandler) {
        super("Disconnect");
        setId("hw-default-button");
        Image changeNetworkIcon = new Image(getClass().getClassLoader().getResourceAsStream("disconnect_icon.png"));
        ImageView imageView = new ImageView(changeNetworkIcon);
        setGraphic(imageView);
        setCursor(Cursor.HAND);
        setOnAction(eventHandler);
    }
}
