package dynamicswordskills.skills;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;

public interface ISkillModifier
{
	/**
	 * Called when the parent skill is first activated, i.e. after the {@link SkillActive#onActivated(net.minecraft.world.World, EntityPlayer)} method returns true
	 * @return true to call {@link IModifiableSkill#applySkillModifier(EntityPlayer, SkillBase)} with this modifier
	 */
	boolean applyOnActivated(EntityPlayer player);

	/**
	 * Called while the parent is animating and a key is pressed, i.e. after calling {@link SkillActive#keyPressedWhileAnimating(Minecraft, KeyBinding, EntityPlayer)}.
	 * A packet will be sent automatically be sent to apply the modifier on the server as well.
	 * @return true to call {@link IModifiableSkill#applySkillModifier(EntityPlayer, SkillBase)} with this modifier
	 */
	@SideOnly(Side.CLIENT)
	boolean applyOnKeyPress(Minecraft mc, KeyBinding key, EntityPlayer player);

}
