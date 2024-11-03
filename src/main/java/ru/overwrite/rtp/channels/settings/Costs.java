package ru.overwrite.rtp.channels.settings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.economy.PlayerPointsUtils;
import ru.overwrite.rtp.utils.Utils;

public record Costs(
        Economy economy,
        MoneyType moneyType,
        double moneyCost,
        int hungerCost,
        float expCost) {

    public enum MoneyType {
        VAULT,
        PLAYERPOINTS
    }

    public boolean processMoneyCost(Player p, Channel channel) {
        if (moneyCost <= 0) return true;

        return switch (moneyType()) {
            case VAULT -> processVaultMoneyCost(p, channel);
            case PLAYERPOINTS -> processPlayerPointsMoneyCost(p, channel);
        };
    }

    private boolean processVaultMoneyCost(Player p, Channel channel) {
        if (economy == null || economy.getBalance(p) < moneyCost) {
            sendNotEnoughMoneyMessage(channel, p);
            return false;
        }
        economy.withdrawPlayer(p, moneyCost);
        return true;
    }

    private boolean processPlayerPointsMoneyCost(Player p, Channel channel) {
        if (PlayerPointsUtils.getBalance(p) < moneyCost) {
            sendNotEnoughMoneyMessage(channel, p);
            return false;
        }
        PlayerPointsUtils.withdraw(p, (int) moneyCost);
        return true;
    }

    private void sendNotEnoughMoneyMessage(Channel channel, Player p) {
        Utils.sendMessage(channel.messages().notEnoughMoney().replace("%required%", Double.toString(moneyCost)), p);
    }

    public boolean processHungerCost(Player p, Channel channel) {
        if (hungerCost <= 0) return true;

        if (p.getFoodLevel() < hungerCost) {
            Utils.sendMessage(channel.messages().notEnoughHunger().replace("%required%", Integer.toString(hungerCost)), p);
            return false;
        }
        p.setFoodLevel(p.getFoodLevel() - hungerCost);
        return true;
    }

    public boolean processExpCost(Player p, Channel channel) {
        if (expCost <= 0) return true;

        if (p.getExp() < expCost) {
            Utils.sendMessage(channel.messages().notEnoughExp().replace("%required%", Float.toString(expCost)), p);
            return false;
        }
        p.setExp(p.getExp() - expCost);
        return true;
    }

    public void processMoneyReturn(Player p) {
        if (moneyCost <= 0) return;

        switch (moneyType()) {
            case VAULT -> processVaultMoneyReturn(p);
            case PLAYERPOINTS -> processPlayerPointsMoneyReturn(p);
        }
    }

    private void processVaultMoneyReturn(Player p) {
        if (economy != null) {
            economy.depositPlayer(p, moneyCost);
        }
    }

    private void processPlayerPointsMoneyReturn(Player p) {
        PlayerPointsUtils.deposit(p, (int) moneyCost);
    }

    public void processHungerReturn(Player p) {
        if (hungerCost > 0) {
            p.setFoodLevel(p.getFoodLevel() + hungerCost);
        }
    }

    public void processExpReturn(Player p) {
        if (expCost > 0) {
            p.setExp(p.getExp() + expCost);
        }
    }
}
