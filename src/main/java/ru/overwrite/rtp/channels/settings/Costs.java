package ru.overwrite.rtp.channels.settings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.PlayerPointsUtils;
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
            case VAULT -> {
                if (economy == null) {
                    yield false;
                }
                if (economy.getBalance(p) < moneyCost) {
                    Utils.sendMessage(channel.getMessages().notEnoughMoneyMessage().replace("%required%", Double.toString(moneyCost)), p);
                    yield false;
                }
                economy.withdrawPlayer(p, moneyCost);
                yield true;
            }
            case PLAYERPOINTS -> {
                if (PlayerPointsUtils.getBalance(p) < moneyCost) {
                    Utils.sendMessage(channel.getMessages().notEnoughMoneyMessage().replace("%required%", Double.toString(moneyCost)), p);
                    yield false;
                }
                PlayerPointsUtils.withdraw(p, (int) moneyCost);
                yield true;
            }
        };
    }

    public boolean processHungerCost(Player p, Channel channel) {
        if (hungerCost <= 0) return true;

        if (p.getFoodLevel() < hungerCost) {
            Utils.sendMessage(channel.getMessages().notEnoughHungerMessage().replace("%required%", Integer.toString(hungerCost)), p);
            return false;
        }
        p.setFoodLevel(p.getFoodLevel() - hungerCost);
        return true;
    }

    public boolean processExpCost(Player p, Channel channel) {
        if (expCost <= 0) return true;

        if (p.getExp() < expCost) {
            Utils.sendMessage(channel.getMessages().notEnoughExpMessage().replace("%required%", Float.toString(expCost)), p);
            return false;
        }
        p.setExp(p.getExp() - expCost);
        return true;
    }

    public void processMoneyReturn(Player p) {
        if (moneyCost <= 0) return;

        switch (moneyType()) {
            case VAULT -> {
                if (economy != null) {
                    economy.depositPlayer(p, moneyCost);
                }
            }
            case PLAYERPOINTS -> PlayerPointsUtils.deposit(p, (int) moneyCost);
        }
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
