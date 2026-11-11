import jp.ichitsubo.sushiya.*;
import java.util.Random;
public final class GameRulesTest {
  static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
  public static void main(String[] args){
    GameState s=new GameState();
    check(s.yen==100000,"初期所持金");
    check(s.seats()==4,"初期座席");
    check(GameRules.MAX_SCORE==73000,"理論上限");
    check(s.score()==7300,"全員Lv1の開始スコア");
    check(GameRules.ippePrice(300,true)==320,"イッペー坊の10円単位端数処理");
    check(GameRules.ippePrice(350,true)==370,"350円の補正");
    check(GameRules.waitMinutes(7,true)==75,"たっつぁんの1.25倍");
    check(GameRules.queueChance(10)==0,"もはや家族は並ばない");
    check(GameRules.maxDecorationsToCarry(1)==1,"2周目は1個");
    check(GameRules.maxDecorationsToCarry(12)==10,"最大10個");
    GameEngine e=new GameEngine(s,new Random(123));
    e.advanceMinutes(1440);
    check(s.day==2,"24時間で翌日");
    e.advanceMinutes(6*1440);
    check(s.day==8,"7日経過");
    check(s.yen==80000,"季節基本維持費の請求");
    s.endedRun=true;s.eventLevels[0]=6;s.regularLevels[0]=7;
    s.nextRun(new int[]{1});
    check(s.run==2&&s.eventLevels[0]==6&&s.regularLevels[0]==1&&s.yen==100000,"周回引継ぎ");
    System.out.println("PASS: GameRules/GameState/GameEngine base tests");
  }
}
