package dev.sxmurxy.mre.modules.command;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public class ChatCommandListener {

    public static void init() {
        // Przechwytuj wiadomości przed wysłaniem
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (message.startsWith(".")) {
                // Wykonaj komendę
                if (CommandManager.executeCommand(message)) {
                    // Jeśli komenda została wykonana, nie wysyłaj wiadomości na czat
                    return false;
                }
            }
            return true; // Pozwól na normalne wysłanie wiadomości
        });
    }
}