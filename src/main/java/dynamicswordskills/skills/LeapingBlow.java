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

package dynamicswordskills.skills;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.lib.ModInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.AddExhaustionPacket;
import dynamicswordskills.network.server.SpawnLeapingBlowPacket;
import dynamicswordskills.util.PlayerUtils;

/**
 * 
 * LEAPING BLOW
 * Activation: Jump while holding block
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
	/** Set to true when jumping and 'attack' key pressed; set to false upon landing */
	private boolean isActive = false;

	public LeapingBlow(String name) {
		super(name);
		setDisablesLMB();
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
	protected float getExhaustion() {
		return 2.0F - (0.1F * level);
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
		return super.canUse(player) && !isActive() && !player.onGround &&
				PlayerUtils.isSwordOrProvider(player.getHeldItem(), this);
	}

	@Override
	public boolean activate(World world, EntityPlayer player) {
		isActive = canUse(player);
		if (isActive()) {
			DSSPlayerInfo.get(player).setCurrentActiveSkill(this);
		}
		return isActive();
	}

	/**
	 * Called from Forge fall Events
	 * @param distance distance fallen, passed from Forge fall Event
	 */
	@SideOnly(Side.CLIENT)
	public void onImpact(EntityPlayer player, float distance) {
		SwordBasic swordSkill = (SwordBasic) DSSPlayerInfo.get(player).getPlayerSkill(swordBasic);
		if (isActive() && swordSkill != null && swordSkill.isActive() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this)) {
			isActive = false;
			if (distance < 1.0F) {
				DSSClientEvents.performComboAttack(Minecraft.getMinecraft(), swordSkill);
			} else {
				player.swingItem();
				PacketDispatcher.sendToServer(new AddExhaustionPacket(getExhaustion()));
				PacketDispatcher.sendToServer(new SpawnLeapingBlowPacket());
			}
		}
	}

	/**
	 * Called upon receipt of SpawnLeapingBlowPacket on server; spawns the entity
	 */
	public void spawnLeapingBlowEntity(World world, EntityPlayer player) {
		Entity entity = new EntityLeapingBlow(world, player).setDamage(getDamage(player)).setLevel(level);
		world.spawnEntityInWorld(entity);
		PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_LEAPINGBLOW, 0.4F, 0.5F);
	}
}
