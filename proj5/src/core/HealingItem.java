package core;

public final class HealingItem implements Interactable {
    private final int healAmount;

    public HealingItem(int healAmount) {
        this.healAmount = healAmount;
    }

    @Override
    public void interact(Player player) {
        int newHealth = player.getHealth() + healAmount;
        player.setHealth(Math.min(newHealth, player.getMaxHP()));
    }

    @Override
    public String hudMessage() {
        return "Warmth restored";
    }
}
