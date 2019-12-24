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

package dynamicswordskills.skills;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.ref.Config;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/**
 * 
 * RISING CUT
 * Description: Rising slash flings enemy upward
 * Activation: Jump while sneaking and attack
 * Effect: Attacks target for normal sword damage and knocks the target into the air
 * Range: 2 + level blocks
 * Exhaustion: 3.0F - (level * 0.2F)
 * Special: May only be used while locked on to a target
 * 
 * Requires onRenderTick to be called each render tick while active.
 *  
 */
public class RisingCut extends SkillActive
{
	/** Flag for activation; set when player jumps while sneaking */
	@SideOnly(Side.CLIENT)
	private int ticksTilFail;

	/** Set when activated and lasts until the player hits the ground or the duration expires */
	private int activeTimer;

	/** True while animation is in progress */
	private int animationTimer;

	/** Stores the entity struck to add velocity on the next update */
	private Entity entityHit;

	/** Flag to prevent a second attack from adding even more motionY */
	private boolean hitEntity;

	public RisingCut(String name) {
		super(name);
	}

	private RisingCut(RisingCut skill) {
		super(skill);
	}

	@Override
	public RisingCut newInstance() {
		return new RisingCut(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getRangeDisplay(2 + level));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return activeTimer > 0;
	}

	@Override
	public boolean isAnimating() {
		return animationTimer > 0;
	}

	@Override
	protected float getExhaustion() {
		return 3.0F - (level * 0.2F);
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && !player.isUsingItem() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return ticksTilFail > 0 && player.motionY > 0.0D && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			return false;
		}
		return (key == mc.gameSettings.keyBindJump || key == mc.gameSettings.keyBindAttack);
	}

	/**
	 * Flags the skill as ready to be activated when the player next attacks,
	 * provided {@link #canExecute} still returns true at that time
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == mc.gameSettings.keyBindJump) {
			if (player.onGround && ticksTilFail == 0 && !isActive() && !player.isUsingItem() && player.isSneaking()) {
				ticksTilFail = 3; // this allows canExecute to return true for 3 ticks
			}
		} else if (canExecute(player) && activate(player)) {
			DSSClientEvents.handlePlayerAttack(mc);
			return true;
		}
		return false;
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		animationTimer = 5 + level;
		// Approximate time it should take to hit the ground with about 5 ticks of leeway
		activeTimer = (20 + 3 * level);
		entityHit = null;
		hitEntity = false;
		player.motionY += 0.3D + (0.115D * level);
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		activeTimer = 0;
		animationTimer = 0;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (player.worldObj.isRemote && ticksTilFail > 0) {
			--ticksTilFail;
		}
		if (isActive()) {
			if (animationTimer > 0) {
				--animationTimer;
			}
			if (activeTimer > 0) {
				--activeTimer;
			}
			if (player.onGround) {
				onDeactivated(player.worldObj, player);
			} else if (entityHit != null) {
				if (!entityHit.isDead) {
					double addY = 0.3D + (0.125D * level);
					double resist = 1.0D;
					if (entityHit instanceof EntityLivingBase) {
						resist = 1.0D - ((EntityLivingBase) entityHit).getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue();
					}
					entityHit.addVelocity(0.0D, addY * resist, 0.0D);
				}
				entityHit = null;
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		player.swingProgress = 0.5F;
		return false;
	}

	/**
	 * Call when an entity is damaged to flag the entity for velocity update next tick.
	 * This is necessary because adding velocity right before the entity is damaged fails.
	 */
	@Override
	public void postImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		if (!hitEntity) {
			boolean flag = !(entity instanceof EntityPlayer) || !PlayerUtils.isBlocking((EntityPlayer) entity);
			this.entityHit = (flag ? entity : null);
		}
		hitEntity = true;
	}

	@Override
	public boolean onFall(EntityPlayer player, LivingFallEvent event) {
		if (isActive()) {
			event.distance -= (1.0F + level);
			onDeactivated(player.worldObj, player);
		}
		return false;
	}
}
