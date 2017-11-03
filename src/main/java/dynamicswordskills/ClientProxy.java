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

package dynamicswordskills;

import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.client.RenderEntitySwordBeam;
import dynamicswordskills.client.RenderNothing;
import dynamicswordskills.client.TargetingTickHandler;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.entity.EntitySwordBeam;
import dynamicswordskills.item.IModItem;
import dynamicswordskills.ref.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy
{
	private final Minecraft mc = Minecraft.getMinecraft();

	@Override
	public void preInit() {
		MinecraftForge.EVENT_BUS.register(new DSSClientEvents());
		MinecraftForge.EVENT_BUS.register(new DSSKeyHandler());
		MinecraftForge.EVENT_BUS.register(new TargetingTickHandler());
		RenderingRegistry.registerEntityRenderingHandler(EntityLeapingBlow.class, new RenderNothing.Factory());
		RenderingRegistry.registerEntityRenderingHandler(EntitySwordBeam.class, new RenderEntitySwordBeam.Factory());
		registerItemRenderer((IModItem) DynamicSwordSkills.skillOrb);
		if (Config.areCreativeSwordsEnabled()) {
			for (Item item : DynamicSwordSkills.skillItems) {
				registerItemRenderer((IModItem) item);
			}
		}
		if (Config.areRandomSwordsEnabled()) {
			registerItemRenderer((IModItem) DynamicSwordSkills.skillWood);
			registerItemRenderer((IModItem) DynamicSwordSkills.skillStone);
			registerItemRenderer((IModItem) DynamicSwordSkills.skillIron);
			registerItemRenderer((IModItem) DynamicSwordSkills.skillDiamond);
			registerItemRenderer((IModItem) DynamicSwordSkills.skillGold);
		}
	}

	private void registerItemRenderer(IModItem item) {
		item.registerResources();
	}

	@Override
	public IThreadListener getThreadFromContext(MessageContext ctx) {
		return (ctx.side.isClient() ? mc : super.getThreadFromContext(ctx));
	}

	@Override
	public EntityPlayer getPlayerEntity(MessageContext ctx) {
		return (ctx.side.isClient() ? mc.player : super.getPlayerEntity(ctx));
	}
}
