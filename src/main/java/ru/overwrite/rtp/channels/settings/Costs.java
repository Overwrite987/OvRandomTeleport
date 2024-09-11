package ru.overwrite.rtp.channels.settings;

public record Costs(
        MoneyType moneyType,
        double moneyCost,
        int hungerCost,
        float expCost) {

    public enum MoneyType {
        VAULT,
        PLAYERPOINTS
    }
}
