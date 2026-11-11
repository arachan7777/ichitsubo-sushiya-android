package jp.ichitsubo.sushiya;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Offline game simulation. All state is serializable via the Android presentation layer.
 * Values not formally finalized are kept in clearly marked provisional algorithms.
 */
public final class SushiWorld {
    public static final int DAYS_PER_SEASON=7, DAYS_PER_YEAR=28, RUN_DAYS=112;
    public static final String[] SEASONS={"春","夏","秋","冬"};
    public static final String[] WEEKDAYS={"月","火","水","木","金","土","日"};
    public static final int[] SEATS={4,8,12}, QUEUE_LIMIT={2,4,8}, ARRIVAL_DRAWS={3,6,9};
    public static final int[] UPKEEP={20000,40000,60000};
    public static final int[] FRIDGE_MAX={500,1000,2000,50000}, CASE_MAX={10,20,50,200};
    public static final String[] STORE_NAMES={"サブちゃんの魚屋","ジローの貝・甲殻類","イチローの高級魚","チチローの目利き"};
    public static final int[] HOURLY={1500,1600,1700,1800};
    public static final String[] STAFF_NAMES={"マゴちゃん","イッペー坊","お千代","たっつぁん"};
    public static final String[] CONTEST_NAMES={"町内寿司屋腕自慢大会","市内寿司職人選手権","県南寿司職人大会","県寿司職人大会"};
    public static final int[] CONTEST_FEES={2000,5000,10000,20000};
    public static final int[] CONTEST_PASS={40,40,40,44};
    public static final int[][] CONTEST_RANKS={{1,2,2,3},{2,3,3,4},{3,4,4,5},{5,5,5,5}};
    public static final int[] QUEUE_PCT={40,55,70,85,0};
    public static final int[] QUEUE_WAIT={30,40,50,60,0};
    public static final int[] RELATION_EXTENDS={12,15,20,24,30}; // 19.5 rounded only in drawing; precise formula below
    public static final int[][] FESTIVAL_DAYS={{1,6},{2,7},{3,5},{4,6}};
    public static final int GUEST_COUNT=33, EVENT_COUNT=8;
    public static final int[] PROMOTION_STAFF_LV={6,5,6,5};
    public int run=1,day=1, minute=480, yen=100000,shopTier=0,fridgeTier=0,caseTier=0;
    public int visitCount=0,carrying=0, currentStore=-1, seasonPayroll=0,restaurantStart=1080,restaurantEnd=1320;
    public int totalSales=0, completedOrders=0, eventVisitDay=0, contestEntry=-1,contestRound=0,contestScore=0;
    public int fishermanPaidSeason=-1, fishermanUnlocked=0, wonContests=0;
    public boolean closedForDay=false, ended=false, endless=false, festivalQueued=false, hasTruck=false;
    public int[] relation=new int[41],stock=new int[85],marketStock=new int[4*85];
    public boolean[] introduced=new boolean[33],caseActive=new boolean[85],staffEmployed=new boolean[4];
    public boolean[] receivedDecor=new boolean[13], equippedDecor=new boolean[13];
    public int[] decorCarried=new int[13];
    public List<Customer> seated=new ArrayList<>(),queue=new ArrayList<>();
    public String log="横丁に小さな寿司屋が開店した。";
    public long seed=4;
    public static final class Customer {
        public int index,wait,stay,charge,servedPieces; // index 0..32 normal, 33..40 event
        public boolean extra,extended,canAdditionalOrder,bonusOmakase;
        public int leaveAt=30;
        public Customer(int i){index=i;}
        public Customer(){}
        public String name(){return index<33?Catalog.REGULAR[index].name:Catalog.SPECIAL[index-33].name;}
    }
    public SushiWorld(){
        for(int i=0;i<10;i++)introduced[i]=true;
        int egg=Catalog.fishIndex(60); if(egg>=0)caseActive[egg]=true;
        refreshMarkets();
    }
    public int season(){return ((day-1)/7)%4;}
    public int year(){return (day-1)/28+1;}
    public int weekday(){return (day-1)%7;}
    public String date(){return year()+"年 "+SEASONS[season()]+" "+((day-1)%7+1)+"日("+WEEKDAYS[weekday()]+")";}
    public String time(){return String.format(java.util.Locale.JAPAN,"%02d:%02d",minute/60,minute%60);}
    public int level(int idx){return Math.min(10,Math.max(1,relation[idx]/100+1));}
    public String tier(int idx){int lv=level(idx);return lv<3?"一見さん":lv<5?"顔見知り":lv<7?"お得意様":lv<10?"常連":"もはや家族";}
    public int score(){int x=0;for(int i=0;i<41;i++)x+=level(i)*(i>=33?5:1);return x*100;}
    public int fridgeCapacity(){return FRIDGE_MAX[fridgeTier];}
    public int caseCapacity(){return CASE_MAX[caseTier];}
    public int totalStock(){int sum=0;for(int n:stock)sum+=n;return sum;}
    public int visibleCaseCount(){int n=0;for(boolean b:caseActive)if(b)n++;return n;}
    public int seats(){return SEATS[shopTier];}
    public int queueCapacity(){return QUEUE_LIMIT[shopTier];}
    public int maxStaff(){return shopTier;}
    public boolean working(int id){return staffEmployed[id];}
    public int totalStaff(){int n=0;for(boolean b:staffEmployed)if(b)n++;return n;}
    public int fixedUpkeep(){return UPKEEP[shopTier]+fridgeTier*5000+caseTier*5000;}
    public int fishPrice(Catalog.Fish f){return f.price;}
    public int sellingPrice(Catalog.Fish f){return working(1)?(int)(Math.floor(f.price*1.05/10+.5)*10):f.price;}
    public int buyPrice(Catalog.Fish f){ // Seasonal rounding awaits tuning.
        return f.inSeason(season())?(int)Math.round(f.buy*.8):f.buy;
    }
    public int market(int store,int fish){return marketStock[store*85+fish];}
    public boolean hasStore(int store){
        if(store==0)return true;
        return introduced[Catalog.guestIndex(store==1?"ジロー":store==2?"イチロー":"チチロー")];
    }
    public boolean deliveryUnlocked(int store){
        String who=store==0?"サブちゃん":store==1?"ジロー":store==2?"イチロー":"チチロー";
        int id=Catalog.guestIndex(who);return id>=0&&level(id)>=5;
    }
    private Random random(long salt){return new Random((day*857315L+run*1729L+salt*101L+seed)*37L);}
    public void refreshMarkets(){
        Arrays.fill(marketStock,0);
        for(int m=0;m<4;m++){
            Random r=random(910+m);List<Integer> pool=new ArrayList<>();
            for(int i=0;i<Catalog.FISH.length;i++){
                Catalog.Fish f=Catalog.FISH[i]; if(f.number==60)continue;
                boolean okay=m==0?f.rank<=3:m==1?(
                        Arrays.asList("ホタテ","つぶ貝","ホッキ貝","青柳","赤貝","トリ貝","煮ハマグリ","ミル貝","アワビ","赤エビ","甘エビ","車エビ","ボタンエビ","シャコ").contains(f.name))
                        :m==2?f.rank>=4&&!f.name.endsWith("巻"):true;
                if(okay)pool.add(i);
            }
            int count=m==0?15+r.nextInt(11):m==1?6+r.nextInt(3):m==2?10:30;
            for(int k=0;k<count&&!pool.isEmpty();k++){
                int total=0;for(int index:pool)total+=Catalog.FISH[index].inSeason(season())?2:1;
                int pick=r.nextInt(total),chosen=0;
                for(int q=0;q<pool.size();q++){pick-=Catalog.FISH[pool.get(q)].inSeason(season())?2:1;if(pick<0){chosen=q;break;}}
                int i=pool.remove(chosen);marketStock[m*85+i]=200;
            }
        }
    }
    public boolean visit(int store){
        if(store<0||store>=4||!hasStore(store)||visitCount>=(hasTruck?2:1))return false;
        if(minute<480||minute>=600||closedForDay)return false;
        if(currentStore>=0)return false;
        currentStore=store;visitCount++;log=STORE_NAMES[store]+"に到着。仕入れる魚を選べる。";return true;
    }
    public void returnStore(){
        if(currentStore<0)return;
        currentStore=-1;log="魚屋から戻った。購入済みの配達品も到着した。";
    }
    public boolean buy(int fishIndex,int units,boolean delivery){
        if(currentStore<0||fishIndex<0||fishIndex>=85||units<1)return false;
        if(market(currentStore,fishIndex)<10*units)return false;
        if(delivery&&!deliveryUnlocked(currentStore))return false;
        if(totalStock()+10*units>fridgeCapacity())return false;
        if(!delivery&&carrying+10*units>(hasTruck?400:200))return false;
        Catalog.Fish f=Catalog.FISH[fishIndex];int cost=buyPrice(f)*units;
        if(yen<cost)return false;
        yen-=cost;stock[fishIndex]+=10*units;marketStock[currentStore*85+fishIndex]-=10*units;
        if(!delivery)carrying+=10*units;
        if(visibleCaseCount()<caseCapacity())caseActive[fishIndex]=true;
        log=f.name+"を"+(10*units)+"貫相当仕入れた。";return true;
    }
    public boolean toggleCase(int fishIndex){
        if(fishIndex<0||fishIndex>=85||Catalog.FISH[fishIndex].number==60)return false;
        if(!caseActive[fishIndex]&&visibleCaseCount()>=caseCapacity())return false;
        caseActive[fishIndex]=!caseActive[fishIndex];return true;
    }
    public boolean discardFish(int fishIndex,int amount){
        if(fishIndex<0||fishIndex>=85||amount<1||stock[fishIndex]<amount)return false;
        stock[fishIndex]-=amount;return true;
    }
    public void award(int index,int points){
        if(index<0||index>=41)return;
        int before=level(index);
        if(before==10&&points<0) return;
        relation[index]=Math.max(0,relation[index]+points);
        if(level(index)>before){String name=guest(index).name;log=name+"との関係がLv"+level(index)+"になった。";
            unlockIntroductions(index);
            if(index==Catalog.guestIndex("課長代理")&&level(index)>=7)log+=" 3坪の拡張条件を達成。";
            if(index==Catalog.guestIndex("タバちゃん")&&level(index)>=8)log+=" 5坪の拡張条件を達成。";
            if(index==Catalog.guestIndex("ヴェンさん")&&level(index)>=7)hasTruck=true; // Provisional reward Lv to be revised to source event.
        }
    }
    private void unlockIntroductions(int source){
        for(int i=10;i<33;i++){
            Catalog.Guest g=Catalog.REGULAR[i];
            if(!introduced[i]&&g.source==source&&level(source)>=g.sourceLevel){
                introduced[i]=true;log+=" "+g.name+"が紹介で来店候補に加わった。";
            }
        }
    }
    public Catalog.Guest guest(int idx){return idx<33?Catalog.REGULAR[idx]:Catalog.SPECIAL[idx-33];}
    public int favouriteIndex(int idx){return Catalog.fishIndex(guest(idx).favorite);}
    public List<Integer> availableFish(){
        List<Integer> result=new ArrayList<>();
        for(int i=0;i<85;i++)if(caseActive[i]&&(stock[i]>0||Catalog.FISH[i].number==60))result.add(i);
        return result;
    }
    public int availableForOrder(int index){
        if(index<0||index>=85)return 0;
        return Catalog.FISH[index].number==60?1000:stock[index];
    }
    public static final class Order {
        public int customer, pieces=8;
        public int[] fish=new int[4], count=new int[4];
        public boolean omakase, substitute;
        public int difficultyRank=1;
        public String summary(){
            StringBuilder b=new StringBuilder();
            for(int i=0;i<fish.length;i++)if(count[i]>0){
                if(b.length()>0)b.append(" ／ ");
                b.append(Catalog.FISH[fish[i]].name).append(count[i]).append("貫");
            }
            return b.toString();
        }
    }
    public Order orderFor(int customer,boolean omakase){
        Order o=new Order();o.customer=customer;o.omakase=omakase;o.pieces=omakase?4:8;
        List<Integer> ready=availableFish();
        if(ready.isEmpty())ready.add(Catalog.fishIndex(60));
        Random r=random(customer*17+minute+ (omakase?999:0));
        int favorite=favouriteIndex(customer);
        List<Integer> chosen=new ArrayList<>();
        if(favorite>=0&&ready.contains(favorite)&&availableForOrder(favorite)>0)chosen.add(favorite);
        Collections.shuffle(ready,r);
        for(int k:ready)if(!chosen.contains(k)&&chosen.size()<4)chosen.add(k);
        // Default order: 4 kinds at most, adjusted by currently available stock
        int remains=o.pieces;
        for(int i=0;i<chosen.size()&&remains>0;i++){
            int k=chosen.get(i);
            int n=Math.min(availableForOrder(k),Math.min(remains,Math.max(1,(int)Math.ceil(remains/(double)(chosen.size()-i)))));
            if(n<=0)continue;
            o.fish[i]=k;o.count[i]=n;remains-=n;
        }
        if(remains>0){int egg=Catalog.fishIndex(60);for(int j=0;j<4;j++)if(o.count[j]==0){o.fish[j]=egg;o.count[j]=remains;remains=0;break;}}
        int weighted=0,sum=0;for(int i=0;i<4;i++)if(o.count[i]>0){weighted+=Catalog.FISH[o.fish[i]].rank*o.count[i];sum+=o.count[i];}
        o.difficultyRank=Math.max(1,(int)Math.round((double)weighted/Math.max(1,sum)));
        return o;
    }
    public boolean canServe(Customer c){return c!=null&&c.servedPieces==0;}
    public int serve(Customer c,Order o,int result){
        if(c==null||o==null||result<0||result>2||!seated.contains(c))return 0;
        if(o.omakase){
            if(c.servedPieces!=8||!c.bonusOmakase&&!c.canAdditionalOrder)return 0;
        }else if(c.servedPieces!=0)return 0;
        if(c.servedPieces+(o.omakase?4:8)>12)return 0;
        int pieces=0,cost=0; boolean fav=false;
        for(int i=0;i<4;i++){
            if(o.count[i]<=0)continue;int fi=o.fish[i];int n=o.count[i];
            if(availableForOrder(fi)<n)return 0;
            if(Catalog.FISH[fi].number!=60)stock[fi]-=n;
            cost+=sellingPrice(Catalog.FISH[fi])*n;
            pieces+=n;
            if(Catalog.FISH[fi].number==guest(c.index).favorite)fav=true;
        }
        c.servedPieces+=pieces;
        if(o.omakase){c.bonusOmakase=false;c.canAdditionalOrder=false;}
        else if(result==2&&pieces==8)c.bonusOmakase=true;
        // Success 2=great,1=good,0=bad.
        int points=result==2?12:result==1?8:-4;
        if(fav&&result>0&&!o.omakase)points+=2;
        award(c.index,points);
        c.charge+=cost;
        completedOrders++;
        log=c.name()+"に"+o.summary()+"を提供。"+(result==2?"大成功！":result==1?"成功。":"失敗…");
        if(o.omakase&&c.extended&&c.leaveAt==45&&c.stay>=45)checkout(c);
        return points;
    }
    private boolean present(int id){for(Customer c:seated)if(c.index==id)return true;for(Customer c:queue)if(c.index==id)return true;return false;}
    private int weightedGuest(Random r){
        List<Integer> pool=new ArrayList<>();List<Integer> weights=new ArrayList<>();int total=0;
        for(int i=0;i<33;i++){
            if(!introduced[i]||present(i))continue;
            if(isStaffGuestExcluded(i))continue;
            Catalog.Guest g=Catalog.REGULAR[i];
            int weight=(g.weekdayMask&(1<<weekday()))!=0?30:10;
            weight*=new int[]{10,11,12,14,16}[tierIndex(level(i))];
            if(isStaffCandidate(i))weight*=2; // Provisional bias, exact value unsettled.
            pool.add(i);weights.add(weight);total+=weight;
        }
        if(total==0)return -1;
        int pick=r.nextInt(total);
        for(int i=0;i<pool.size();i++){pick-=weights.get(i);if(pick<0)return pool.get(i);}
        return pool.get(pool.size()-1);
    }
    public static int tierIndex(int lv){return lv<3?0:lv<5?1:lv<7?2:lv<10?3:4;}
    private boolean isStaffCandidate(int guestId){
        for(String s:STAFF_NAMES)if(Catalog.REGULAR[guestId].name.equals(s))return true;return false;
    }
    private boolean isStaffGuestExcluded(int guestId){
        for(int j=0;j<4;j++)if(staffEmployed[j]&&Catalog.REGULAR[guestId].name.equals(STAFF_NAMES[j]))return true;return false;
    }
    private void admit(int id,Random r){
        Customer c=new Customer(id);
        if(seated.size()<seats()){seated.add(c);if(working(0))award(id,1);log=c.name()+"が来店。";}
        else {
            int tier=tierIndex(level(id));
            if(tier==4){log=c.name()+"は満席を見て、ほかの客を気遣って帰った。";return;}
            if(queue.size()>=queueCapacity()||r.nextInt(100)>=QUEUE_PCT[tier]){log=c.name()+"は満席のため帰った。";return;}
            c.wait=working(3)?(int)Math.round(QUEUE_WAIT[tier]*1.25):QUEUE_WAIT[tier];
            queue.add(c);log=c.name()+"が行列に並んだ。";
        }
    }
    private int arrivalChance(){
        int h=minute/60;
        return h==12||h>=18&&h<21?40:h==11||h==13||h==17||h==21?32:20;
    }
    private void maybeFestival(Random r){
        if(eventVisitDay==day)return;
        int sd=(day-1)%7+1,ss=season();
        for(int i=0;i<8;i++){
            if(i/2==ss&&sd==FESTIVAL_DAYS[ss][i%2]){
                eventVisitDay=day;admit(33+i,r);
                log=Catalog.SPECIAL[i].festival+"。"+Catalog.SPECIAL[i].name+"が特別に訪れた。";
                return;
            }
        }
    }
    private void checkout(Customer c){
        if(!seated.remove(c))return;
        yen+=c.charge;totalSales+=c.charge;
        log=c.name()+"がお会計 "+c.charge+"円。";
    }
    private void fifteenMinutes(){
        Random r=random(minute*9);
        for(Customer c:new ArrayList<>(seated)){
            c.stay+=15;
            if(c.stay==30){
                double base=new double[]{.12,.15,.195,.24,.30}[tierIndex(level(c.index))];
                c.extended=r.nextDouble()<base;
                if(c.extended){
                    c.leaveAt=r.nextBoolean()?45:60;
                    log=c.name()+"がもう少し店にいることにした。";
                }else c.leaveAt=30;
            }
            if(c.stay==45&&c.extended&&c.servedPieces==8&&!c.bonusOmakase)
                c.canAdditionalOrder=true;
            if(c.stay>=c.leaveAt&&!(c.stay==45&&c.canAdditionalOrder)){
                checkout(c);
            }
        }
        for(Customer q:new ArrayList<>(queue)){
            q.wait-=15;
            if(q.wait<=0){queue.remove(q);log=q.name()+"は待ちきれず帰った。";}
        }
        while(seated.size()<seats()&&!queue.isEmpty()){
            Customer c=queue.remove(0);c.stay=0;seated.add(c);
            if(working(0))award(c.index,1);
        }
        if(minute<restaurantStart||minute>=restaurantEnd)return;
        maybeFestival(r);
        for(int i=0;i<ARRIVAL_DRAWS[shopTier];i++)if(r.nextInt(100)<arrivalChance()){
            int next=weightedGuest(r);if(next>=0)admit(next,r);
        }
    }
    public void advanceQuarter(){
        if(ended||currentStore>=0)return;
        if(minute>=restaurantEnd){log="営業時間が終了した。翌日の仕入れと営業に進める。";return;}
        if(minute<restaurantStart)minute=restaurantStart;
        else minute+=15;
        if(minute<restaurantEnd)fifteenMinutes();
        else {
            for(Customer c:new ArrayList<>(seated))checkout(c);
            queue.clear();closedForDay=true;log="本日の営業を終了した。";
        }
    }
    public void nextDay(){
        if(currentStore>=0)return;
        if(day>=RUN_DAYS&&!endless){ended=true;log="4年の集計 "+String.format(java.util.Locale.JAPAN,"%,d",score())+"点";return;}
        if(day%7==0){Arrays.fill(stock,0);int fee=fixedUpkeep()+seasonPayroll;yen-=fee;seasonPayroll=0;log="季節の維持費・給与 "+fee+"円を精算。";}
        day++;minute=480;visitCount=0;carrying=0;currentStore=-1;closedForDay=false;eventVisitDay=0;
        seated.clear();queue.clear();contestEntry=-1;contestRound=0;contestScore=0;
        refreshMarkets();deliverFish();
        log+=" "+date()+"。朝の仕入れが始まる。";
    }
    private void deliverFish(){
        int seasonNo=(day-1)/7;
        if(fishermanPaidSeason!=seasonNo)return;
        Random r=random(9880);int pct=r.nextInt(100);
        if(pct>=90){log="漁師からの直送は不漁で届かなかった。";return;}
        int target=pct<40?3:pct<75?4:5;
        List<Integer> pool=new ArrayList<>();
        for(int i=0;i<85;i++)if(Catalog.FISH[i].rank==target)pool.add(i);
        Collections.shuffle(pool,r);
        int species=Math.min(pool.size(),2+r.nextInt(3)),quantity=100+r.nextInt(51);
        for(int k=0;k<species;k++){
            int index=pool.get(k),amount=(k==species-1?quantity:Math.max(10,quantity/(species-k)/10*10));
            amount=Math.min(amount,fridgeCapacity()-totalStock());if(amount<=0)break;
            stock[index]+=amount;quantity-=amount;
        }
        log="漁師直送の魚が午前3時に届いた。";
    }
    public boolean contractFisherman(){
        int id=Catalog.guestIndex("潮さん");
        if(id<0||!introduced[id]||level(id)<5||yen<35000)return false; // unlocking event detail provisional.
        int s=(day-1)/7;if(fishermanPaidSeason==s)return false;
        yen-=35000;fishermanPaidSeason=s;log="漁師直送を今季35,000円で契約した。";return true;
    }
    public boolean canExpand(){
        if(shopTier>=2)return false;
        int id=Catalog.guestIndex(shopTier==0?"課長代理":"タバちゃん");
        return level(id)>=(shopTier==0?7:8)&&yen>=(shopTier==0?300000:700000);
    }
    public boolean expand(){
        if(!canExpand())return false;
        yen-=shopTier==0?300000:700000;shopTier++;
        log=shopTier==1?"旧蕎麦屋を譲り受け、3坪の店になった。":"タバコ屋跡へ広がり、5坪の店になった。";
        return true;
    }
    public boolean toggleStaff(int id){
        if(id<0||id>=4||minute>=restaurantStart||currentStore>=0)return false;
        int guest=Catalog.guestIndex(STAFF_NAMES[id]);
        if(!introduced[guest]||level(guest)<PROMOTION_STAFF_LV[id])return false;
        if(!staffEmployed[id]&&totalStaff()>=maxStaff())return false;
        staffEmployed[id]=!staffEmployed[id];return true;
    }
    public void closePayroll(){
        int hours=Math.max(0,(restaurantEnd-restaurantStart)/60);
        for(int i=0;i<4;i++)if(staffEmployed[i])seasonPayroll+=hours*HOURLY[i];
    }
    public boolean joinContest(){
        if(contestEntry>=0||wonContests>=4||day%7!=0||minute>=720||yen<CONTEST_FEES[wonContests])return false;
        yen-=CONTEST_FEES[wonContests];contestEntry=wonContests;contestRound=0;contestScore=0;
        log=CONTEST_NAMES[contestEntry]+"にエントリーした。";return true;
    }
    public int contestRank(){if(contestEntry<0)return 1;return CONTEST_RANKS[contestEntry][Math.min(3,contestRound)];}
    public void contestJudge(int outcome){
        if(contestEntry<0||contestRound>=4)return;
        contestScore+=outcome==2?12:outcome==1?8:-4;contestRound++;
        if(contestRound==4){
            if(contestScore>=CONTEST_PASS[contestEntry]){
                if(contestEntry==0)caseTier=Math.max(caseTier,1);
                if(contestEntry==1)fridgeTier=Math.max(fridgeTier,1);
                if(contestEntry==2)fridgeTier=Math.max(fridgeTier,2);
                if(contestEntry==3)caseTier=Math.max(caseTier,2);
                wonContests=Math.max(wonContests,contestEntry+1);
                log="大会優勝！ 合計"+contestScore+"点。設備を獲得した。";
            }else log="大会終了 "+contestScore+"点。次の季節に再挑戦できる。";
            contestEntry=-1;
        }
    }
    public void continueEndless(){
        if(!ended)return;
        ended=false;endless=true;
        nextDay();
        log="5年目以降も、同じ寿司屋で営業を続けられる。";
    }
    public void beginNextRun(int[] chosenDecor) {
        if(!ended)throw new IllegalStateException("4年経過前は周回できません");
        if(chosenDecor.length>Math.min(10,run))throw new IllegalArgumentException("装飾品持込数超過");
        int[] carry=Arrays.copyOfRange(relation,33,41);int oldRun=run;
        SushiWorld n=new SushiWorld();
        // Preserve only event guest relationship points and selected decorations.
        run=oldRun+1;day=1;minute=480;yen=100000;shopTier=0;fridgeTier=0;caseTier=0;
        seasonPayroll=0;restaurantStart=1080;restaurantEnd=1320;visitCount=0;carrying=0;currentStore=-1;
        totalSales=0;completedOrders=0;eventVisitDay=0;contestEntry=-1;contestRound=0;contestScore=0;
        fishermanPaidSeason=-1;wonContests=0;hasTruck=false;closedForDay=false;ended=false;endless=false;
        relation=n.relation;System.arraycopy(carry,0,relation,33,8);
        introduced=n.introduced;stock=n.stock;marketStock=n.marketStock;
        caseActive=n.caseActive;staffEmployed=n.staffEmployed;receivedDecor=n.receivedDecor;equippedDecor=n.equippedDecor;
        decorCarried=n.decorCarried;for(int x:chosenDecor)if(x>=0&&x<13){receivedDecor[x]=true;decorCarried[x]=1;}
        seated.clear();queue.clear();log="新しい周回が始まった。特別客の常連度と選んだ装飾品だけを引き継いだ。";
    }
}
