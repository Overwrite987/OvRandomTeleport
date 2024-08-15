package ru.overwrite.rtp.utils;

import org.bukkit.ChatColor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.testng.Assert.assertEquals;

public class UtilsTest {
    @DataProvider
    public Object[][] replacePlaceholdersData() {
        return new Object[][]{
                {
                        "Hello, I'm %name%!",
                        simple("John"),
                        "Hello, I'm John!"
                }, {
                        "Your balance is %balance% and your ID is %id%.",
                        ofMap(Map.of("balance", "100", "id", "12345")),
                        "Your balance is 100 and your ID is 12345."
                }, {
                        "No placeholders% here.",
                        simple("1337"),
                        "No placeholders% here."
                }, {
                        "%invalid% placeholder here.",
                        simple(null),
                        "%invalid% placeholder here."
                }
        };
    }

    @Test(dataProvider = "replacePlaceholdersData")
    public void replacePlaceholdersTest(String input, UnaryOperator<String> placeholders, String expected) {
        assertEquals(
                Utils.replacePlaceholders(input, placeholders),
                expected
        );
    }

    private static UnaryOperator<String> simple(String value) {
        return (s) -> value;
    }

    private static UnaryOperator<String> ofMap(Map<String, String> map) {
        return map::get;
    }

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