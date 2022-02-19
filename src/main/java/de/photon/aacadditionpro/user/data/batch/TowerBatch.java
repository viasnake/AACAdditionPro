package de.photon.aacadditionpro.user.data.batch;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.batch.Batch;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.minecraft.world.InternalPotion;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TowerBatch extends Batch<TowerBatch.TowerBlockPlace>
{
    public static final Location DUMMY_LOCATION = new Location(null, 0, 0, 0);

    public static final Broadcaster<Snapshot<TowerBlockPlace>> TOWER_BATCH_BROADCASTER = new Broadcaster<>();
    private static final int TOWER_BATCH_SIZE = 6;

    public TowerBatch(@NotNull User user)
    {
        super(TOWER_BATCH_BROADCASTER, user, TOWER_BATCH_SIZE, new TowerBlockPlace(DUMMY_LOCATION, user.getPlayer()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Value
    public static class TowerBlockPlace
    {
        long time = System.currentTimeMillis();
        Location locationOfBlock;
        Optional<PotionEffect> jumpBoost;
        Optional<PotionEffect> levitation;

        public TowerBlockPlace(Location locationOfBlock, Player player)
        {
            this.locationOfBlock = locationOfBlock;
            this.jumpBoost = InternalPotion.JUMP.getPotionEffect(player);
            this.levitation = InternalPotion.LEVITATION.getPotionEffect(player);
        }

        public long timeOffset(@NotNull TowerBlockPlace other)
        {
            return MathUtil.absDiff(time, other.getTime());
        }
    }
}
