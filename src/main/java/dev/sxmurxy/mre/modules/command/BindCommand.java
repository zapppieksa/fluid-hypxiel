package dev.sxmurxy.mre.modules.command;

import org.lwjgl.glfw.GLFW;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleManager;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Binds a key to a module", ".bind <module> <key>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            sendUsage();
            return;
        }

        String moduleName = args[0];
        String keyName = args[1].toUpperCase();

        // Debug - wypisz dostępne moduły


        Module module = ModuleManager.getModuleByName(moduleName.toLowerCase());
        if (module == null) {
            // Spróbuj znaleźć moduł ignorując wielkość liter
            for (Module m : ModuleManager.getModules()) {
                if (m.getName().equalsIgnoreCase(moduleName)) {
                    module = m;
                    break;
                }
            }
        }

        if (module == null) {
            sendMessage("§cModule not found: " + moduleName);
            return;
        }

        int keyCode = getGLFWKey(keyName);
        if (keyCode == -1) {
            sendMessage("§cInvalid key: " + keyName);
            return;
        }

        module.setKey(keyCode);
        sendMessage("§fFluid §7» §fBound §b" + module.getName() + " §fto §b" + keyName + " ");
    }

    private int getGLFWKey(String keyName) {
        try {
            // Obsługa specjalnych klawiszy
            switch (keyName) {
                case "SPACE": return GLFW.GLFW_KEY_SPACE;
                case "ENTER": return GLFW.GLFW_KEY_ENTER;
                case "TAB": return GLFW.GLFW_KEY_TAB;
                case "LSHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
                case "RSHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
                case "LCTRL": return GLFW.GLFW_KEY_LEFT_CONTROL;
                case "RCTRL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
                case "LALT": return GLFW.GLFW_KEY_LEFT_ALT;
                case "RALT": return GLFW.GLFW_KEY_RIGHT_ALT;
            }

            // Standardowe klawisz
            return GLFW.class.getField("GLFW_KEY_" + keyName).getInt(null);
        } catch (Exception e) {
            return -1;
        }
    }
}