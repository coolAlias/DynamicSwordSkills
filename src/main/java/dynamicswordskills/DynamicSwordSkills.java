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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
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
		Config.init(event);
		tabSkills = new CreativeTabs("dss.skills") {
			@Override
			@SideOnly(Side.CLIENT)
			public Item getTabIconItem() {
				return DynamicSwordSkills.skillOrb;
			}
		};
		skillOrb = new ItemSkillOrb().setRegistryName(ModInfo.ID, "skillorb").setUnlocalizedName("dss.skillorb");
		GameRegistry.register(skillOrb);
		if (Config.areCreativeSwordsEnabled()) {
			skillItems = new ArrayList<Item>(SkillBase.getNumSkills());
			Item item = null;
			for (SkillBase skill : SkillBase.getSkills()) {
				if (!(skill instanceof SkillActive)) {
					continue;
				}
				int level = (skill.getMaxLevel() == SkillBase.MAX_LEVEL ? Config.getSkillSwordLevel() : Config.getSkillSwordLevel() * 2);
				item = new ItemSkillProvider(ToolMaterial.WOOD, "stick", skill, (byte) level)
						.setRegistryName(ModInfo.ID, "training_stick_" + skill.getUnlocalizedName())
						.setUnlocalizedName("dss.training_stick")
						.setCreativeTab(DynamicSwordSkills.tabSkills);
				skillItems.add(item);
				GameRegistry.register(item);
			}
		}
		if (Config.areRandomSwordsEnabled()) {
			skillWood = new ItemRandomSkill(ToolMaterial.WOOD, "wooden_sword").setRegistryName(ModInfo.ID, "skill_sword_wood").setUnlocalizedName("dss.skill_sword.wood");
			GameRegistry.register(skillWood);
			skillStone = new ItemRandomSkill(ToolMaterial.STONE, "stone_sword").setRegistryName(ModInfo.ID, "skill_sword_stone").setUnlocalizedName("dss.skill_sword.stone");
			GameRegistry.register(skillStone);
			skillIron = new ItemRandomSkill(ToolMaterial.IRON, "iron_sword").setRegistryName(ModInfo.ID, "skill_sword_iron").setUnlocalizedName("dss.skill_sword.iron");
			GameRegistry.register(skillIron);
			skillGold = new ItemRandomSkill(ToolMaterial.GOLD, "golden_sword").setRegistryName(ModInfo.ID, "skill_sword_gold").setUnlocalizedName("dss.skill_sword.gold");
			GameRegistry.register(skillGold);
			skillDiamond = new ItemRandomSkill(ToolMaterial.DIAMOND, "diamond_sword").setRegistryName(ModInfo.ID, "skill_sword_diamond").setUnlocalizedName("dss.skill_sword.diamond");
			GameRegistry.register(skillDiamond);
		}
		proxy.preInit();
		registerSounds();
		EntityRegistry.registerModEntity(EntityLeapingBlow.class, "leapingblow", 0, this, 64, 10, true);
		EntityRegistry.registerModEntity(EntitySwordBeam.class, "swordbeam", 1, this, 64, 10, true);
		PacketDispatcher.initialize();
		registerCapabilities();
	}

	private void registerCapabilities() {
		CapabilityPlayerInfo.register();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init();
		MinecraftForge.EVENT_BUS.register(new DSSCombatEvents());
		MinecraftForge.EVENT_BUS.register(new LootHandler());
		DSSCombatEvents.initializeDrops();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
		GameRegistry.addRecipe(new RecipeInfuseSkillOrb());
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

	@Mod.EventHandler
	public void processMissingMappings(FMLMissingMappingsEvent event) {
		event.get().stream().forEach(s -> {
			ResourceLocation location = null;
			if (s.resourceLocation.getResourcePath().startsWith("skillitem_")) {
				location = new ResourceLocation(s.resourceLocation.getResourceDomain(), s.resourceLocation.getResourcePath().replace("skillitem_", "training_stick_").toLowerCase());
			} else if (s.resourceLocation.getResourcePath().startsWith("skillsword_")) {
				location = new ResourceLocation(s.resourceLocation.getResourceDomain(), s.resourceLocation.getResourcePath().replace("skillsword", "skill_sword").toLowerCase());
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
	 * Call during mod initialization to register all mod sounds
	 */
	private void registerSounds() {
		String[] sounds = {"armor_break","hurt_flesh","leaping_blow","level_up","mortal_draw","slam","special_drop","spin_attack","sword_cut","sword_miss","sword_strike","whoosh"};
		for (String sound : sounds) {
			registerSound(new ResourceLocation(ModInfo.ID, sound));
		}
	}

	private void registerSound(ResourceLocation location) {
		GameRegistry.register(new SoundEvent(location).setRegistryName(location));
	}
}
