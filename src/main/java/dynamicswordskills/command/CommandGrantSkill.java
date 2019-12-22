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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 
 * Grants the skill named at the designated level; if the player's skill level
 * is already equal or higher, nothing happens.
 *
 */
public class CommandGrantSkill extends CommandBase
{
	public static final ICommand INSTANCE = new CommandGrantSkill();

	public CommandGrantSkill() {}

	@Override
	public String getCommandName() {
		return "grantskill";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	/**
	 * 	grantskill <player> <skill> <level> OR grantskill <player> all
	 */
	@Override
	public String getCommandUsage(ICommandSender player) {
		return "commands.grantskill.usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		EntityPlayerMP commandSender = CommandBase.getCommandSenderAsPlayer(sender);
		EntityPlayerMP player = CommandBase.getPlayer(server, sender, args[0]);
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		if (args.length == 2 && ("all").equals(args[1])) {
			boolean flag = true;
			for (SkillBase skill : SkillBase.getSkills()) {
				if (Config.isSkillEnabled(skill) && !skills.grantSkill(skill, skill.getMaxLevel())) {
					flag = false;
				}
			}
			if (flag) {
				PlayerUtils.sendTranslatedChat(player, "commands.grantskill.notify.all");
				if (commandSender != player) {
					PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.all", player.getDisplayName());
				}
			} else {
				PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.partial", player.getDisplayName());
			}
		} else if (args.length == 3) {
			SkillBase skill = SkillBase.getSkillByName(args[1]);
			if (skill == null) {
				throw new CommandException("commands.skill.generic.unknown", args[1]);
			}
			int level = CommandBase.parseInt(args[2], 1, 10);
			int oldLevel = skills.getTrueSkillLevel(skill);
			if (level > oldLevel) { // grants skill up to level or max level, whichever is reached first
				if (!Config.isSkillEnabled(skill)) {
					throw new CommandException("commands.grantskill.failure.disabled", new TextComponentTranslation(skill.getTranslationString()));
				} else if (skills.grantSkill(skill, (byte) level)) {
					PlayerUtils.sendTranslatedChat(player, "commands.grantskill.notify.one", new TextComponentTranslation(skill.getTranslationString()), skills.getTrueSkillLevel(skill));
					if (commandSender != player) {
						PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.one", player.getDisplayName(), new TextComponentTranslation(skill.getTranslationString()), skills.getTrueSkillLevel(skill));
					}
				} else {
					throw new CommandException("commands.grantskill.failure.player", player.getDisplayName(), new TextComponentTranslation(skill.getTranslationString()));
				}
			} else {
				throw new CommandException("commands.grantskill.failure.low", player.getDisplayName(), new TextComponentTranslation(skill.getTranslationString()), oldLevel);
			}
		} else {
			throw new WrongUsageException(getCommandUsage(sender));
		}
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
		switch(args.length) {
		case 1: return CommandBase.getListOfStringsMatchingLastWord(args, server.getAllUsernames());
		case 2: return CommandBase.getListOfStringsMatchingLastWord(args, SkillBase.getSkillNames());
		default: return Collections.<String>emptyList();
		}
	}
}
