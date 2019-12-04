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

package dynamicswordskills.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * 
 * A collection of methods related to target acquisition
 *
 */
public class TargetUtils
{
	/** Maximum range within which to search for targets */
	private static final int MAX_DISTANCE = 256;
	/** Max distance squared, used for comparing target distances (avoids having to call sqrt) */
	private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;

	/**
	 * Returns the player's current reach distance based on game mode.
	 * The values were determined via actual in-game testing as the reach distances
	 * found in EntityRenderer#getMouseOver and PlayerControllerMP#getBlockReachDistance
	 * do not seem to accurately reflect the actual distance at which an attack will miss.
	 * Note that the only important distance check is handled server side in
	 * NetHandlerPlayServer#processUseEntity.
	 */
	public static double getReachDistanceSq(EntityPlayer player) {
		return player.capabilities.isCreativeMode ? 36.0D : 12.0D;
	}

	/**
	 * Returns true if current target is within the player's reach distance, used mainly
	 * for predicting misses from the client side; does not use the mouse over object.
	 */
	public static boolean canReachTarget(EntityPlayer player, Entity target) {
		return (player.canEntityBeSeen(target) && player.getDistanceSqToEntity(target) < getReachDistanceSq(player));
	}

	/**
	 * Returns MovingObjectPosition of Entity or Block impacted, or null if nothing was struck
	 * @param entity	The entity checking for impact, e.g. an arrow
	 * @param shooter	An entity not to be collided with, generally the shooter
	 * @param hitBox	The amount by which to expand the collided entities' bounding boxes when checking for impact (may be negative)
	 * @param flag		Optional flag to allow collision with shooter, e.g. (ticksInAir >= 5)
	 */
	public static MovingObjectPosition checkForImpact(World world, Entity entity, Entity shooter, double hitBox, boolean flag) {
		double posY = entity.posY + (entity.height / 2.0D); // fix for Dash
		Vec3 vec3 = new Vec3(entity.posX, posY, entity.posZ);
		Vec3 vec31 = new Vec3(entity.posX + entity.motionX, posY + entity.motionY, entity.posZ + entity.motionZ);
		MovingObjectPosition mop = world.rayTraceBlocks(vec3, vec31, false, true, false);
		vec3 = new Vec3(entity.posX, posY, entity.posZ);
		vec31 = new Vec3(entity.posX + entity.motionX, posY + entity.motionY, entity.posZ + entity.motionZ);
		if (mop != null) {
			vec31 = new Vec3(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
		}
		Entity target = null;
		List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(entity, entity.getEntityBoundingBox().addCoord(entity.motionX, entity.motionY, entity.motionZ).expand(1.0D, 1.0D, 1.0D));
		double d0 = 0.0D;
		for (int i = 0; i < list.size(); ++i) {
			Entity entity1 = list.get(i);
			if (entity1.canBeCollidedWith() && (entity1 != shooter || flag)) {
				AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(hitBox, hitBox, hitBox);
				MovingObjectPosition mop1 = axisalignedbb.calculateIntercept(vec3, vec31);
				if (mop1 != null) {
					double d1 = vec3.distanceTo(mop1.hitVec);
					if (d1 < d0 || d0 == 0.0D) {
						target = entity1;
						d0 = d1;
					}
				}
			}
		}
		if (target != null) {
			mop = new MovingObjectPosition(target);
		}
		if (mop != null && mop.entityHit instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) mop.entityHit;
			if (player.capabilities.disableDamage || (shooter instanceof EntityPlayer
					&& !((EntityPlayer) shooter).canAttackPlayer(player)))
			{
				mop = null;
			}
		}
		return mop;
	}

	/**
	 * Returns true if the entity is directly in the crosshairs
	 */
	@SideOnly(Side.CLIENT)
	public static boolean isMouseOverEntity(Entity entity) {
		MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
		return (mop != null && mop.entityHit == entity);
	}

	/**
	 * Returns the Entity that the mouse is currently over, or null
	 */
	@SideOnly(Side.CLIENT)
	public static Entity getMouseOverEntity() {
		MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
		return (mop == null ? null : mop.entityHit);
	}

