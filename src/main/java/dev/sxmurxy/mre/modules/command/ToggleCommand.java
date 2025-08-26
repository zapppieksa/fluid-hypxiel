package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleManager;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", "Toggles a module on/off", ".toggle <module> | .t <module>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            sendUsage();
            return;
        }

        String moduleName = args[0];

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

        module.toggle();

        String status = module.isToggled() ? "§benabled" : "§bdisabled";
        sendMessage("§fFluid §7» §b" + module.getName() + " §fhas been " + status);
    }
}
