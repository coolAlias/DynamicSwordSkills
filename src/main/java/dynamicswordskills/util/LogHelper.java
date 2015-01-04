/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.util;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.Logger;

public class LogHelper
{
	private static Logger logger;

	protected LogHelper() {}

	/**
	 * Initializes the logger; call this method before attempting to log any messages
	 */
	public static void init(FMLPreInitializationEvent event) {
		logger = event.getModLog();
	}

	/**
	 * Fatal error causes program to exit with message
	 */
	public static void fatal(String message) {
		logger.fatal(message);
	}

	public static void error(String message) {
		logger.error(message);
	}

	public static void warn(String message) {
		logger.warn(message);
	}

	public static void info(String message) {
		logger.info(message);
	}

	/**
	 * Prints a debug message to the fml-client log, but not the console
	 */
	public static void debug(String message) {
		logger.debug(message);
	}

	/**
	 * Prints a trace message to the fml-client log, but not the console
	 */
	public static void trace(String message) {
		logger.trace(message);
	}
}
