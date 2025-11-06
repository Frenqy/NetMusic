package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@IPNIgnore
public class ComputerMenuScreen extends ContainerScreen<ComputerMenu> {
    private static final ResourceLocation BG = new ResourceLocation(NetMusic.MOD_ID, "textures/gui/computer.png");
    private static final Pattern URL_HTTP_REG = Pattern.compile("(http|ftp|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?");
    private static final Pattern URL_FILE_REG = Pattern.compile("^[a-zA-Z]:\\\\(?:[^\\\\/:*?\"<>|\\r\\n]+\\\\)*[^\\\\/:*?\"<>|\\r\\n]*$");
    private static final Pattern TIME_REG = Pattern.compile("^\\d+$");
    private TextFieldWidget urlTextField;
    private TextFieldWidget nameTextField;
    private TextFieldWidget timeTextField;
    private CheckboxButton readOnlyButton;
    private ITextComponent tips = StringTextComponent.EMPTY;

    public ComputerMenuScreen(ComputerMenu screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageHeight = 216;
    }

    @Override
    protected void init() {
        super.init();
        this.initUrlTextFieldWidget();
        this.initNameTextFieldWidget();
        this.initTimeTextFieldWidget();
        this.readOnlyButton = new CheckboxButton(leftPos + 58, topPos + 55, 80, 20,
                new TranslationTextComponent("gui.netmusic.cd_burner.read_only"), false);
        this.addButton(this.readOnlyButton);
        this.addButton(new Button(leftPos + 7, topPos + 77, 135, 20,
                new TranslationTextComponent("gui.netmusic.cd_burner.craft"), (b) -> handleCraftButton()));
    }

    private void initUrlTextFieldWidget() {
        String perText = "";
        boolean focus = false;
        if (urlTextField != null) {
            perText = urlTextField.getValue();
            focus = urlTextField.isFocused();
        }
        urlTextField = new TextFieldWidget(getMinecraft().font, leftPos + 10, topPos + 18, 120, 16, new StringTextComponent("Music URL Box"));
        urlTextField.setValue(perText);
        urlTextField.setBordered(false);
        urlTextField.setMaxLength(32500);
        urlTextField.setTextColor(0xF3EFE0);
        urlTextField.setFocus(focus);
        urlTextField.moveCursorToEnd();
        this.addWidget(this.urlTextField);
    }

    private void initNameTextFieldWidget() {
        String perText = "";
        boolean focus = false;
        if (nameTextField != null) {
            perText = nameTextField.getValue();
            focus = nameTextField.isFocused();
        }
        nameTextField = new TextFieldWidget(getMinecraft().font, leftPos + 10, topPos + 39, 120, 16, new StringTextComponent("Music Name Box"));
        nameTextField.setValue(perText);
        nameTextField.setBordered(false);
        nameTextField.setMaxLength(256);
        nameTextField.setTextColor(0xF3EFE0);
        nameTextField.setFocus(focus);
        nameTextField.moveCursorToEnd();
        this.addWidget(this.nameTextField);
    }

    private void initTimeTextFieldWidget() {
        String perText = "";
        boolean focus = false;
        if (timeTextField != null) {
            perText = timeTextField.getValue();
            focus = timeTextField.isFocused();
        }
        timeTextField = new TextFieldWidget(getMinecraft().font, leftPos + 10, topPos + 61, 40, 16, new StringTextComponent("Music Time Box"));
        timeTextField.setValue(perText);
        timeTextField.setBordered(false);
        timeTextField.setMaxLength(5);
        timeTextField.setTextColor(0xF3EFE0);
        timeTextField.setFocus(focus);
        timeTextField.moveCursorToEnd();
        this.addWidget(this.timeTextField);
    }

