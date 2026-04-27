package core;

public class Coin implements Interactable {
    private int value;

    public Coin(int value) {
        this.value = value;
    }

    @Override
    public void interact(Player player) {
        player.addMoney(value);
    }
}
