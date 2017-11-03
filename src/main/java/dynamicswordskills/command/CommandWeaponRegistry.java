/**
    Copyright (C) <2017> <coolAlias>

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

package dynamicswordskills.command;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import swordskillsapi.api.item.WeaponRegistry;

public class CommandWeaponRegistry extends CommandBase
{
	public static final ICommand INSTANCE = new CommandWeaponRegistry();

	private CommandWeaponRegistry() {}

	@Override
	public String getName() {
		return "dssweaponregistry";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	/**
	 * dssweaponregistry <allow|forbid> <sword|weapon> modid:item_name
	 */
	@Override
	public String getUsage(ICommandSender sender) {
		return "commands.dssweaponregistry.usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args == null || args.length != 3) {
			throw new WrongUsageException(getUsage(sender));
		}
		String[] parts = WeaponRegistry.parseString(args[2]);
		if (parts != null) {
			Item item = Item.REGISTRY.getObject(new ResourceLocation(parts[0], parts[1]));
			if (item == null) {
				throw new WrongUsageException("commands.dssweaponregistry.item.unknown", parts[1], parts[0]);
			}
			boolean isSword = isSword(args[1]);
			String msg = "commands.dssweaponregistry." + (isSword ? "sword." : "weapon.");
			if (isAllow(args[0])) {
				msg += "allow.";
				if (isSword) {
					msg += (WeaponRegistry.INSTANCE.registerSword("Command", parts[0], item) ? "success" : "fail");
				} else if (WeaponRegistry.INSTANCE.registerWeapon("Command", parts[0], item)) {
					msg += "success";
				} else {
					msg += "fail";
				}
			} else {
				msg += "forbid.";
				if (isSword) {
					msg += (WeaponRegistry.INSTANCE.removeSword("Command", parts[0], item) ? "success" : "fail");
				} else if (WeaponRegistry.INSTANCE.removeWeapon("Command", parts[0], item)) {
					msg += "success";
				} else {
					msg += "fail";
				}
			}
			sender.sendMessage(new TextComponentTranslation(msg, args[2]));
		} else {
			throw new WrongUsageException(getUsage(sender));
		}
	}

	private boolean isAllow(String arg) throws CommandException {
		if (arg.equalsIgnoreCase("allow")) {
			return true;
		} else if (arg.equalsIgnoreCase("forbid")) {
			return false;
		}
		throw new WrongUsageException("commands.dssweaponregistry.action.unknown");
	}

	private boolean isSword(String arg) throws CommandException {
		if (arg.equalsIgnoreCase("sword")) {
			return true;
		} else if (arg.equalsIgnoreCase("weapon")) {
			return false;
		}
		throw new WrongUsageException("commands.dssweaponregistry.type.unknown");
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
		switch (args.length) {
		case 1: return CommandBase.getListOfStringsMatchingLastWord(args, "allow", "forbid");
		case 2: return CommandBase.getListOfStringsMatchingLastWord(args, "sword", "weapon");
		}
		return null;
	}
}
