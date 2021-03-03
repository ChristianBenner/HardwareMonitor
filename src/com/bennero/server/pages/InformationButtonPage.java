/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * An additional term included with this license is the requirement to preserve legal notices and author attributions
 * such as this one. Do not remove the original author license notices from the program unless given permission from
 * the original author: christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.server.pages;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A page that displays configurable information/text and a button
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class InformationButtonPage extends StackPane
{
    public InformationButtonPage(String title, String info, String buttonText, EventHandler buttonEvent)
    {
        init(title, info, buttonText, buttonEvent);
    }

    private void init(String title, String info, String buttonText, EventHandler buttonEvent)
    {
        super.setId("standard-pane");
        VBox slide = new VBox();
        slide.setSpacing(5.0);
        Label titleLabel = new Label(title);
        slide.setId("hw-welcome-page-pane");
        titleLabel.setId("hw-welcome-page-title");
        slide.setAlignment(Pos.CENTER);
        slide.getChildren().add(titleLabel);

        if (info != null)
        {
            Label infoLabel = new Label(info);
            infoLabel.setId("hw-welcome-page-subtitle");
            slide.getChildren().add(infoLabel);
        }

        Button button = new Button(buttonText);
        button.setId("hw-default-button");
        button.setOnAction(buttonEvent);
        slide.getChildren().add(button);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        slide.getChildren().add(progressIndicator);

        StackPane.setAlignment(slide, Pos.CENTER);
        super.getChildren().add(slide);
    }
}