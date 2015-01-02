package dynamicswordskills;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

import org.apache.logging.log4j.Logger;

import dynamicswordskills.api.ItemRandomSkill;
import dynamicswordskills.api.ItemSkillProvider;
import dynamicswordskills.command.DSSCommands;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.entity.EntitySwordBeam;
import dynamicswordskills.item.CombatSkillsTab;
import dynamicswordskills.item.ItemSkillOrb;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;

@Mod(modid = ModInfo.ID, version = ModInfo.VERSION)
public class DynamicSwordSkills
{
	@Mod.Instance(ModInfo.ID)
	public static DynamicSwordSkills instance;

	@SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.COMMON_PROXY)
	public static CommonProxy proxy;

	public static Logger logger;

	/** Whether Battlegear2 mod is loaded */
	public static boolean isBG2Enabled;

	public static CreativeTabs tabSkills;

	public static Item skillOrb;

	public static List<Item> skillItems;

	/** Various randomized skill swords */
	public static Item
	skillWood,
	skillStone,
	skillGold,
	skillIron,
	skillDiamond;

	private boolean shouldLoad;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		shouldLoad = !Loader.isModLoaded("zeldaswordskills");
		isBG2Enabled = false; // TODO when BG2 updates: Loader.isModLoaded("battlegear2");
		if (shouldLoad) {
			Config.init(event);
			tabSkills = new CombatSkillsTab("dssTab");
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
					item = new ItemSkillProvider(ToolMaterial.IRON, skill, (byte) level).setUnlocalizedName("dss.skillitem" + skill.getId()).setCreativeTab(DynamicSwordSkills.tabSkills);
					skillItems.add(item);
					GameRegistry.registerItem(item, item.getUnlocalizedName().substring(5));
				}
			}
			if (Config.areRandomSwordsEnabled()) {
				skillWood = new ItemRandomSkill(ToolMaterial.WOOD, "wooden_sword").setUnlocalizedName("dss.skillwood");
				GameRegistry.registerItem(skillWood, skillWood.getUnlocalizedName().substring(5));
				skillStone = new ItemRandomSkill(ToolMaterial.STONE, "stone_sword").setUnlocalizedName("dss.skillstone");
				GameRegistry.registerItem(skillStone, skillStone.getUnlocalizedName().substring(5));
				skillGold = new ItemRandomSkill(ToolMaterial.GOLD, "golden_sword").setUnlocalizedName("dss.skillgold");
				GameRegistry.registerItem(skillGold, skillGold.getUnlocalizedName().substring(5));
				skillIron = new ItemRandomSkill(ToolMaterial.IRON, "iron_sword").setUnlocalizedName("dss.skilliron");
				GameRegistry.registerItem(skillIron, skillIron.getUnlocalizedName().substring(5));
				skillDiamond = new ItemRandomSkill(ToolMaterial.EMERALD, "diamond_sword").setUnlocalizedName("dss.skilldiamond");
				GameRegistry.registerItem(skillDiamond, skillDiamond.getUnlocalizedName().substring(5));
			}
			EntityRegistry.registerModEntity(EntityLeapingBlow.class, "leapingblow", 0, this, 64, 10, true);
			EntityRegistry.registerModEntity(EntitySwordBeam.class, "swordbeam", 1, this, 64, 10, true);
			PacketDispatcher.initialize();
		}
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (shouldLoad) {
			proxy.registerRenderers();
			DSSCombatEvents events = new DSSCombatEvents();
			MinecraftForge.EVENT_BUS.register(events);
			FMLCommonHandler.instance().bus().register(events); // for PlayerLoggedInEvent
			DSSCombatEvents.initializeDrops();
			if (Config.getLootWeight() > 0) {
				registerSkillOrbLoot();
			}
			if (Config.areRandomSwordsEnabled()) {
				registerRandomSwordLoot();
			}
			NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
		}
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		if (shouldLoad) {
			DSSCommands.registerCommands(event);
		}
	}

	private void registerSkillOrbLoot() {
		for (SkillBase skill : SkillBase.getSkills()) {
			if (Config.isSkillEnabled(skill.getId())) {
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
