package game.core;

public class Clock {
    /** 倍速,如 0.75 / 1 / 1.5 / 2。 */
    public float speed = 1f;
    public boolean paused;

    public float scale() {
        return paused ? 0f : speed;
    }

    public void update(float rawDelta, GameState state) {
        float d = rawDelta * scale();
        if (d <= 0) return;
        state.tick(d);
    }
}
