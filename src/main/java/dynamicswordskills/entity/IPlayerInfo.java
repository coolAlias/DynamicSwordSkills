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

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import dynamicswordskills.capability.SimpleCapabilityProvider;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 
 * Quick and dirty capability for retrieving original DSSPlayerInfo class instance
 *
 */
public interface IPlayerInfo {

	DSSPlayerInfo get();

	public static class PlayerInfo implements IPlayerInfo {
		private final DSSPlayerInfo info;
		public PlayerInfo(DSSPlayerInfo info) {
			this.info = info;
		}
		@Override
		public DSSPlayerInfo get() {
			return this.info;
		}
	}

	public static class CapabilityPlayerInfo {
		@CapabilityInject(IPlayerInfo.class)
		public static final Capability<IPlayerInfo> PLAYER_INFO = null;

		public static final ResourceLocation ID = new ResourceLocation(ModInfo.ID, "PlayerInfo");

		@Nullable
		public static IPlayerInfo getMaxHealth(EntityLivingBase entity) {
			return entity.getCapability(PLAYER_INFO, null);
		}

		public static ICapabilityProvider createProvider(IPlayerInfo info) {
			return new SimpleCapabilityProvider<>(PLAYER_INFO, null, info);
		}
		public static void register() {
			CapabilityManager.INSTANCE.register(IPlayerInfo.class, new Capability.IStorage<IPlayerInfo>() {
				@Override
				public NBTBase writeNBT(Capability<IPlayerInfo> capability, IPlayerInfo instance, EnumFacing side) {
					return instance.get().writeNBT(new NBTTagCompound());
				}

				@Override
				public void readNBT(Capability<IPlayerInfo> capability, IPlayerInfo instance, EnumFacing side, NBTBase nbt) {
					instance.get().readNBT((NBTTagCompound) nbt);
				}
			}, new Callable<IPlayerInfo>() {
				@Override
				public IPlayerInfo call() throws Exception {
					return new PlayerInfo(null);
				}
			});
			MinecraftForge.EVENT_BUS.register(new CapabilityPlayerInfo());
		}
		@SubscribeEvent
		public void attach(AttachCapabilitiesEvent.Entity event) {
			if (event.getEntity() instanceof EntityPlayer) {
				IPlayerInfo info = new IPlayerInfo.PlayerInfo(new DSSPlayerInfo((EntityPlayer) event.getEntity()));
				event.addCapability(ID, createProvider(info));
			}
		}
	}
}
