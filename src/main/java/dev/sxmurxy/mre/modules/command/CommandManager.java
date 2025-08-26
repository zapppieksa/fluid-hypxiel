package dev.sxmurxy.mre.modules.command;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {
    public static final List<Command> commands = new ArrayList<>();
    public static final Map<String, String> aliases = new HashMap<>(); // Aliasy komend
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    static {
        // Dodaj aliasy
        aliases.put("t", "toggle");
        aliases.put("toogle", "toggle"); // dla błędów w pisowni :)
    }

    public static void register(Command command) {

        commands.add(command);
    }

    public static boolean executeCommand(String input) {
        if (!input.startsWith(".")) return false;

        String[] split = input.substring(1).split(" ");
        String name = split[0].toLowerCase();
        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, args.length);

        // Sprawdź czy to alias
        if (aliases.containsKey(name)) {
            name = aliases.get(name);
        }

        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                try {
                    command.execute(args);
                    return true;
                } catch (Exception e) {
                    // Debug - wypisz błąd
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                                net.minecraft.text.Text.of("§cError while executing command: " + e.getMessage()), false
                        );
                    }
                    e.printStackTrace();
                    return true;
                }
            }
        }

        // Komenda nie znaleziona
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(
                    net.minecraft.text.Text.of("§cUnknown command: " + name), false
            );
        }
        return false;
    }

    public static List<Command> getCommands() {
        return commands;
    }
}
