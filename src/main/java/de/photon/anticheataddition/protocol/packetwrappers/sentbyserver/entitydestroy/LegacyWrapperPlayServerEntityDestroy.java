package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.entitydestroy;

import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyWrapperPlayServerEntityDestroy extends AbstractPacket implements IWrapperServerEntityDestroy
{
    public LegacyWrapperPlayServerEntityDestroy()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public LegacyWrapperPlayServerEntityDestroy(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    public List<Integer> getEntityIDs()
    {
        return Arrays.stream(handle.getIntegerArrays().read(0)).boxed().collect(Collectors.toUnmodifiableList());
    }

    public void setEntityIds(List<Integer> value)
    {
        handle.getIntegerArrays().write(0, value.stream().mapToInt(Integer::intValue).toArray());
    }
}