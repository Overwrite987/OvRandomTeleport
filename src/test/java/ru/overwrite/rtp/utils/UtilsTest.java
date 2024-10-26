package ru.overwrite.rtp.utils;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UtilsTest {

    @DataProvider
    public Object[][] translateAlternateColorCodesData() {
        return new Object[][]{
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

    @DataProvider
    public Object[][] replaceEachData() {
        return new Object[][]{
                {"Test", new String[]{"Test"}, new String[]{"Tset"}},
                {"Hello World", new String[]{"Hello"}, new String[]{"Bye"}},
                {"CaSe TeSt", new String[]{"CaSe", "tEsT"}, new String[]{"cAsE", "FaIl"}},
                {"Multiple things things things things to replace", new String[]{"things"}, new String[]{"another_things"}}
        };
    }

    @Test(dataProvider = "replaceEachData")
    public void replaceEachTest(String input, String[] searchList, String[] replacementList) {
        assertEquals(
                Utils.replaceEach(input, searchList, replacementList),
                replaceNormal(input, searchList, replacementList)
        );
    }

    private static String replaceNormal(@NotNull String text, @NotNull String[] searchList, @NotNull String[] replacementList) {
        if (text.isEmpty() || searchList.length == 0 || replacementList.length == 0) {
            return text;
        }

        if (searchList.length != replacementList.length) {
            throw new IllegalArgumentException("Search and replacement arrays must have the same length.");
        }

        for (int i = 0; i < searchList.length; i++) {
            text = text.replace(searchList[i], replacementList[i]);
        }

        return text;
    }
}