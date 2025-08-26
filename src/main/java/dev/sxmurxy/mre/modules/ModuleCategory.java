package dev.sxmurxy.mre.modules;

import lombok.Getter;

@Getter
public enum ModuleCategory {
    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    MISCELLANEOUS("Miscellaneous"),
    OTHER("Other");

    final String name;

    ModuleCategory(String name) {
        this.name = name;
    }
}
