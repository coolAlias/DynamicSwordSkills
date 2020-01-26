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

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.network.bidirectional.DeactivateSkillPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;

/**
 * 
 * Base class for active skills. Extend this class to add specific functionality.
 * 
 * Note that additional fields in child classes are not saved to NBT, so should not be
 * used to store any data that needs to be maintained between game sessions.
 * 
 * Unless the skill's activation is handled exclusively by the client side, ONLY activate or trigger
 * the skill on the server, as a packet will be sent automatically to notify the client.
 *
 */
public abstract class SkillActive extends SkillBase
{
	public SkillActive(String translationKey) {
		super(translationKey);
	}

	protected SkillActive(SkillActive skill) {
		super(skill);
	}

	@Override
	public String getActivationDisplay() {
		return StatCollector.translateToLocal(getTranslationKey() + ".activation");
	}

	/**
	 * Return false if this skill may not be directly activated manually, in which case it
	 * should have some other method of {@link #trigger(World, EntityPlayer) triggering}
	 * @return	Default returns TRUE, allowing activation via {@link #activate}
	 */
	protected boolean allowUserActivation() {
		return true;
	}

	/** Returns true if this skill is currently active, however that is defined by the child class */
	public abstract boolean isActive();

	/** Amount of exhaustion added to the player each time this skill is used */
	protected abstract float getExhaustion();

	/**
	 * Return true to automatically add exhaustion amount upon activation.
	 * Used by LeapingBlow, since it may or may not trigger upon landing.
	 */
	protected boolean autoAddExhaustion() {
		return true;
	}

	@Override
	protected void resetModifiers(EntityPlayer player) {}

	/**
	 * Returns true if this skill can currently be used by the player (i.e. activated or triggered)
	 * @return 	Default returns true if the skill's level is at least one and either the player
	 * 			is in Creative Mode or the food bar is not empty 
	 */
	public boolean canUse(EntityPlayer player) {
		return (level > 0 && (player.capabilities.isCreativeMode || player.getFoodStats().getFoodLevel() > 0));
	}

