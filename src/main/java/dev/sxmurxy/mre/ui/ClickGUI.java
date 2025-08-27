package dev.sxmurxy.mre.ui;

import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.ModuleManager;
import dev.sxmurxy.mre.modules.settings.ISetting;
import dev.sxmurxy.mre.modules.settings.impl.BoolSetting;
import dev.sxmurxy.mre.modules.settings.impl.EnumSetting;
import dev.sxmurxy.mre.modules.settings.impl.NumberSetting;
import dev.sxmurxy.mre.modules.settings.impl.TextSetting;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.ModuleManager;
import dev.sxmurxy.mre.modules.settings.ISetting;
import dev.sxmurxy.mre.modules.settings.impl.BoolSetting;
import dev.sxmurxy.mre.modules.settings.impl.EnumSetting;
import dev.sxmurxy.mre.modules.settings.impl.NumberSetting;
import dev.sxmurxy.mre.modules.settings.impl.TextSetting;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;

public class ClickGUI extends Screen {
    private static final Logger LOGGER = LogManager.getLogger(ClickGUI.class);
    private TextSetting activeTextSetting = null;
    private final Map<NumberSetting, Rectangle> numberInputPositions = new HashMap<>();
    private NumberSetting activeNumberSetting = null;
    private final Map<NumberSetting, StringBuilder> numberInputBuffers = new HashMap<>();
    private int logoImageBig = -1;
    private int gridimage = -1;
    private final int y = 50;
    private final int categoryWidth = 100;
    private final int categorySpacing = 20;
    private final int headerHeight = 60;
    private long lastPlayerUpdate = 0;
    private static final long PLAYER_UPDATE_INTERVAL = 2000;
    private final double moduleSpacing = 5;
    private TextSetting searchSetting = new TextSetting("Search", null, "Search");
    private boolean isSearchActive = false;
    private final Map<TextSetting, Rectangle> searchBoxPosition = new HashMap<>();
    private boolean isDraggingSlider = false;
    private NumberSetting draggedSetting = null;
    private final int modulePadding = 2;
    private final int settingSpacing = 4;
    private final int settingPadding = 4;
    private float animationOffsetGradient = 0;
    private final int settingHeight = 16;
    private final Color categoryBackgroundColor = new Color(7, 8, 11, 94);
    private final Color categoryHeaderColor = new Color(34, 0, 135, 153);
    private final Color moduleBackgroundColor = new Color(49, 51, 56, 71);
    private final Color moduleBackgroundColor1 = new Color(124, 124, 124, 71);
    private final Color moduleToggledBackgroundColor = new Color(48, 117, 255, 255);
    private final Color settingBackgroundColor = new Color(25, 25, 25, 200);
    private final Color sliderColor = new Color(190, 2, 196, 255);
    private final Color sliderBackgroundColor = new Color(119, 119, 119, 255);
    private final Color enumSettingBackgroundColor = new Color(0, 0, 0, 23);
    public Color bpsColor22 = new Color(66, 66, 66, 255);
    private final Color activeCategoryColor = new Color(20, 82, 204, 255);
    private  Color activeCategoryColor2 = new Color(255, 255, 255, 255);
    private  Color activeCategoryColor3 = new Color(255, 255, 255, 255);
    private  Color activeCategoryColor4 = new Color(255, 255, 255, 255);
    private final Color activeModuleStartColor = new Color(48, 117, 255, 255);
    private final Color activeModuleEndColor = new Color(107, 156, 255, 255);
    private final Map<ModuleCategory, Float> categoryAlphas = new HashMap<>();
    private final Map<ModuleCategory, Boolean> categoryTargetStates = new HashMap<>();
    private final float FADE_SPEED = 0.05f;
    private final String CONFIG_DIR = "config/dripware/";
    private final String GUI_CONFIG_FILE = CONFIG_DIR + "gui_state.properties";
    public Color bpsColor = new Color(163, 230, 255);
    private long vg = 0L;
    private int fontHandle = -1;
    private int fontHandle1 = -1;
    private Module bindingModule = null;
    private int logoImage = -1;
    private int checkmarkimage = -1;
    private int checkmark2image = -1;
    private int checkmark2image_black = -1;
    private int combatimage = -1;
    private int visualsimage = -1;
    private int moveimage = -1;
    private int playerimage = -1;
    private int miscimage = -1;
    private int backimage = -1;
    private int searchimage = -1;
    private int arrowimage = -1;
    public static String discordUsername = "...";
    private static MinecraftClient mc = MinecraftClient.getInstance();
    private float scaleFactor = 1f;
    private Map<Module, Rectangle> modulePositions = new HashMap<>();
    private Map<ISetting, Rectangle> settingPositions = new HashMap<>();
    private Map<String, Rectangle> enumSettingPositions = new HashMap<>();
    private Map<Module, Boolean> expandedModules = new HashMap<>();
    private Map<ModuleCategory, Rectangle> categoryPositions = new HashMap<>();
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private float scrollOffset = 0f;
    private float maxScrollOffset = 0f;
    private Module selectedModuleForSettings = null;
    private float settingsScrollOffset = 0f;
    private float maxSettingsScrollOffset = 0f;
    private final Map<Module, Rectangle> toggleButtonPositions = new HashMap<>();
    private static final Supplier<MsdfFont> NIGA_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("atlas2").data("atlas2").build());
    private final Map<String, Rectangle> commandRemoveButtonPositions = new HashMap<>();
    private final Map<String, Rectangle> commandAddButtonPositions = new HashMap<>();
    private final Map<String, Rectangle> commandTextPositions = new HashMap<>();
    private TextSetting activeCommandInput = null;
    private Module addingCommandForModule = null;
    private static final Map<String, ModuleCategory> categoryNameToEnum = new HashMap<>();
    static {
        categoryNameToEnum.put("Combat", ModuleCategory.COMBAT);
        categoryNameToEnum.put("Visuals", ModuleCategory.RENDER);
        categoryNameToEnum.put("Movement", ModuleCategory.MOVEMENT);
        categoryNameToEnum.put("Player", ModuleCategory.PLAYER);
        categoryNameToEnum.put("Misc", ModuleCategory.MISCELLANEOUS);
        categoryNameToEnum.put("Other", ModuleCategory.OTHER);
    }
    private long animationStartTime = 0;
    private boolean animationInitialized = false;
    private final int ANIMATION_DURATION = 200;
    private final Map<ModuleCategory, Float> categoryScaleFactors = new HashMap<>();
    private final float SCALE_SPEED = 0.1f;
    private boolean isClosing = false;
    private long closeAnimationStartTime = 0;
    private float blurAlpha = 0.0f;
    private final float BLUR_FADE_SPEED = 0.03f;
    private Module contextMenuModule = null;
    private float contextMenuX = 0;
    private float contextMenuY = 0;
    private final Map<ISetting, Rectangle> contextMenuSettingPositions = new HashMap<>();

    public ClickGUI() {
        super(Text.of("ClickGUI2"));
    }

    @Override
    protected void init() {
        super.init();
        modulePositions.clear();
        toggleButtonPositions.clear();
        contextMenuSettingPositions.clear();
        blurAlpha = 0.0f;
        for (Module module : ModuleManager.getModules()) {
            expandedModules.putIfAbsent(module, false);
            for (ModuleCategory category : ModuleCategory.values()) {
                categoryScaleFactors.putIfAbsent(category, 0.0f);
                categoryTargetStates.putIfAbsent(category, true);
            }
        }
        loadGuiState();
        // Restore contextMenuModule based on expandedModules
        for (Map.Entry<Module, Boolean> entry : expandedModules.entrySet()) {
            if (entry.getValue() && contextMenuModule == null) {
                contextMenuModule = entry.getKey();
            }
        }
    }

    private void loadGuiState() {
        File configFile = new File(GUI_CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                String savedCategory = props.getProperty("selectedCategory");
                if (savedCategory != null) {
                    for (ModuleCategory category : ModuleCategory.values()) {
                        if (category.getName().equals(savedCategory)) {
                            selectedCategory = category;
                            break;
                        }
                    }
                }
                // Load expanded modules
                String expandedModulesStr = props.getProperty("expandedModules", "");
                if (!expandedModulesStr.isEmpty()) {
                    String[] moduleNames = expandedModulesStr.split(",");
                    for (String moduleName : moduleNames) {
                        Module module = ModuleManager.getModuleByName(moduleName.trim());
                        if (module != null) {
                            expandedModules.put(module, true);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load GUI state: " + e.getMessage());
            }
        }
    }

    private void saveGuiState() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File configFile = new File(GUI_CONFIG_FILE);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            Properties props = new Properties();
            if (selectedCategory != null) {
                props.setProperty("selectedCategory", selectedCategory.getName());
            }
            // Save expanded modules
            String expandedModulesStr = expandedModules.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(entry -> entry.getKey().getName())
                    .collect(Collectors.joining(","));
            props.setProperty("expandedModules", expandedModulesStr);
            props.store(fos, "ClickGUI State");
        } catch (IOException e) {
            LOGGER.warn("Failed to save GUI state: " + e.getMessage());
        }
    }

    int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        int rectWidth = 400;
        int rectHeight = 250;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        float x = (screenWidth - rectWidth) / 2f;
        float y = (screenHeight - rectHeight) / 2f;

        float targetBlurAlpha = isClosing ? 0.0f : 1.0f;
        blurAlpha += (targetBlurAlpha - blurAlpha) * BLUR_FADE_SPEED;

        if (blurAlpha > 0.01f) {
            // Existing blur rendering (if any)
        }

        ModuleCategory[] categories = {
                ModuleCategory.COMBAT,
                ModuleCategory.RENDER,
                ModuleCategory.MOVEMENT,
                ModuleCategory.PLAYER,
                ModuleCategory.OTHER,
                ModuleCategory.MISCELLANEOUS
        };

        int totalWidth = categories.length * categoryWidth + (categories.length - 1) * categorySpacing;
        int startX = (screenWidth - totalWidth) / 2;

        categoryPositions.clear();
        contextMenuSettingPositions.clear();

        boolean allCategoriesHidden = true;
        for (ModuleCategory category : categories) {
            float currentScale = categoryScaleFactors.getOrDefault(category, 0.0f);
            boolean targetState = categoryTargetStates.getOrDefault(category, true);
            float targetScale = targetState ? 1.0f : 0.0f;
            currentScale += (targetScale - currentScale) * SCALE_SPEED;
            categoryScaleFactors.put(category, currentScale);

            if (currentScale > 0.01f) {
                allCategoriesHidden = false;
            }

            if (currentScale > 0.01f) {
                context.getMatrices().push();
                context.getMatrices().translate(x + rectWidth / 2f, y + rectHeight / 2f, 0);
                context.getMatrices().scale(currentScale, currentScale, 1.0f);
                context.getMatrices().translate(-(x + rectWidth / 2f), -(y + rectHeight / 2f), 0);


                BuiltRectangle rectangle22 = Builder.rectangle()
                        .size(new SizeState(350, 250))
                        .color(new QuadColorState(new Color(21, 22, 23, 255)))
                        .radius(new QuadRadiusState(0f, 0f, 5f, 5f))
                        .smoothness(1.0f)
                        .build();
                rectangle22.render(context.getMatrices().peek().getPositionMatrix(), x + 100, y);

                BuiltBlur blur2223999 = Builder.blur()
                        .size(new SizeState(101, 250))
                        .radius(new QuadRadiusState(5f, 5f, 0f, 0f))
                        .blurRadius(5f)
                        .smoothness(1f)
                        .color(new QuadColorState(new Color(1.0f, 1.0f, 1.0f, blurAlpha)))
                        .build();
                blur2223999.render(context.getMatrices().peek().getPositionMatrix(), x , y);



                BuiltRectangle rectangle22334435 = Builder.rectangle()
                        .size(new SizeState(101, 250))
                        .color(new QuadColorState(new Color(25, 23, 23, 70)))
                        .radius(new QuadRadiusState(5f, 5f, 0f, 0f))
                        .smoothness(1.0f)
                        .build();
                rectangle22334435.render(context.getMatrices().peek().getPositionMatrix(), x , y);

                BuiltText text19 = Builder.text()
                        .font(NIGA_FONT.get())
                        .text("Fluid")
                        .color(new Color(255, 255, 255, 255))
                        .size(14f)
                        .thickness(0.01f)
                        .build();
                text19.render(context.getMatrices().peek().getPositionMatrix(), x + 47 , y + 13);

                AbstractTexture abstractTexture11 = MinecraftClient.getInstance().getTextureManager()
                        .getTexture(Identifier.of("mre", "textures/log.png"));
                BuiltTexture texture111 = Builder.texture()
                        .size(new SizeState(20, 20))
                        .radius(new QuadRadiusState(0f))
                        .texture(0.0f, 0.0f, 1.0f, 1.0f, abstractTexture11)
                        .color(new QuadColorState(Color.WHITE))
                        .build();
                texture111.render(context.getMatrices().peek().getPositionMatrix(), x + 20, y + 11);

                // Render categories (Combat, Render, Movement, etc.)
                BuiltBlur blur22234 = Builder.blur()
                        .size(new SizeState(77, 19))
                        .radius(new QuadRadiusState(5f))
                        .blurRadius(15f)
                        .smoothness(6f)
                        .color(new QuadColorState(new Color(1.0f, 1.0f, 1.0f, blurAlpha)))
                        .build();
                blur22234.render(context.getMatrices().peek().getPositionMatrix(), x + 13, y + 44  );
                BuiltRectangle rectangle22334 = Builder.rectangle()
                        .size(new SizeState(77, 19))
                        .color(new QuadColorState(selectedCategory == ModuleCategory.MOVEMENT ? activeCategoryColor : new Color(70, 70, 70, 0)))
                        .radius(new QuadRadiusState(5f))
                        .smoothness(1.0f)
                        .build();
                rectangle22334.render(context.getMatrices().peek().getPositionMatrix(), x + 13, y + 44  );
                if(selectedCategory == ModuleCategory.MOVEMENT ){
                    activeCategoryColor3 = new Color(255,255,255,255);
                    activeCategoryColor4 = new Color(255,255,255,255);
                }else {
                    activeCategoryColor3 = new Color(122,122,122,255);
                    activeCategoryColor4 = new Color(122,122,122,255);
                }
                BuiltText text1999 = Builder.text()
                        .font(NIGA_FONT.get())
                        .text("Macros")
                        .color(activeCategoryColor3)
                        .size(8f)
                        .thickness(0.01f)
                        .build();
                text1999.render(context.getMatrices().peek().getPositionMatrix(), x + 33, y + 48.5f );

                categoryPositions.put(ModuleCategory.MOVEMENT, new Rectangle((int)(x + 13), (int)(y + 44  ), 77, 19));



                AbstractTexture abstractTexture11134 = MinecraftClient.getInstance().getTextureManager()
                        .getTexture(Identifier.of("mre", "textures/icons8-script-100.png"));
                BuiltTexture texture111134 = Builder.texture()
                        .size(new SizeState(12, 12))
                        .radius(new QuadRadiusState(0f))
                        .texture(1.0f, 1.0f, 1.0f, 1.0f, abstractTexture11134)
                        .color(new QuadColorState(activeCategoryColor4))
                        .build();
                texture111134.render(context.getMatrices().peek().getPositionMatrix(), x + 17, y + 47.5  );

                BuiltBlur blur2223 = Builder.blur()
                        .size(new SizeState(19, 19))
                        .radius(new QuadRadiusState(5f))
                        .blurRadius(15f)
                        .smoothness(6f)
                        .color(new QuadColorState(new Color(1.0f, 1.0f, 1.0f, blurAlpha)))
                        .build();
                blur2223.render(context.getMatrices().peek().getPositionMatrix(), x + 13, y + 44 + 19 + 5);
                BuiltRectangle rectangle2233 = Builder.rectangle()
                        .size(new SizeState(77, 19))
                        .color(new QuadColorState(selectedCategory == ModuleCategory.RENDER ? activeCategoryColor : new Color(70, 70, 70, 0)))
                        .radius(new QuadRadiusState(5f))
                        .smoothness(1.0f)
                        .build();
                rectangle2233.render(context.getMatrices().peek().getPositionMatrix(), x + 13, y + 44 + 19 + 5);
                if(selectedCategory == ModuleCategory.RENDER ){
                    activeCategoryColor2 = new Color(255,255,255,255);
                }else {
                    activeCategoryColor2 = new Color(122,122,122,255);
                }
                BuiltText text199 = Builder.text()
                        .font(NIGA_FONT.get())
                        .text("Visuals")
                        .color(activeCategoryColor2)
                        .size(8f)
                        .thickness(0.01f)
                        .build();
                text199.render(context.getMatrices().peek().getPositionMatrix(), x + 33.5f, y + 48.5f + 19 + 5);

                categoryPositions.put(ModuleCategory.RENDER, new Rectangle((int)(x + 13), (int)(y + 44 + 19 + 5), 77, 19));

                AbstractTexture abstractTexture1113 = MinecraftClient.getInstance().getTextureManager()
                        .getTexture(Identifier.of("mre", "textures/icons8-eye-100.png"));
                BuiltTexture texture11113 = Builder.texture()
                        .size(new SizeState(13, 13))
                        .radius(new QuadRadiusState(0f))
                        .texture(1.0f, 1.0f, 1.0f, 1.0f, abstractTexture1113)
                        .color(new QuadColorState(Color.WHITE))
                        .build();
                texture11113.render(context.getMatrices().peek().getPositionMatrix(), x + 16, y + 47 + 19 + 5);



                BuiltRectangle rectangle223345 = Builder.rectangle()
                        .size(new SizeState(19, 19))
                        .color(new QuadColorState(selectedCategory == ModuleCategory.MISCELLANEOUS ? activeCategoryColor : new Color(70, 70, 70, 0)))
                        .radius(new QuadRadiusState(5f))
                        .smoothness(1.0f)
                        .build();
                rectangle223345.render(context.getMatrices().peek().getPositionMatrix(), x + 13, y + 44 + 19 + 5 + 19 + 5 );
                categoryPositions.put(ModuleCategory.MISCELLANEOUS, new Rectangle((int)(x + 13), (int)(y + 44 + 19 + 5 + 19 + 5 ), 19, 19));

                AbstractTexture abstractTexture111345 = MinecraftClient.getInstance().getTextureManager()
                        .getTexture(Identifier.of("mre", "textures/icons8-settings-50.png"));
                BuiltTexture texture1111345 = Builder.texture()
                        .size(new SizeState(11, 11))
                        .radius(new QuadRadiusState(0f))
                        .texture(1.0f, 1.0f, 1.0f, 1.0f, abstractTexture111345)
                        .color(new QuadColorState(Color.WHITE))
                        .build();
                texture1111345.render(context.getMatrices().peek().getPositionMatrix(), x + 17.5f, y + 47 + 19 + 5  + 19 + 6);



                // Render modules
                if (selectedCategory != null) {
                    float moduleX = x + 110; // Starting x position for modules
                    float moduleY = y + 10 + scrollOffset; // Starting y position with scroll offset
                    float moduleWidth = 107; // Fixed width of each module
                    float moduleHeight = headerHeight; // Fixed height of each module
                    float maxX = x + rectWidth - 10; // Right boundary of the menu rectangle
                    float rowHeight = (float)moduleHeight + (float)moduleSpacing; // Height of each row including spacing
                    int modulesPerRow = (int) ((maxX - (x + 52)) / (moduleWidth + moduleSpacing)); // Number of modules that fit in a row
                    int currentColumn = 0; // Current column in the row

                    modulePositions.clear();
                    List<Module> modules = ModuleManager.getModulesByCategory(selectedCategory);
                    float contextMenuHeight = 0;
                    boolean passedContextModule = false;

                    // Calculate context menu height if applicable
                    if (contextMenuModule != null && modules.contains(contextMenuModule)) {
                        List<ISetting> settings = ModuleManager.getSettingsForModule(contextMenuModule);
                        if (!settings.isEmpty()) {
                            contextMenuHeight = settings.size() * (settingHeight + settingSpacing) + settingPadding * 2 + 2;
                        } else {
                            contextMenuModule = null; // No settings, close menu
                            for (Module m : expandedModules.keySet()) {
                                expandedModules.put(m, false);
                            }
                            saveGuiState();
                        }
                    }

                    // Render modules in a grid layout
                    for (Module module : modules) {
                        if (contextMenuModule == module) {
                            passedContextModule = true;
                        }




                        // Set radius to 0 if this module has its context menu open, otherwise use 5f
                        float radius = (module == contextMenuModule) ? 5f : 0f;
                        float radius1 = (module == contextMenuModule) ? 0f : 5f;
                        float radius2 = (module == contextMenuModule) ? 0f : 5f;
                        float radius3 = (module == contextMenuModule) ? 5f : 0f;

                        // Render the module




                        BuiltRectangle moduleRect1 = Builder.rectangle()
                                .size(new SizeState(moduleWidth, moduleHeight))
                                .color(new QuadColorState(moduleBackgroundColor))
                                .radius(new QuadRadiusState(5))
                                .smoothness(1.0f)
                                .build();
                        moduleRect1.render(context.getMatrices().peek().getPositionMatrix(), moduleX, moduleY);




                        BuiltRectangle moduleRect = Builder.rectangle()
                                .size(new SizeState(moduleWidth, 20))
                                .color(module.isToggled() ?
                                        new QuadColorState(
                                                activeCategoryColor,
                                                activeCategoryColor,
                                                activeCategoryColor,
                                                activeCategoryColor
                                        ) :
                                        new QuadColorState(moduleBackgroundColor1))
                                .radius(new QuadRadiusState(radius,radius1,radius2,radius3))
                                .smoothness(1.0f)
                                .build();
                        moduleRect.render(context.getMatrices().peek().getPositionMatrix(), moduleX, moduleY + 40);
                        if(module.getName().equals("Sprint")) {
                            AbstractTexture abstractTexture11134519 = MinecraftClient.getInstance().getTextureManager()
                                    .getTexture(Identifier.of("mre", "textures/icons8-exercise-100.png"));
                            BuiltTexture texture111134519 = Builder.texture()
                                    .size(new SizeState(25, 25))
                                    .radius(new QuadRadiusState(0f))
                                    .texture(0.0f, 0.0f, 1.01f, 1.01f, abstractTexture11134519)
                                    .color(new QuadColorState(Color.WHITE))
                                    .build();
                            texture111134519.render(context.getMatrices().peek().getPositionMatrix(), moduleX + 42, moduleY + 7f);
                        }
                        if(module.getName().equals("Pathfinding")) {
                            AbstractTexture abstractTexture11134519 = MinecraftClient.getInstance().getTextureManager()
                                    .getTexture(Identifier.of("mre", "textures/icons8-xyz-100.png"));
                            BuiltTexture texture111134519 = Builder.texture()
                                    .size(new SizeState(25, 25))
                                    .radius(new QuadRadiusState(0f))
                                    .texture(0.0f, 0.0f, 1.01f, 1.01f, abstractTexture11134519)
                                    .color(new QuadColorState(Color.WHITE))
                                    .build();
                            texture111134519.render(context.getMatrices().peek().getPositionMatrix(), moduleX + 42, moduleY + 7f);
                        }
                        if(module.getName().equals("Wheat S-Shape")) {
                            AbstractTexture abstractTexture11134519 = MinecraftClient.getInstance().getTextureManager()
                                    .getTexture(Identifier.of("mre", "textures/icon-wheat.png"));
                            BuiltTexture texture111134519 = Builder.texture()
                                    .size(new SizeState(25, 25))
                                    .radius(new QuadRadiusState(0f))
                                    .texture(0.0f, 0.0f, 1.01f, 1.01f, abstractTexture11134519)
                                    .color(new QuadColorState(Color.WHITE))
                                    .build();
                            texture111134519.render(context.getMatrices().peek().getPositionMatrix(), moduleX + 42, moduleY + 7f);
                        }
                        modulePositions.put(module, new Rectangle((int)moduleX, (int)moduleY, (int)moduleWidth, (int)moduleHeight));

                        if (!ModuleManager.getSettingsForModule(module).isEmpty()) {
                            AbstractTexture abstractTexture1113451 = MinecraftClient.getInstance().getTextureManager()
                                    .getTexture(Identifier.of("mre", "textures/icons8-dots-100.png"));
                            BuiltTexture texture11113451 = Builder.texture()
                                    .size(new SizeState(13, 13))
                                    .radius(new QuadRadiusState(0f))
                                    .texture(0.0f, 0.0f, 1.0f, 1.0f, abstractTexture1113451)
                                    .color(new QuadColorState(Color.WHITE))
                                    .build();
                            texture11113451.render(context.getMatrices().peek().getPositionMatrix(), moduleX + 90, moduleY + 3.5f);
                        }

                        BuiltText text1 = Builder.text()
                                .font(NIGA_FONT.get())
                                .text(module.getName())
                                .color(new Color(255, 255, 255, 255))
                                .size(8f)
                                .thickness(0.01f)
                                .build();
                        text1.render(context.getMatrices().peek().getPositionMatrix(), moduleX + 8, moduleY + 45);

                        // Move to the next position
                        currentColumn++;
                        if (currentColumn >= modulesPerRow) {
                            // Move to the next row
                            currentColumn = 0;
                            moduleX = x + 110;
                            moduleY += rowHeight;
                        } else {
                            // Move to the right
                            moduleX += moduleWidth + moduleSpacing;
                        }

                        // Adjust moduleY for modules after contextMenuModule
                        if (passedContextModule && contextMenuModule != null) {
                            Rectangle moduleRectContext = modulePositions.get(contextMenuModule);
                            if (moduleRectContext != null) {
                                float menuHeight = ModuleManager.getSettingsForModule(contextMenuModule).size() * (settingHeight + settingSpacing) + settingPadding * 2 + 2;
                                float menuY = moduleRectContext.y + moduleRectContext.height + 2;
                                // Check if context menu is below (i.e., not flipped above due to screen height)
                                if (menuY + menuHeight <= screenHeight && currentColumn == 0) {
                                    moduleY += menuHeight; // Add offset for modules below the context menu
                                }
                            }
                        }
                    }

                    // Render context menu after modules, only if contextMenuModule is in the current category
                    if (contextMenuModule != null && modules.contains(contextMenuModule)) {
                        List<ISetting> settings = ModuleManager.getSettingsForModule(contextMenuModule);
                        if (!settings.isEmpty()) {
                            float menuWidth = 105;
                            float menuHeight = contextMenuHeight;
                            Rectangle moduleRect = modulePositions.get(contextMenuModule);
                            if (moduleRect != null) {
                                float menuX = moduleRect.x;
                                float menuY = moduleRect.y + moduleRect.height -0.5f;
                                if (menuY + menuHeight > screenHeight) {
                                    menuY = moduleRect.y - menuHeight - 2;
                                }

                                BuiltRectangle menuRect = Builder.rectangle()
                                        .size(new SizeState(menuWidth, menuHeight))
                                        .color(new QuadColorState(moduleBackgroundColor))
                                        .radius(new QuadRadiusState(0f,5f,5f,0f))
                                        .smoothness(1.0f)
                                        .build();
                                menuRect.render(context.getMatrices().peek().getPositionMatrix(), menuX, menuY);

                                float settingY = menuY + settingPadding;
                                for (ISetting setting : settings) {
                                    float settingX = menuX + settingPadding;
                                    float settingWidth = menuWidth - 2 * settingPadding;

                                    BuiltRectangle settingRect = Builder.rectangle()
                                            .size(new SizeState(settingWidth, settingHeight))
                                            .color(new QuadColorState(new Color(200, 200, 200, 0)))
                                            .radius(new QuadRadiusState(3f))
                                            .smoothness(1.0f)
                                            .build();
                                    settingRect.render(context.getMatrices().peek().getPositionMatrix(), settingX, settingY);
                                    contextMenuSettingPositions.put(setting, new Rectangle((int)settingX, (int)settingY, (int)settingWidth, settingHeight));

                                    BuiltText settingText = Builder.text()
                                            .font(NIGA_FONT.get())
                                            .text(setting.getName())
                                            .color(new Color(255, 255, 255, 255))
                                            .size(7f)
                                            .thickness(0.01f)
                                            .build();
                                    settingText.render(context.getMatrices().peek().getPositionMatrix(), settingX + 4, settingY + 4);

                                    if (setting instanceof BoolSetting boolSetting) {
                                        Color toggleColor = boolSetting.get() ? new Color(0, 255, 0, 255) : new Color(255, 0, 0, 255);
                                        BuiltRectangle toggleRect = Builder.rectangle()
                                                .size(new SizeState(12, 12))
                                                .color(new QuadColorState(toggleColor))
                                                .radius(new QuadRadiusState(3f))
                                                .smoothness(1.0f)
                                                .build();
                                        toggleRect.render(context.getMatrices().peek().getPositionMatrix(), settingX + settingWidth - 16, settingY + 2);
                                    } else if (setting instanceof EnumSetting enumSetting) {
                                        BuiltText valueText = Builder.text()
                                                .font(NIGA_FONT.get())
                                                .text(enumSetting.get())
                                                .color(new Color(200, 200, 200, 255))
                                                .size(7f)
                                                .thickness(0.01f)
                                                .build();
                                        valueText.render(context.getMatrices().peek().getPositionMatrix(), settingX + settingWidth - 50, settingY + 4);
                                    } else if (setting instanceof NumberSetting numberSetting) {
                                        float sliderWidth = settingWidth - 60;
                                        float sliderProgress = (float)((numberSetting.get() - numberSetting.min) / (numberSetting.max - numberSetting.min));
                                        BuiltRectangle sliderBg = Builder.rectangle()
                                                .size(new SizeState(sliderWidth, 4))
                                                .color(new QuadColorState(sliderBackgroundColor))
                                                .radius(new QuadRadiusState(2f))
                                                .smoothness(1.0f)
                                                .build();
                                        sliderBg.render(context.getMatrices().peek().getPositionMatrix(), settingX + 40, settingY + 6);
                                        BuiltRectangle slider = Builder.rectangle()
                                                .size(new SizeState(sliderWidth * sliderProgress, 4))
                                                .color(new QuadColorState(sliderColor))
                                                .radius(new QuadRadiusState(2f))
                                                .smoothness(1.0f)
                                                .build();
                                        slider.render(context.getMatrices().peek().getPositionMatrix(), settingX + 40, settingY + 6);
                                        BuiltText valueText = Builder.text()
                                                .font(NIGA_FONT.get())
                                                .text(numberSetting.onlyint ? String.valueOf(numberSetting.get().intValue()) : String.format("%.2f", numberSetting.get()))
                                                .color(new Color(200, 200, 200, 255))
                                                .size(7f)
                                                .thickness(0.01f)
                                                .build();
                                        valueText.render(context.getMatrices().peek().getPositionMatrix(), settingX + settingWidth - 20, settingY + 4);
                                    } else if (setting instanceof TextSetting textSetting) {
                                        BuiltText valueText = Builder.text()
                                                .font(NIGA_FONT.get())
                                                .text(textSetting.get().isEmpty() ? "..." : textSetting.get())
                                                .color(new Color(200, 200, 200, 255))
                                                .size(7f)
                                                .thickness(0.01f)
                                                .build();
                                        valueText.render(context.getMatrices().peek().getPositionMatrix(), settingX + settingWidth - 50, settingY + 4);
                                    }

                                    settingY += settingHeight + settingSpacing;
                                }
                            }
                        }
                    }

                    // Update maxScrollOffset for the grid layout
                    int totalRows = (int) Math.ceil((double) modules.size() / modulesPerRow);
                    double totalHeight = totalRows * rowHeight + (contextMenuModule != null && modules.contains(contextMenuModule) ? contextMenuHeight : 0);
                    maxScrollOffset = (float) Math.max(0, totalHeight - (screenHeight - (y + 10)));
                    scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
                }

                context.getMatrices().pop();
            }
        }

        if (isClosing && System.currentTimeMillis() - closeAnimationStartTime >= ANIMATION_DURATION) {
            super.close();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !isClosing) {
            isClosing = true;
            closeAnimationStartTime = System.currentTimeMillis();
            for (ModuleCategory category : ModuleCategory.values()) {
                categoryTargetStates.put(category, false);
            }
            // Update expandedModules before closing
            for (Module module : expandedModules.keySet()) {
                expandedModules.put(module, module == contextMenuModule);
            }
            contextMenuModule = null;
            saveGuiState();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isClosing) {
            return false;
        }

        if (button == 0) {
            for (Map.Entry<ModuleCategory, Rectangle> entry : categoryPositions.entrySet()) {
                ModuleCategory category = entry.getKey();
                Rectangle rect = entry.getValue();
                if (rect.contains((int)mouseX, (int)mouseY)) {
                    selectedCategory = category;
                    scrollOffset = 0f;
                    // Update expandedModules to maintain context menu state
                    for (Module module : expandedModules.keySet()) {
                        expandedModules.put(module, module == contextMenuModule);
                    }
                    saveGuiState();
                    return true;
                }
            }
            for (Map.Entry<Module, Rectangle> entry : modulePositions.entrySet()) {
                Module module = entry.getKey();
                Rectangle rect = entry.getValue();
                if (rect.contains((int)mouseX, (int)mouseY)) {
                    module.toggle();
                    // Update expandedModules to maintain context menu state
                    for (Module m : expandedModules.keySet()) {
                        expandedModules.put(m, m == contextMenuModule);
                    }
                    saveGuiState();
                    return true;
                }
            }
            if (contextMenuModule != null) {
                for (Map.Entry<ISetting, Rectangle> entry : contextMenuSettingPositions.entrySet()) {
                    ISetting setting = entry.getKey();
                    Rectangle rect = entry.getValue();
                    if (rect.contains((int)mouseX, (int)mouseY)) {
                        if (setting instanceof BoolSetting boolSetting) {
                            boolSetting.set(!boolSetting.get());
                        } else if (setting instanceof EnumSetting enumSetting) {
                            enumSetting.next();
                        } else if (setting instanceof NumberSetting numberSetting) {
                            isDraggingSlider = true;
                            draggedSetting = numberSetting;
                        } else if (setting instanceof TextSetting textSetting) {
                            activeTextSetting = textSetting;
                        }
                        return true;
                    }
                }
            }
        } else if (button == 1) {
            for (Map.Entry<Module, Rectangle> entry : modulePositions.entrySet()) {
                Module module = entry.getKey();
                Rectangle rect = entry.getValue();
                if (rect.contains((int)mouseX, (int)mouseY)) {
                    if (contextMenuModule == module) {
                        contextMenuModule = null;
                        expandedModules.put(module, false);
                    } else {
                        contextMenuModule = module;
                        contextMenuX = rect.x;
                        contextMenuY = rect.y + rect.height + 2;
                        expandedModules.put(module, true);
                    }
                    saveGuiState();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingSlider && draggedSetting != null && button == 0) {
            Rectangle rect = contextMenuSettingPositions.get(draggedSetting);
            if (rect != null) {
                float settingX = rect.x + settingPadding + 40;
                float settingWidth = rect.width - 60;
                float relativeX = (float)mouseX - settingX;
                float progress = Math.max(0, Math.min(1, relativeX / settingWidth));
                double value = draggedSetting.min + progress * (draggedSetting.max - draggedSetting.min);
                if (draggedSetting.onlyint) {
                    value = Math.round(value);
                } else {
                    value = Math.round(value * 100.0) / 100.0; // Round to 2 decimal places for 0.01 increments
                }
                draggedSetting.set(value);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingSlider) {
            isDraggingSlider = false;
            draggedSetting = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isClosing) {
            scrollOffset -= verticalAmount * 10;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        // Update expandedModules before closing
        for (Module module : expandedModules.keySet()) {
            expandedModules.put(module, module == contextMenuModule);
        }
        saveGuiState();
    }
}