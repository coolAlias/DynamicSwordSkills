/**
    Copyright (C) <2015> <coolAlias>

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

package dynamicswordskills.util;

import java.util.Random;

import mods.battlegear2.api.core.IBattlePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ISkillItem;
import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.api.ISword;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.PlaySoundPacket;
import dynamicswordskills.skills.SkillBase;

/**
 * 
 * A collection of utility methods related to the player
 *
 */
public class PlayerUtils
{
	/**
	 * Returns whether the player is using an item, accounting for possibility of Battlegear2 offhand item use
	 */
	public static boolean isUsingItem(EntityPlayer player) {
		if (player.isUsingItem()) {
			return true;
		} else if (DynamicSwordSkills.isBG2Enabled) {
			return ((IBattlePlayer) player).isBattlemode() && ((IBattlePlayer) player).isBlockingWithShield();
		}
		return false;
	}

	/** Returns true if the player's held item is a {@link #isSwordItem(Item) sword} */
	public static boolean isHoldingSword(EntityPlayer player) {
		return (player.getHeldItem() != null && isSwordItem(player.getHeldItem().getItem()));
	}

	/** Returns true if the player's held item is a {@link #isSwordItem(Item) sword} or {@link ISkillItem} */
	public static boolean isHoldingSkillItem(EntityPlayer player) {
		return (player.getHeldItem() != null && isSkillItem(player.getHeldItem().getItem()));
	}

	/** Returns true if the item is either an {@link ItemSword} or {@link ISword} */
	public static boolean isSwordItem(Item item) {
		return (item instanceof ItemSword || item instanceof ISword);
	}

	/** Returns true if the item is either a {@link #isSwordItem(Item) sword} or {@link ISkillItem} */
	public static boolean isSkillItem(Item item) {
		return (isSwordItem(item) || item instanceof ISkillItem);
	}

	/** Returns true if the stack is either a {@link #isSwordItem(Item) sword} or {@link ISkillProvider provider} of this skill */
	public static boolean isSwordOrProvider(ItemStack stack, SkillBase skill) {
		Item item = (stack != null ? stack.getItem() : null);
		return (isSwordItem(item) || (item instanceof ISkillProvider && ((ISkillProvider) item).getSkillId(stack) == skill.getId()));
	}

	/** Returns the difference between player's max and current health */
	public static float getHealthMissing(EntityPlayer player) {
		return player.capabilities.isCreativeMode ? 0.0F : (player.getMaxHealth() - player.getHealth());
	}

	/** Sends a pre-translated chat message to the player */
	public static void sendChat(EntityPlayer player, String message) {
		player.addChatMessage(new ChatComponentText(message));
	}

	/** Sends a translated chat message to the player */
	public static void sendTranslatedChat(EntityPlayer player, String message) {
		sendChat(player, StatCollector.translateToLocal(message));
	}

	/** Sends a formatted, translated chat message to the player */
	public static void sendFormattedChat(EntityPlayer player, String message, Object... args) {
		sendChat(player, StatCollector.translateToLocalFormatted(message, args));
	}

	/**
	 * Sends a packet to the client to play a sound on the client side only, or
	 * sends a packet to the server to play a sound on the server for all to hear.
	 * To avoid playing a sound twice, only call the method from one side or the other, not both.
	 */
	public static void playSound(EntityPlayer player, String sound, float volume, float pitch) {
		if (player.worldObj.isRemote) {
			PacketDispatcher.sendToServer(new PlaySoundPacket(sound, volume, pitch, player));
		} else {
			PacketDispatcher.sendTo(new PlaySoundPacket(sound, volume, pitch), (EntityPlayerMP) player);
		}
	}

	/**
	 * Plays a sound with randomized volume and pitch.
	 * Sends a packet to the client to play a sound on the client side only, or
	 * sends a packet to the server to play a sound on the server for all to hear.
	 * To avoid playing a sound twice, only call the method from one side or the other, not both.
	 * @param f		Volume: nextFloat() * f + add
	 * @param add	Pitch: 1.0F / (nextFloat() * f + add)
	 */
	public static void playRandomizedSound(EntityPlayer player, String sound, float f, float add) {
		float volume = player.worldObj.rand.nextFloat() * f + add;
		float pitch = 1.0F / (player.worldObj.rand.nextFloat() * f + add);
		playSound(player, sound, volume, pitch);
	}

