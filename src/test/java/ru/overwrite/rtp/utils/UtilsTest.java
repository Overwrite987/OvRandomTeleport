package ru.overwrite.rtp.utils;

import org.bukkit.ChatColor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UtilsTest {

    @DataProvider
    public Object[][] translateAlternateColorCodesData() {
        return new Object[][] {
                {"Test"},
                {"&aHello world"},
                {"&r"},
                {"&x&1&2&3&4&5&6Hex"}
        };
    }

    @Test(dataProvider = "translateAlternateColorCodesData")
    public void translateAlternateColorCodesTest(String input) {
        assertEquals(
                Utils.translateAlternateColorCodes('&', input),
                ChatColor.translateAlternateColorCodes('&', input)
        );
    }
}