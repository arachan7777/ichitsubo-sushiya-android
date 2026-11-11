import jp.ichitsubo.sushiya.*;
import java.util.*;

public final class SushiWorldTest {
  static void check(boolean c,String msg){if(!c)throw new AssertionError(msg);}
  public static void main(String[] args){
    check(Catalog.FISH.length==85,"85種類の寿司ネタ");
    check(Catalog.REGULAR.length==33,"通常客33人");
    check(Catalog.SPECIAL.length==8,"行事特別客8人");
    check(Catalog.fishIndex(60)>=0,"玉子のデータ");
    for(Catalog.Guest g:Catalog.REGULAR)check(Catalog.fish(g.favorite)!=null,"好物 "+g.name);
    for(Catalog.Guest g:Catalog.SPECIAL)check(Catalog.fish(g.favorite)!=null,"特別客の好物");
    SushiWorld g=new SushiWorld();
    check(g.yen==100000&&g.day==1&&g.minute==480,"初期状態");
    check(g.seats()==4&&g.fridgeCapacity()==500&&g.caseCapacity()==10,"初期の店");
    check(g.score()==7300,"初期の関係スコア");
    check(g.market(0,Catalog.fishIndex(60))==0,"玉子は無料供給");
    check(g.visit(0),"初期魚屋訪問");
    int fi=-1;for(int i=0;i<85;i++)if(g.market(0,i)>0){fi=i;break;}
    check(fi>=0,"魚屋の品揃え");
    check(g.buy(fi,1,false),"魚10貫相当を購入");
    check(g.stock[fi]==10,"仕入れ後の在庫");
    g.returnStore();
    check(g.totalStock()==10,"冷蔵庫在庫");
    int index=Catalog.guestIndex("課長代理");
    g.award(index,300);
    check(g.level(index)==4,"100ポイント毎のLv");
    check(g.introduced[Catalog.guestIndex("すずさん")],"紹介条件を満たした");
    SushiWorld.Order order=g.orderFor(index,false);
    check(order.pieces==8&&order.difficultyRank>=1&&order.difficultyRank<=5,"注文");
    SushiWorld.Customer c=new SushiWorld.Customer(index);
    g.seated.add(c);
    int result=g.serve(c,order,2);
    check(result>=12&&c.servedPieces==8&&c.bonusOmakase,"調理大成功で4貫の追加注文が成立");
    check(g.serve(c,order,2)==0,"初回注文は重複できない");
    SushiWorld.Order extra=g.orderFor(index,true);
    int extraPoints=g.serve(c,extra,1);
    check(extraPoints>=8&&c.servedPieces==12&&!c.bonusOmakase,"追加4貫で最大12貫");
    check(g.serve(c,extra,1)==0,"追加注文は重複できない");
    g.minute=1080;g.advanceQuarter();
    check(g.minute==1095,"15分時間進行");
    int event=Catalog.REGULAR.length;
    g.award(event,350);
    int eventScore=g.score();
    g.ended=true;g.beginNextRun(new int[0]);
    check(g.run==2&&g.day==1&&g.yen==100000,"周回の初期化");
    check(g.relation[event]==350&&g.relation[index]==0,"特別客だけ引き継ぐ");
    check(g.score()<eventScore,"通常客はリセット");
    // Opening-to-close gameplay including interactions and revenue.
    SushiWorld full=new SushiWorld();
    check(full.visit(0),"仕入れの訪問");
    full.returnStore();
    full.minute=1080;
    for(int t=0;t<16;t++){
      if(!full.seated.isEmpty()){
        for(SushiWorld.Customer guest:new ArrayList<>(full.seated)){
          if(guest.servedPieces==0)full.serve(guest,full.orderFor(guest.index,false),1);
          else if(guest.canAdditionalOrder||guest.bonusOmakase)
            full.serve(guest,full.orderFor(guest.index,true),1);
        }
      }
      full.advanceQuarter();
    }
    check(full.closedForDay,"18時から22時まで営業できる");
    check(full.completedOrders>0,"実際の営業中に客を迎えて寿司を売れる");
    check(full.totalSales>0,"お会計が所持金に反映される");
    full.closePayroll();
    full.nextDay();
    check(full.day==2&&full.minute==480,"翌朝の仕入れに戻る");
    for(int i=0;i<110;i++)full.nextDay();
    check(full.day==112,"4年目最終日まで進行する");
    full.nextDay();
    check(full.ended,"4年目終了時に集計される");
    full.continueEndless();
    check(full.endless&&!full.ended&&full.day==113,"無期限営業を選択できる");
    System.out.println("PASS: 85 items, 41 guests, fish purchase, interactive service, bonus order, payout, daily loop, 4-year ending, endless play");
  }
}
