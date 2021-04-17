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

package com.bennero.server.message;

import com.bennero.common.PageData;
import com.bennero.common.messages.PageDataPositions;
import javafx.scene.paint.Color;

import static com.bennero.common.Constants.NAME_STRING_NUM_BYTES;
import static com.bennero.common.networking.NetworkUtils.readInt;
import static com.bennero.common.networking.NetworkUtils.readString;

/**
 * PageSetupMessage stores the data of a page creation request. The PageSetupMessage is sent by a connected client
 * only. If a PageSetupMessage is received but the ID of the page already exists, the page with that ID will be
 * updated with the new information (allowing to change of attributes such as page colour, titles etc).
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class PageSetupMessage
{
    private PageData pageData;

    private PageSetupMessage(PageData pageData)
    {
        this.pageData = pageData;
    }

    public static PageSetupMessage readPageSetupMessage(byte[] bytes)
    {
        final int pageId = bytes[PageDataPositions.ID_POS] & 0xFF;
        final int pageColourR = bytes[PageDataPositions.COLOUR_R_POS] & 0xFF;
        final int pageColourG = bytes[PageDataPositions.COLOUR_G_POS] & 0xFF;
        final int pageColourB = bytes[PageDataPositions.COLOUR_B_POS] & 0xFF;
        final int titleColourR = bytes[PageDataPositions.TITLE_COLOUR_R_POS] & 0xFF;
        final int titleColourG = bytes[PageDataPositions.TITLE_COLOUR_G_POS] & 0xFF;
        final int titleColourB = bytes[PageDataPositions.TITLE_COLOUR_B_POS] & 0xFF;
        final int subtitleColourR = bytes[PageDataPositions.SUBTITLE_COLOUR_R_POS] & 0xFF;
        final int subtitleColourG = bytes[PageDataPositions.SUBTITLE_COLOUR_G_POS] & 0xFF;
        final int subtitleColourB = bytes[PageDataPositions.SUBTITLE_COLOUR_B_POS] & 0xFF;
        final int pageRows = bytes[PageDataPositions.ROWS_POS] & 0xFF;
        final int pageColumns = bytes[PageDataPositions.COLUMNS_POS] & 0xFF;
        final int nextPageId = bytes[PageDataPositions.NEXT_ID_POS] & 0xFF;
        final int pageTransitionType = bytes[PageDataPositions.TRANSITION_TYPE_POS] & 0xFF;
        final int pageTransitionTime = readInt(bytes, PageDataPositions.TRANSITION_TIME_POS);
        final int pageDurationMs = readInt(bytes, PageDataPositions.DURATION_MS_POS);
        final String title = readString(bytes, PageDataPositions.TITLE_POS, NAME_STRING_NUM_BYTES);
        final int titleEnabled = bytes[PageDataPositions.TITLE_ENABLED_POS] & 0xFF;
        final int titleAlignment = bytes[PageDataPositions.TITLE_ALIGNMENT_POS] & 0xFF;
        final String subtitle = readString(bytes, PageDataPositions.SUBTITLE_POS, NAME_STRING_NUM_BYTES);
        final int subtitleEnabled = bytes[PageDataPositions.SUBTITLE_POS_ENABLED_POS] & 0xFF;
        final int subtitleAlignment = bytes[PageDataPositions.SUBTITLE_POS_ALIGNMENT_POS] & 0xFF;

        Color backgroundColour = Color.rgb(pageColourR, pageColourG, pageColourB);
        Color titleColor = Color.rgb(titleColourR, titleColourG, titleColourB);
        Color subtitleColor = Color.rgb(subtitleColourR, subtitleColourG, subtitleColourB);

        PageData pageData = new PageData(pageId, backgroundColour, titleColor, subtitleColor, pageRows, pageColumns,
                nextPageId, pageTransitionType, pageTransitionTime, pageDurationMs, title,
                titleEnabled == 0x01, titleAlignment, subtitle, subtitleEnabled == 0x01,
                subtitleAlignment);

        return new PageSetupMessage(pageData);
    }

    public PageData getPageData()
    {
        return pageData;
    }
}
