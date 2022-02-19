package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import de.photon.aacadditionpro.util.pluginmessage.labymod.LabyModProtocol;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class LabyModSentinel extends SentinelModule implements Listener, PluginMessageListener
{
    @LoadFromConfiguration(configPath = ".TablistBanner.enabled")
    private boolean tablistBanner;
    @LoadFromConfiguration(configPath = ".TablistBanner.url")
    private String tablistBannerUrl;

    @LoadFromConfiguration(configPath = ".Voicechat")
    private boolean voicechat;

    public LabyModSentinel()
    {
        super("LabyMod");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message)
    {
        val byteBuf = Unpooled.wrappedBuffer(message);
        val key = LabyModProtocol.readString(byteBuf, Short.MAX_VALUE);
        //val json = LabyModProtocol.readString(byteBuf, Short.MAX_VALUE);

        // LabyMod user joins the server
        if ("INFO".equals(key)) {
            // Sentinel-commands
            detection(player);

            // Send permissions
            LabyModProtocol.sendPermissionMessage(player);

            // Voicechat
            if (!voicechat) LabyModProtocol.disableVoiceChat(player);
            if (tablistBanner) LabyModProtocol.sendServerBanner(player, tablistBannerUrl);
        }

        byteBuf.release();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannel(MessageChannel.LABYMOD_CHANNEL)
                           .addOutgoingMessageChannel(MessageChannel.LABYMOD_CHANNEL)
                           .build();
    }
}
