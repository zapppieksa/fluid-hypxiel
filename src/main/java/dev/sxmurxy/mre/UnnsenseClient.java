package dev.sxmurxy.mre;

import com.google.gson.Gson;
import dev.sxmurxy.mre.client.pathfinding.PathfinderAPI;
import dev.sxmurxy.mre.modules.ModuleManager;
import dev.sxmurxy.mre.modules.command.*;
import dev.sxmurxy.mre.modules.settings.SettingManager;
import dev.sxmurxy.mre.ui.ClickGUI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class UnnsenseClient implements ClientModInitializer {

    public static UnnsenseClient INSTANCE;
    public static KeyBinding clickgui2;
    public static KeyBinding drag;
    public static KeyBinding drag2;
    public static KeyBinding commandChat;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean hasConnected = false;
    // Configuration (hardcoded for simplicity)
    private static final String SERVER_URL = "wss://5b2c1e136b9a.ngrok-free.app";
    private static final String HTTP_SERVER_URL = "https://5b2c1e136b9a.ngrok-free.app";
    private static final boolean SHOW_NOTIFICATIONS = true;
    public static final Set<String> connectedPlayers = ConcurrentHashMap.newKeySet();

    private final Gson gson = new Gson();
    private boolean triedConnecting = false;
    public static final String MOD_ID = "wifmga";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private boolean webhookSent = false;
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1394633728613027962/elCyChlzBo0QKYHmF3-oF1gNLo33wWoLpaGJJw-hX_4BJuy1T0ycq2rR86G7X425v9Jb";
    private long lastPlayerCheck = 0;
    private static final long PLAYER_CHECK_INTERVAL = 10000; // 60 seconds
    private volatile int onlineCount = 0;
    public SettingManager settingManager = new SettingManager();

    public UnnsenseClient() {
        INSTANCE = this;
    }


    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);

        registerKeybindings();



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickgui2.wasPressed()) {
                client.setScreen(new ClickGUI());
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
                    // This ensures the PathExecutor is constantly updated, allowing it to
                    // execute the path with humanized movement and rotations.
                    PathfinderAPI.tick();
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (drag.wasPressed()) {

            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (commandChat.wasPressed()) {
                if (client.currentScreen == null) {
                    openCommandChat();
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) return; // Skip if GUI/chat open

            ModuleManager.getModules().forEach(module -> {
                if (module.getKey() != -1) {
                    if (InputUtil.isKeyPressed(client.getWindow().getHandle(), module.getKey())) {
                        if (!module.wasKeyPressed()) {
                            module.toggle();
                            module.setKeyPressed(true);
                        }
                    } else {
                        module.setKeyPressed(false);
                    }
                }
            });
        });

        ChatCommandListener.init();
        CommandManager.register(new BindCommand());
        CommandManager.register(new UnbindCommand());
        CommandManager.register(new ToggleCommand());
        CommandManager.register(new HelpCommand());
        CommandManager.register(new PathfindCommand());

        ModuleManager.register();
    }

    public void onClientTick(MinecraftClient minecraftClient) {


        ModuleManager.getModules().forEach(module -> {
            if (module.isToggled()) {
                module.onUpdate();
            }
        });
    }


    private void registerKeybindings() {
        clickgui2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripware.openmenu2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.dripware2"
        ));
        drag = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripware.opendrag",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.dripware"
        ));
        drag2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripware.opendrag2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.dripware"
        ));
        commandChat = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripware.commandchat",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                "category.dripware"
        ));
    }

    private void openCommandChat() {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatScreen chatScreen = new ChatScreen("");
        client.setScreen(chatScreen);

        try {
            Field chatInputField = ChatScreen.class.getDeclaredField("chatField");
            chatInputField.setAccessible(true);
            TextFieldWidget textField = (TextFieldWidget) chatInputField.get(chatScreen);
            textField.setText(".");
            textField.setCursor(1,false);
        } catch (Exception e) {
            try {
                Field chatInputField = ChatScreen.class.getDeclaredField("message");
                chatInputField.setAccessible(true);
                chatInputField.set(chatScreen, ".");
            } catch (Exception ex) {
                LOGGER.error("Failed to set text in chat: " + ex.getMessage());
            }
        }
    }





    public static UnnsenseClient getInstance() {
        if (INSTANCE == null) INSTANCE = new UnnsenseClient();
        return INSTANCE;
    }
}