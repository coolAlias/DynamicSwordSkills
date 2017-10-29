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

package dynamicswordskills.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class EntityLeapingBlow extends EntityThrowable
{
	/** Keeps track of entities already affected so they don't get attacked twice */
	private List<Integer> affectedEntities = new ArrayList<Integer>(); 

	/** Base damage should be set from player's Leaping Blow skill */
	private float damage = 2.0F;

	/** Base number of ticks this entity can exist */
	private int lifespan = 12;

	/** Skill level of swordsman; used in many calculations */
	private int level = 0;

	private static final float BASE_SIZE = 1.0F, HEIGHT = 0.5F;

	public EntityLeapingBlow(World world) {
		super(world);
		this.setSize(BASE_SIZE, HEIGHT);
	}

	public EntityLeapingBlow(World world, EntityLivingBase thrower) {
		super(world, thrower);
		this.setSize(BASE_SIZE, HEIGHT);
		this.posY = thrower.posY + 0.2D;
		this.motionY = 0.0D;
	}

	public EntityLeapingBlow(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.setSize(BASE_SIZE, HEIGHT);
	}

	/**
	 * Each level increases the distance traveled as well as the AoE
	 */
	public EntityLeapingBlow setLevel(int level) {
		this.level = level;
		this.lifespan += level;
		return this;
	}

	/**
	 * Sets amount of damage that will be caused onImpact
	 */
	public EntityLeapingBlow setDamage(float amount) {
		this.damage = amount;
		return this;
	}

	/** Max distance (squared) from thrower that damage can still be applied */
	private double getRangeSquared() {
		return (3.0D + level) * (3.0D + level);
	}

	/** Duration of weakness effect */
	private int getPotionDuration() {
		return (50 + (level * 10));
	}

	/** Returns area within which to search for targets each tick */
	private AxisAlignedBB getAoE() {
		return getEntityBoundingBox().expand((0.25F * level), 0.0F, (0.25F * level));
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (inGround || ticksExisted > lifespan) {
			setDead();
		}
		if (!worldObj.isRemote) {
			List<EntityLivingBase> targets = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, getAoE());
			for (EntityLivingBase target : targets) {
				if (!affectedEntities.contains(target.getEntityId()) && target != getThrower() && !TargetUtils.isTargetInFrontOf(this, target, 30F)) {
					affectedEntities.add(target.getEntityId());
					float d = damage;
					if (getThrower() != null) {
						double d0 = (1.0D - getThrower().getDistanceSqToEntity(target) / getRangeSquared());
						d *= (d0 > 1.0D ? 1.0D : d0);
						if (d < 0.5D) { return; }
					}
					if (target.attackEntityFrom(DamageUtils.causeIndirectSwordDamage(this, getThrower()), d)) {
						target.addPotionEffect(new PotionEffect(MobEffects.POISON, getPotionDuration()));
					}
				}
			}
		} else {
			/** Velocity x and z for spawning particles to left and right of entity */
			double vX = motionZ;
			double vZ = motionX;
			AxisAlignedBB bb = getEntityBoundingBox();
			int i = MathHelper.floor_double(posX + (bb.maxX - bb.minX) / 2);
			int j = MathHelper.floor_double(posY) - 1;
			int k = MathHelper.floor_double(posZ + (bb.maxZ - bb.minZ) / 2);
			IBlockState state = worldObj.getBlockState(new BlockPos(i, j, k));
			EnumParticleTypes particle = (state.getRenderType() == EnumBlockRenderType.INVISIBLE ? EnumParticleTypes.CRIT : EnumParticleTypes.BLOCK_CRACK);
			int[] stateId = (state.getRenderType() == EnumBlockRenderType.INVISIBLE ? new int[]{} : new int[] {Block.getStateId(state)});
			for (int n = 0; n < 4; ++n) {
				worldObj.spawnParticle(particle, posX, posY, posZ, vX + rand.nextGaussian(), 0.01D, vZ + rand.nextGaussian(), stateId);
				worldObj.spawnParticle(particle, posX, posY, posZ, -vX + rand.nextGaussian(), 0.01D, -vZ + rand.nextGaussian(), stateId);
			}
		}
	}

	@Override
	protected void onImpact(RayTraceResult result) {
		if (!worldObj.isRemote) {
			if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
				Entity entity = result.entityHit;
				if (entity instanceof EntityLivingBase && !affectedEntities.contains(entity.getEntityId()) && entity != getThrower()) {
					affectedEntities.add(entity.getEntityId());
					if (entity.attackEntityFrom(DamageUtils.causeIndirectSwordDamage(this, getThrower()), damage)) {
						PlayerUtils.playSoundAtEntity(worldObj, entity, ModInfo.SOUND_HURT_FLESH, 0.4F, 0.5F);
						((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.POISON, 60));
					}
				}
			} else {
				if (worldObj.getBlockState(result.getBlockPos()).getMaterial().blocksMovement()) {
					setDead();
				}
			}
		}
	}

	/**
	 * Returns the velocity to use for {@link EntityThrowable#setHeadingFromThrower}
	 */
	public float getVelocity() {
		return 0.5F;
	}

	@Override
	public float getGravityVelocity() {
		return 0.0F;
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setFloat("damage", damage);
		compound.setInteger("level", level);
		compound.setInteger("lifespan", lifespan);
		compound.setIntArray("affectedEntities", ArrayUtils.toPrimitive(affectedEntities.toArray(new Integer[affectedEntities.size()])));
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		damage = compound.getFloat("damage");
		level = compound.getInteger("level");
		lifespan = compound.getInteger("lifespan");
		int[] entities = compound.getIntArray("affectedEntities");
		for (int i = 0; i < entities.length; ++i) {
			affectedEntities.add(entities[i]);
		}
	}
}
