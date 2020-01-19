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

import com.google.common.collect.Lists;

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
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

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
	public void processCommand(ICommandSender sender, String[] args) {
		EntityPlayerMP commandSender = getCommandSenderAsPlayer(sender);
		EntityPlayerMP player = getPlayer(sender, args[0]);
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		if (args.length == 2 && ("all").equals(args[1])) {
			boolean flag = true;
			for (SkillBase skill : SkillRegistry.getValues()) {
				if (Config.isSkillAllowed(skill) && !skills.grantSkill(skill, skill.getMaxLevel())) {
					flag = false;
				}
			}
			if (flag) {
				PlayerUtils.sendTranslatedChat(player, "commands.grantskill.notify.all");
				if (commandSender != player) {
					PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.all", player.getCommandSenderName());
				}
			} else {
				PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.partial", player.getCommandSenderName());
			}
		} else if (args.length == 3) {
			SkillBase skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(args[1]));
			if (skill == null) {
				throw new CommandException("commands.skill.generic.unknown", args[1]);
			}
			int level = parseIntBounded(sender, args[2], 1, 10);
			int oldLevel = skills.getTrueSkillLevel(skill);
			if (level > oldLevel) { // grants skill up to level or max level, whichever is reached first
				if (!Config.isSkillAllowed(skill)) {
					throw new CommandException("commands.grantskill.failure.disabled", new ChatComponentTranslation(skill.getNameTranslationKey()));
				} else if (skills.grantSkill(skill, (byte) level)) {
					PlayerUtils.sendTranslatedChat(player, "commands.grantskill.notify.one", new ChatComponentTranslation(skill.getNameTranslationKey()), skills.getTrueSkillLevel(skill));
					if (commandSender != player) {
						PlayerUtils.sendTranslatedChat(commandSender, "commands.grantskill.success.one", player.getCommandSenderName(), new ChatComponentTranslation(skill.getNameTranslationKey()), skills.getTrueSkillLevel(skill));
					}
				} else {
					throw new CommandException("commands.grantskill.failure.player", player.getCommandSenderName(), new ChatComponentTranslation(skill.getNameTranslationKey()));
				}
			} else {
				throw new CommandException("commands.grantskill.failure.low", player.getCommandSenderName(), new ChatComponentTranslation(skill.getNameTranslationKey()), oldLevel);
			}
		} else {
			throw new WrongUsageException(getCommandUsage(sender));
		}
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		switch(args.length) {
		case 1: return CommandBase.getListOfStringsMatchingLastWord(args, getPlayers());
		case 2:
			List<String> options = Lists.<String>newArrayList();
			for (ResourceLocation name : SkillRegistry.getKeys()) {
				options.add(name.toString());
			}
			return CommandBase.getListOfStringsMatchingLastWord(args, options.toArray(new String[0]));
		default: return null;
		}
	}

	protected String[] getPlayers() {
		return MinecraftServer.getServer().getAllUsernames();
	}
}
