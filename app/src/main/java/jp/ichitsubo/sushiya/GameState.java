package jp.ichitsubo.sushiya;

import java.util.Arrays;

/** Serializable game state. Keep state separate from screen rendering. */
public final class GameState {
    public int run = 1;
    public int day = 1;
    public int minuteOfDay = 3 * 60;
    public int yen = GameRules.STARTING_YEN;
    public int shopTier = 0;
    public int fridgeTier = 0;
    public int fishCaseTier = 0;
    public int totalSeasonalStaffYen = 0;
    public int startMinutes = 18 * 60;
    public int endMinutes = 22 * 60;
    public boolean fishermanSubscribed = false;
    public boolean endedRun = false;
    public int[] regularLevels = new int[GameRules.REGULAR_GUESTS];
    public int[] eventLevels = new int[GameRules.EVENT_GUESTS];
    public boolean[] staff = new boolean[GameRules.STAFF_NAMES.length];
    public String lastEvent = "開店準備中。";

    public GameState() { Arrays.fill(regularLevels,1); Arrays.fill(eventLevels,1); }
    public int seats() { return GameRules.SHOP_SEATS[shopTier]; }
    public int maxStaff() { return shopTier; }
    public int seasonalFixedUpkeep() { return GameRules.SEASONAL_SHOP_UPKEEP[shopTier] + fridgeUpkeep() + fishCaseUpkeep(); }
    public int fridgeUpkeep() { return fridgeTier * 5_000; }
    public int fishCaseUpkeep() { return fishCaseTier * 5_000; }
    public int dateYear() { return (day-1)/GameRules.DAYS_PER_YEAR+1; }
    public int dateSeason() { return ((day-1)/GameRules.DAYS_PER_SEASON)%GameRules.SEASONS_PER_YEAR; }
    public int dayOfSeason() { return (day-1)%GameRules.DAYS_PER_SEASON+1; }
    public String dateLabel() { return dateYear()+"年目 "+new String[]{"春","夏","秋","冬"}[dateSeason()]+" "+dayOfSeason()+"日目"; }
    public int score() { return GameRules.calculateScore(regularLevels,eventLevels); }
    public void nextRun(int[] selectedDecorItems) {
        if (!endedRun) throw new IllegalStateException("4年の集計前には周回できません");
        int previousRun=run;
        if(selectedDecorItems.length > GameRules.maxDecorationsToCarry(previousRun))
            throw new IllegalArgumentException("装飾品持ち込み上限超過");
        int[] persistentEventLevels=eventLevels.clone();
        GameState fresh=new GameState();
        run=previousRun+1; day=fresh.day; minuteOfDay=fresh.minuteOfDay; yen=fresh.yen;
        shopTier=0; fridgeTier=0; fishCaseTier=0; totalSeasonalStaffYen=0;
        startMinutes=fresh.startMinutes; endMinutes=fresh.endMinutes;
        fishermanSubscribed=false; endedRun=false;
        regularLevels=fresh.regularLevels; eventLevels=persistentEventLevels; staff=fresh.staff;
        lastEvent="周回を開始。イベント限定客の常連度は引き継ぎました。";
        // Selected decoration IDs are supplied by a future full decoration inventory implementation.
    }
}
