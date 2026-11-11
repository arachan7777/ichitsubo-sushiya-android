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
    int result=g.serve(c,order,2);
    check(result>=12&&c.servedPieces==8,"調理大成功");
    g.minute=1080;g.advanceQuarter();
    check(g.minute==1095,"15分時間進行");
    int event=Catalog.REGULAR.length;
    g.award(event,350);
    int eventScore=g.score();
    g.ended=true;g.beginNextRun(new int[0]);
    check(g.run==2&&g.day==1&&g.yen==100000,"周回の初期化");
    check(g.relation[event]==350&&g.relation[index]==0,"特別客だけ引き継ぐ");
    check(g.score()<eventScore,"通常客はリセット");
    System.out.println("PASS: 85 fish, 41 guests, ordering, buying, timing results, relation, save state model, loop");
  }
}
