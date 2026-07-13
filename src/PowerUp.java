/**
 * <p>The enum representing a power-up.</p>
 * Power-ups can be used by the player
 * to gain an advantage during a level, such as freezing enemies,
 * doubling points, and recovering health points.
 *
 * @author Kevin Qi
 * @author Leon Li
 * @version 1.1
 */
public enum PowerUp {
    FREEZE(1200),
    SCORE(2000),
    HEALTH(1500);

    /// The amount of points the power-up costs.
    public final int cost;

    /**
     * Creates a new PowerUp enum object.
     *
     * @param cost The cost.
     */
    PowerUp(int cost) {
        this.cost = cost;
    }
}
