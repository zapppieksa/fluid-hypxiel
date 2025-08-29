package dev.sxmurxy.mre.modules;

import dev.sxmurxy.mre.UnnsenseClient;
import dev.sxmurxy.mre.modules.movement.SShapeWheat;
import dev.sxmurxy.mre.modules.movement.Sprint;
import dev.sxmurxy.mre.modules.movement.uuidreveal;
import dev.sxmurxy.mre.modules.pathfinder.PathfindingModule;
import dev.sxmurxy.mre.modules.render.*;
import dev.sxmurxy.mre.modules.settings.ISetting;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {

    @Getter
    public static final List<Module> modules = new ArrayList<>();

    public static void registerModule(Module module) {
        modules.add(module);
    }

    public static Module getModuleByName(String name) {
        return modules.stream().filter(module -> module.name.equals(name)).findFirst().orElse(null);
    }

    public static ArrayList<Module> getModulesInAlphabeticalOrder() {
        ArrayList<Module> temp = new ArrayList<>(modules);
        ArrayList<Module> sortedTemp = new ArrayList<>(temp);
        sortedTemp.sort(Comparator.comparing(Module::getName));
        return sortedTemp;
    }

    public static List<Module> getModulesByCategory(ModuleCategory category) {
        List<Module> modules1 = new ArrayList<>();
        for(Module m : modules){
            if(m.getCategory().equals(category)){
                modules1.add(m);
            }
        }
        return modules1;
    }
    public static List<ISetting> getSettingsForModule(Module module) {
        return UnnsenseClient.getInstance().settingManager.settings.stream()
                .filter(setting -> setting.getModule() == module)
                .collect(Collectors.toList());
    }
    public static void register() {
        registerModule(new Sprint());
        registerModule(new Arraylist());
        registerModule(new Fullbright());
        registerModule(new uuidreveal());
        registerModule(new SShapeWheat());
        registerModule(new PathfindingModule());
        registerModule(new Testxray());
    }

}
