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

package dynamicswordskills.util;

import java.util.Random;

import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.PlaySoundPacket;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import swordskillsapi.api.item.IWeapon;
import swordskillsapi.api.item.WeaponRegistry;

/**
 * 
 * A collection of utility methods related to the player
 *
 */
public class PlayerUtils
{
	/**
	 * Returns whether the player is blocking
	 */
	public static boolean isBlocking(EntityPlayer player) {
		return player.isActiveItemStackBlocking();
	}

	/**
	 * Returns true if the item is a sword: i.e. if it is an {@link ItemSword},
	 * an {@link IWeapon} (returns {@link IWeapon#isSword(ItemStack)}),
	 * or registered to the {@link WeaponRegistry} as a sword
	 */
	public static boolean isSword(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		} else if (stack.getItem() instanceof IWeapon) {
			return ((IWeapon) stack.getItem()).isSword(stack);
		}
		return WeaponRegistry.INSTANCE.isSword(stack.getItem());
	}

	/**
	 * Returns true if the item is any kind of weapon: a {@link #isSword(ItemStack) sword},
	 * an {@link IWeapon}, or registered to the {@link WeaponRegistry} as a weapon
	 */
	public static boolean isWeapon(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		} else if (stack.getItem() instanceof IWeapon) {
			return ((IWeapon) stack.getItem()).isWeapon(stack);
		}
		return (isSword(stack) || WeaponRegistry.INSTANCE.isWeapon(stack.getItem()));
	}

	/** Returns true if the stack is either a {@link #isSwordItem(Item) sword} or {@link ISkillProvider provider} of this skill */
	public static boolean isSwordOrProvider(ItemStack stack, SkillBase skill) {
		Item item = stack.getItem();
		return (isSword(stack) || (item instanceof ISkillProvider && ((ISkillProvider) item).getSkillId(stack) == skill.getId()));
	}

	/** Returns the difference between player's max and current health */
	public static float getHealthMissing(EntityPlayer player) {
		return player.capabilities.isCreativeMode ? 0.0F : (player.getMaxHealth() - player.getHealth());
	}

	/** Sends a translated chat message with optional arguments to the player */
	public static void sendTranslatedChat(EntityPlayer player, String message, Object... args) {
		player.sendMessage(new TextComponentTranslation(message, args));
	}

	/**
	 * Sends a packet to the client to play a sound on the client side only, or
	 * sends a packet to the server to play a sound on the server for all to hear.
	 * To avoid playing a sound twice, only call the method from one side or the other, not both.
	 */
	public static void playSound(EntityPlayer player, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		if (player.getEntityWorld().isRemote) {
			PacketDispatcher.sendToServer(new PlaySoundPacket(sound, category, volume, pitch, player));
		} else {
			PacketDispatcher.sendTo(new PlaySoundPacket(sound, category, volume, pitch), (EntityPlayerMP) player);
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
	public static void playRandomizedSound(EntityPlayer player, SoundEvent sound, SoundCategory category, float f, float add) {
		float volume = player.getEntityWorld().rand.nextFloat() * f + add;
		float pitch = 1.0F / (player.getEntityWorld().rand.nextFloat() * f + add);
		playSound(player, sound, category, volume, pitch);
	}

	/**
	 * Plays a sound on the server with randomized volume and pitch; no effect if called on client
	 * @param f		Volume: nextFloat() * f + add
	 * @param add	Pitch: 1.0F / (nextFloat() * f + add)
	 */
	public static void playSoundAtEntity(World world, Entity entity, SoundEvent sound, SoundCategory category, float f, float add) {
		float volume = world.rand.nextFloat() * f + add;
		float pitch = 1.0F / (world.rand.nextFloat() * f + add);
		world.playSound(null, entity.getPosition(), sound, category, volume, pitch);
	}

	/**
	 * Spawns the provided ItemStack as an EntityItem with randomized position and motion
	 * Used by blocks to scatter items when broken
	 */
	public static void spawnItemWithRandom(World world, ItemStack stack, double x, double y, double z) {
		if (!world.isRemote && !stack.isEmpty()) {
			double spawnX = x + world.rand.nextFloat();
			double spawnY = y + world.rand.nextFloat();
			double spawnZ = z + world.rand.nextFloat();
			float f3 = 0.05F;
			EntityItem entityitem = new EntityItem(world, spawnX, spawnY, spawnZ, stack);
			entityitem.motionX = (-0.5F + world.rand.nextGaussian()) * f3;
			entityitem.motionY = (4 + world.rand.nextGaussian()) * f3;
			entityitem.motionZ = (-0.5F + world.rand.nextGaussian()) * f3;
			entityitem.setDefaultPickupDelay();
			world.spawnEntity(entityitem);
		}
	}

	/**
	 * Spawns XP Orbs for the amount given with randomized position and motion
	 */
	public static void spawnXPOrbsWithRandom(World world, Random rand, BlockPos pos, int xpAmount) {
		if (!world.isRemote) {
			while (xpAmount > 0) {
				int xp = (xpAmount > 50 ? 50 : EntityXPOrb.getXPSplit(xpAmount));
				xpAmount -= xp;
				float spawnX = pos.getX() + rand.nextFloat();
				float spawnY = pos.getY() + rand.nextFloat();
				float spawnZ = pos.getZ() + rand.nextFloat();
				EntityXPOrb xpOrb = new EntityXPOrb(world, spawnX, spawnY, spawnZ, xp);
				xpOrb.motionY += (4 + rand.nextGaussian()) * 0.05F;
				world.spawnEntity(xpOrb);
			}
		}
	}

	/**
	 * Drops any item in the entity's main hand and spawns it into the world
	 */
	public static void dropHeldItem(EntityLivingBase entity) {
		if (!entity.getEntityWorld().isRemote && !entity.getHeldItemMainhand().isEmpty()) {
			EntityItem drop = new EntityItem(entity.getEntityWorld(), entity.posX,
					entity.posY - 0.30000001192092896D + (double) entity.getEyeHeight(),
					entity.posZ, entity.getHeldItemMainhand().copy());
			float f = 0.3F;
			float f1 = entity.getEntityWorld().rand.nextFloat() * (float) Math.PI * 2.0F;
			drop.motionX = (double)(-MathHelper.sin(entity.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(entity.rotationPitch / 180.0F * (float) Math.PI) * f);
			drop.motionZ = (double)(MathHelper.cos(entity.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(entity.rotationPitch / 180.0F * (float) Math.PI) * f);
			drop.motionY = (double)(-MathHelper.sin(entity.rotationPitch / 180.0F * (float) Math.PI) * f + 0.1F);
			f = 0.02F * entity.getEntityWorld().rand.nextFloat();
			drop.motionX += Math.cos((double) f1) * (double) f;
			drop.motionY += (double)((entity.getEntityWorld().rand.nextFloat() - entity.getEntityWorld().rand.nextFloat()) * 0.1F);
			drop.motionZ += Math.sin((double) f1) * (double) f;
			drop.setPickupDelay(40);
			entity.getEntityWorld().spawnEntity(drop);
			entity.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}
}
