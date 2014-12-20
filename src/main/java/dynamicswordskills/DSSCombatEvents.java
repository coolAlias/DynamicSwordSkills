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

package dynamicswordskills;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.lib.Config;
import dynamicswordskills.lib.ModInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.MortalDrawPacket;
import dynamicswordskills.skills.ArmorBreak;
import dynamicswordskills.skills.Dodge;
import dynamicswordskills.skills.EndingBlow;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.LeapingBlow;
import dynamicswordskills.skills.MortalDraw;
import dynamicswordskills.skills.Parry;
import dynamicswordskills.skills.RisingCut;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.SwordBreak;

/**
 * 
 * Event handler for all combat-related events
 *
 */
public class DSSCombatEvents
{
	/** Mapping of mobs to skill orb drops */
	private static final Map<Class<? extends EntityLivingBase>, ItemStack> dropsList = new HashMap<Class<? extends EntityLivingBase>, ItemStack>();

	/** Adds a mob-class to skill orb mapping */
	private static void addDrop(Class<? extends EntityLivingBase> mobClass, SkillBase skill) {
		ItemStack stack = new ItemStack(DynamicSwordSkills.skillOrb, 1, skill.getId());
		dropsList.put(mobClass, stack);
	}

	public static void initializeDrops() {
		addDrop(EntityZombie.class, SkillBase.swordBasic);
		addDrop(EntitySkeleton.class, SkillBase.swordBasic);
		addDrop(EntityEnderman.class, SkillBase.dodge);
		addDrop(EntitySilverfish.class, SkillBase.dodge);
		addDrop(EntitySlime.class, SkillBase.dash);
		addDrop(EntityHorse.class, SkillBase.dash);
		addDrop(EntityPigZombie.class, SkillBase.parry);
		addDrop(EntityOcelot.class, SkillBase.parry);
		addDrop(EntitySpider.class, SkillBase.endingBlow);
		addDrop(EntityCaveSpider.class, SkillBase.leapingBlow);
		addDrop(EntityMagmaCube.class, SkillBase.leapingBlow);
		addDrop(EntityBlaze.class, SkillBase.spinAttack);
		addDrop(EntityBat.class, SkillBase.risingCut);
		addDrop(EntityCreeper.class, SkillBase.armorBreak);
		addDrop(EntityIronGolem.class, SkillBase.swordBreak);
		addDrop(EntityGhast.class, SkillBase.superSpinAttack);
		addDrop(EntityWitch.class, SkillBase.mortalDraw);
	}

	/**
	 * Returns the type of skill orb that the mob will drop this time;
	 * this is not always the same as the stack stored in dropsList
	 */
	private static ItemStack getOrbDrop(EntityLivingBase mob) {
		if (dropsList.get(mob.getClass()) != null && mob.worldObj.rand.nextFloat() > Config.getChanceForRandomDrop()) {
			return dropsList.get(mob.getClass());
		} else {
			ItemStack orb = null;
			boolean flag = mob instanceof EntityPlayer;
			int id = mob.worldObj.rand.nextInt(SkillBase.getNumSkills());
			if (SkillBase.doesSkillExist(id) && (!flag || Config.arePlayerDropsEnabled())) {
				float chance = (flag ? Config.getPlayerDropFactor() : 1) * Config.getRandomMobDropChance();
				if (dropsList.get(mob.getClass()) != null || mob.worldObj.rand.nextFloat() < chance) {
					orb = new ItemStack(DynamicSwordSkills.skillOrb, 1, id);
				}
			}

			return orb;
		}
	}

	@SubscribeEvent
	public void onLivingDrops(LivingDropsEvent event) {
		if (event.source.getEntity() instanceof EntityPlayer) {
			EntityLivingBase mob = event.entityLiving;
			ItemStack orb = getOrbDrop(mob);
			if (orb != null && (Config.areOrbDropsEnabled() || (Config.arePlayerDropsEnabled() && event.entity instanceof EntityPlayer))) {
				if (mob.worldObj.rand.nextFloat() < (Config.getDropChance(orb.getItemDamage()) + (0.005F * event.lootingLevel))) {
					event.drops.add(new EntityItem(mob.worldObj, mob.posX, mob.posY, mob.posZ, orb.copy()));
					mob.worldObj.playSoundEffect(mob.posX, mob.posY, mob.posZ, ModInfo.SOUND_SPECIAL_DROP, 1.0F, 1.0F);
				}
			}
		}
	}

	

	/**
	 * Used for anti-spam of left click, if enabled in the configuration settings.
	 */
	public static void setPlayerAttackTime(EntityPlayer player) {
		if (!player.capabilities.isCreativeMode) {
			player.attackTime = Math.max(player.attackTime, Config.getBaseSwingSpeed());
		}
	}

