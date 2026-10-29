package local.tvhome;

/** What the music is doing right now, for the backdrop to move with. */
interface Pulse {
    /** Stillness, for when no music plays. */
    Pulse NONE = new Pulse() {
        @Override
        public float level(int glow) {
            return 0f;
        }

        @Override
        public float tempo() {
            return 0f;
        }

        @Override
        public float beat(long now) {
            return 0f;
        }

        @Override
        public int surges() {
            return 0;
        }

        @Override
        public float lift() {
            return 1f;
        }
    };

    /** How brightly each glow should shine, 0 (resting) to 1 (a peak); {@code glow} counts bass, mids, highs. */
    float level(int glow);

    /** The song's beats per minute, or 0 while it has no clear beat. */
    float tempo();

    /** Beats counted at {@code now}, in milliseconds; a whole number falls on the beat. */
    float beat(long now);

    /** Surges heard so far: the moments the music suddenly lifts, like a drop or the beat kicking in. */
    int surges();

    /** The bass right now against the song's usual: below 1 in a breakdown, about 1 in the groove. */
    float lift();
}
