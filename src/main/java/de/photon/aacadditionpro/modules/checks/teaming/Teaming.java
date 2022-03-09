package de.photon.aacadditionpro.modules.checks.teaming;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.datastructure.kdtree.QuadTreeSet;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.minecraft.world.Region;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.stream.Collectors;

public class Teaming extends ViolationModule implements Listener
{
    private final Set<Region> safeZones = ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".safe_zones").stream()
                                                     .map(Region::parseRegion)
                                                     .collect(Collectors.toUnmodifiableSet());

    private final Set<World> enabledWorlds = ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".enabled_worlds").stream()
                                                        .map(key -> Preconditions.checkNotNull(Bukkit.getWorld(key), "Config loading error: Unable to identify world for the teaming check. Please check your world names listed in the config."))
                                                        .collect(Collectors.toUnmodifiableSet());

    @LoadFromConfiguration(configPath = ".proximity_range")
    private double proximityRange;
    @LoadFromConfiguration(configPath = ".no_pvp_time")
    private int noPvpTime;
    @LoadFromConfiguration(configPath = ".allowed_size")
    private int allowedSize;

    public Teaming()
    {
        super("Teaming");
    }

    private boolean playerNotInSafeZone(Player player)
    {
        return this.safeZones.stream().noneMatch(safeZone -> safeZone.isInsideRegion(player.getLocation()));
    }

    @Override
    public void enable()
    {
        val period = (AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".delay") * 20L) / 1000L;

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    val players = new QuadTreeSet<Player>();

                    for (World world : enabledWorlds) {
                        // No need to clear playersOfWorld here, that is automatically done below.
                        // Add the users of the world.
                        for (Player player : world.getPlayers()) {
                            val user = User.getUser(player);
                            DebugSender.getInstance().sendDebug("Player: " + user.getPlayer().getName() + " IN " + !User.isUserInvalid(user, this) +
                                                                " GM " + user.getPlayer().getGameMode().name() + " TS " + user.getTimestampMap().at(TimestampKey.TEAMING_COMBAT_TAG).passedTime() +
                                                                " SZ " + playerNotInSafeZone(player));

                            // Only add users if they meet the preconditions
                            // User has to be online and not bypassed
                            if (!User.isUserInvalid(user, this) &&
                                // Correct gamemodes
                                user.inAdventureOrSurvivalMode() &&
                                // Not engaged in pvp
                                user.getTimestampMap().at(TimestampKey.TEAMING_COMBAT_TAG).notRecentlyUpdated(noPvpTime) &&
                                // Not in a bypassed region
                                playerNotInSafeZone(player))
                            {
                                DebugSender.getInstance().sendDebug("Add: " + player.getName());
                                players.add(player.getLocation().getX(), player.getLocation().getZ(), player);
                            }
                        }

                        while (!players.isEmpty()) {
                            // Use getAny() so the node itself is contained in the team below.
                            var firstNode = players.getAny();
                            val proximityRangeSquared = proximityRange * proximityRange;
                            val team = players.queryCircle(firstNode, proximityRange).stream()
                                              // Check for y-distance.
                                              .filter(node -> node.getElement().getLocation().distanceSquared(firstNode.getElement().getLocation()) <= proximityRangeSquared)
                                              .peek(players::remove)
                                              .map(QuadTreeSet.Node::getElement)
                                              .collect(Collectors.toUnmodifiableSet());

                            DebugSender.getInstance().sendDebug("Team: " + team.stream().map(Player::getName).collect(Collectors.joining(", ")));

                            // Team is too big
                            if (team.size() > this.allowedSize) this.getManagement().flag(Flag.of(team));
                        }
                    }
                }, 1L, period);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .teamVl()
                                       .loadThresholdsToManagement()
                                       .withDecay(300, 1)
                                       .build();
    }
}