	/**
	 * This event is called when an entity is attacked by another entity; it is only
	 * called on the server unless the source of the attack is an EntityPlayer
	 */
	@SubscribeEvent
	public void onAttacked(LivingAttackEvent event) {
		if (!event.isCanceled() && event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			if (skills.isSkillActive(SkillBase.dodge)) {
				event.setCanceled(((Dodge) skills.getPlayerSkill(SkillBase.dodge)).dodgeAttack(player));
			} else if (skills.isSkillActive(SkillBase.parry)) {
				if (event.source.getSourceOfDamage() instanceof EntityLivingBase) {
					EntityLivingBase attacker = (EntityLivingBase) event.source.getSourceOfDamage();
					event.setCanceled(((Parry) skills.getPlayerSkill(SkillBase.parry)).parryAttack(player, attacker));
				}
			} else if (skills.isSkillActive(SkillBase.swordBreak)) {
				if (event.source.getSourceOfDamage() instanceof EntityLivingBase) {
					EntityLivingBase attacker = (EntityLivingBase) event.source.getSourceOfDamage();
					event.setCanceled(((SwordBreak) skills.getPlayerSkill(SkillBase.swordBreak)).breakAttack(player, attacker));
				}
			} else if (skills.isSkillActive(SkillBase.mortalDraw) && event.source.getEntity() != null) {
				if (!player.worldObj.isRemote) {
					if (((MortalDraw) skills.getPlayerSkill(SkillBase.mortalDraw)).drawSword(player, event.source.getEntity())) {
						PacketDispatcher.sendTo(new MortalDrawPacket(), (EntityPlayerMP) player);
						event.setCanceled(true);
					}
				}
			}
		}
	}

	/**
	 * Use LOW or LOWEST priority to prevent interrupting a combo when the event may be canceled elsewhere.
	 */
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onHurt(LivingHurtEvent event) {
		if (event.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.source.getEntity();
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			ICombo combo = skills.getComboSkill();
			if (combo != null && combo.getCombo() != null && !combo.getCombo().isFinished()) {
				event.ammount += combo.getCombo().getSize();
			}
			if (skills.isSkillActive(SkillBase.armorBreak)) {
				((ArmorBreak) skills.getPlayerSkill(SkillBase.armorBreak)).onImpact(player, event);
				return;
			} else if (skills.isSkillActive(SkillBase.mortalDraw)) {
				((MortalDraw) skills.getPlayerSkill(SkillBase.mortalDraw)).onImpact(player, event);
			}
		}
		if (event.ammount > 0.0F && event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			ICombo combo = DSSPlayerInfo.get(player).getComboSkill();
			if (combo != null && event.ammount > 0) {
				combo.onPlayerHurt(player, event);
			}
		}
		if (event.ammount > 0.0F && event.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.source.getEntity();
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			if (skills.isSkillActive(SkillBase.risingCut)) {
				((RisingCut) skills.getPlayerSkill(SkillBase.risingCut)).onImpact(event.entity);
			} else if (skills.isSkillActive(SkillBase.endingBlow)) {
				((EndingBlow) skills.getPlayerSkill(SkillBase.endingBlow)).onImpact(player, event);
			}
			if (skills.getComboSkill() != null) {
				skills.getComboSkill().onHurtTarget(player, event);
			}
		}
	}

	@SubscribeEvent
	public void onLivingDeathEvent(LivingDeathEvent event) {
		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			DSSPlayerInfo.saveProxyData((EntityPlayer) event.entity);
		}
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		if (event.entity instanceof EntityPlayer) {
			DSSPlayerInfo.get((EntityPlayer) event.entity).onUpdate();
		}
	}

	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			DSSPlayerInfo.loadProxyData(player);
			DSSPlayerInfo.get(player).verifyStartingGear();
		}
	}

	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event) {
		if (event.entity instanceof EntityPlayer && DSSPlayerInfo.get((EntityPlayer) event.entity) == null) {
			DSSPlayerInfo.register((EntityPlayer) event.entity);
		}
	}

	/**
	 * NOTE 1: Leaping Blow's onImpact method is client-side only
	 * NOTE 2: LivingFallEvent is only called when not in Creative mode
	 */
	@SubscribeEvent
	public void onFall(LivingFallEvent event) {
		if (event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			if (player.worldObj.isRemote && skills.isSkillActive(SkillBase.leapingBlow)) {
				((LeapingBlow) skills.getPlayerSkill(SkillBase.leapingBlow)).onImpact(player, event.distance);
			}
			if (skills.reduceFallAmount > 0.0F) {
				event.distance -= skills.reduceFallAmount;
				skills.reduceFallAmount = 0.0F;
			}
		}
	}

	/**
	 * NOTE: Leaping Blow's onImpact method is client-side only
	 */
	@SubscribeEvent
	public void onCreativeFall(PlayerFlyableFallEvent event) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(event.entityPlayer);
		if (skills != null && event.entityPlayer.worldObj.isRemote) {
			if (skills.isSkillActive(SkillBase.leapingBlow)) {
				((LeapingBlow) skills.getPlayerSkill(SkillBase.leapingBlow)).onImpact(event.entityPlayer, event.distance);
			}
		}
	}
}
