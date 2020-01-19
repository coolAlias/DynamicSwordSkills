package dynamicswordskills.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.GuiConfigEntries.CategoryEntry;
import cpw.mods.fml.client.config.IConfigElement;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;

public class GuiFactoryConfig implements cpw.mods.fml.client.IModGuiFactory
{
	public GuiFactoryConfig() {
	}

	@Override
	public void initialize(Minecraft mc) {
	}

	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() {
		return GuiConfigDss.class;
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	@Override
	public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
		return null;
	}

	public static class GuiConfigDss extends GuiConfig
	{
		public GuiConfigDss(GuiScreen parent) {
			super(parent, getConfigElements(), ModInfo.ID, false, false, I18n.format("dss.config.title"));
		}

		private static List<IConfigElement> getConfigElements() {
			List<IConfigElement> list = new ArrayList<IConfigElement>();
			list.add(new DummyCategoryElement("dssClientConfig", "dss.config.client.name", ClientEntry.class));
			return list;
		}

		@Override
		public void onGuiClosed() {
			Config.refreshClient();
		}

		public static class ClientEntry extends CategoryEntry
		{
			public ClientEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement prop) {
				super(owningScreen, owningEntryList, prop);
			}

			@Override
			protected GuiScreen buildChildScreen() {
				List<IConfigElement> list = (new ConfigElement(Config.config.getCategory("client"))).getChildElements();
				List<IConfigElement> skill_gui = (new ConfigElement(Config.config.getCategory("skillgui"))).getChildElements();
				List<IConfigElement> combo_hud = (new ConfigElement(Config.config.getCategory("combohud"))).getChildElements();
				List<IConfigElement> ending_blow_hud = (new ConfigElement(Config.config.getCategory("endingblowhud"))).getChildElements();
				list.add(new DummyCategoryElement("dssSkillGuiConfig", "dss.config.client.skillgui.name", skill_gui));
				list.add(new DummyCategoryElement("dssComboHudConfig", "dss.config.client.combohud.name", combo_hud));
				list.add(new DummyCategoryElement("dssEndingBlowHudConfig", "dss.config.client.endingblowhud.name", ending_blow_hud));
				return new GuiConfig(this.owningScreen,
						list,
						this.owningScreen.modID,
						"client",
						this.configElement.requiresWorldRestart() || this.owningScreen.allRequireWorldRestart,
						this.configElement.requiresMcRestart() || this.owningScreen.allRequireMcRestart,
						GuiConfig.getAbridgedConfigPath(Config.config.toString()));
			}
		}
	}
}
