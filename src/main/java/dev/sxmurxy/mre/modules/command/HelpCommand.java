package dev.sxmurxy.mre.modules.command;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Shows all available commands", ".help");
    }

    @Override
    public void execute(String[] args) {

        sendMessage("§f.t <module> §7- toogle module");
        sendMessage("§f.toogle <module> §7- toogle module");
        sendMessage("§f.bind <module> <key> §7- bound module to key");
        sendMessage("§f.unbind <module> §7- unbound module");



    }
}