	/**
	 * Plays a sound on the server with randomized volume and pitch; no effect if called on client
	 * @param f		Volume: nextFloat() * f + add
	 * @param add	Pitch: 1.0F / (nextFloat() * f + add)
	 */
	public static void playSoundAtEntity(World world, Entity entity, String sound, float f, float add) {
		float volume = world.rand.nextFloat() * f + add;
		float pitch = 1.0F / (world.rand.nextFloat() * f + add);
		world.playSoundAtEntity(entity, sound, volume, pitch);
	}

	/**
	 * Spawns the provided ItemStack as an EntityItem with randomized position and motion
	 * Used by blocks to scatter items when broken
	 */
	public static void spawnItemWithRandom(World world, ItemStack stack, double x, double y, double z) {
		if (!world.isRemote && stack != null) {
			double spawnX = x + world.rand.nextFloat();
			double spawnY = y + world.rand.nextFloat();
			double spawnZ = z + world.rand.nextFloat();
			float f3 = 0.05F;
			EntityItem entityitem = new EntityItem(world, spawnX, spawnY, spawnZ, stack);
			entityitem.motionX = (-0.5F + world.rand.nextGaussian()) * f3;
			entityitem.motionY = (4 + world.rand.nextGaussian()) * f3;
			entityitem.motionZ = (-0.5F + world.rand.nextGaussian()) * f3;
			entityitem.delayBeforeCanPickup = 10;
			world.spawnEntityInWorld(entityitem);
		}
	}

	/**
	 * Spawns XP Orbs for the amount given with randomized position and motion
	 */
	public static void spawnXPOrbsWithRandom(World world, Random rand, int x, int y, int z, int xpAmount) {
		if (!world.isRemote) {
			while (xpAmount > 0) {
				int xp = (xpAmount > 50 ? 50 : EntityXPOrb.getXPSplit(xpAmount));
				xpAmount -= xp;
				float spawnX = x + rand.nextFloat();
				float spawnY = y + rand.nextFloat();
				float spawnZ = z + rand.nextFloat();
				EntityXPOrb xpOrb = new EntityXPOrb(world, spawnX, spawnY, spawnZ, xp);
				xpOrb.motionY += (4 + rand.nextGaussian()) * 0.05F;
				world.spawnEntityInWorld(xpOrb);
			}
		}
	}

	/**
	 * Drops the entity's currently held item into the world
	 */
	public static void dropHeldItem(EntityLivingBase entity) {
		if (!entity.worldObj.isRemote && entity.getHeldItem() != null) {
			EntityItem drop = new EntityItem(entity.worldObj, entity.posX,
					entity.posY - 0.30000001192092896D + (double) entity.getEyeHeight(),
					entity.posZ, entity.getHeldItem().copy());
			float f = 0.3F;
			float f1 = entity.worldObj.rand.nextFloat() * (float) Math.PI * 2.0F;
			drop.motionX = (double)(-MathHelper.sin(entity.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(entity.rotationPitch / 180.0F * (float) Math.PI) * f);
			drop.motionZ = (double)(MathHelper.cos(entity.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(entity.rotationPitch / 180.0F * (float) Math.PI) * f);
			drop.motionY = (double)(-MathHelper.sin(entity.rotationPitch / 180.0F * (float) Math.PI) * f + 0.1F);
			f = 0.02F * entity.worldObj.rand.nextFloat();
			drop.motionX += Math.cos((double) f1) * (double) f;
			drop.motionY += (double)((entity.worldObj.rand.nextFloat() - entity.worldObj.rand.nextFloat()) * 0.1F);
			drop.motionZ += Math.sin((double) f1) * (double) f;
			drop.delayBeforeCanPickup = 40;
			entity.worldObj.spawnEntityInWorld(drop);
			entity.setCurrentItemOrArmor(0, (ItemStack) null);
		}
	}
}
