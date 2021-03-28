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

import com.bennero.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

/**
 * TerminalOverlay is a developer/debug only class used to show logging information over the top of the hardware
 * monitor interface. It is useful for debugging the application whilst in fullscreen mode or on systems with no OS GUI
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class TerminalOverlay extends TextArea
{
    public TerminalOverlay()
    {
        // Create terminal UI
        setId("hw-debug-terminal");
        setWrapText(true);
        setMouseTransparent(true);

        Logger.addLogEventHandler(logEvent ->
        {
            TerminalOverlay ref = this;

            Platform.runLater(() ->
            {
                appendText(logEvent.getLogText() + "\n");
                setScrollTop(0.0);
                setScrollLeft(0.0);

                FadeTransition transition = new FadeTransition(Duration.seconds(1), ref);
                transition.setFromValue(1.0);
                transition.setToValue(0.3);
                transition.play();
            });
        });
    }
}
