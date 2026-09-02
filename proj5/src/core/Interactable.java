package core;

/** Something the player can pick up / trigger by walking onto it. */
public interface Interactable {
    /** Applies this item's effect to the player. */
    void interact(Player player);

    /** Short HUD message describing what just happened. */
    String hudMessage();
}
