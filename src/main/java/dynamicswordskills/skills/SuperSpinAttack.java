package dynamicswordskills.skills;

import java.util.List;

import javax.annotation.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.ref.Config;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

/**
 * 
 * Allows player to continue Spin Attack by pressing the attack key while spinning,
 * and also grants extended attack range while spinning.
 * 
 * The player may spin up to one extra time per skill level.
 * 
 * Requires player to have sufficient health (see {@link Config#getHealthAllowance(int)}) when first activated.
 *
 */
public class SuperSpinAttack extends SkillBase implements ISkillModifier
{
	public SuperSpinAttack(String translationKey) {
		super(translationKey);
	}

	private SuperSpinAttack(SuperSpinAttack skill) {
		super(skill);
	}

	@Override
	public SuperSpinAttack newInstance() {
		return new SuperSpinAttack(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP;
	}

	@Nullable
	public String getActivationDisplay() {
		return StatCollector.translateToLocal(getTranslationKey() + ".activation");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.range", String.format("%.1f", getRangeModifier())));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.spins", level));
	}

	@Override
	protected void resetModifiers(EntityPlayer player) {
	}

	/** Returns true if players current health is within the allowed limit */
	private boolean checkHealth(EntityPlayer player) {
		return player.capabilities.isCreativeMode || PlayerUtils.getHealthMissing(player) <= Config.getHealthAllowance(level);
	}

	/** Returns the range modifier to apply to attack reach while spinning */
	public float getRangeModifier() {
		return level * 0.3F;
	}

	@Override
	public boolean applyOnActivated(EntityPlayer player) {
		return checkHealth(player); // extend range etc.
	}

	@Override
	public boolean applyOnKeyPress(Minecraft mc, KeyBinding key, EntityPlayer player) {
		return key == mc.gameSettings.keyBindAttack; // refresh spin
	}
}
