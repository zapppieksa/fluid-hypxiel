package dev.sxmurxy.mre.modules.command;
import dev.sxmurxy.mre.modules.pathfinding.PathfinderAPI; // Zaimportuj swoje PathfinderAPI
import net.minecraft.util.math.BlockPos;

public class PathfindCommand extends Command {

    public PathfindCommand() {
        super("pathfind", "Starts pathfinding to the specified coordinates.", ".pathfind <x> <y> <z>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3) {
            sendUsage();
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);

            BlockPos targetPos = new BlockPos(x, y, z);

            // Sprawdź, czy instancja API jest dostępna
            if (PathfinderAPI.getInstance() == null) {
                sendMessage("§cError: PathfinderAPI is not initialized.");
                return;
            }

            // Użyj publicznego API do rozpoczęcia pathfindingu
            PathfinderAPI.walkTo(targetPos);

        } catch (NumberFormatException e) {
            sendMessage("§cInvalid coordinates. Please use numbers.");
            sendUsage();
        }
    }
}
