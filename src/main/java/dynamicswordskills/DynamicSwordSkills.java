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
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.command.DSSCommands;
import dynamicswordskills.crafting.RecipeInfuseSkillOrb;
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
import dynamicswordskills.skills.Skills;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
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

@Mod(modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VERSION, updateJSON = ModInfo.VERSION_LIST, guiFactory = ModInfo.ID + ".client.gui.GuiFactoryConfig")
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
		Skills.init();
		Config.init(event);
		tabSkills = new CreativeTabs("dss.skills") {
			@Override
			@SideOnly(Side.CLIENT)
			public ItemStack createIcon() {
				return new ItemStack(DynamicSwordSkills.skillOrb);
			}
		};
		skillOrb = new ItemSkillOrb(Skills.getSkillIdMap()).setRegistryName(ModInfo.ID, "skillorb").setTranslationKey("dss.skillorb");
		if (Config.areCreativeSwordsEnabled()) {
			skillItems = new ArrayList<Item>(SkillRegistry.getValues().size());
			Item item = null;
			// Hack to maintain original display order
			List<SkillBase> skills = SkillRegistry.getSortedList(SkillRegistry.SORT_BY_ID);
			for (SkillBase skill : skills) {
				if (!(skill instanceof SkillActive)) {
					continue;
				}
				int level = (skill.getMaxLevel() == SkillBase.MAX_LEVEL ? Config.getSkillSwordLevel() : Config.getSkillSwordLevel() * 2);
				item = new ItemSkillProvider(ToolMaterial.WOOD, "stick", skill, (byte) level)
						.setRegistryName(ModInfo.ID, "training_stick_" + skill.getRegistryName().getPath())
						.setTranslationKey("dss.training_stick")
						.setCreativeTab(DynamicSwordSkills.tabSkills);
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
		DSSCombatEvents.initializeDrops();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Config.postInit();
		MinecraftForge.EVENT_BUS.register(new LootHandler());
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
	public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		event.getRegistry().register(new RecipeInfuseSkillOrb());
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

	@SubscribeEvent
	public void processMissingMappings(RegistryEvent.MissingMappings<Item> event) {
		event.getMappings().stream().forEach(s -> {
			ResourceLocation location = null;
			if (s.key.getPath().startsWith("skillitem_")) {
				// Update old skillitem to training_stick
				String skill_name = s.key.getPath().substring("skillitem_".length());
				SkillBase skill = SkillRegistry.get(new ResourceLocation(s.key.getNamespace(), skill_name));
				if (Skills.superSpinAttack.is(skill)) {
					s.ignore();
				} else if (skill != null) {
					location = new ResourceLocation(s.key.getNamespace(), "training_stick_" + skill.getRegistryName().getPath().toLowerCase());
				}
			} else if (s.key.getPath().startsWith("training_stick_")) {
				// Handle skill registry name changes
				String skill_name = s.key.getPath().substring("training_stick_".length());
				SkillBase skill = SkillRegistry.get(new ResourceLocation(s.key.getNamespace(), skill_name));
				if (Skills.superSpinAttack.is(skill)) {
					s.ignore();
				} else if (skill != null && !skill.getRegistryName().getPath().equals(skill_name)) {
					location = new ResourceLocation(s.key.getNamespace(), "training_stick_" + skill.getRegistryName().getPath().toLowerCase());
				}
			} else if (s.key.getPath().startsWith("skillsword_")) {
				location = new ResourceLocation(s.key.getNamespace(), s.key.getPath().replace("skillsword", "skill_sword").toLowerCase());
			}
			if (location != null) {
				Item item = Item.REGISTRY.getObject(location);
				if (item == null) {
					s.fail();
				} else {
					s.remap(item);
				}
			}
		});
	}

	/**
	 * Parses a String into a key, or NULL if format was invalid
	 * @param name A valid key string e.g. 'modid:registry_name'
	 */
	public static ResourceLocation getResourceLocation(String name) {
		try {
			return new ResourceLocation(name);
		} catch (NullPointerException e) {
			DynamicSwordSkills.logger.error(String.format("Invalid key string: %s", name));
		}
		return null;
	}
}
