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
import dynamicswordskills.DSSCombatEvents;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 
 * LEAPING BLOW
 * Activation: Jump while holding block, then attack
 * Damage: Regular sword damage (without enchantment bonuses), +1 extra damage per skill level
 * Effect: Adds Weakness I for (50 + (10 * level)) ticks
 * Range: Technique travels roughly 3 blocks + 1/2 block per level
 * Area: Approximately (0.5F + (0.25F * level)) radius in a straight line
 * Exhaustion: 2.0F minus 0.1F per level (1.5F at level 5)
 * 
 * Upon landing, all targets directly in front of the player take damage and
 * are weakened temporarily.
 * 
 */
public class LeapingBlow extends SkillActive
{
	/** Activation window for pressing the attack key, set when player initially leaps */
	private int ticksTilFail;

	/** Set to true when activated; set to false upon landing */
	private boolean isActive = false;

	public LeapingBlow(String name) {
		super(name);
	}

	private LeapingBlow(LeapingBlow skill) {
		super(skill);
	}

	@Override
	public LeapingBlow newInstance() {
		return new LeapingBlow(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getDamageDisplay(level, true));
		desc.add(getRangeDisplay(3.0F + 0.5F * level));
		desc.add(getAreaDisplay(0.5F + 0.25F * level));
		desc.add(getDurationDisplay(getPotionDuration(player), false));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public boolean isAnimating() {
		return isActive() || ticksTilFail > 0;
	}

	@Override
	protected float getExhaustion() {
		return 2.0F - (0.1F * level);
	}

	/**
	 * LeapingBlow adds exhaustion after entity is spawned, rather than on initial activation
	 */
	@Override
	protected boolean autoAddExhaustion() {
		return false;
	}

	/** Returns player's base damage (which includes all attribute bonuses) plus 1.0F per level */
	private float getDamage(EntityPlayer player) {
		return (float)(level + player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
	}

	/** Duration of weakness effect; used for tooltip display only */
	private int getPotionDuration(EntityPlayer player) {
		return (50 + (level * 10));
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this) && !TargetUtils.isInLiquid(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return ticksTilFail > 0 && !player.onGround && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key) {
		return (key == mc.gameSettings.keyBindJump 
				|| key == DSSKeyHandler.keys[DSSKeyHandler.KEY_ATTACK].getKey() 
				|| (Config.allowVanillaControls() && key == mc.gameSettings.keyBindAttack));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == mc.gameSettings.keyBindJump) {
			if (player.onGround && PlayerUtils.isBlocking(player) && canUse(player)) {
				ticksTilFail = 10;
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
				KeyBinding.setKeyBindState(DSSKeyHandler.keys[DSSKeyHandler.KEY_BLOCK].getKeyCode(), false);
				return true;
			}
		} else if (canExecute(player) && activate(player)) {
			DSSCombatEvents.setPlayerAttackTime(player); // prevent left-click spam
			return true;
		}
		return false;
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		isActive = true;
		ticksTilFail = 0;
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		isActive = false;
		ticksTilFail = 0;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		// Handle on client because onGround is always true on the server
		if (player.worldObj.isRemote) {
			if (isActive() && (player.onGround || TargetUtils.isInLiquid(player))) {
				deactivate(player);
			} else if (ticksTilFail > 0) {
				--ticksTilFail;
			}
		}
	}

	/**
	 * Called from Forge fall Events (note that these are not fired if player lands in liquid!)
	 * @param distance distance fallen, passed from Forge fall Event
	 */
	public void onImpact(EntityPlayer player, float distance) {
		SwordBasic swordSkill = (SwordBasic) DSSPlayerInfo.get(player).getPlayerSkill(swordBasic);
		if (isActive() && swordSkill != null && swordSkill.isActive() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this)) {
			if (player.worldObj.isRemote) {
				if (distance < 1.0F) {
					DSSClientEvents.performComboAttack(Minecraft.getMinecraft(), swordSkill);
				} else {
					player.swingItem();
				}
			} else if (distance >= 1.0F) {
				// add exhaustion here, now that skill has truly activated:
				player.addExhaustion(getExhaustion());
				Entity entity = new EntityLeapingBlow(player.worldObj, player).setDamage(getDamage(player)).setLevel(level);
				player.worldObj.spawnEntityInWorld(entity);
				PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_LEAPINGBLOW, 0.4F, 0.5F);
			}
		}
		onDeactivated(player.worldObj, player);
	}
}
