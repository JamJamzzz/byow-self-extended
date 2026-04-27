package core;

public class HealingItem implements Interactable {
    private int healAmount;

    public HealingItem(int healAmount) {
        this.healAmount = healAmount;
    }

    @Override
    public void interact(Player player) {
        int newHealth = player.getHealth() + healAmount;
        player.setHealth(Math.min(newHealth, player.getMaxHP()));
    }
}