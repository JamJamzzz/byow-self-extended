package core;

public final class Coin implements Interactable {
    private final int value;

    public Coin(int value) {
        this.value = value;
    }

    @Override
    public void interact(Player player) {
        player.addMoney(value);
    }

    @Override
    public String hudMessage() {
        return "+" + value + " coin";
    }
}
