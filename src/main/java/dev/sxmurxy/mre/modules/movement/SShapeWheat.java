package dev.sxmurxy.mre.modules.movement;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class SShapeWheat extends Module {
    private boolean isFarming = false;
    private long lastActionTime = 0;
    private State currentState = State.LEFT;
    private final Random random = new Random();
    private final int baseDelay = 50; // Bazowe opóźnienie dla płynności (ms)
    private boolean wasScreenOpen = false;
    private long screenClosedTime = 0;
    private final int screenCloseDelay = 1000; // 1 sekunda opóźnienia po zamknięciu screena

    private enum State {
        LEFT, FORWARD1, RIGHT, FORWARD2
    }

    public SShapeWheat() {
        super("SShapeWheat", "Automatyczne farming w S-shape na Hypixel Skyblock z detekcją kolizji.", ModuleCategory.MOVEMENT);
        // Klawisz aktywacji: R
    }

    @Override
    public void onUpdate() {
        if (mc == null || mc.player == null) {
            return;
        }

        // Additional checks for safety
        if (mc.world == null || !mc.player.isAlive()) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Sprawdź czy jakiś screen jest otwarty
        boolean isScreenOpen = mc.currentScreen != null;

        if (isScreenOpen) {
            wasScreenOpen = true;
            return;
        }

        // Jeśli screen był otwarty i właśnie został zamknięty, zapisz czas
        if (wasScreenOpen && !isScreenOpen) {
            wasScreenOpen = false;
            screenClosedTime = currentTime;
            return;
        }

        // Sprawdź czy minęła 1 sekunda od zamknięcia screena
        if (screenClosedTime > 0 && currentTime - screenClosedTime < screenCloseDelay) {
            return;
        }

        if (currentTime - lastActionTime < baseDelay) {
            return;
        }

        // Wykonuj akcje w zależności od stanu
        switch (currentState) {
            case LEFT:
                handleLeftMovement();
                break;
            case FORWARD1:
                handleForwardMovement(State.RIGHT);
                break;
            case RIGHT:
                handleRightMovement();
                break;
            case FORWARD2:
                handleForwardMovement(State.LEFT);
                break;
        }
        if(!isToggled()){
            releaseAllKeys();
        }
        lastActionTime = currentTime;
    }

    private void handleLeftMovement() {
        // Trzymaj lewy przycisk myszy
        mc.options.attackKey.setPressed(true);

        // Ustaw klawisz A (lewo) jako wciśnięty
        setKeyPressed(mc.options.leftKey, true);
        releaseOtherMovementKeys(mc.options.leftKey);

        // Sprawdź kolizję po lewej stronie
        if (checkCollisionInDirection(-90)) { // -90 stopni to lewo
            // Kolizja - puszczaj lewy przycisk myszy i przejdź do ruchu prosto
            mc.options.attackKey.setPressed(true);
            currentState = State.FORWARD1;
        }
    }

    private void handleRightMovement() {
        // Nie trzymaj lewego przycisku myszy podczas ruchu w prawo
        mc.options.attackKey.setPressed(true);

        // Ustaw klawisz D (prawo) jako wciśnięty
        setKeyPressed(mc.options.rightKey, true);
        releaseOtherMovementKeys(mc.options.rightKey);

        // Sprawdź kolizję po prawej stronie
        if (checkCollisionInDirection(90)) { // 90 stopni to prawo
            // Kolizja - przejdź do ruchu prosto
            currentState = State.FORWARD2;
        }
    }

    private void handleForwardMovement(State nextState) {
        // Nie trzymaj lewego przycisku myszy podczas ruchu prosto
        mc.options.attackKey.setPressed(true);

        // Ustaw klawisz W (prosto) jako wciśnięty
        setKeyPressed(mc.options.forwardKey, true);
        releaseOtherMovementKeys(mc.options.forwardKey);

        // Sprawdź kolizję przed sobą
        if (checkCollisionInDirection(0)) { // 0 stopni to prosto
            // Kolizja - przejdź do następnego stanu
            currentState = nextState;
        }
    }

    private boolean checkCollisionInDirection(float yawOffset) {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        Vec3d playerPos = mc.player.getPos();
        float playerYaw = mc.player.getYaw() + yawOffset;

        // Konwertuj yaw na radiany i oblicz kierunek
        double radians = Math.toRadians(playerYaw);
        double x = -Math.sin(radians);
        double z = Math.cos(radians);

        // Sprawdź kolizję na wysokości gracza
        Vec3d start = playerPos.add(0, 0.5, 0); // Sprawdź na wysokości środka gracza
        Vec3d end = start.add(x * 1.2, 0, z * 1.2); // Sprawdź 1.2 bloka przed sobą

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.BLOCK;
    }

    private void setKeyPressed(KeyBinding keyBinding, boolean pressed) {
        if (keyBinding == null) return;

        if (pressed) {
            keyBinding.setPressed(true);
        } else {
            keyBinding.setPressed(false);
        }
    }

    private void setMouseButtonPressed(int button, boolean pressed) {
        if (mc.options == null) return;

        try {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                // Użyj KeyBinding dla lewego przycisku myszy
                mc.options.attackKey.setPressed(pressed);
            }
        } catch (Exception e) {
            // Obsłuż błędy bezpiecznie
        }
    }

    private void releaseOtherMovementKeys(KeyBinding exceptKey) {
        if (mc.options.forwardKey != exceptKey) {
            setKeyPressed(mc.options.forwardKey, false);
        }
        if (mc.options.backKey != exceptKey) {
            setKeyPressed(mc.options.backKey, false);
        }
        if (mc.options.leftKey != exceptKey) {
            setKeyPressed(mc.options.leftKey, false);
        }
        if (mc.options.rightKey != exceptKey) {
            setKeyPressed(mc.options.rightKey, false);
        }
    }

    private void releaseAllKeys() {
        setKeyPressed(mc.options.forwardKey, false);
        setKeyPressed(mc.options.backKey, false);
        setKeyPressed(mc.options.leftKey, false);
        setKeyPressed(mc.options.rightKey, false);
        setMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT, false);
        // Dodatkowe zwolnienie attackKey
        mc.options.attackKey.setPressed(false);
    }

    @Override
    public void onDisable() {
        System.out.println("2");
        // Zwolnij wszystkie klawisze i przyciski myszy
        releaseAllKeys();
        currentState = State.LEFT; // Resetuj stan
    }

    @Override
    public void onEnable() {
        System.out.println("1");
        currentState = State.LEFT; // Zacznij od ruchu w lewo
        lastActionTime = System.currentTimeMillis();
        wasScreenOpen = false;
        screenClosedTime = 0;
    }
}