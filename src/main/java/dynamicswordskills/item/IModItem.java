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

package dynamicswordskills.item;

import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * Interface to allow simpler registration of textures for mod items
 *
 */
public interface IModItem {

	/**
	 * Register any item variant names here using {@link ModelBakery#addVariantName}
	 */
	@SideOnly(Side.CLIENT)
	public void registerVariants();

	/**
	 * Register all of the Item's renderers here, including for any subtypes.
	 * 
	 * A default implementation to register an item with modid:itemname would be:
	 *
	 *	String name = getUnlocalizedName();
	 *	name = YourMod.MODID + ":" + name.substring(name.lastIndexOf(".") + 1);
	 *	mesher.register(this, 0, new ModelResourceLocation(name, "inventory"));
	 */
	@SideOnly(Side.CLIENT)
	public void registerRenderer(ItemModelMesher mesher);

}