	/**
	 * Returns true if target is not the current seeker and meets all other filter criteria
	 */
	public static final boolean isTargetValid(Entity target, EntityLivingBase seeker, List<Predicate<Entity>> filters) {
		if (target == seeker) {
			return false;
		}
		for (Predicate<Entity> p : filters) {
			if (p instanceof TargetPredicate) {
				((TargetPredicate<Entity>) p).setSeeker(seeker);
			}
			if (!p.apply(target)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true for the following 'mob' type entities:
	 *   - Instances of IMob and IRangedAttackMob
	 *   - EntityLiving types if {@link EntityLiving#getAttackTarget()} is not null
	 *   - EntityLivingBase types if they have an ATTACK_DAMAGE attribute value > 0
	 */
	public static final boolean isMobEntity(Entity entity) {
		if (entity instanceof IMob || entity instanceof IRangedAttackMob) {
			return true;
		} else if (entity instanceof EntityLiving && ((EntityLiving) entity).getAttackTarget() != null) {
			return true;
		} else if (entity instanceof EntityLivingBase) {
			IAttributeInstance damage = ((EntityLivingBase) entity).getAttributeMap().getAttributeInstance(SharedMonsterAttributes.attackDamage);
			if (damage != null && damage.getAttributeValue() > 0) {
				return true;
			}
		}
		return false;
	}

	/** Calls {@link #acquireAllLookTargets(EntityLivingBase, int, double, List)} with the {@link #getDefaultSelectors()} */
	public static final EntityLivingBase acquireLookTarget(EntityLivingBase seeker, int distance, double radius, boolean closestToSeeker) {
		return acquireLookTarget(seeker, distance, radius, closestToSeeker, getDefaultSelectors());
	}

	/**
	 * Returns the EntityLivingBase closest to the point at which the entity is looking and within the distance and radius specified
	 * @param distance max distance to check for target, in blocks; negative value will check to MAX_DISTANCE
	 * @param radius max distance, in blocks, to search on either side of the vector's path
	 * @param closestToEntity if true, the target closest to the seeker and still within the line of sight search radius is returned
	 * @return the entity the seeker is looking at or null if no entity within sight search range
	 */
	public static final EntityLivingBase acquireLookTarget(EntityLivingBase seeker, int distance, double radius, boolean closestToSeeker, List<Predicate<Entity>> filters) {
		if (distance < 0 || distance > MAX_DISTANCE) {
			distance = MAX_DISTANCE;
		}
		EntityLivingBase currentTarget = null;
		double currentDistance = MAX_DISTANCE_SQ;
		Vec3 vec3 = seeker.getLookVec();
		double targetX = seeker.posX;
		double targetY = seeker.posY + seeker.getEyeHeight() - 0.10000000149011612D;
		double targetZ = seeker.posZ;
		double distanceTraveled = 0;
		while ((int) distanceTraveled < distance) {
			targetX += vec3.xCoord;
			targetY += vec3.yCoord;
			targetZ += vec3.zCoord;
			distanceTraveled += vec3.lengthVector();
			AxisAlignedBB bb = new AxisAlignedBB(targetX-radius, targetY-radius, targetZ-radius, targetX+radius, targetY+radius, targetZ+radius);
			List<EntityLivingBase> list = seeker.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, bb);
			for (EntityLivingBase target : list) {
				if (isTargetValid(target, seeker, filters) && isTargetInSight(vec3, seeker, target)) {
					double newDistance = (closestToSeeker ? target.getDistanceSqToEntity(seeker) : target.getDistanceSq(targetX, targetY, targetZ));
					if (newDistance < currentDistance) {
						currentTarget = target;
						currentDistance = newDistance;
					}
				}
			}
		}
		return currentTarget;
	}

	/** Calls {@link #acquireAllLookTargets(EntityLivingBase, int, double, List)} with the {@link #getDefaultSelectors()} */
	public static final List<EntityLivingBase> acquireAllLookTargets(EntityLivingBase seeker, int distance, double radius) {
		return acquireAllLookTargets(seeker, distance, radius, getDefaultSelectors());
	}

	/**
	 * Similar to the single entity version, but this method returns a List of all EntityLivingBase entities
	 * that are within the entity's field of vision, up to a certain range and distance away
	 */
	public static final List<EntityLivingBase> acquireAllLookTargets(EntityLivingBase seeker, int distance, double radius, List<Predicate<Entity>> filters) {
		if (distance < 0 || distance > MAX_DISTANCE) {
			distance = MAX_DISTANCE;
		}
		List<EntityLivingBase> targets = new ArrayList<EntityLivingBase>();
		Vec3 vec3 = seeker.getLookVec();
		double targetX = seeker.posX;
		double targetY = seeker.posY + seeker.getEyeHeight() - 0.10000000149011612D;
		double targetZ = seeker.posZ;
		double distanceTraveled = 0;
		while ((int) distanceTraveled < distance) {
			targetX += vec3.xCoord;
			targetY += vec3.yCoord;
			targetZ += vec3.zCoord;
			distanceTraveled += vec3.lengthVector();
			AxisAlignedBB bb = new AxisAlignedBB(targetX-radius, targetY-radius, targetZ-radius, targetX+radius, targetY+radius, targetZ+radius);
			List<EntityLivingBase> list = seeker.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, bb);
			for (EntityLivingBase target : list) {
				if (isTargetValid(target, seeker, filters) && isTargetInSight(vec3, seeker, target)) {
					if (!targets.contains(target)) {
						targets.add(target);
					}
				}
			}
		}
		return targets;
	}

	/**
	 * Returns whether the target is in the seeker's field of view based on relative position
	 * @param fov seeker's field of view; a wider angle returns true more often
	 */
	public static final boolean isTargetInFrontOf(Entity seeker, Entity target, float fov) {
		double dx = target.posX - seeker.posX;
		double dz;
		for (dz = target.posZ - seeker.posZ; dx * dx + dz * dz < 1.0E-4D; dz = (Math.random() - Math.random()) * 0.01D) {
			dx = (Math.random() - Math.random()) * 0.01D;
		}
		float yaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - seeker.rotationYaw;
		yaw = yaw - 90;
		while (yaw < -180) { yaw += 360; }
		while (yaw >= 180) { yaw -= 360; }
		return yaw < fov && yaw > -fov;
	}

	/**
	 * Returns true if the target's position is within the area that the seeker is facing and the target can be seen
	 */
	public static final boolean isTargetInSight(EntityLivingBase seeker, Entity target) {
		return isTargetInSight(seeker.getLookVec(), seeker, target);
	}

	/**
	 * Returns true if the target's position is within the area that the seeker is facing and the target can be seen
	 */
	private static final boolean isTargetInSight(Vec3 vec3, EntityLivingBase seeker, Entity target) {
		return seeker.canEntityBeSeen(target) && isTargetInFrontOf(seeker, target, 60);
	}

	/**
	 * Whether the entity is currently standing in any liquid
	 */
	public static boolean isInLiquid(Entity entity) {
		BlockPos pos = new BlockPos(entity);
		Block block = entity.worldObj.getBlockState(pos).getBlock();
		return block.getMaterial().isLiquid();
	}

	/**
	 * Knocks the pushed entity back slightly as though struck by the pushing entity
	 * @param strength The strength of the push; see {@link EntityLivingBase#knockBack(Entity, float, double, double) EntityLivingBase#knockBack}
	 */
	public static final void knockTargetBack(EntityLivingBase pushedEntity, EntityLivingBase pushingEntity, float strength) {
		if (pushedEntity.canBePushed()) {
			double dx = pushingEntity.posX - pushedEntity.posX;
			double dz = pushingEntity.posZ - pushedEntity.posZ;
			pushedEntity.knockBack(pushingEntity, strength, dx, dz);
		}
	}

	/**
	 * Returns the default target selector predicates:
	 * - {@link #COLLIDABLE_ENTITY_SELECTOR}
	 * - {@link #NON_RIDING_SELECTOR}
	 * - {@link #NON_TEAM_SELECTOR}
	 * - {@link #VISIBLE_ENTITY_SELECTOR}
	 */
	public static final List<Predicate<Entity>> getDefaultSelectors() {
		List<Predicate<Entity>> list = Lists.<Predicate<Entity>>newArrayList();
		list.add(COLLIDABLE_ENTITY_SELECTOR);
		list.add(NON_RIDING_SELECTOR);
		list.add(NON_TEAM_SELECTOR);
		list.add(VISIBLE_ENTITY_SELECTOR);
		return list;
	}

	/** Select entities that can be collided with */
	public static final Predicate<Entity> COLLIDABLE_ENTITY_SELECTOR = new Predicate<Entity>() {
		public boolean apply(@Nullable Entity entity) {
			return entity != null && entity.canBeCollidedWith();
		}
	};

	/** Select entities that are considered hostile mobs */
	public static final Predicate<Entity> HOSTILE_MOB_SELECTOR = new Predicate<Entity>() {
		public boolean apply(@Nullable Entity entity) {
			return TargetUtils.isMobEntity(entity);
		}
	};

	/** Select only non-player entities */
	public static final Predicate<Entity> NON_PLAYER_SELECTOR = new Predicate<Entity>() {
		public boolean apply(@Nullable Entity entity) {
			return !(entity instanceof EntityPlayer);
		}
	};

	/** Select entities that are not riding or being ridden by the seeker */
	public static final TargetPredicate<Entity> NON_RIDING_SELECTOR = new TargetPredicate<Entity>() {
		public boolean apply(@Nullable Entity entity) {
			if (entity == null) {
				return false;
			} else if (this.seeker == null) {
				return true;
			}
			return entity.ridingEntity != this.seeker && this.seeker.ridingEntity != entity;
		}
	};

	/** Select entities that are not on the same team as the seeker */
	public static final TargetPredicate<Entity> NON_TEAM_SELECTOR = new TargetPredicate<Entity>() {
		public boolean apply(@Nullable Entity entity) {
			return entity instanceof EntityLivingBase && (this.seeker == null || !((EntityLivingBase) entity).isOnSameTeam(this.seeker));
		}
	};

	/** Select entities that are not invisible */
	public static final Predicate<Entity> VISIBLE_ENTITY_SELECTOR = new Predicate<Entity>() {
		public boolean apply(@Nullable Entity entity) {
			return entity != null && !entity.isInvisible();
		}
	};

	/**
	 * Class for entity selectors that rely on knowing the seeker
	 */
	public abstract static class TargetPredicate<T extends Entity> implements Predicate<T>
	{
		@Nullable
		protected EntityLivingBase seeker;
		public void setSeeker(@Nullable EntityLivingBase seeker) {
			this.seeker = seeker;
		}
	}
}
