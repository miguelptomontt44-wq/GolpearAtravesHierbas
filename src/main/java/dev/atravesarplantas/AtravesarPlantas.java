package dev.atravesarplantas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * Permite golpear mobs que estan detras de pasto, flores, helechos, pasto alto
 * y cualquier bloque sin colision (plantas de 1 o 2 bloques).
 *
 * En vanilla, el cursor apunta a la planta y el golpe se "gasta" rompiendola,
 * por lo que no puedes atacar al mob que esta detras. Este plugin traza un rayo
 * hacia las entidades ignorando los bloques atravesables y ejecuta el ataque.
 */
public final class AtravesarPlantas extends JavaPlugin implements Listener {

    private double reach;
    private double raySize;
    private boolean protectPlant;
    private boolean allowPlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadSettings() {
        reloadConfig();
        reach = Math.max(0.5, getConfig().getDouble("reach", 3.0));
        raySize = Math.max(0.0, getConfig().getDouble("ray-size", 0.0));
        protectPlant = getConfig().getBoolean("protect-plant", true);
        allowPlayers = getConfig().getBoolean("allow-players", true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeftClickBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("atravesarplantas.use")) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !isSeeThroughPlant(clicked)) return;

        Entity target = findTarget(player);
        if (target == null) return;

        // Evita que se rompa la planta (y que el click se consuma en el bloque).
        if (protectPlant) {
            event.setCancelled(true);
        }

        player.attack(target);
    }

    /** Bloques que no tienen colision (pasto, flores, pasto alto, helechos, etc.) y no son liquidos. */
    private boolean isSeeThroughPlant(Block block) {
        return !block.isEmpty() && !block.isLiquid() && block.isPassable();
    }

    private Entity findTarget(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        Predicate<Entity> filter = e -> {
            if (e.equals(player)) return false;
            if (!(e instanceof LivingEntity living)) return false;
            if (living.isDead() || !living.isValid()) return false;
            if (e instanceof Player other) {
                if (!allowPlayers) return false;
                if (other.getGameMode() == GameMode.SPECTATOR) return false;
            }
            return player.canSee(e);
        };

        RayTraceResult entityHit = player.getWorld()
                .rayTraceEntities(eye, dir, reach, raySize, filter);
        if (entityHit == null || entityHit.getHitEntity() == null) return null;

        // Comprobar que no haya un bloque solido entre el jugador y el mob.
        double entityDistance = eye.toVector().distance(entityHit.getHitPosition());
        RayTraceResult blockHit = player.getWorld().rayTraceBlocks(
                eye, dir, entityDistance, FluidCollisionMode.NEVER, true);
        if (blockHit != null && blockHit.getHitBlock() != null) {
            return null; // hay un bloque solido de por medio
        }

        return entityHit.getHitEntity();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("atravesarplantas.admin")) {
            sender.sendMessage(color(getConfig().getString("messages.no-permission", "&cNo tienes permiso.")));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage(color(getConfig().getString("messages.reloaded", "&aRecargado.")));
            return true;
        }
        sender.sendMessage(color("&eUso: /" + label + " reload"));
        return true;
    }

    private Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
