package dev.atravesarplantas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Merchant;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * Permite golpear mobs e interactuar (clic derecho) con cofres y entidades
 * que estan detras de pasto, flores, pasto alto, helechos y cualquier planta
 * sin colision (de 1 o 2 bloques).
 *
 * En vanilla el cursor apunta a la planta y el click se "gasta" en ella.
 * Este plugin traza un rayo ignorando los bloques atravesables para encontrar
 * lo que realmente hay detras.
 */
public final class AtravesarPlantas extends JavaPlugin implements Listener {

    // Ataque
    private double reach;
    private double raySize;
    private boolean protectPlant;
    private boolean allowPlayers;

    // Interaccion (clic derecho)
    private boolean interactEnabled;
    private double interactBlockReach;
    private double interactEntityReach;

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

        interactEnabled = getConfig().getBoolean("interact.enabled", true);
        interactBlockReach = Math.max(0.5, getConfig().getDouble("interact.block-reach", 4.5));
        interactEntityReach = Math.max(0.5, getConfig().getDouble("interact.entity-reach", 3.0));
    }

    // ------------------------------------------------------------------
    // Clic izquierdo: golpear mobs a traves de plantas
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeftClickBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("atravesarplantas.use")) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !isSeeThroughPlant(clicked)) return;

        Entity target = findAttackTarget(player);
        if (target == null) return;

        // Evita que se rompa la planta (y que el click se consuma en el bloque).
        if (protectPlant) {
            event.setCancelled(true);
        }

        player.attack(target);
    }

    // ------------------------------------------------------------------
    // Clic derecho: abrir cofres / interactuar con entidades a traves de plantas
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (!interactEnabled) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("atravesarplantas.interact")) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        // Igual que en vanilla: agachado = "quiero colocar un bloque / usar el item".
        if (player.isSneaking()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !isSeeThroughPlant(clicked)) return;

        if (interactThrough(player)) {
            // Evita colocar bloques / usar el item sobre la planta.
            event.setCancelled(true);
        }
    }

    /**
     * Busca lo que hay realmente detras de la planta y interactua con ello.
     *
     * @return true si el click fue "consumido" por el plugin.
     */
    private boolean interactThrough(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        World world = player.getWorld();

        // Primer bloque que no sea atravesable.
        RayTraceResult blockHit = world.rayTraceBlocks(
                eye, dir, interactBlockReach, FluidCollisionMode.NEVER, true);
        double blockDist = Double.MAX_VALUE;
        if (blockHit != null && blockHit.getHitBlock() != null) {
            blockDist = eye.toVector().distance(blockHit.getHitPosition());
        }

        // Entidad interactuable mas cercana.
        Predicate<Entity> filter = e -> !e.equals(player) && e.isValid() && isInteractableEntity(e);
        RayTraceResult entityHit = world.rayTraceEntities(eye, dir, interactEntityReach, 0.0, filter);
        double entityDist = Double.MAX_VALUE;
        if (entityHit != null && entityHit.getHitEntity() != null) {
            entityDist = eye.toVector().distance(entityHit.getHitPosition());
        }

        if (entityHit != null && entityHit.getHitEntity() != null && entityDist <= blockDist) {
            return handleEntity(player, entityHit.getHitEntity());
        }
        if (blockHit != null && blockHit.getHitBlock() != null) {
            return handleBlock(player, blockHit);
        }
        return false;
    }

    private boolean handleBlock(Player player, RayTraceResult hit) {
        Block block = hit.getHitBlock();
        if (block == null) return false;

        Material type = block.getType();
        boolean enderChest = type == Material.ENDER_CHEST;
        boolean craftingTable = type == Material.CRAFTING_TABLE;

        Container container = null;
        if (!enderChest && !craftingTable) {
            BlockState state = block.getState(false);
            if (isSupportedContainer(state)) {
                container = (Container) state;
            } else {
                return false; // no es algo que sepamos abrir -> comportamiento vanilla
            }
        }

        // Cofres tapados por un bloque solido encima no se abren (como en vanilla).
        if (container instanceof Chest && block.getRelative(BlockFace.UP).getType().isOccluding()) {
            return true;
        }

        // Respeta plugins de proteccion: lanzamos un evento de interaccion real sobre el bloque.
        BlockFace face = hit.getHitBlockFace() != null ? hit.getHitBlockFace() : BlockFace.UP;
        PlayerInteractEvent probe = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(),
                block, face, EquipmentSlot.HAND);
        probe.callEvent();
        if (probe.useInteractedBlock() == Event.Result.DENY) {
            return true; // otro plugin lo prohibe
        }

        if (container != null) {
            if (container.isLocked()) return true;
            player.openInventory(container.getInventory());
        } else if (enderChest) {
            player.openInventory(player.getEnderChest());
        } else {
            player.openWorkbench(block.getLocation(), true);
        }
        return true;
    }

    private boolean isSupportedContainer(BlockState state) {
        return state instanceof Chest
                || state instanceof Barrel
                || state instanceof ShulkerBox
                || state instanceof Hopper
                || state instanceof Dispenser
                || state instanceof Dropper
                || state instanceof Furnace
                || state instanceof BrewingStand;
    }

    private boolean isInteractableEntity(Entity e) {
        if (e instanceof Player) return false;
        if (e instanceof Merchant) return true;                           // aldeanos, comerciante errante
        if (e instanceof Boat || e instanceof RideableMinecart) return true; // montar
        return e instanceof Minecart && e instanceof InventoryHolder;     // vagonetas con cofre / tolva
    }

    private boolean handleEntity(Player player, Entity entity) {
        // Respeta plugins de proteccion.
        if (!new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND).callEvent()) {
            return true;
        }

        if (entity instanceof Merchant merchant) {
            if (entity instanceof Villager villager
                    && (villager.getProfession() == Villager.Profession.NONE
                    || villager.getProfession() == Villager.Profession.NITWIT)) {
                return true; // sin oficio: no comercia
            }
            if (entity instanceof Ageable ageable && !ageable.isAdult()) {
                return true; // los bebes no comercian
            }
            if (!merchant.isTrading()) {
                player.openMerchant(merchant, true);
            }
            return true;
        }

        if (entity instanceof Minecart && entity instanceof InventoryHolder holder) {
            player.openInventory(holder.getInventory());
            return true;
        }

        if (entity instanceof Boat || entity instanceof RideableMinecart) {
            int capacity = entity instanceof Boat ? 2 : 1;
            if (entity.getPassengers().size() < capacity) {
                entity.addPassenger(player);
            }
            return true;
        }

        return false;
    }

    // ------------------------------------------------------------------
    // Utilidades compartidas
    // ------------------------------------------------------------------

    /**
     * Plantas: bloques sin colision que no tienen interaccion propia.
     * Se excluyen botones, palancas, carteles, etc., para no estorbar su uso normal.
     */
    private boolean isSeeThroughPlant(Block block) {
        return !block.isEmpty()
                && !block.isLiquid()
                && block.isPassable()
                && !block.getType().isInteractable();
    }

    private Entity findAttackTarget(Player player) {
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
