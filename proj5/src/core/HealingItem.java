package core;

public class HealingItem {
    private int healAmount;

    public HealingItem(int healAmount) {
        this.healAmount = healAmount;
    }

    @Override
    public void interact(Player player) {
        // heal the player but don't go over max hp
        int newHealth = player.getHealth() + healAmount;
        player.setHealth(Math.min(newHealth, player.getMaxHP()));
    }
}
