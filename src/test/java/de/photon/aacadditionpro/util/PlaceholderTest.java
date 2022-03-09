package de.photon.aacadditionpro.util;

import de.photon.aacadditionpro.Dummy;
import de.photon.aacadditionpro.util.execute.Placeholders;
import lombok.val;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlaceholderTest
{
    @Test
    void noPlaceholder()
    {
        val empty = "";
        val player = Dummy.mockPlayer();
        Assertions.assertEquals(empty, Placeholders.replacePlaceholders(empty, player));

        val string = "Some Spigot";
        Assertions.assertEquals(string, Placeholders.replacePlaceholders(string, player));

        val color = ChatColor.translateAlternateColorCodes('&', "&4Some Spigot") + ChatColor.RESET;
        Assertions.assertEquals(color, Placeholders.replacePlaceholders(color, player));
    }
}
