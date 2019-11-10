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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dynamicswordskills.api.ItemRandomSkill;
import dynamicswordskills.api.ItemSkillProvider;
import dynamicswordskills.command.DSSCommands;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.entity.EntitySwordBeam;
import dynamicswordskills.entity.IPlayerInfo.CapabilityPlayerInfo;
import dynamicswordskills.item.ItemSkillOrb;
import dynamicswordskills.loot.LootHandler;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import swordskillsapi.api.item.WeaponRegistry;

@Mod(modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VERSION, updateJSON = ModInfo.VERSION_LIST)
public class DynamicSwordSkills
{
	@Mod.Instance(ModInfo.ID)
	public static DynamicSwordSkills instance;

	@SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.COMMON_PROXY)
	public static CommonProxy proxy;

	public static final Logger logger = LogManager.getLogger(ModInfo.ID);

	/** Expected FPS used as a reference to normalize e.g. client-side motion adjustments */
	public static final float BASE_FPS = 30F;

	public static CreativeTabs tabSkills;

	public static Item skillOrb;

	public static List<Item> skillItems;

	/** Various randomized skill swords */
	public static Item
	skillWood,
	skillStone,
	skillIron,
	skillDiamond,
	skillGold;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		if (Loader.isModLoaded("zeldaswordskills")) {
			throw new RuntimeException("Dynamic Sword Skills may not be loaded at the same time as Zelda Sword Skills! Please remove one or the other.");
		}
		Config.init(event);
		tabSkills = new CreativeTabs("dss.skills") {
			@Override
			@SideOnly(Side.CLIENT)
			public ItemStack createIcon() {
				return new ItemStack(DynamicSwordSkills.skillOrb);
			}
		};
		skillOrb = new ItemSkillOrb().setRegistryName(ModInfo.ID, "skillorb").setTranslationKey("dss.skillorb");
		if (Config.areCreativeSwordsEnabled()) {
			skillItems = new ArrayList<Item>(SkillBase.getNumSkills());
			Item item = null;
			for (SkillBase skill : SkillBase.getSkills()) {
				if (!(skill instanceof SkillActive)) {
					continue;
				}
				int level = (skill.getMaxLevel() == SkillBase.MAX_LEVEL ? Config.getSkillSwordLevel() : Config.getSkillSwordLevel() * 2);
				item = new ItemSkillProvider(ToolMaterial.IRON, "iron_sword", skill, (byte) level).setCreativeTab(DynamicSwordSkills.tabSkills);
				skillItems.add(item);
			}
		}
		if (Config.areRandomSwordsEnabled()) {
			skillWood = new ItemRandomSkill(ToolMaterial.WOOD, "wooden_sword");
			skillStone = new ItemRandomSkill(ToolMaterial.STONE, "stone_sword");
			skillIron = new ItemRandomSkill(ToolMaterial.IRON, "iron_sword");
			skillGold = new ItemRandomSkill(ToolMaterial.GOLD, "golden_sword");
			skillDiamond = new ItemRandomSkill(ToolMaterial.DIAMOND, "diamond_sword");
		}
		proxy.preInit();
		PacketDispatcher.initialize();
		registerCapabilities();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init();
		MinecraftForge.EVENT_BUS.register(new DSSCombatEvents());
		MinecraftForge.EVENT_BUS.register(new LootHandler());
		DSSCombatEvents.initializeDrops();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
		FMLInterModComms.sendRuntimeMessage(ModInfo.ID, "VersionChecker", "addVersionCheck", ModInfo.VERSION_LIST);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Config.postInit();
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		DSSCommands.registerCommands(event);
	}

	@Mod.EventHandler
	public void processMessages(FMLInterModComms.IMCEvent event) {
		for (final FMLInterModComms.IMCMessage msg : event.getMessages()) {
			WeaponRegistry.INSTANCE.processMessage(msg);
		}
	}

	@SubscribeEvent
	public void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		final EntityEntry[] entities = {
				createEntityEntryBuilder("leapingblow", EntityLeapingBlow.class, 64, 10, true).build(),
				createEntityEntryBuilder("swordbeam", EntitySwordBeam.class, 64, 10, true).build()
		};
		event.getRegistry().registerAll(entities);
	}

	private int entityId = 0;
	private <E extends Entity> EntityEntryBuilder<E> createEntityEntryBuilder(String name, Class<? extends E> clazz, int trackingRange, int updateFrequency, boolean sendVelocityUpdates) {
		final EntityEntryBuilder<E> builder = EntityEntryBuilder.create();
		final ResourceLocation location = new ResourceLocation(ModInfo.ID, name);
		return builder.id(location, entityId++).name(location.toString()).entity(clazz).tracker(trackingRange, updateFrequency, sendVelocityUpdates);
	}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event) {
		event.getRegistry().register(skillOrb);
		if (Config.areCreativeSwordsEnabled()) {
			event.getRegistry().registerAll(skillItems.toArray(new Item[skillItems.size()]));
		}
		if (Config.areRandomSwordsEnabled()) {
			event.getRegistry().registerAll(skillWood, skillStone, skillIron, skillGold, skillDiamond);
		}
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent event) {
		proxy.registerModels(event);
	}

	@SubscribeEvent
	public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
		final SoundEvent[] sounds = {
				createSound(new ResourceLocation(ModInfo.ID, "armor_break")),
				createSound(new ResourceLocation(ModInfo.ID, "hurt_flesh")),
				createSound(new ResourceLocation(ModInfo.ID, "leaping_blow")),
				createSound(new ResourceLocation(ModInfo.ID, "level_up")),
				createSound(new ResourceLocation(ModInfo.ID, "mortal_draw")),
				createSound(new ResourceLocation(ModInfo.ID, "slam")),
				createSound(new ResourceLocation(ModInfo.ID, "special_drop")),
				createSound(new ResourceLocation(ModInfo.ID, "spin_attack")),
				createSound(new ResourceLocation(ModInfo.ID, "sword_cut")),
				createSound(new ResourceLocation(ModInfo.ID, "sword_miss")),
				createSound(new ResourceLocation(ModInfo.ID, "sword_strike")),
				createSound(new ResourceLocation(ModInfo.ID, "whoosh")),
		};
		event.getRegistry().registerAll(sounds);
	}

	private SoundEvent createSound(ResourceLocation location) {
		return new SoundEvent(location).setRegistryName(location);
	}

	private void registerCapabilities() {
		CapabilityPlayerInfo.register();
	}
}
