package dev.sxmurxy.mre.modules.command;

public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;

    public Command(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public String getName() {
        return name;
    }

    public abstract void execute(String[] args);

    public void sendUsage() {
        sendMessage("§cUsage: " + usage);
    }

    public void sendMessage(String message) {
        // zakładam że masz klienta MC
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                    net.minecraft.text.Text.of(message), false
            );
        }
    }
}
