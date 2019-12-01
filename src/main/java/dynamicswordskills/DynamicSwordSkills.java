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

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import dynamicswordskills.api.ItemRandomSkill;
import dynamicswordskills.api.ItemSkillProvider;
import dynamicswordskills.api.WeaponRegistry;
import dynamicswordskills.command.DSSCommands;
import dynamicswordskills.crafting.RecipeInfuseSkillOrb;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.entity.EntitySwordBeam;
import dynamicswordskills.item.ItemSkillOrb;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;

@Mod(modid = ModInfo.ID, version = ModInfo.VERSION, updateJSON = ModInfo.VERSION_LIST, guiFactory = ModInfo.ID + ".client.gui.GuiFactoryConfig")
public class DynamicSwordSkills
{
	@Mod.Instance(ModInfo.ID)
	public static DynamicSwordSkills instance;

	@SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.COMMON_PROXY)
	public static CommonProxy proxy;

	public static final Logger logger = LogManager.getLogger(ModInfo.ID);

	/** Expected FPS used as a reference to normalize e.g. client-side motion adjustments */
	public static final float BASE_FPS = 30F;

	/** Whether Battlegear2 mod is loaded */
	public static boolean isBG2Enabled;

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
		isBG2Enabled = Loader.isModLoaded("battlegear2");
		Config.init(event);
		tabSkills = new CreativeTabs("dss.skills") {
			@Override
			@SideOnly(Side.CLIENT)
			public Item getTabIconItem() {
				return DynamicSwordSkills.skillOrb;
			}
		};
		skillOrb = new ItemSkillOrb().setUnlocalizedName("dss.skillorb");
		GameRegistry.registerItem(skillOrb, skillOrb.getUnlocalizedName().substring(5));
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
				GameRegistry.registerItem(item);
			}
		}
		if (Config.areRandomSwordsEnabled()) {
			skillWood = new ItemRandomSkill(ToolMaterial.WOOD, "wooden_sword").setRegistryName(ModInfo.ID, "skill_sword_wood").setUnlocalizedName("dss.skill_sword.wood");
			GameRegistry.registerItem(skillWood);
			skillStone = new ItemRandomSkill(ToolMaterial.STONE, "stone_sword").setRegistryName(ModInfo.ID, "skill_sword_stone").setUnlocalizedName("dss.skill_sword.stone");
			GameRegistry.registerItem(skillStone);
			skillIron = new ItemRandomSkill(ToolMaterial.IRON, "iron_sword").setRegistryName(ModInfo.ID, "skill_sword_iron").setUnlocalizedName("dss.skill_sword.iron");
			GameRegistry.registerItem(skillIron);
			skillGold = new ItemRandomSkill(ToolMaterial.GOLD, "golden_sword").setRegistryName(ModInfo.ID, "skill_sword_gold").setUnlocalizedName("dss.skill_sword.gold");
			GameRegistry.registerItem(skillGold);
			skillDiamond = new ItemRandomSkill(ToolMaterial.EMERALD, "diamond_sword").setRegistryName(ModInfo.ID, "skill_sword_diamond").setUnlocalizedName("dss.skill_sword.diamond");
			GameRegistry.registerItem(skillDiamond);
		}
		proxy.preInit();
		EntityRegistry.registerModEntity(EntityLeapingBlow.class, "leapingblow", 0, this, 64, 10, true);
		EntityRegistry.registerModEntity(EntitySwordBeam.class, "swordbeam", 1, this, 64, 10, true);
		PacketDispatcher.initialize();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init();
		MinecraftForge.EVENT_BUS.register(new DSSCombatEvents());
		DSSCombatEvents.initializeDrops();
		if (Config.getLootWeight() > 0) {
			registerSkillOrbLoot();
		}
		if (Config.areRandomSwordsEnabled()) {
			registerRandomSwordLoot();
		}
		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
		FMLInterModComms.sendRuntimeMessage(ModInfo.ID, "VersionChecker", "addVersionCheck", ModInfo.VERSION_LIST);
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
		for (MissingMapping s : event.get()) {
			ResourceLocation location = null;
			if (s.resourceLocation.getResourcePath().matches("^dss.skillitem([0-9])+$")) {
				int i = Integer.valueOf(s.resourceLocation.getResourcePath().replace("dss.skillitem", ""));
				SkillBase skill = SkillBase.getSkill(i);
				if (skill != null) {
					location = new ResourceLocation(s.resourceLocation.getResourceDomain(), "training_stick_" + skill.getUnlocalizedName());
				}
			} else if (s.resourceLocation.getResourcePath().matches("^dss.skill(wood|stone|iron|diamond|gold)$")) {
				location = new ResourceLocation(s.resourceLocation.getResourceDomain(), s.resourceLocation.getResourcePath().replace("dss.skill", "skill_sword_").toLowerCase());
			}
			if (location != null) {
				Item item = Item.itemRegistry.getObject(location);
				if (item == null) {
					s.fail();
				} else {
					s.remap(item);
				}
			}
		};
	}

	private void registerSkillOrbLoot() {
		for (SkillBase skill : SkillBase.getSkills()) {
			if (Config.isSkillEnabled(skill)) {
				addLootToAll(new WeightedRandomChestContent(new ItemStack(skillOrb, 1, skill.getId()), 1, 1, Config.getLootWeight()), false);
			}
		}
	}

	private void registerRandomSwordLoot() {
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillWood), 1, 1, 4), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillStone), 1, 1, 3), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillGold), 1, 1, 2), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillIron), 1, 1, 2), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillDiamond), 1, 1, 1), false);
	}

	/**
	 * Adds weighted chest contents to all ChestGenHooks, with possible exception of Bonus Chest
	 */
	private void addLootToAll(WeightedRandomChestContent loot, boolean bonus) {
		ChestGenHooks.getInfo(ChestGenHooks.MINESHAFT_CORRIDOR).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_DESERT_CHEST).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_JUNGLE_CHEST).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_CORRIDOR).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_LIBRARY).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_CROSSING).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.VILLAGE_BLACKSMITH).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST).addItem(loot);
		if (bonus) {
			ChestGenHooks.getInfo(ChestGenHooks.BONUS_CHEST).addItem(loot);
		}
	}
}
