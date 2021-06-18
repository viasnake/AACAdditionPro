package de.photon.aacadditionpro.user.data.subdata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.datastructure.buffer.RingBuffer;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.mathematics.RotationUtil;
import de.photon.aacadditionpro.util.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.ArrayList;

public class LookPacketData
{
    private static final byte QUEUE_CAPACITY = 20;

    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new LookPacketDataUpdater());
    }

    @Getter
    private final RingBuffer<RotationChange> rotationChangeQueue = new RingBuffer<>(QUEUE_CAPACITY, new RotationChange(0, 0));

    /**
     * Calculates the total rotation change in the last time.
     *
     * @return an array which contains information about the angle changes. <br>
     * [0] is the sum of the angle changes <br>
     * [1] is the sum of the angle offsets
     */
    public float[] getAngleInformation()
    {
        val result = new float[2];

        // Ticks that must be added to fill up the gaps in the queue.
        short gapFillers = 0;

        synchronized (this.rotationChangeQueue) {
            val rotationCache = new ArrayList<>(this.rotationChangeQueue.size());
            val elementArray = this.rotationChangeQueue.toArray(new RotationChange[0]);

            // Start at 1 as of the 0 element being the first "last element".
            for (int i = 1; i < elementArray.length; ++i) {
                if (MathUtil.absDiff(System.currentTimeMillis(), elementArray[i].getTime()) > 1000) continue;

                val ticks = (short) (MathUtil.absDiff(elementArray[i].getTime(),
                                                      // Using -1 for the last element is fine as there is always the last element.
                                                      elementArray[i - 1].getTime()) / 50
                );

                // The current tick should be ignored, no gap filler.
                // How many ticks have been left out?
                if (ticks > 1) gapFillers += (ticks - 1);

                // Angle calculations
                val angle = elementArray[i - 1].angle(elementArray[i]);
                rotationCache.add(angle);
                // Angle change sum
                result[0] += angle;
            }

            // Angle offset sum
            result[1] = (float) MathUtil.absDiff(
                    // The average of the elements
                    result[0] / (rotationCache.size() + gapFillers) * rotationCache.size(),
                    // The sum of all elements
                    result[0]);
        }

        return result;
    }

    @AllArgsConstructor
    public static class RotationChange
    {
        @Getter
        private final long time = System.currentTimeMillis();
        @Getter
        private float yaw;
        @Getter
        private float pitch;

        /**
         * Merges a {@link RotationChange} with this {@link RotationChange}.
         */
        public void merge(final RotationChange rotationChange)
        {
            this.yaw += rotationChange.yaw;
            this.pitch += rotationChange.pitch;
        }

        /**
         * Calculates the total angle between two {@link RotationChange} - directions.
         */
        public float angle(final RotationChange rotationChange)
        {
            return RotationUtil.getDirection(this.getYaw(), this.getPitch()).angle(RotationUtil.getDirection(rotationChange.getYaw(), rotationChange.getPitch()));
        }
    }

    /**
     * A singleton class to reduce the reqired {@link com.comphenix.protocol.events.PacketListener}s to a minimum.
     */
    private static class LookPacketDataUpdater extends PacketAdapter
    {
        public LookPacketDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            val user = User.safeGetUserFromPacketEvent(event);
            if (user == null) return;

            final IWrapperPlayClientLook lookWrapper = event::getPacket;

            val rotationChange = new RotationChange(lookWrapper.getYaw(), lookWrapper.getPitch());

            // Same tick -> merge
            synchronized (user.getLookPacketData().rotationChangeQueue) {
                if (rotationChange.getTime() - user.getLookPacketData().getRotationChangeQueue().head().getTime() < 55) {
                    user.getLookPacketData().getRotationChangeQueue().head().merge(rotationChange);
                } else {
                    user.getLookPacketData().getRotationChangeQueue().add(rotationChange);
                }
            }

            // Huge angle change
            // Use the queue values here to because the other ones are already updated.
            if (RotationUtil.getDirection(user.getDataMap().getFloat(DataKey.FloatKey.LAST_PACKET_YAW), user.getDataMap().getFloat(DataKey.FloatKey.LAST_PACKET_PITCH))
                            .angle(RotationUtil.getDirection(lookWrapper.getYaw(), lookWrapper.getPitch())) > 35)
            {
                user.getTimestampMap().at(TimestampKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).update();
            }

            // Update the values here so the RotationUtil calculation is functional.
            user.getDataMap().setFloat(DataKey.FloatKey.LAST_PACKET_YAW, lookWrapper.getYaw());
            user.getDataMap().setFloat(DataKey.FloatKey.LAST_PACKET_PITCH, lookWrapper.getPitch());
        }
    }
}
