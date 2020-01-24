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

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillRegistry;
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
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;

/**
 * 
 * Increases the specified skill's level by one for the target player, using the command sender as the default.
 * If a target player is specified, the optional level parameter will increase the target player's skill to that level.
 * Using "all" as the skill name applies the same operation to all available skills.
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
	 * 	grantskill <skill | all> <player> <level>
	 */
	@Override
	public String getCommandUsage(ICommandSender player) {
		return "commands.grantskill.usage";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1 || args.length > 3) {
			throw new WrongUsageException(getCommandUsage(sender));
		}
		EntityPlayerMP commandSender = CommandBase.getCommandSenderAsPlayer(sender);
		EntityPlayerMP player = (args.length > 1 ? CommandBase.getPlayer(sender, args[1]) : commandSender);
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		int level = (args.length < 3 ? 0 : CommandBase.parseInt(args[2], 1, 100));
		if (("all").equals(args[0])) {
			boolean flag = false;
			for (SkillBase skill : SkillRegistry.getValues()) {
				if (!Config.isSkillAllowed(skill)) {
					continue;
				} else if (level < 1) {
					if (skills.grantSkill(skill)) {
						flag = true;
					}
				} else {
					byte lvl = (byte)Math.min(level, skill.getMaxLevel());
					if (skills.grantSkill(skill, lvl)) {
						flag = true;
					}
				}
			}
			String suffix = (level < 1 ? "one" : "lvl");
			if (flag) {
				PlayerUtils.sendTranslatedChat(player, "commands.grantskill.notify.all." + suffix, level);
				if (commandSender != player) {
					PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.all." + suffix, player.getDisplayName(), level);
				}
			} else {
				PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.failure.all." + suffix, player.getDisplayName(), level);
			}
		} else {
			SkillBase skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(args[0]));
			if (skill == null) {
				throw new CommandException("commands.skill.generic.unknown", args[0]);
			}
			int oldLevel = skills.getTrueSkillLevel(skill);
			level = (level < 1 ? oldLevel + 1 : level);
			if (level > oldLevel) { // grants skill up to level or max level, whichever is reached first
				if (!Config.isSkillAllowed(skill)) {
					throw new CommandException("commands.grantskill.failure.disabled", new ChatComponentTranslation(skill.getNameTranslationKey()));
				} else if (skills.grantSkill(skill, (byte) level)) {
					PlayerUtils.sendTranslatedChat(player, "commands.grantskill.notify.one", new ChatComponentTranslation(skill.getNameTranslationKey()), skills.getTrueSkillLevel(skill));
					if (commandSender != player) {
						PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.one", player.getDisplayName(), new ChatComponentTranslation(skill.getNameTranslationKey()), skills.getTrueSkillLevel(skill));
					}
				} else {
					throw new CommandException("commands.grantskill.failure.player", player.getDisplayName(), new ChatComponentTranslation(skill.getNameTranslationKey()));
				}
			} else {
				throw new CommandException("commands.grantskill.failure.low", player.getDisplayName(), new ChatComponentTranslation(skill.getNameTranslationKey()), oldLevel);
			}
		}
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		switch(args.length) {
		case 1: return getListOfStringsMatchingLastWord(args, SkillRegistry.getKeys());
		case 2: return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		default: return Collections.<String>emptyList();
		}
	}
}
