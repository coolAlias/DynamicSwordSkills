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

package dynamicswordskills.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.client.RenderHelperQ;
import dynamicswordskills.client.gui.GuiContainedButton.GuiButtonContainer;
import dynamicswordskills.client.gui.GuiElement.GuiCompositeElement;
import dynamicswordskills.client.gui.GuiElementContainer.GuiTextElementContainer;
import dynamicswordskills.client.gui.IGuiPageButton.PageButtonImage;
import dynamicswordskills.client.gui.SkillSlot.TitledSkillSlot;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class GuiSkills extends GuiScreen
{
	public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ModInfo.ID + ":textures/gui/gui_skills.png");

	/** Default text color */
	public static final int TEXT_COLOR = 4210752;

	/** Default text color when hovered */
	public static final int HOVER_COLOR = 16777120;

	/** Table of contents */
	protected final Map<SkillGroup, Integer> index = Maps.<SkillGroup, Integer>newHashMap();

	/** List of pages containing skill slots */
	protected final List<Page<?>> pages = new ArrayList<Page<?>>();

	/** The X size of the GUI window in pixels. */
	protected int xSize = 281;

	/** The Y size of the GUI window in pixels. */
	protected int ySize = 180;

	/** Starting X and Y position for the Gui. Inconsistent use for Gui backgrounds. */
	protected int guiLeft;

	/** Starting Y position for the Gui. Inconsistent use for Gui backgrounds. */
	protected int guiTop;

	/** The left-hand page is controlled by pagination buttons */
	protected Page<?> pageLeft;

	/** The right-hand page is set based on interactions with the left-hand page contents */
	protected Page<?> pageRight;

	/** The table of contents page */
	protected Page<?> pageContents;

	/** Page number footer element supplied to all left-hand pages */
	protected PageNumberFooter footer;

	/** Scroll bars for the main contents of the left and right page, respectively */
	protected GuiScrollBar scrollBarLeft, scrollBarRight;

	/** Reference to current player's skills info */
	protected DSSPlayerInfo skills;

	/** Currently selected skill for displaying a description */
	protected SkillBase currentSkill = null;

	public GuiSkills() {
		super();
	}

	@Override
	public void initGui() {
		super.initGui();
		boolean unicodeFlag = this.fontRendererObj.getUnicodeFlag();
		this.skills = DSSPlayerInfo.get(this.mc.thePlayer);
		this.guiLeft = (this.width - this.xSize) / 2;
		this.guiTop = (this.height - this.ySize) / 2;
		if (this.pages.isEmpty()) {
			this.initPages();
		}
		// Buttons need absolute coordinates since they are rendered outside the context of the guiLeft/Top translation
		int btns = 0;
		boolean drawPaginationLabel = Config.showPaginationLabels();
		int paginationHeight = (drawPaginationLabel ? 22 : 13);
		this.buttonList.add((new PageButtonImage(btns++, this.guiLeft + 12, this.guiTop + 158, 16, paginationHeight, new ChatComponentTranslation("skill.dss.gui.button.label.prev").getUnformattedText(), GUI_TEXTURE, 300, 180, 284, 18, 16, 13) {
			@Override
			public int getPageIndex(int current, int numPages) {
				return (current > 0 ? current - 1 : current);
			}
			@Override
			public void func_146113_a(SoundHandler handler) {
				// no-op - sounds handled externally
			}
		}).setDrawLabelText(drawPaginationLabel));
		this.buttonList.add((new PageButtonImage(btns++, this.guiLeft + 253, this.guiTop + 158, 16, paginationHeight, new ChatComponentTranslation("skill.dss.gui.button.label.next").getUnformattedText(), GUI_TEXTURE, 300, 180, 284, 31, 16, 13) {
			@Override
			public int getPageIndex(int current, int numPages) {
				return (current + 1 < numPages ? current + 1 : current);
			}
			@Override
			public void func_146113_a(SoundHandler handler) {
				// no-op - sounds handled externally
			}
		}).setDrawLabelText(drawPaginationLabel));
		// table of contents looks better in unicode
		this.fontRendererObj.setUnicodeFlag(true);
		boolean plainText = Config.showPlainTextIndex();
		int h = (plainText ? 12 : 14);
		int w = 104;
		int ip = (plainText ? 2 : 4); // inner left and right padding between element border and inner text
		int op = 2; // padding between elements
		int max = 80 - ip; // maximum string length for the category label
		int i = 1;
		this.buttonList.add(new IndexButton(btns++, this.guiLeft + 154, this.guiTop + 55, w, h, new ChatComponentTranslation("skill.dss.gui.button.label.main").getUnformattedText(), 0).pad(ip, 0).setDrawButtonBox(!plainText));
		for (SkillGroup group : SkillGroup.getAll()) {
			Integer pageIndex = this.index.get(group);
			if (pageIndex == null) {
				continue;
			}
			int dy = 55 + (i * (h + op));
			String s = group.getDisplayName();
			boolean ellipsis = false;
			while (this.fontRendererObj.getStringWidth(s) > max) {
				s = s.substring(0, s.length() - 1);
				ellipsis = true;
			}
			if (ellipsis) {
				s += "...";
			}
			this.buttonList.add(new GroupIndexButton(group, btns++, this.guiLeft + 154, this.guiTop + dy, w, h, s, pageIndex).pad(ip, 0).setDrawButtonBox(!plainText));
			i++;
		}
		this.pageContents = this.getTableOfContents();
		if (this.currentSkill == null) {
			this.pageRight = this.pageContents;
			this.scrollBarRight = this.getScrollBar(this.pageRight.body, 259, 3);
		} else {
			this.hideIndexButtons();
		}
		if (this.pageLeft == null) {
			this.setPageLeft(0);
		}
		this.fontRendererObj.setUnicodeFlag(unicodeFlag);
	}

	protected void initPages() {
		this.footer = new PageNumberFooter(76, 161, 101, 9);
		this.pages.add(getTitlePage());
		SkillSlotContainer container = null;
		for (SkillGroup group : SkillGroup.getAll()) {
			List<SkillBase> skills = group.getSkills(new GroupFilter(this.skills));
			if (skills.isEmpty()) {
				continue;
			}
			group.sort(skills);
			int pageIndex = this.pages.size();
			int op = 3; // vertical padding between elements
			container = (SkillSlotContainer)new SkillSlotContainer(this, group, 18, 32, 110, 127).setElementPadding(0, op).pad(2, 2, 2, 4);
			for (SkillBase skill : skills) {
				if (!container.add(new TitledSkillSlot(skill, 104, 18))) {
					if (!container.isEmpty()) {
						this.pages.add(getSkillContainerPage(container)); // add current page
					}
					container = (SkillSlotContainer)new SkillSlotContainer(this, group, 18, 32, 110, 127).setElementPadding(0, op).pad(2, 2, 2, 4);
					container.add(new TitledSkillSlot(skill, 104, 18));
				}
			}
			if (!container.isEmpty()) {
				this.pages.add(getSkillContainerPage(container)); // add last page
			}
			// Add index only for non-empty group
			if (pageIndex < this.pages.size()) {
				this.index.put(group, pageIndex);
			}
		}
		this.footer.numPages = this.pages.size();
	}

	protected GuiScrollBar getScrollBar(IGuiElementScrollable parent, int x, int dy) {
		return new GuiCompositeScrollBar(parent,
				new GuiImageElement(x + 1, 40 + dy, 1, 111 - (dy * 2), GUI_TEXTURE, 300, 180, 282, 35),
				new GuiImageElement(x, 40 + dy, 3, 7, GUI_TEXTURE, 300, 180, 281, 28),
				x, 34 + dy, 3, 123 - (dy * 2),
				new GuiImageElement(x, 34 + dy, 3, 5, GUI_TEXTURE, 300, 180, 281, 18),
				new GuiImageElement(x, 152 - dy, 3, 5, GUI_TEXTURE, 300, 180, 281, 23)
				);
	}

	protected Page<?> getTitlePage() {
		// Left-hand page header area: 42, 25, 79, 9
		// Left-hand page content area: 20, 34, 106, 123
		String headerText = new ChatComponentTranslation("skill.dss.gui.main.title").getUnformattedText().toUpperCase();
		GuiTextElement header = new GuiTextElement(42, 26, 80, 9, new ChatComponentTranslation(headerText), TEXT_COLOR, false);
		GuiElementContainer<GuiTextElement> body = new GuiTextElementContainer(18, 32, 110, 127).setElementPadding(0, mc.fontRenderer.FONT_HEIGHT);
		body.pad(7, 0, 7, 5);
		body.add(((GuiTextElement) new GuiTextElement(body, new ChatComponentTranslation("skill.dss.gui.main.body"), TEXT_COLOR, true).pad(0)));
		return new Page<GuiTextElement>(0, 10, 7, 121, 164, body, header, this.footer);
	}

	protected Page<?> getTableOfContents() {
		// Right-hand page header area: 159, 37, 101, 13
		// Right-hand page content area: 159, 53, 101, 101
		String headerText = new ChatComponentTranslation("skill.dss.gui.heading.index").getUnformattedText().toUpperCase();
		GuiTextElement header = new GuiTextElement(153, 24, 104, 13, GuiTextElement.getBoldComponent(new ChatComponentText(headerText), null), TEXT_COLOR, true);
		int bh = (Config.showPlainTextIndex() ? 12 : 14);
		GuiElementContainer<GuiContainedButton> body = new GuiButtonContainer(153, 37, 110, 117, bh, this.guiLeft, this.guiTop).setElementPadding(0, 2);
		for (GuiButton button : (List<GuiButton>)this.buttonList) {
			if (button instanceof GuiContainedButton) {
				body.add((GuiContainedButton) button);
			}
		}
		return new Page<GuiContainedButton>(0, 148, 7, 121, 164, body, header);
	}

	protected Page<SkillSlot> getSkillContainerPage(SkillSlotContainer body) {
		String headerText = body.group.getDisplayName().toUpperCase();
		GuiTextElement header = new GuiTextElement(42, 24, 79, 9, GuiTextElement.getBoldComponent(new ChatComponentText(headerText), null), TEXT_COLOR, true);
		return new Page<SkillSlot>(this.pages.size(), 10, 7, 121, 164, body, header, this.footer);
	}

	protected Page<?> getSkillDescriptionPage(SkillBase skill) {
		int pad = 5; // padding between sections
		String lvl = String.valueOf(this.currentSkill.getLevel());
		GuiElement header = new GuiCompositeElement(153, 24, 106, 9, 
				new GuiTextElement(153, 24, 96, 9, GuiTextElement.getBoldComponent(new ChatComponentTranslation(skill.getDisplayName().toUpperCase()), EnumChatFormatting.DARK_GRAY), TEXT_COLOR, true),
				new GuiTextElement(250, 24, 11, 9, GuiTextElement.getBoldComponent(new ChatComponentText(lvl), EnumChatFormatting.DARK_GRAY), TEXT_COLOR, true).setCentered(true)
				);
		GuiElementContainer<GuiTextElement> body = new GuiTextElementContainer(153, 35, 110, 121);
		body.pad(2, 5, 2, 3);
		if (Config.isSkillDisabled(this.mc.thePlayer, currentSkill)) {
			String tk = "skill.dss.disabled." + (Config.isSkillAllowed(currentSkill) ? "client" : "server");
			body.add((GuiTextElement)(new GuiTextElement(body, GuiTextElement.getBoldComponent(new ChatComponentTranslation(tk), EnumChatFormatting.DARK_RED), TEXT_COLOR, true).pad(0, 0, pad, 0)));
		}
		// Skill summary displays level, max level, and additional details
		body.add(new GuiTextElement(body, GuiTextElement.getBoldComponent(new ChatComponentTranslation("skill.dss.gui.heading.summary"), null), TEXT_COLOR, true));
		List<String> details = Lists.<String>newArrayList();
		details.add(this.currentSkill.getLevelDisplay(false));
		this.currentSkill.addInformation(details, this.mc.thePlayer);
		for (String s : details) {
			body.add(new GuiTextElement(body, new ChatComponentText(s), TEXT_COLOR, true));
		}
		// Activation requirements, if any
		String activation = this.currentSkill.getActivationDisplay();
		if (activation != null) {
			body.add((GuiTextElement)(new GuiTextElement(body, GuiTextElement.getBoldComponent(new ChatComponentTranslation("skill.dss.gui.heading.activation"), null), TEXT_COLOR, true).pad(pad, 0, 0, 0)));
			body.add(new GuiTextElement(body, activation, TEXT_COLOR, true));
		}
		// Full description
		body.add((GuiTextElement)(new GuiTextElement(body, GuiTextElement.getBoldComponent(new ChatComponentTranslation("skill.dss.gui.heading.description"), null), TEXT_COLOR, true).pad(pad, 0, 0, 0)));
		body.add(new GuiTextElement(body, new ChatComponentTranslation(this.currentSkill.getTranslationKey() + ".description"), TEXT_COLOR, true));
		return new Page<GuiTextElement>(0, 148, 7, 121, 164, body, header);
	}

	protected void resetPageRight() {
		if (this.pageRight != null) {
			return;
		}
		if (this.currentSkill != null) {
			this.pageRight = this.getSkillDescriptionPage(this.currentSkill);
			this.hideIndexButtons();
		} else {
			this.pageRight = this.pageContents;
		}
		this.scrollBarRight = this.getScrollBar(this.pageRight.body, 259, 3);
	}

	protected void setPageLeft(int index) {
		index = MathHelper.clamp_int(index, 0, this.pages.size() - 1);
		if (this.pageLeft == null || index != this.pageLeft.index) {
			if (this.pageLeft != null) {
				if (Config.clickedPageSound()) {
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				}
				if (this.pageLeft.body instanceof SkillSlotContainer) {
					((SkillSlotContainer) this.pageLeft.body).clearFilters();
				}
			}
			this.pageLeft = this.pages.get(index);
			this.scrollBarLeft = getScrollBar(this.pageLeft.body, 15, 0);
			if (index == 0) {
				this.currentSkill = null;
				this.pageRight = null;
				this.resetPageRight();
			}
			// Reset hover state for table of contents buttons when page changed
			for (GuiButton button : (List<GuiButton>)this.buttonList) {
				if (button instanceof GroupIndexButton && this.pageLeft != null && this.pageLeft.body instanceof SkillSlotContainer) {
					boolean flag = (((GroupIndexButton) button).group == ((SkillSlotContainer) this.pageLeft.body).group);
					((GroupIndexButton) button).forceHover = flag;
				} else if (button instanceof JumpToPageButton) {
					((JumpToPageButton) button).forceHover = ((JumpToPageButton) button).pageIndex == index;
				}
			}
		}
	}

	protected void hideIndexButtons() {
		for (GuiButton button : (List<GuiButton>)this.buttonList) {
			if (button instanceof JumpToPageButton) {
				button.visible = false;
			}
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button instanceof GroupIndexButton && this.pageLeft.body instanceof SkillSlotContainer && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			boolean flag = ((SkillSlotContainer) this.pageLeft.body).filter(((GroupIndexButton) button).group);
			((GroupIndexButton) button).forceHover = flag;
			if (flag) {
				this.scrollBarLeft.resetScroll();
			}
			if (Config.clickedGroupFilterSound()) {
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			}
			return;
		}
		if (button instanceof IGuiPageButton) {
			int index = ((IGuiPageButton) button).getPageIndex(this.pageLeft.index, this.pages.size());
			this.setPageLeft(index);
		}
	}

	@Override
	protected void keyTyped(char c, int key) {
		if (key == 1 || key == this.mc.gameSettings.keyBindInventory.getKeyCode() || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_SKILLS_GUI].getKeyCode()) {
			this.mc.thePlayer.closeScreen();
		}
		int index = this.pageLeft.index;
		if (key == Keyboard.KEY_NEXT) {
			++index;
		} else if (key == Keyboard.KEY_PRIOR) {
			--index;
		}
		this.setPageLeft(index);
	}

	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		this.scrollBarLeft.handleMouseInput();
		if (this.pageRight != null) {
			this.scrollBarRight.handleMouseInput();
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		SkillSlot slot = this.getSlotAtPosition(mouseX - this.guiLeft, mouseY - this.guiTop);
		if (slot != null && slot.skill != null) {
			switch (mouseButton) {
			case 0: // left-click
				this.pageRight = null;
				if (!slot.skill.is(this.currentSkill)) {
					this.currentSkill = this.skills.getPlayerSkill(slot.skill);
				} else {
					this.currentSkill = null;
				}
				if (Config.clickedSkillSound()) {
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				}
				break;
			case 1: // right-click
				if (Config.isSkillAllowed(slot.skill) && this.skills.getSkillLevel(slot.skill) > 0) {
					this.skills.toggleDisabledSkill(slot.skill);
					if (Config.clickedSkillSound()) {
						this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
					}
					// Refresh description
					if (slot.skill.is(this.currentSkill)) {
						this.pageRight = this.getSkillDescriptionPage(this.currentSkill);
					}
				}
				break;
			}
		}
	}

	@Override
	public void onGuiClosed() {
		this.skills.syncDisabledSkills();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		((GuiButton)this.buttonList.get(0)).visible = this.pageLeft.index > 0;
		((GuiButton)this.buttonList.get(1)).visible = this.pages.size() > 0 && this.pageLeft.index + 1 < this.pages.size();
		this.footer.pageIndex = this.pageLeft.index + 1;
		this.drawDefaultBackground();
		RenderHelperQ.drawTexturedRect(GUI_TEXTURE, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize, 300, 180);
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		super.drawScreen(mouseX, mouseY, partialTicks);
		GL11.glPushMatrix();
		GL11.glTranslatef((float)this.guiLeft, (float)this.guiTop, 0.0F);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		this.resetPageRight();
		this.drawForegroundLayer(mouseX - this.guiLeft, mouseY - this.guiTop);
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	protected void drawForegroundLayer(int mouseX, int mouseY) {
		// Draw the right-hand page first so any text overlays from the left-hand page render on top
		if (this.pageRight != null) {
			if (this.pageRight.body.isScrollControlRequired(this.scrollBarRight.isHorizontal)) {
				this.scrollBarRight.drawElement(this.mc, mouseX, mouseY);
				this.pageRight.body.scrollElementTo(this.scrollBarRight.getScrollAmount(), this.scrollBarRight.isHorizontal);
			}
			this.pageRight.drawElement(this.mc, mouseX, mouseY);
		}
		if (this.pageLeft.body.isScrollControlRequired(this.scrollBarLeft.isHorizontal)) {
			this.scrollBarLeft.drawElement(this.mc, mouseX, mouseY);
			this.pageLeft.body.scrollElementTo(this.scrollBarLeft.getScrollAmount(), this.scrollBarLeft.isHorizontal);
		}
		this.pageLeft.drawElement(this.mc, mouseX, mouseY);
		if (Config.showSkillGroupTooltips()) {
			this.displaySkillGroupTooltip(mouseX, mouseY);
		}
	}

	protected void displaySkillGroupTooltip(int mouseX, int mouseY) {
		for (GuiButton button : (List<GuiButton>)this.buttonList) {
			if (!button.visible || !button.func_146115_a()) {
				continue;
			} else if (button instanceof GroupIndexButton) {
				List<String> tooltip = ((GroupIndexButton) button).group.getTooltip();
				if (tooltip != null && !tooltip.isEmpty()) {
					this.drawHoveringText(tooltip, mouseX, mouseY, this.mc.fontRenderer);
				}
			}
			break;
		}
	}

	protected SkillSlot getSlotAtPosition(int x, int y) {
		if (!(this.pageLeft.body instanceof SkillSlotContainer)) {
			return null;
		}
		for (SkillSlot slot : ((SkillSlotContainer) this.pageLeft.body).getElements()) {
			// Container element applies its left padding only temporarily, so account for that here 
			if (slot.isMouseOverElement(x - this.pageLeft.body.padding.left, y)) {
				// Fix slots beyond body border sometimes clickable prior to first scroll event
				int maxY = this.pageLeft.body.yPos + this.pageLeft.body.height - this.pageLeft.body.getPadding().height();
				return (slot.getElementPosY() + slot.getPadding().height() < maxY ? slot : null);
			}
		}
		return null;
	}

	protected static class GroupFilter implements Predicate<SkillBase>
	{
		private final DSSPlayerInfo skills;
		public GroupFilter(DSSPlayerInfo skills) {
			this.skills = skills;
		}
		@Override
		public boolean test(SkillBase t) {
			if (!Config.showBannedSkills() && !Config.isSkillAllowed(t)) {
				return false;
			}
			return Config.showUnknownSkills() || this.skills.getSkillLevel(t) > 0;
		}
	};

	public static class PageNumberFooter extends GuiElement
	{
		public int numPages;
		public int pageIndex;
		public PageNumberFooter(int xPos, int yPos, int width, int height) {
			super(xPos, yPos, width, height);
		}
		@Override
		public void drawElement(Minecraft mc, int mouseX, int mouseY) {
			if (this.numPages > 1) {
				String s = String.valueOf(this.pageIndex);
				mc.fontRenderer.drawString(s, this.xPos - (mc.fontRenderer.getStringWidth(s) / 2), this.yPos, 4210752);
			}
		}
	}

	public static class IndexButton extends JumpToPageButton
	{
		public IndexButton(int id, int x, int y, int width, int height, String label, int pageIndex) {
			super(id, x, y, width, height, label, pageIndex);
		}
		@Override
		public void func_146113_a(SoundHandler handler) {
			// no-op - sounds handled externally
		}
	}

	public static class GroupIndexButton extends IndexButton
	{
		public final SkillGroup group;
		public GroupIndexButton(SkillGroup group, int id, int x, int y, int width, int height, String label, int pageIndex) {
			super(id, x, y, width, height, label, pageIndex);
			this.group = group;
		}
	}

	public static class SkillSlotContainer extends GuiElementContainer<SkillSlot>
	{
		public final GuiSkills skillScreen;

		/** The currently hovered slot, if any */
		protected SkillSlot hovered;

		@Nullable
		public final SkillGroup group;

		protected final Set<SkillGroup> filters = Sets.<SkillGroup>newHashSet();

		public SkillSlotContainer(GuiSkills screen, @Nullable SkillGroup group, int xPos, int yPos, int width, int height) {
			super(xPos, yPos, width, height);
			this.skillScreen = screen;
			this.group = group;
			this.lineHeight = 18;
		}

		/**
		 * Applies or removes the group filter, updating the displayed skills
		 * @return true if the filter was applied, false if removed
		 */
		public boolean filter(SkillGroup filter) {
			if (filter == this.group) {
				return true;
			}
			boolean removed = this.filters.contains(filter);
			if (removed) {
				this.filters.remove(filter);
			} else {
				this.filters.add(filter);
			}
			for (SkillSlot s : this.elements) { 
				if (s.skill != null) {
					boolean flag = false;
					for (SkillGroup g : this.filters) {
						if (!Config.isSkillInGroup(s.skill, g)) {
							flag = true;
							break;
						}
					}
					s.setDisabled(flag);
				}
			};
			this.markDirty();
			return !removed;
		}

		/**
		 * Clears all filters applied to this group
		 */
		public void clearFilters() {
			this.filters.clear();
			for (SkillSlot s : this.elements) { 
				s.setDisabled(false);
			}
			this.markDirty();
		}

		@Override
		public boolean canAdd(@Nullable SkillSlot slot) {
			return slot.skill == null || Config.isSkillInGroup(slot.skill, this.group);
		}

		@Override
		protected void onElementAdded(@Nullable SkillSlot element) {
			if (element != null) {
				int i = this.elements.size() - 1;
				element.setElementPosition(this.xPos, this.yPos + (i * (element.height + this.elementPadY)));
			}
		}

		@Override
		public SkillSlotContainer setElementPadding(int x, int y) {
			super.setElementPadding(x, y);
			this.lineHeight = 18 + y;
			return this;
		}

		@Override
		public void drawElement(Minecraft mc, int mouseX, int mouseY) {
			this.hovered = null;
			super.drawElement(mc, mouseX, mouseY);
			if (this.hovered != null && this.hovered.skill != null) {
				SkillBase instance = this.skillScreen.skills.getPlayerSkill(this.hovered.skill);
				this.renderToolTip((instance == null ? this.hovered.skill : instance), mc, mouseX, mouseY);
			}
		}

		@Override
		protected void drawElement(SkillSlot slot, Minecraft mc, int mouseX, int mouseY) {
			slot.isSkillKnown = this.skillScreen.skills.getSkillLevel(slot.skill) > 0;
			slot.selected = (slot.skill != null && slot.skill.is(this.skillScreen.currentSkill));
			super.drawElement(slot, mc, mouseX, mouseY);
			if (slot.isMouseOverElement(mouseX, mouseY)) {
				this.hovered = slot;
				slot.drawHoveredGradient(-2130706433, -2130706433, new Padding(-1, -2, -1, -1));
			}
			if (slot.selected) {
				int a = 204;
				int r = 204;
				int g = 102;
				int b = 0;
				int c = (a << 24) + (r << 16) + (g << 8) + b;
				slot.drawBorderBox(2, c);
			}
		}

		protected void renderToolTip(SkillBase skill, Minecraft mc, int mouseX, int mouseY) {
			boolean unicodeFlag = mc.fontRenderer.getUnicodeFlag();
			mc.fontRenderer.setUnicodeFlag(false);
			List<String> tooltip = Lists.<String>newArrayList();
			if (skill.getLevel() < 1) {
				String displayName = (skill.showNameIfUnknown(mc.thePlayer) ? skill.getDisplayName() : new ChatComponentTranslation("skill.dss.unknown.name").getUnformattedText());
				tooltip.add(displayName);
				if (Config.isSkillAllowed(skill)) {
					tooltip.add(EnumChatFormatting.GRAY + (EnumChatFormatting.ITALIC + new ChatComponentTranslation("skill.dss.unknown.tooltip").getUnformattedText()));
				} else {
					tooltip.add(EnumChatFormatting.DARK_RED + new ChatComponentTranslation("skill.dss.disabled.server").getUnformattedText());
				}
			} else if (!Config.isSkillAllowed(skill)) {
				tooltip.add(skill.getDisplayName());
				tooltip.add(EnumChatFormatting.DARK_RED + new ChatComponentTranslation("skill.dss.disabled.server").getUnformattedText());
			} else {
				tooltip.add(skill.getDisplayName());
				if (this.skillScreen.skills.isSkillDisabled(skill)) {
					tooltip.add(EnumChatFormatting.DARK_RED + new ChatComponentTranslation("skill.dss.disabled.client").getUnformattedText());
				}
				tooltip.add(EnumChatFormatting.GOLD + skill.getLevelDisplay(false));
				for (String s : skill.getTooltip(this.skillScreen.mc.thePlayer, false)) {
					tooltip.add(EnumChatFormatting.GRAY + s);
				}
			}
			this.skillScreen.drawHoveringText(tooltip, mouseX, mouseY, mc.fontRenderer);
			mc.fontRenderer.setUnicodeFlag(unicodeFlag);
		}
	}
}