	/**
	 * Called only on the client side as a pre-activation check for some skills;
	 * typical use is to check if all necessary keys have been pressed
	 */
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return true;
	}

	/**
	 * Returning true allows {@link #keyPressed} and {@link #receiveActiveKeys} to be called, as appropriate
	 * @param isLockedOn Whether the player is currently locked on to a target with an ILockOnTarget skill
	 */
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		return false;
	}

	/**
	 * Equivalent of {@link #keyPressed} but called only while this skill is animating.
	 * Will not be called for the ATTACK key if {@link DSSPlayerInfo#canAttack()} returns false.
	 * Will not be called for the USE_ITEM key if {@link DSSPlayerInfo#canUseItem()} returns false.
	 * Note that key presses while animating are always considered 'handled' i.e. the button state will not be changed
	 */
	@SideOnly(Side.CLIENT)
	public void keyPressedWhileAnimating(Minecraft mc, KeyBinding key, EntityPlayer player) {
	}

	/**
	 * This method is called if {@link #isKeyListener} returns true for the given key and the
	 * skill is not currently animating, allowing the skill to handle the key input accordingly.
	 * Note that each key press may only be handled once, on a first-come first-serve basis.
	 * @return	True signals that the key press was "handled" and prevents propagation to any
	 * 			remaining listeners; this should usually only occur when a skill is activated
	 */
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		return false;
	}

	/**
	 * This method is called if {@link #isKeyListener} returns true for the given key,
	 * allowing each skill to handle key releases accordingly.
	 * Skills do not have to be {@link #isActive()} to receive key releases.
	 */
	@SideOnly(Side.CLIENT)
	public void keyReleased(Minecraft mc, KeyBinding key, EntityPlayer player) {}

	/**
	 * Whether this skill automatically sends an {@link ActivateSkillPacket} to the client from {@link #trigger}
	 */
	protected boolean sendClientUpdate() {
		return true;
	}

	/**
	 * Called after a skill is activated via {@link #trigger}; on the client, this only
	 * gets called after receiving the {@link ActivateSkillPacket} sent when triggered on
	 * the server. If {@link #sendClientUpdate} returns false, then the packet is not sent
	 * and this method will not be called on the client, in which case any client-side
	 * requirements (e.g. player.swingItem) should be done when sending the activation
	 * packet to the server.
	 * 
	 * Anything that needs to happen when the skill is activated should be done here,
	 * such as setting timers, etc.
	 * 
	 * @return	Return true to have this skill added to the currently active skills list;
	 * 			Typically returns {@link #isActive}, which is almost always true after this method is called
	 */
	protected abstract boolean onActivated(World world, EntityPlayer player);

	/**
	 * Called when the skill is forcefully deactivated on either side via {@link #deactivate}.
	 * 
	 * Each implementation MUST guarantee that {@link isActive} no longer returns
	 * true once the method has completed.
	 * 
	 * {@link #isAnimating} is not required to return false after deactivation, though it usually should.
	 */
	protected abstract void onDeactivated(World world, EntityPlayer player);

	/**
	 * Use this method when a player tries to manually activate a skill, e.g. from a key binding,
	 * HUD, or other such means. An activation packet is sent to the server if called client-side.
	 * {@link #allowUserActivation()} may prevent activation via this method.
	 * @return The result of {@link DSSPlayerInfo#activateSkill(SkillBase, boolean)}
	 */
	public final boolean activate(EntityPlayer player) {
		if (Config.isSkillDisabled(player, this) || !allowUserActivation()) {
			return false;
		} else if (player.worldObj.isRemote) {
			PacketDispatcher.sendToServer(new ActivateSkillPacket(this, false));
			if (sendClientUpdate()) {
				return true; // prevent activateSkill from getting called twice
			}
		}
		return DSSPlayerInfo.get(player).activateSkill(this, false);
	}

	/**
	 * Forcefully deactivates a skill.
	 * 
	 * Call this method on either side to ensure that the skill is deactivated on both.
	 * If the skill is currently {@link #isActive active}, then {@link #onDeactivated}
	 * is called and a {@link DeactivateSkillPacket} is sent to the other side.
	 * 
	 * If the skill is still active after onDeactivated was called, a SEVERE message
	 * is generated noting the skill that failed to meet onDeactivated's specifications,
	 * as such behavior may result in severe instability or even crashes and should be
	 * fixed immediately.
	 */
	public final void deactivate(EntityPlayer player) throws IllegalStateException {
		if (isActive()) {
			onDeactivated(player.worldObj, player);
			if (isActive()) {
				DynamicSwordSkills.logger.error(getDisplayName() + " is still active after onDeactivated called - this may result in SEVERE errors or even crashes!!!");
			} else if (player.worldObj.isRemote) {
				PacketDispatcher.sendToServer(new DeactivateSkillPacket(this));
			} else {
				PacketDispatcher.sendTo(new DeactivateSkillPacket(this), (EntityPlayerMP) player);
			}
		}
	}

	/**
	 * This method should not be called directly; use {@link DSSPlayerInfo#activateSkill} instead.
	 * 
	 * If {@link #canUse} returns true, the skill will be activated.
	 * {@link #getExhaustion} is added if {@link #autoAddExhaustion} is true, and an
	 * {@link ActivateSkillPacket} is sent to the client if required.
	 * 
	 * Finally, {@link #onActivated} is called, allowing the skill to initialize its active state.
	 * 
	 * @param wasTriggered Whether the skill was triggered via some means other than direct user interaction (see {@link #allowUserActivation})
	 * @return	Returns {@link #onActivated}, signaling whether or not to add the skill to the list of currently active skills.
	 */
	@SuppressWarnings("unchecked")
	public final <T extends SkillActive & IModifiableSkill> boolean trigger(World world, EntityPlayer player, boolean wasTriggered) {
		if (Config.isSkillDisabled(player, this)) {
			// Force client to deactivate in case client config settings differ
			if (!world.isRemote) {
				PacketDispatcher.sendTo(new DeactivateSkillPacket(this), (EntityPlayerMP) player);
			}
			PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.use.disabled", new ChatComponentTranslation(getNameTranslationKey()));
		} else if (!wasTriggered && !allowUserActivation()) {
			// no-op
		} else if (canUse(player)) {
			if (autoAddExhaustion() && !player.capabilities.isCreativeMode) {
				player.addExhaustion(getExhaustion());
			}
			if (!world.isRemote) {
				if (sendClientUpdate()) {
					PacketDispatcher.sendTo(new ActivateSkillPacket(this, wasTriggered), (EntityPlayerMP) player);
				}
			}
			if (onActivated(world, player)) {
				if (this instanceof IModifiableSkill) {
					SkillActive.applyActivationSkillModifiers((T) this, player);
				}
				postActivated(player);
				return true;
			}
		} else if (level > 0) {
			PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.use.fail", new ChatComponentTranslation(getNameTranslationKey()));
		}
		return false;
	}

	/**
	 * Called after {@link #onActivated(World, EntityPlayer)} has returned true and any {@link ISkillModifier}s have had a chance to be applied
	 */
	protected void postActivated(EntityPlayer player) {
	}

	/**
	 * Applies all modifiers that {@link ISkillModifier#applyOnActivated(SkillActive, EntityPlayer) apply on activation} to the parent skill,
	 * provided the player has at least 1 level in the modifier and it is not {@link Config#isSkillDisabled(SkillBase) disabled}
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends SkillActive & IModifiableSkill, M extends SkillBase & ISkillModifier> void applyActivationSkillModifiers(T parent, EntityPlayer player) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		for (SkillBase t : parent.getSkillModifiers()) {
			if (Config.isSkillDisabled(player, t)) {
				continue;
			}
			SkillBase instance = skills.getPlayerSkill(t);
			if (instance instanceof ISkillModifier && instance.getLevel() > 0 && ((ISkillModifier) instance).applyOnActivated(player)) {
				parent.applySkillModifier((M) instance, player);
			}
		}
	}

	/**
	 * Return true to flag this skill as requiring animation, in which case interactions
	 * via mouse or keyboard are disabled while {@link #isAnimating} returns true
	 * @return Default is TRUE - override for skills that do not have animations
	 */
	public boolean hasAnimation() {
		return true;
	}

	/**
	 * Whether this skill's animation is currently in progress, in which case {@link #onRenderTick}
	 * will be called each render tick and mouse/keyboard interactions are disabled.
	 * @return Default implementation returns {@link #isActive()}
	 */
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return isActive();
	}

	/**
	 * This method is called each render tick that {@link #isAnimating} returns true
	 * @param partialTickTime	The current render tick time
	 * @return Return true to prevent the targeting camera from auto-updating the player's view
	 */
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		return false;
	}

	/**
	 * Use this method to e.g. attack the target entity with a different damage source.
	 * Called from {@link LivingAttackEvent}; for players, this is called on both sides. 
	 * 
	 * @param player The skill-using player inflicting damage (i.e. event.source.getEntity() is the player)
	 * @param entity The entity damaged, i.e. LivingHurtEvent's entityLiving
	 * @param source The DamageSource from the event
	 * @param amount The damage amount
	 * @return       True to cancel the event
	 */
	public boolean onAttack(EntityPlayer player, EntityLivingBase entity, DamageSource source, float amount) {
		return false;
	}

	/**
	 * Called from LivingAttackEvent only if the skill is currently {@link #isActive() active}
	 * @param player	The skill-using player under attack
	 * @param source	The source of damage; source#getEntity() is the entity that will strike the player,
	 * 					source.getSourceOfDamage() is either the same, or the entity responsible for
	 * 					unleashing the other entity (such as the shooter of an arrow)
	 * @return			Return true to cancel the attack event
	 */
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		return false;
	}

	/**
	 * Use this method to modify the damage amount for an entity attacked while this skill is active.
	 * Called from LivingHurtEvent with NORMAL priority.
	 * 
	 * @param player The skill-using player inflicting damage (i.e. event.source.getEntity() is the player)
	 * @param entity The entity damaged, i.e. LivingHurtEvent's entityLiving
	 * @param amount The current damage amount from {@link LivingHurtEvent#amount}
	 * @return       The modified damage amount to inflict, or 0 to cancel the event
	 */
	public float onImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		return amount;
	}

	/**
	 * Use this method to activate effects after damage has been done.
	 * Called from LivingHurtEvent with LOWEST priority.
	 * 
	 * @param player	The skill-using player inflicting damage (i.e. event.source.getEntity() is the player)
	 * @param entity	The entity damaged, i.e. LivingHurtEvent's entityLiving
	 * @param amount	The current damage amount from {@link LivingHurtEvent#ammount}
	 */
	public void postImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
	}

	/**
	 * Called from Forge fall Events if the skill is currently {@link #isActive() active};
	 * note that these are not fired if the player lands in liquid
	 * @return True to prevent further processing without having to cancel the event or set distance to 0
	 */
	public boolean onFall(EntityPlayer player, LivingFallEvent event) {
		return false;
	}

	/**
	 * Called from Forge fall Events if the skill is currently {@link #isActive() active};
	 * note that these are not fired if the player lands in liquid
	 * @return True to prevent further processing without having to cancel the event or set distance to 0
	 */
	public boolean onCreativeFall(EntityPlayer player, PlayerFlyableFallEvent event) {
		return false;
	}
}
