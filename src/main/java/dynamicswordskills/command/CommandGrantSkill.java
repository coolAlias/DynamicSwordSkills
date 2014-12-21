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

package dynamicswordskills.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;

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
		if (args.length == 2 && ("all").equals(args[1])) {
			EntityPlayerMP player = getPlayer(sender, args[0]);
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			boolean flag = true;
			for (SkillBase skill : SkillBase.getSkills()) {
				if (!skills.grantSkill(skill.getId(), skill.getMaxLevel())) {
					flag = false;
				}
			}
			if (flag) {
				PlayerUtils.sendFormattedChat(player, "commands.grantskill.success.all", player.getCommandSenderName());
			} else {
				PlayerUtils.sendFormattedChat(player, "commands.grantskill.success.partial", player.getCommandSenderName());
			}
		} else if (args.length == 3) {
			EntityPlayerMP player = getPlayer(sender, args[0]);
			SkillBase skill = SkillBase.getSkillByName(args[1]);
			if (skill == null) {
				throw new CommandException("commands.skill.generic.unknown", new Object[] {args[1]});
			}
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			int level = parseIntBounded(sender, args[2], 1, 10);
			int oldLevel = skills.getTrueSkillLevel(skill);
			if (level > oldLevel) { // grants skill up to level or max level, whichever is reached first
				if (skills.grantSkill(skill.getId(), (byte) level)) {
					PlayerUtils.sendFormattedChat(player, "commands.grantskill.success.one", player.getCommandSenderName(), skills.getTrueSkillLevel(skill), skill.getDisplayName());
				} else {
					PlayerUtils.sendFormattedChat(player, "commands.grantskill.failure.player", player.getCommandSenderName(), skill.getDisplayName());
				}
			} else {
				throw new CommandException("commands.grantskill.failure.low", new Object[] {player.getCommandSenderName(), oldLevel, skill.getDisplayName()});
			}
		} else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		switch(args.length) {
		case 1: return getListOfStringsMatchingLastWord(args, getPlayers());
		case 2: return getListOfStringsMatchingLastWord(args, SkillBase.getSkillNames());
		default: return null;
		}
	}

	protected String[] getPlayers() {
		return MinecraftServer.getServer().getAllUsernames();
	}
}
