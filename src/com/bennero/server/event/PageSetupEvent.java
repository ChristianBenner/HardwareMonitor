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

import com.bennero.common.PageData;
import com.bennero.common.messages.PageCreateMessage;
import javafx.event.Event;
import javafx.scene.paint.Color;

/**
 * PageSetupEvent creates an event that is used to provide a PageSetupMessage object back to a listener through an
 * EventHandler
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see PageCreateMessage
 * @see Event
 * @since 1.0
 */
public class PageSetupEvent extends Event {
    private final PageData pageData;

    public PageSetupEvent(final PageCreateMessage message) {
        super(message, null, null);

//        final int colourR = message.getColourR() & 0xFF;
//        final int colourG = message.getColourG() & 0xFF;
//        final int colourB = message.getColourB() & 0xFF;
//        final int titleColourR = message.getTitleColourR() & 0xFF;
//        final int titleColourG = message.getTitleColourG() & 0xFF;
//        final int titleColourB = message.getTitleColourB() & 0xFF;
//        final int subtitleColourR = message.getSubtitleColourR() & 0xFF;
//        final int subtitleColourG = message.getSubtitleColourG() & 0xFF;
//        final int subtitleColourB = message.getSubtitleColourB() & 0xFF;
//        final int rows = message.getRows() & 0xFF;
//        final int columns = message.getColumns()& 0xFF;
//        final int transitionType = message.getTransitionType() & 0xFF;
//        final int titleAlignment = message.getTitleAlignment() & 0xFF;
//        final int subtitleAlignment = message.getSubtitleAlignment() & 0xFF;
//
        pageData = new PageData(
                message.getPageId(),
                toColor(message.getColourR(), message.getColourG(), message.getColourB()),
                toColor(message.getTitleColourR(), message.getTitleColourG(), message.getTitleColourB()),
                toColor(message.getSubtitleColourR(), message.getSubtitleColourG(), message.getSubtitleColourB()),
                message.getRows() & 0xFF, message.getColumns() & 0xFF,
                message.getNextPageId(),
                message.getTransitionType() & 0xFF, message.getTransitionTime(), message.getDurationMs(),
                message.getTitle(), message.isTitleEnabled(), message.getTitleAlignment() & 0xFF,
                message.getSubtitle(), message.isSubtitleEnabled(), message.getSubtitleAlignment() & 0xFF,
                message.getBackgroundImage()
        );
    }

    private Color toColor(byte r, byte g, byte b) {
        return Color.rgb(r & 0xFF, g & 0xFF, b & 0xFF);
    }

    public PageData getPageData() {
        return pageData;
    }
}