package dev.sxmurxy.mre.modules.settings;


import dev.sxmurxy.mre.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingManager {
    public List<ISetting> settings = new ArrayList<>();

    public List<ISetting> getSettingsByModule(Module m) {
        List<ISetting> toret = new ArrayList<>();
        for(ISetting s : settings) {
            if(s.getModule() == m) {
                toret.add(s);
            }
        }
        return toret;
    }



    public List<ISetting> getSettingsForModule(Module module) {
        return settings.stream()
                .filter(s -> s.getModule() == module)
                .collect(Collectors.toList());
    }

    public void addSetting(ISetting setting) {
        settings.add(setting);
    }
    public ISetting getSettingByName(String s) {
        return settings.stream().filter(set -> set.getName().equalsIgnoreCase(s)).findFirst().get();
    }

    public ISetting getSettingByNameAndModule(String s, Module module) {
        for(ISetting ptr : settings) {
            if(ptr.getModule() == module && ptr.getName().equalsIgnoreCase(s)) {
                return ptr;
            }
        }
        return null;
    }
}