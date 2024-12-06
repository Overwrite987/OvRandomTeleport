package ru.overwrite.rtp.channels.settings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.economy.PlayerPointsUtils;

public record Costs(
        Economy economy,
        MoneyType moneyType,
        double moneyCost,
        int hungerCost,
        int expCost) {

    public enum MoneyType {
        VAULT,
        PLAYERPOINTS
    }

    public boolean processMoneyCost(Player player, Channel channel) {
        if (moneyCost <= 0) {
            return true;
        }

        return switch (moneyType()) {
            case VAULT -> processVaultMoneyCost(player, channel);
            case PLAYERPOINTS -> processPlayerPointsMoneyCost(player, channel);
        };
    }

    private boolean processVaultMoneyCost(Player player, Channel channel) {
        if (economy == null) {
            return true;
        }
        if (economy.getBalance(player) < moneyCost) {
            sendNotEnoughMoneyMessage(channel, player);
            return false;
        }
        economy.withdrawPlayer(player, moneyCost);
        return true;
    }

    private boolean processPlayerPointsMoneyCost(Player player, Channel channel) {
        if (PlayerPointsUtils.getBalance(player) < moneyCost) {
            sendNotEnoughMoneyMessage(channel, player);
            return false;
        }
        PlayerPointsUtils.withdraw(player, (int) moneyCost);
        return true;
    }

    private void sendNotEnoughMoneyMessage(Channel channel, Player player) {
        Utils.sendMessage(channel.messages().notEnoughMoney().replace("%required%", Double.toString(moneyCost)), player);
    }

    public boolean processHungerCost(Player player, Channel channel) {
        if (hungerCost <= 0) {
            return true;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (player.getFoodLevel() < hungerCost) {
            Utils.sendMessage(channel.messages().notEnoughHunger().replace("%required%", Integer.toString(hungerCost)), player);
            return false;
        }
        player.setFoodLevel(player.getFoodLevel() - hungerCost);
        return true;
    }

    public boolean processExpCost(Player player, Channel channel) {
        if (expCost <= 0) {
            return true;
        }

        if (player.getTotalExperience() < expCost) {
            Utils.sendMessage(channel.messages().notEnoughExp().replace("%required%", Integer.toString(expCost)), player);
            return false;
        }
        player.setTotalExperience(player.getTotalExperience() - expCost);
        return true;
    }

    public void processMoneyReturn(Player player) {
        if (moneyCost <= 0) {
            return;
        }

        switch (moneyType()) {
            case VAULT -> processVaultMoneyReturn(player);
            case PLAYERPOINTS -> processPlayerPointsMoneyReturn(player);
        }
    }

    private void processVaultMoneyReturn(Player player) {
        if (economy != null) {
            economy.depositPlayer(player, moneyCost);
        }
    }

    private void processPlayerPointsMoneyReturn(Player player) {
        PlayerPointsUtils.deposit(player, (int) moneyCost);
    }

    public void processHungerReturn(Player player) {
        if (hungerCost > 0) {
            player.setFoodLevel(player.getFoodLevel() + hungerCost);
        }
    }

    public void processExpReturn(Player player) {
        if (expCost > 0) {
            player.setExp(player.getExp() + expCost);
        }
    }
}