    private void handleCraftButton() {
        ItemStack cd = this.getMenu().getInput().getStackInSlot(0);
        if (cd.isEmpty()) {
            this.tips = new TranslationTextComponent("gui.netmusic.cd_burner.cd_is_empty");
            return;
        }
        ItemMusicCD.SongInfo songInfo = ItemMusicCD.getSongInfo(cd);
        if (songInfo != null && songInfo.readOnly) {
            this.tips = new TranslationTextComponent("gui.netmusic.cd_burner.cd_read_only");
            return;
        }
        String urlText = urlTextField.getValue();
        if (StringUtils.isBlank(urlText)) {
            this.tips = new TranslationTextComponent("gui.netmusic.computer.url.empty");
            return;
        }
        String nameText = nameTextField.getValue();
        if (StringUtils.isBlank(nameText)) {
            this.tips = new TranslationTextComponent("gui.netmusic.computer.name.empty");
            return;
        }
        String timeText = timeTextField.getValue();
        if (StringUtils.isBlank(timeText)) {
            this.tips = new TranslationTextComponent("gui.netmusic.computer.time.empty");
            return;
        }
        if (!TIME_REG.matcher(timeText).matches()) {
            this.tips = new TranslationTextComponent("gui.netmusic.computer.time.not_number");
            return;
        }
        int time = Integer.parseInt(timeText);
        if (URL_HTTP_REG.matcher(urlText).matches()) {
            ItemMusicCD.SongInfo song = new ItemMusicCD.SongInfo(urlText, nameText, time, this.readOnlyButton.selected(), 0);
            NetworkHandler.CHANNEL.sendToServer(new SetMusicIDMessage(song));
            return;
        }
        if (URL_FILE_REG.matcher(urlText).matches()) {
            File file = Paths.get(urlText).toFile();
            if (!file.isFile()) {
                this.tips = new TranslationTextComponent("gui.netmusic.computer.url.local_file_error");
                return;
            }
            try {
                URL url = file.toURI().toURL();
                ItemMusicCD.SongInfo song = new ItemMusicCD.SongInfo(url.toString(), nameText, time, this.readOnlyButton.selected(), 0);
                NetworkHandler.CHANNEL.sendToServer(new SetMusicIDMessage(song));
                return;
            } catch (MalformedURLException e) {
                e.fillInStackTrace();
            }
        }
        this.tips = new TranslationTextComponent("gui.netmusic.computer.url.error");
    }

    @Override
    protected void renderLabels(MatrixStack poseStack, int x, int y) {
    }

    @Override
    protected void renderBg(MatrixStack poseStack, float partialTicks, int x, int y) {
        renderBackground(poseStack);
        int posX = this.leftPos;
        int posY = (this.height - this.imageHeight) / 2;
        getMinecraft().getTextureManager().bind(BG);
        blit(poseStack, posX, posY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(MatrixStack poseStack, int x, int y, float partialTicks) {
        super.render(poseStack, x, y, partialTicks);
        urlTextField.render(poseStack, x, y, partialTicks);
        nameTextField.render(poseStack, x, y, partialTicks);
        timeTextField.render(poseStack, x, y, partialTicks);
        if (StringUtils.isBlank(urlTextField.getValue()) && !urlTextField.isFocused()) {
            font.draw(poseStack, new TranslationTextComponent("gui.netmusic.computer.url.tips").withStyle(TextFormatting.ITALIC), this.leftPos + 12, this.topPos + 18, TextFormatting.GRAY.getColor());
        }
        if (StringUtils.isBlank(nameTextField.getValue()) && !nameTextField.isFocused()) {
            font.draw(poseStack, new TranslationTextComponent("gui.netmusic.computer.name.tips").withStyle(TextFormatting.ITALIC), this.leftPos + 12, this.topPos + 39, TextFormatting.GRAY.getColor());
        }
        if (StringUtils.isBlank(timeTextField.getValue()) && !timeTextField.isFocused()) {
            font.draw(poseStack, new TranslationTextComponent("gui.netmusic.computer.time.tips").withStyle(TextFormatting.ITALIC), this.leftPos + 11, this.topPos + 61, TextFormatting.GRAY.getColor());
        }
        font.drawWordWrap(tips, this.leftPos + 8, this.topPos + 100, 162, 0xCF0000);
        renderTooltip(poseStack, x, y);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String urlValue = this.urlTextField.getValue();
        String nameValue = this.nameTextField.getValue();
        String timeValue = this.timeTextField.getValue();
        super.resize(minecraft, width, height);
        this.urlTextField.setValue(urlValue);
        this.nameTextField.setValue(nameValue);
        this.timeTextField.setValue(timeValue);
    }

    @Override
    public void tick() {
        super.tick();
        this.urlTextField.tick();
        this.nameTextField.tick();
        this.timeTextField.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.urlTextField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.urlTextField);
            return true;
        }
        if (this.nameTextField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.nameTextField);
            return true;
        }
        if (this.timeTextField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.timeTextField);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputMappings.Input mouseKey = InputMappings.getKey(keyCode, scanCode);
        // 防止 E 键关闭界面
        if (this.getMinecraft().options.keyInventory.isActiveAndMatches(mouseKey)) {
            if (urlTextField.isFocused() || nameTextField.isFocused() || timeTextField.isFocused()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.urlTextField.setValue(text);
            this.nameTextField.setValue(text);
            this.timeTextField.setValue(text);
        } else {
            this.urlTextField.insertText(text);
            this.nameTextField.insertText(text);
            this.timeTextField.insertText(text);
        }
    }
}
