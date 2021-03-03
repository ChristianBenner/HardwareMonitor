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

package com.bennero.server.event;

import com.bennero.common.PageData;
import com.bennero.server.message.PageSetupMessage;
import javafx.event.Event;

/**
 * PageSetupEvent creates an event that is used to provide a PageSetupMessage object back to a listener through an
 * EventHandler
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see PageSetupMessage
 * @see Event
 * @since 1.0
 */
public class PageSetupEvent extends Event
{
    private final PageSetupMessage pageSetupMessage;

    public PageSetupEvent(final PageSetupMessage pageSetupMessage)
    {
        super(pageSetupMessage, null, null);
        this.pageSetupMessage = pageSetupMessage;
    }

    public PageData getPageData()
    {
        return pageSetupMessage.getPageData();
    }
}