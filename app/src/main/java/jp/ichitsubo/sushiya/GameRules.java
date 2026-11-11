package jp.ichitsubo.sushiya;

/** Approved rules; figures marked provisional in the specification stay provisional. */
public final class GameRules {
    private GameRules() {}
    public static final int DAYS_PER_SEASON = 7;
    public static final int SEASONS_PER_YEAR = 4;
    public static final int DAYS_PER_YEAR = DAYS_PER_SEASON * SEASONS_PER_YEAR;
    public static final int DAYS_PER_RUN = DAYS_PER_YEAR * 4;
    public static final int STARTING_YEN = 100_000;
    public static final int[] SHOP_SEATS = {4,8,12};
    public static final int[] QUEUE_CAPACITY = {2,4,8};
    public static final int[] ARRIVAL_DRAWS_PER_QUARTER_HOUR = {3,6,9};
    public static final int[] SEASONAL_SHOP_UPKEEP = {20_000,40_000,60_000};
    public static final int[] SHOP_EXPANSION_PRICES = {300_000,700_000};
    public static final int[] STAFF_HOURLY_YEN = {1500,1600,1700,1800};
    public static final String[] STAFF_NAMES = {"マゴちゃん","イッペー坊","お千代","たっつぁん"};
    public static final int[] RELATION_QUEUE_CHANCE = {40,55,70,85,0};
    public static final int[] RELATION_QUEUE_MINUTES = {30,40,50,60,0};
    public static final int[] FISH_CARRYING_CAPACITY = {200,400};
    public static final int[] FISH_STORES_PER_DAY = {1,2};
    public static final int DIRECT_FISHERMAN_SEASONAL_YEN = 35_000;
    public static final int EVENT_GUEST_SCORE_MULTIPLIER = 5;
    public static final int SCORE_DISPLAY_MULTIPLIER = 100;
    public static final int REGULAR_GUESTS = 33;
    public static final int EVENT_GUESTS = 8;
    public static final int MAX_RELATION_LEVEL = 10;
    public static final int MAX_SCORE = (REGULAR_GUESTS * MAX_RELATION_LEVEL + EVENT_GUESTS * MAX_RELATION_LEVEL * EVENT_GUEST_SCORE_MULTIPLIER) * SCORE_DISPLAY_MULTIPLIER;
    public static final int MAX_DECOR_CARRYOVER = 10;

    public static int queueChance(int level) { return RELATION_QUEUE_CHANCE[relationTier(level)]; }
    public static int waitMinutes(int level, boolean tatsuanEmployed) {
        int normal = RELATION_QUEUE_MINUTES[relationTier(level)];
        return tatsuanEmployed ? (int)Math.round(normal * 1.25) : normal;
    }
    public static int relationTier(int level) {
        if(level <= 2) return 0;
        if(level <= 4) return 1;
        if(level <= 6) return 2;
        if(level <= 9) return 3;
        return 4;
    }
    public static int ippePrice(int priceYen, boolean employed) {
        if (!employed) return priceYen;
        return (int)(Math.floor(priceYen * 1.05 / 10.0 + 0.5) * 10);
    }
    public static int maxDecorationsToCarry(int completedRuns) {
        return Math.min(MAX_DECOR_CARRYOVER, Math.max(0, completedRuns));
    }
    public static int calculateScore(int[] regularLevels, int[] eventLevels) {
        int raw=0;
        for (int n:regularLevels) raw += Math.max(1,Math.min(MAX_RELATION_LEVEL,n));
        for (int n:eventLevels) raw += Math.max(1,Math.min(MAX_RELATION_LEVEL,n)) * EVENT_GUEST_SCORE_MULTIPLIER;
        return raw * SCORE_DISPLAY_MULTIPLIER;
    }
}
