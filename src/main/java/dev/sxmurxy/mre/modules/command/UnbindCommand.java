package dev.sxmurxy.mre.modules.command;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleManager;
public class UnbindCommand extends Command {

    public UnbindCommand() {
        super("unbind", "Removes key binding from a module", ".unbind <module>");
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

        if (module.getKey() == -1) {
            sendMessage("§cModule §f" + module.getName() + " §chas no key assigned!");
            return;
        }

        module.setKey(-1); // Usuń bind
        sendMessage("§fFluid §7» §fBound §b" + module.getName() + " §fto §bNONE ");
    }
}
