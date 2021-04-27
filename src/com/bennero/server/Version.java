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

/**
 * Specifies the version of the hardware monitor software. Comprised of major, minor and patch versions.
 * Major: A major release marks a milestone according to design and requirements
 * Minor: Addition of one or more design components/requirements. A minor version change should also be done if any
 * network messages have been altered as this effects compatibility with hardware monitor editor.
 * Patch: Small changes such as bug fixes or minor feature implementations. Any network message changes should be minor
 * not patch as it signifies incompatibility with hardware monitor editors that do not have the network message change.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class Version
{
    public static final byte VERSION_MAJOR = 1;
    public static final byte VERSION_MINOR = 1;
    public static final byte VERSION_PATCH = 0;

    public static String getVersionString()
    {
        return "v" + VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH;
    }
}
