package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.client.pathfinding.Pathfinder;
import dev.sxmurxy.mre.modules.pathfinder.PathfindingModule;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import static dev.sxmurxy.mre.modules.Module.mc;

public class PathfindCommand extends Command {

    public PathfindCommand() {
        super("pathfind", "Advanced pathfinding control.", ".pathfind <x y z|here|stop|settings|status|help>");
    }

    @Override
    public void execute(String[] args) {
        PathfindingModule pathfinding = PathfindingModule.getInstance();
        if (args.length < 1) { handleHelp(); return; }
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "stop": handleStop(pathfinding); break;
            case "here": case "cursor": handlePathfindToCursor(pathfinding); break;
            case "settings": handleSettings(pathfinding, args); break;
            case "status": handleStatus(pathfinding); break;
            case "help": handleHelp(); break;
            default: handleCoordinates(pathfinding, args); break;
        }
    }

    private void handleCoordinates(PathfindingModule pathfinding, String[] args) {
        if (args.length < 3) { sendMessage("§cUsage: .pathfind <x> <y> <z>"); return; }
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            BlockPos target = new BlockPos(x, y, z);
            sendMessage("§aPathfinding to " + target.toShortString());
            pathfinding.pathfindTo(target);
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid coordinates.");
        }
    }

    private void handleStop(PathfindingModule pathfinding) {
        pathfinding.stopPathfinding();
        sendMessage("§ePathfinding stopped.");
    }

    private void handlePathfindToCursor(PathfindingModule pathfinding) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos target = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            sendMessage("§aPathfinding to cursor target: " + target.toShortString());
            pathfinding.pathfindTo(target);
        } else {
            sendMessage("§cYou are not looking at a block.");
        }
    }

    private void handleSettings(PathfindingModule pathfinding, String[] args) {
        if (args.length < 3) {
            sendMessage("§bUsage: .pathfind settings <setting> <value>");
            sendMessage("§7Available: aotv, etherwarp, debug, mode (walk/optimized)");
            return;
        }
        String setting = args[1].toLowerCase();
        String valueStr = args[2].toLowerCase();
        boolean valueBool = valueStr.equals("on") || valueStr.equals("true");

        switch (setting) {
            case "aotv": pathfinding.setAotvEnabled(valueBool); break;
            case "etherwarp": pathfinding.setEtherwarpEnabled(valueBool); break;
            case "debug": pathfinding.setDebugMode(valueBool); break;
            case "mode":
                if (valueStr.equals("walk")) pathfinding.setPathfindingMode(Pathfinder.PathfindingMode.WALK);
                else if (valueStr.equals("optimized")) pathfinding.setPathfindingMode(Pathfinder.PathfindingMode.OPTIMIZED);
                else { sendMessage("§cInvalid mode. Use 'walk' or 'optimized'."); return; }
                break;
            default: sendMessage("§cUnknown setting."); return;
        }
        sendMessage("§7Set " + setting + " to " + valueStr);
    }

    private void handleStatus(PathfindingModule pathfinding) {
        sendMessage("§b--- Pathfinder Status ---");
        sendMessage("§7- Active: " + (pathfinding.isPathfinding() ? "§aYes" : "§cNo"));
        sendMessage(String.format("§7- Paths Completed: §a%d", pathfinding.getPathsCompleted()));
        sendMessage(String.format("§7- Paths Failed: §c%d", pathfinding.getPathsFailed()));
        if (pathfinding.getLastPathfindTime() > 0) {
            sendMessage(String.format("§7- Last Calc Time: §f%dms", pathfinding.getLastPathfindTime()));
        }
    }

    private void handleHelp() {
        sendMessage("§b--- Pathfinder Help ---");
        sendMessage("§7.pathfind <x y z> - Pathfind to coordinates.");
        sendMessage("§7.pathfind here - Pathfind to the block you are looking at.");
        sendMessage("§7.pathfind stop - Stops the current task.");
        sendMessage("§7.pathfind status - Shows current status and stats.");
        sendMessage("§7.pathfind settings <setting> <value> - Change a setting.");
    }
}

