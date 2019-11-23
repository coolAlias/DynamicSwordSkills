/**
    Copyright (C) <2019> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
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

package dynamicswordskills.client;

import javax.annotation.Nullable;

import net.minecraft.client.settings.KeyBinding;

public class KeyBindingHolder
{
	@Nullable
	private final KeyBinding key;

	public KeyBindingHolder(@Nullable KeyBinding key) {
		this.key = key;
	}

	public boolean isEnabled() {
		return this.key != null;
	}

	@Nullable
	public KeyBinding getKey() {
		return this.key;
	}

	/**
	 * Returns the current KeyBinding's key code or {@link Integer#MIN_VALUE} if the keybinding is not enabled
	 */
	public int getKeyCode() {
		return (this.isEnabled() ? this.key.getKeyCode() : Integer.MIN_VALUE);
	}

	/**
	 * Returns {@link KeyBinding#getIsKeyPressed()} if the key is enabled, otherwise false
	 */
	public boolean getIsKeyPressed() {
		return (this.isEnabled() ? this.key.getIsKeyPressed() : false);
	}

	/**
	 * Returns {@link KeyBinding#isPressed()} if the key is enabled, otherwise false
	 */
	public boolean isPressed() {
		return (this.isEnabled() ? this.key.isPressed() : false);
	}
}
