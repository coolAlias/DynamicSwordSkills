/**
    Copyright (C) <2016> <coolAlias>

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

import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

public class CommandRemoveSkill extends CommandBase
{
	public static final ICommand INSTANCE = new CommandRemoveSkill();

	public CommandRemoveSkill() {}

	@Override
	public String getCommandName() {
		return "removeskill";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	/**
	 * removeskill <player> <skill | all>
	 */
	@Override
	public String getCommandUsage(ICommandSender player) {
		return "commands.removeskill.usage";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 2) {
			EntityPlayerMP commandSender = CommandBase.getCommandSenderAsPlayer(sender);
			EntityPlayerMP player = CommandBase.getPlayer(sender, args[0]);
			boolean all = ("all").equals(args[1]);
			SkillBase skill = null;
			if (!all) {
				skill = SkillBase.getSkillByName(args[1]);
				if (skill == null) {
					throw new CommandException("commands.skill.generic.unknown", args[1]);
				}
			}
			if (DSSPlayerInfo.get(player).removeSkill(args[1])) {
				if (all) {
					PlayerUtils.sendTranslatedChat(commandSender, "commands.removeskill.success.all", player.getCommandSenderName());
				} else {
					PlayerUtils.sendTranslatedChat(commandSender, "commands.removeskill.success.one", player.getCommandSenderName(), new ChatComponentTranslation(skill.getTranslationString()));
				}
			} else { // player didn't have this skill
				if (all) {
					throw new CommandException("commands.removeskill.failure.all", player.getCommandSenderName());
				} else {
					throw new CommandException("commands.removeskill.failure.one", player.getCommandSenderName(), new ChatComponentTranslation(skill.getTranslationString()));
				}
			}
		} else {
			throw new WrongUsageException(getCommandUsage(sender));
		}
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		switch(args.length) {
		case 1: return CommandBase.getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		case 2: return CommandBase.getListOfStringsMatchingLastWord(args, SkillBase.getSkillNames());
		default: return null;
		}
	}
}
