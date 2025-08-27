package dev.sxmurxy.mre.modules.movement;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class uuidreveal extends Module {

    private String lastShown = "";

    public uuidreveal() {
        super("uuidreveal", "Pokazuje ID skina gracza na którego patrzysz.", ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onUpdate() {
        if (mc == null || mc.player == null || mc.world == null || !mc.player.isAlive()) {
            return;
        }

        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                GameProfile profile = player.getGameProfile();

                if (profile.getProperties().containsKey("textures")) {
                    for (Property prop : profile.getProperties().get("textures")) {
                        try {
                            String decoded = new String(Base64.getDecoder().decode(prop.value()), StandardCharsets.UTF_8);
                            // W JSON-ie jest link do skina, np:
                            // "SKIN":{"url":"http://textures.minecraft.net/texture/<skinID>"}
                            String skinId = extractSkinId(decoded);

                            if (skinId != null && !skinId.equals(lastShown)) {
                                mc.player.sendMessage(Text.of("SkinID gracza " + player.getName().getString() + ": " + skinId), false);
                                lastShown = skinId;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private String extractSkinId(String json) {
        // Bardzo prosty parser – wyszukuje fragment "textures.minecraft.net/texture/xxxxx"
        String marker = "textures.minecraft.net/texture/";
        int idx = json.indexOf(marker);
        if (idx != -1) {
            int start = idx + marker.length();
            int end = json.indexOf("\"", start);
            if (end > start) {
                return json.substring(start, end);
            }
        }
        return null;
    }

    @Override
    public void onDisable() {
        System.out.println("SkinIDReveal disabled");
    }

    @Override
    public void onEnable() {
        System.out.println("SkinIDReveal enabled");
        lastShown = "";
    }
}
