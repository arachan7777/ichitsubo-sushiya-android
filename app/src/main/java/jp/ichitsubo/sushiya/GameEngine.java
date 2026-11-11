package jp.ichitsubo.sushiya;

import java.util.Random;

/** Early vertical slice: time, season accounting, relationship math. Remaining original-spec systems not yet implemented. */
public final class GameEngine {
    private final Random rng;
    public final GameState state;
    public GameEngine(GameState state, Random rng) { this.state=state;this.rng=rng; }

    public void advanceMinutes(int minutes) {
        if(state.endedRun) return;
        for(int n=0;n<minutes;n++) {
            state.minuteOfDay++;
            if(state.minuteOfDay>=1440) { state.minuteOfDay=0; state.day++; }
            if(state.minuteOfDay==180) onThreeAm();
            if(state.minuteOfDay>=state.startMinutes && state.minuteOfDay<state.endMinutes && state.minuteOfDay%15==0)
                simulateArrivals();
            if(state.day>GameRules.DAYS_PER_RUN) {
                state.endedRun=true;
                state.lastEvent="4年間の集計："+state.score()+"点。継続営業・周回の画面は実装途中です。";
                break;
            }
        }
    }
    private void onThreeAm() {
        if(state.day>1 && (state.day-1)%GameRules.DAYS_PER_SEASON==0) {
            int upkeep=state.seasonalFixedUpkeep()+state.totalSeasonalStaffYen;
            state.yen-=upkeep;state.totalSeasonalStaffYen=0;
            state.lastEvent="季節の維持費・給与を一括精算："+upkeep+"円";
        } else { state.lastEvent="午前3時：魚屋の在庫補充（購入画面は未実装）。"; }
    }
    private void simulateArrivals() {
        int hour=state.minuteOfDay/60;
        int chance=(hour==12 || (hour>=18 && hour<=20))?40:((hour==11||hour==13||hour==17||hour==21)?32:20);
        int arrivals=0;
        for(int i=0;i<GameRules.ARRIVAL_DRAWS_PER_QUARTER_HOUR[state.shopTier];i++)
            if(rng.nextInt(100)<chance) arrivals++;
        if(arrivals>0) state.lastEvent="来店候補 "+arrivals+"人（待機列・注文連携は未実装）";
    }
    public boolean expandShop() {
        if(state.shopTier>=2) return false;
        int cost=GameRules.SHOP_EXPANSION_PRICES[state.shopTier];
        if(state.yen<cost) return false;
        // Unlock checks not wired yet: do not expose expansion as available in the app.
        return false;
    }
    public void awardRelation(int guestIndex, boolean event, int delta) {
        int[] arr=event?state.eventLevels:state.regularLevels;
        if(guestIndex<0||guestIndex>=arr.length) throw new IllegalArgumentException("不正な客番号");
        // Relation POINT tracking is not yet wired. Do not interpret delta points as levels.
        throw new UnsupportedOperationException("常連度ポイントの保存は実装途中");
    }
    public void recordShiftHours(int staffId,int hours) {
        if(staffId<0||staffId>=state.staff.length||hours<0)throw new IllegalArgumentException();
        if(state.staff[staffId]) state.totalSeasonalStaffYen+=GameRules.STAFF_HOURLY_YEN[staffId]*hours;
    }
}
