package dev.sxmurxy.mre;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
//import ru.vidtu.ias.screen.AccountScreen;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;


public class YourCustomMainMenuScreen extends Screen {
    private long vg = 0L;
    private int fontHandle = -1;
    private int logoImage = -1;
    private record CustomButton(String label, int x, int y, int width, int height, Runnable action) {
        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private final List<CustomButton> buttons = new ArrayList<>();

    protected YourCustomMainMenuScreen() {
        super(Text.of("Custom Main Menu"));
    }



    @Override
    protected void init() {
        super.init();



        buttons.clear();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;

        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 4 + 48;

        buttons.add(new CustomButton("Singleplayer", centerX, startY + 0 * (buttonHeight + spacing), buttonWidth, buttonHeight,
                () -> client.setScreen(new SelectWorldScreen(this))));

        buttons.add(new CustomButton("Multiplayer", centerX, startY + 1 * (buttonHeight + spacing), buttonWidth, buttonHeight,
                () -> client.setScreen(new MultiplayerScreen(this))));

        buttons.add(new CustomButton("Options", centerX, startY + 2 * (buttonHeight + spacing), buttonWidth, buttonHeight,
                () -> client.setScreen(new OptionsScreen(this, client.options))));

     //   buttons.add(new CustomButton("Alt Manager", centerX, startY + 3 * (buttonHeight + spacing), buttonWidth, buttonHeight,
     //           () -> client.setScreen(new AccountScreen(this))));





    }
    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {

        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        float scale = (float) client.getWindow().getScaleFactor();

        AbstractTexture abstractTexture1 = MinecraftClient.getInstance().getTextureManager()
                .getTexture(Identifier.of("mre", "textures/icons8-chart-32.png"));
        BuiltTexture texture1 = Builder.texture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState( 0f))
                .texture(0.0f, 0.0f, 1f, 1f, abstractTexture1)
                .color(new QuadColorState( Color.WHITE))
                .build();
        texture1.render(matrix, 0,0);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (CustomButton b : buttons) {
                if (b.isHovered((int) mouseX, (int) mouseY)) {
                    b.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }



    @Override
    public void removed() {

        super.removed();
    }
}
