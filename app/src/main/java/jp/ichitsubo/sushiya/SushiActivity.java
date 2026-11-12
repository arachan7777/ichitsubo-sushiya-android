package jp.ichitsubo.sushiya;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Touch-playable Android sushi restaurant. The artwork is rendered locally as crisp pixel art.
 * Dialogue episodes and unfinished original-story assets are not represented as complete.
 */
public final class SushiActivity extends Activity {
    private SushiWorld game;
    private LinearLayout body,menu;
    private TextView headline,logView;
    private PixelShopView scene;
    private String tab="店内";
    private android.app.AlertDialog activePanel;
    private boolean outside=false, automatic=false;
    private long nextQuarterAt=0L;
    private final android.os.Handler gameClock=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable clockTick=new Runnable(){
        @Override public void run(){
            long now=android.os.SystemClock.elapsedRealtime();
            if(automatic && game!=null && activePanel==null && !game.ended &&
                game.currentStore<0 && !game.closedForDay &&
                game.minute>=game.restaurantStart && game.minute<game.restaurantEnd){
                if(nextQuarterAt==0)nextQuarterAt=now+15000L;
                if(now>=nextQuarterAt){
                    game.advanceQuarter();nextQuarterAt=now+15000L;
                    if(game.minute>=game.restaurantEnd)automatic=false;
                    changed();
                }
            }else nextQuarterAt=0L;
            gameClock.postDelayed(this,300);
        }
    };
    @Override protected void onResume(){super.onResume();gameClock.postDelayed(clockTick,300);}
    private void stopClock(){automatic=false;nextQuarterAt=0L;}
    private void dismissPanel(){
        if(activePanel!=null){android.app.AlertDialog d=activePanel;activePanel=null;d.dismiss();}
    }
    private void openPanel(String name){
        stopClock();dismissPanel();
        LinearLayout original=menu;
        LinearLayout panel=new LinearLayout(this);
        panel.setPadding(dp(8),dp(2),dp(8),dp(8));
        panel.setOrientation(LinearLayout.VERTICAL);
        menu=panel;
        if(name.equals("魚屋"))showMarkets();
        else if(name.equals("常連"))showPeople();
        else showManagement();
        menu=original;
        ScrollView scroll=new ScrollView(this);
        scroll.addView(panel);
        activePanel=new AlertDialog.Builder(this)
            .setTitle(name.equals("魚屋")?"今日の仕入れ":name.equals("常連")?"常連・横丁の人々":"寿司屋の経営")
            .setView(scroll).setNegativeButton("店内に戻る",null).create();
        activePanel.setOnDismissListener(d->{ if(activePanel==d)activePanel=null; });
        activePanel.show();
        if(activePanel.getWindow()!=null)
            activePanel.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),-2);
    }
    private void chooseCustomer(){
        stopClock();
        if(game.seated.isEmpty()){toast("今は客席が空いている。営業を進めるとお客が訪れる。");return;}
        String[] choices=new String[game.seated.size()];
        for(int i=0;i<choices.length;i++){
            SushiWorld.Customer c=game.seated.get(i);
            choices[i]=c.name()+"  "+(c.servedPieces==0?"注文を待っている":c.servedPieces+"貫提供済み");
        }
        new AlertDialog.Builder(this).setTitle("接客するお客を選ぶ")
            .setItems(choices,(d,i)->customerDialog(game.seated.get(i)))
            .setNegativeButton("閉じる",null).show();
    }
    private static final int BG=0xfff2e8d4, INK=0xff203e40, PRIMARY=0xff355d59, ACCENT=0xffb96542, CARD=0xfffff7e7;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(INK);
        game=restore();render(true);
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable bg(int color,int stroke,int corner){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(corner));if(stroke!=0)g.setStroke(dp(1),stroke);return g;
    }
    private TextView label(String text,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(text);v.setTextColor(color);v.setTextSize(size);
        v.setPadding(dp(8),dp(7),dp(8),dp(7));
        if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT,1);
        return v;
    }
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private Button button(String title,Runnable action,int color){
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Color.WHITE);
        b.setBackground(bg(color,0,8));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(43));lp.setMargins(dp(3),dp(3),dp(3),dp(3));b.setLayoutParams(lp);
        b.setOnClickListener(v->action.run());return b;
    }
    private void add(String text,Runnable action){menu.addView(button(text,action,PRIMARY));}
    private void small(String text,Runnable action){menu.addView(button(text,action,ACCENT));}
    private void caption(String text){menu.addView(label(text,14,INK,false));}
    private void header(String text){TextView t=label(text,18,INK,true);t.setPadding(dp(9),dp(12),dp(8),dp(6));menu.addView(t);}
    private void two(String left,Runnable a,String right,Runnable b){
        LinearLayout l=row();
        Button lft=button(left,a,PRIMARY),rgt=button(right,b,PRIMARY);
        l.addView(lft,new LinearLayout.LayoutParams(0,dp(44),1));
        l.addView(rgt,new LinearLayout.LayoutParams(0,dp(44),1));
        menu.addView(l);
    }
    private void toast(String message){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("閉じる",null).show();}
    private void persist(){
        try{
            JSONObject j=new JSONObject();
            j.put("run",game.run);j.put("day",game.day);j.put("minute",game.minute);j.put("yen",game.yen);
            j.put("shopTier",game.shopTier);j.put("fridgeTier",game.fridgeTier);j.put("caseTier",game.caseTier);
            j.put("seasonPayroll",game.seasonPayroll);j.put("visitCount",game.visitCount);j.put("carrying",game.carrying);
            j.put("currentStore",game.currentStore);j.put("restaurantStart",game.restaurantStart);j.put("restaurantEnd",game.restaurantEnd);
            j.put("totalSales",game.totalSales);j.put("completedOrders",game.completedOrders);
            j.put("eventVisitDay",game.eventVisitDay);j.put("contestEntry",game.contestEntry);
            j.put("contestRound",game.contestRound);j.put("contestScore",game.contestScore);
            j.put("fishermanPaidSeason",game.fishermanPaidSeason);j.put("wonContests",game.wonContests);
            j.put("closedForDay",game.closedForDay);j.put("ended",game.ended);j.put("hasTruck",game.hasTruck);
            j.put("endless",game.endless);
            j.put("log",game.log);
            j.put("relation",ints(game.relation));j.put("stock",ints(game.stock));
            j.put("introduced",bools(game.introduced));j.put("caseActive",bools(game.caseActive));
            j.put("staffEmployed",bools(game.staffEmployed));j.put("receivedDecor",bools(game.receivedDecor));
            j.put("equippedDecor",bools(game.equippedDecor));j.put("decorCarried",ints(game.decorCarried));
            JSONArray seated=new JSONArray(),queue=new JSONArray();
            for(SushiWorld.Customer c:game.seated)seated.put(customerJson(c));
            for(SushiWorld.Customer c:game.queue)queue.put(customerJson(c));
            j.put("seated",seated);j.put("queue",queue);
            getPreferences(MODE_PRIVATE).edit().putString("sushi_state",j.toString()).apply();
        }catch(Exception e){android.util.Log.e("SushiSave","save",e);}
    }
    private JSONObject customerJson(SushiWorld.Customer c)throws Exception{
        JSONObject j=new JSONObject();j.put("index",c.index);j.put("wait",c.wait);j.put("stay",c.stay);
        j.put("charge",c.charge);j.put("servedPieces",c.servedPieces);j.put("extra",c.extra);
        j.put("extended",c.extended);j.put("leaveAt",c.leaveAt);
        j.put("canAdditionalOrder",c.canAdditionalOrder);j.put("bonusOmakase",c.bonusOmakase);
        return j;
    }
    private static JSONArray ints(int[] a){JSONArray j=new JSONArray();for(int v:a)j.put(v);return j;}
    private static JSONArray bools(boolean[] a){JSONArray j=new JSONArray();for(boolean v:a)j.put(v);return j;}
    private static void readInts(JSONObject o,String name,int[] a)throws Exception{
        JSONArray v=o.getJSONArray(name);if(v.length()!=a.length)throw new Exception("セーブ配列長が異なる "+name);
        for(int i=0;i<a.length;i++)a[i]=v.getInt(i);
    }
    private static void readBools(JSONObject o,String name,boolean[] a)throws Exception{
        JSONArray v=o.getJSONArray(name);if(v.length()!=a.length)throw new Exception("セーブ配列長が異なる "+name);
        for(int i=0;i<a.length;i++)a[i]=v.getBoolean(i);
    }
    private SushiWorld.Customer customer(JSONObject j)throws Exception{
        SushiWorld.Customer c=new SushiWorld.Customer(j.getInt("index"));
        c.wait=j.getInt("wait");c.stay=j.getInt("stay");c.charge=j.getInt("charge");
        c.servedPieces=j.getInt("servedPieces");c.extra=j.optBoolean("extra",false);
        c.extended=j.optBoolean("extended",false);c.leaveAt=j.optInt("leaveAt",30);
        c.canAdditionalOrder=j.optBoolean("canAdditionalOrder",false);
        c.bonusOmakase=j.optBoolean("bonusOmakase",false);
        return c;
    }
    private SushiWorld restore(){
        String raw=getPreferences(MODE_PRIVATE).getString("sushi_state","");
        SushiWorld s=new SushiWorld();if(raw.isEmpty())return s;
        try{
            JSONObject j=new JSONObject(raw);
            s.run=j.getInt("run");s.day=j.getInt("day");s.minute=j.getInt("minute");s.yen=j.getInt("yen");
            s.shopTier=j.getInt("shopTier");s.fridgeTier=j.getInt("fridgeTier");s.caseTier=j.getInt("caseTier");
            s.seasonPayroll=j.getInt("seasonPayroll");s.visitCount=j.getInt("visitCount");s.carrying=j.getInt("carrying");
            s.currentStore=j.getInt("currentStore");s.restaurantStart=j.getInt("restaurantStart");s.restaurantEnd=j.getInt("restaurantEnd");
            s.totalSales=j.getInt("totalSales");s.completedOrders=j.getInt("completedOrders");
            s.eventVisitDay=j.getInt("eventVisitDay");s.contestEntry=j.getInt("contestEntry");
            s.contestRound=j.getInt("contestRound");s.contestScore=j.getInt("contestScore");
            s.fishermanPaidSeason=j.getInt("fishermanPaidSeason");s.wonContests=j.getInt("wonContests");
            s.closedForDay=j.getBoolean("closedForDay");s.ended=j.getBoolean("ended");s.hasTruck=j.getBoolean("hasTruck");
            s.endless=j.optBoolean("endless",false);
            s.log=j.optString("log","");
            readInts(j,"relation",s.relation);readInts(j,"stock",s.stock);
            readBools(j,"introduced",s.introduced);readBools(j,"caseActive",s.caseActive);
            readBools(j,"staffEmployed",s.staffEmployed);readBools(j,"receivedDecor",s.receivedDecor);
            readBools(j,"equippedDecor",s.equippedDecor);readInts(j,"decorCarried",s.decorCarried);
            for(int i=0;i<j.getJSONArray("seated").length();i++)s.seated.add(customer(j.getJSONArray("seated").getJSONObject(i)));
            for(int i=0;i<j.getJSONArray("queue").length();i++)s.queue.add(customer(j.getJSONArray("queue").getJSONObject(i)));
            s.refreshMarkets();
        }catch(Exception e){android.util.Log.e("SushiSave","load",e);}
        return s;
    }
    private void changed(){dismissPanel();persist();render(false);}
    private void render(boolean reset){
        if(reset){
            ScrollView scroll=new ScrollView(this);
            scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
            body=new LinearLayout(this);body.setPadding(dp(8),dp(5),dp(8),dp(16));body.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(body);setContentView(scroll);
            TextView title=label("🍣 一坪寿司屋",24,INK,true);
            title.setGravity(Gravity.CENTER);body.addView(title);
            headline=label("",14,INK,true);
            headline.setGravity(Gravity.CENTER);
            headline.setBackground(bg(CARD,0,8));body.addView(headline);
            scene=new PixelShopView(this,game);
            scene.setTapListener(new PixelShopView.TapListener(){
                @Override public void onGuest(SushiWorld.Customer c){stopClock();customerDialog(c);}
                @Override public void onCounter(){chooseCustomer();}
                @Override public void onDoor(){stopClock();outside=!outside;scene.setOutside(outside);}
            });
            body.addView(scene,new LinearLayout.LayoutParams(-1,dp(270)));
            logView=label("",13,0xff825436,false);
            logView.setMinHeight(dp(54));logView.setGravity(Gravity.CENTER_VERTICAL);
            body.addView(logView);
            LinearLayout nav=row();
            String[] tabs={"🐟仕入れ","👥常連","🏠経営","🚶店外"};
            for(int i=0;i<tabs.length;i++){
                final int idx=i;Button b=button(tabs[i],()->{
                    if(idx==3){stopClock();outside=!outside;scene.setOutside(outside);}
                    else openPanel(idx==0?"魚屋":idx==1?"常連":"経営");
                },ACCENT);
                b.setTextSize(12);
                nav.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));
            }
            body.addView(nav);
            menu=new LinearLayout(this);menu.setOrientation(LinearLayout.VERTICAL);body.addView(menu);
        }
        scene.update(game,outside?1:0);
        headline.setText("第"+game.run+"周  "+game.date()+"  "+game.time()
            +"\n所持金 "+String.format(Locale.JAPAN,"%,d",game.yen)+"円"
            +"   常連スコア "+String.format(Locale.JAPAN,"%,d",game.score())+"点");
        logView.setText("✦ "+game.log);
        menu.removeAllViews();
        if(game.ended){showEnd();return;}
        showRestaurant();
    }
    private void showRestaurant(){
        if(game.currentStore>=0){
            caption(SushiWorld.STORE_NAMES[game.currentStore]+"で買い出し中");
            add("🐟 魚屋でネタを選ぶ",()->openPanel("魚屋"));
            return;
        }
        if(game.minute<game.restaurantStart&&!game.closedForDay){
            caption("仕入れとネタケースを準備して、暖簾を掲げる。");
            two("🐟 魚屋へ",()->openPanel("魚屋"),
                "🍣 ネタケース",()->{stopClock();caseDialog();});
            add("暖簾を出して開店（"+String.format(Locale.JAPAN,"%02d:%02d",
                game.restaurantStart/60,game.restaurantStart%60)+"）",()->{
                game.minute=game.restaurantStart;game.log="暖簾を掲げた。今日の営業が始まる。";changed();
            });
        }else if(game.minute>=game.restaurantStart&&game.minute<game.restaurantEnd&&!game.closedForDay){
            caption("客席 "+game.seated.size()+"/"+game.seats()+"  行列 "+
                game.queue.size()+"/"+game.queueCapacity()+
                "。お客のドット絵をタップすると注文を受けられる。");
            add(automatic?"⏸ 自動営業を止める":"▶ 自動営業（現実15秒＝ゲーム内15分）",()->{
                automatic=!automatic;nextQuarterAt=0L;changed();
            });
            two("⏩ 15分進める",()->{stopClock();game.advanceQuarter();changed();},
                "👤 お客を選ぶ",this::chooseCustomer);
            if(game.contestEntry>=0)
                small("🏆 寿司大会の課題 "+(game.contestRound+1)+"/4",()->{stopClock();contestTiming();});
            small("🍣 ネタケースを管理",()->{stopClock();caseDialog();});
        }else{
            stopClock();
            caption("本日の営業が終了した。");
            add("🌅 翌日の横丁へ",()->{
                game.closePayroll();game.nextDay();changed();
            });
        }
    }
    private void showMarkets(){
        header("横丁の魚屋へ買い出し");
        caption("午前8:00～10:00。自転車は1日1店舗・200貫まで。"
            +"\n訪問 "+game.visitCount+"/"+(game.hasTruck?2:1)
            +"　持ち帰り "+game.carrying+"/"+(game.hasTruck?400:200)+"貫"
            +"\n所持金 "+String.format(Locale.JAPAN,"%,d",game.yen)+"円"
            +"　冷蔵庫 "+game.totalStock()+"/"+game.fridgeCapacity()+"貫");
        if(game.currentStore<0){
            if(game.minute<480||game.minute>=600){
                caption("買い出しは朝8時から10時まで。");return;
            }
            boolean any=false;
            for(int i=0;i<4;i++){
                final int id=i;
                if(game.hasStore(i)){
                    any=true;
                    add("🐟 "+SushiWorld.STORE_NAMES[i]+"へ行く",()->{
                        if(!game.visit(id)){
                            toast("今日はもう訪問できない。時刻または訪問回数を確認。");
                            return;
                        }
                        // Keep the store screen open after visiting. Do not strand
                        // the player at the shop with the only visit consumed.
                        changed();
                        openPanel("魚屋");
                    });
                }else caption("🔒 "+SushiWorld.STORE_NAMES[i]+"（紹介で解放）");
            }
            if(game.visitCount>0)
                small("買い出しを終えて寿司屋へ（10:00）",()->{
                    game.minute=600;
                    game.log="魚屋の仕入れを終えて寿司屋に戻った。";
                    changed();
                });
            if(!any)caption("訪問できる仕入れ先がない。");
            return;
        }
        int store=game.currentStore;
        header("🐟 "+SushiWorld.STORE_NAMES[store]);
        int products=0;
        for(int i=0;i<85;i++)if(game.market(store,i)>=10)products++;
        caption("本日の入荷 "+products+"種類。魚を選んで10貫単位で仕入れる。"
            +"\n旬は仕入れ値20％引き。"
            +"\n配達："+(game.deliveryUnlocked(store)?"解放済み":"未解放"));
        add("🐟 ネタを選んで購入する",this::selectMarketFish);
        small("この魚屋から寿司屋へ戻る",()->{
            game.returnStore();
            changed();
            if(game.visitCount<(game.hasTruck?2:1)&&game.minute<600)openPanel("魚屋");
        });
    }
    private void selectMarketFish(){
        if(game.currentStore<0){openPanel("魚屋");return;}
        int store=game.currentStore;
        List<Integer> fish=new ArrayList<>();
        List<String> items=new ArrayList<>();
        for(int i=0;i<85;i++){
            if(game.market(store,i)<10)continue;
            Catalog.Fish f=Catalog.FISH[i];
            fish.add(i);
            items.add((f.inSeason(game.season())?"旬！ ":"")+"★"+f.rank+" "
                +f.name+" / 10貫 "+String.format(Locale.JAPAN,"%,d",game.buyPrice(f))+"円"
                +" / 残"+game.market(store,i)+"貫");
        }
        if(items.isEmpty()){toast("この魚屋の販売在庫は売り切れた。");return;}
        new AlertDialog.Builder(this).setTitle("仕入れるネタを選ぶ（"+items.size()+"種類）")
            .setItems(items.toArray(new String[0]),(d,index)->choosePurchase(fish.get(index)))
            .setNegativeButton("魚屋に戻る",null).show();
    }
    private void choosePurchase(int index){
        if(game.currentStore<0){toast("魚屋にいないため購入できない。");return;}
        Catalog.Fish f=Catalog.FISH[index];
        int[] qty={10,20,50,100,200};
        boolean delivery=game.deliveryUnlocked(game.currentStore);
        int count=delivery?qty.length*2:qty.length;
        String[] options=new String[count];
        for(int i=0;i<count;i++){
            int units=qty[i%qty.length]/10;
            boolean shipping=delivery&&i>=qty.length;
            options[i]=qty[i%qty.length]+"貫分　"
                +String.format(Locale.JAPAN,"%,d",game.buyPrice(f)*units)+"円"
                +(shipping?"（配達）":"（持ち帰り）");
        }
        new AlertDialog.Builder(this).setTitle(f.name+"の仕入れ")
            .setMessage("希望する数量と運び方を選ぶ。1回の購入は10貫単位。")
            .setItems(options,(d,i)->{
                int units=qty[i%qty.length]/10;
                boolean shipping=delivery&&i>=qty.length;
                if(!game.buy(index,units,shipping)){
                    toast("購入できない。残金・冷蔵庫容量・積載量・店頭在庫を確認。");
                    return;
                }
                changed();
                // Remain at the same fish market for repeat purchases.
                openPanel("魚屋");
            })
            .setNegativeButton("ネタ一覧に戻る",(d,i)->selectMarketFish())
            .show();
    }
    private void caseDialog(){
        final AlertDialog[] activeDialog={null};
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        TextView status=label("ネタケース "+game.visibleCaseCount()+"/"+game.caseCapacity()
            +"種類　冷蔵庫 "+game.totalStock()+"/"+game.fridgeCapacity()+"貫",14,INK,true);
        list.addView(status);
        for(int i=0;i<85;i++){
            if(game.stock[i]<=0&&Catalog.FISH[i].number!=60)continue;
            final int id=i;Catalog.Fish f=Catalog.FISH[i];
            Button b=button((game.caseActive[i]?"● ":"○ ")+f.name+" 在庫"+(f.number==60?"無限":game.stock[i])+"貫",()->{
                boolean toggled=game.toggleCase(id);
                if(activeDialog[0]!=null)activeDialog[0].dismiss();
                if(!toggled)toast("ネタケースの陳列枠が不足している、または玉子は常設。");
                else changed();
                if(toggled)caseDialog();
            },PRIMARY);list.addView(b);
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        activeDialog[0]=new AlertDialog.Builder(this).setTitle("ネタケース").setView(scroll).setPositiveButton("閉じる",null).show();
    }
    private void customerDialog(SushiWorld.Customer c){
        Catalog.Guest guest=game.guest(c.index);
        Catalog.Fish fav=Catalog.fish(guest.favorite);
        String info=c.name()+"\n常連度 Lv"+game.level(c.index)+"（"+game.relation[c.index]+"点）"
            +"\n好物："+(fav==null?"不明":fav.name)
            +"\n既に提供した寿司："+c.servedPieces+"貫";
        if(!game.seated.contains(c)){toast("そのお客は既に退店した。");return;}
        if(c.bonusOmakase||c.canAdditionalOrder){
            new AlertDialog.Builder(this).setTitle("おまかせ4貫の追加注文")
                .setMessage(info+"\n\n追加の寿司4貫を握ることができる。")
                .setPositiveButton("追加注文を受ける",(d,w)->orderDialog(c,true))
                .setNegativeButton("戻る",null).show();
            return;
        }
        if(c.servedPieces>0){
            new AlertDialog.Builder(this).setTitle(c.name()).setMessage(info)
                .setPositiveButton("戻る",null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("ご注文を承ります")
            .setMessage(info).setPositiveButton("注文を受ける", (dialog,which)->orderDialog(c,false))
            .setNegativeButton("まだ接客しない",null).show();
    }
    private void orderDialog(SushiWorld.Customer customer,boolean extra){
        SushiWorld.Order order=game.orderFor(customer.index,extra);
        String txt=(extra?"おまかせ追加4貫":"最初のご注文 最大8貫")
            +"\n"+order.summary()
            +"\nランク平均 "+order.difficultyRank
            +"\n\n針が中央の色つき範囲に来たらタッチ。大成功なら関係値＋12。";
        new AlertDialog.Builder(this).setTitle(customer.name()+"の注文")
            .setMessage(txt)
            .setPositiveButton("寿司を握る", (dialog,which)->timing(customer,order))
            .setNegativeButton("注文を確認する",null).show();
    }
    private void timing(SushiWorld.Customer customer,SushiWorld.Order order){
        scene.setCooking(true);
        TimingView view=new TimingView(this,order.difficultyRank,result->{
            int gained=game.serve(customer,order,result);
            if(gained==0){changed();toast("注文の処理に失敗した。お客が退店したか、ネタが不足している。");return;}
            String report=customer.name()+"に"+order.pieces+"貫を提供。\n"
                +(result==2?"大成功！":result==1?"成功！":"失敗…")
                +"\n関係値 "+(gained>=0?"+":"")+gained;
            if(result==2&&!order.omakase&&order.pieces==8){
                if(game.working(2))game.award(customer.index,2);
                changed();
                new AlertDialog.Builder(this).setTitle("おまかせ追加注文！")
                    .setMessage(report+"\n\n大成功したため、直後に4貫の追加注文。")
                    .setPositiveButton("追加4貫を握る",(d,w)->orderDialog(customer,true)).show();
            } else {changed();toast(report);}
        });
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(order.omakase?"おまかせ4貫を握る":"寿司8貫を握る")
            .setView(view)
            .setNegativeButton("中断",null).create();
        view.setOnResultDismiss(()->dialog.dismiss());
        dialog.setOnDismissListener(d->scene.setCooking(false));
        dialog.show();
    }
    private void showPeople(){
        header("常連さんの記録");
        caption("通常客33人・行事特別客8人。会話や注文を通じて関係Lvが上がり、紹介によってお客が増える。"
               +"\n人物ごとの長編エピソードと手描き回想は制作途中。");
        for(int i=0;i<33;i++){
            final int id=i;Catalog.Guest c=Catalog.REGULAR[i];
            if(!game.introduced[i])continue;
            Catalog.Fish fav=Catalog.fish(c.favorite);
            add(c.name+"  Lv"+game.level(i)+"  "+game.tier(i),()->{
                toast(c.name+"\n関係値："+game.relation[id]+" / 900でLv10"
                    +"\n好物："+(fav==null?"":fav.name)
                    +"\n好みの曜日："+weekdays(c.weekdayMask)+"\n個別の物語は制作途中。");
            });
        }
        header("行事限定の8人");
        for(int i=0;i<8;i++){
            int idx=i+33;
            Catalog.Guest c=Catalog.SPECIAL[i];
            caption(c.festival+" ・ "+c.name+"   Lv"+game.level(idx)+"（周回で引き継ぎ）");
        }
    }
    private String weekdays(int mask){
        StringBuilder b=new StringBuilder();for(int i=0;i<7;i++)if((mask&(1<<i))!=0)b.append(SushiWorld.WEEKDAYS[i]);
        return b.length()==0?"行事開催日":b.toString();
    }
    private void showManagement(){
        header("経営と店づくり");
        caption("店舗 "+new String[]{"1坪（4席）","3坪（8席）","5坪（12席）"}[game.shopTier]
            +"　所持金 "+game.yen+"円"
            +"\n季節固定維持費 "+game.fixedUpkeep()+"円、給与累積 "+game.seasonPayroll+"円");
        if(game.canExpand())small("店舗を拡張する（"+(game.shopTier==0?300000:700000)+"円）",()->{game.expand();changed();});
        else caption("店舗拡張：課長代理Lv7→3坪、タバちゃんLv8→5坪。改装費30万／70万円。");
        header("アルバイト");
        caption("3坪で1人、5坪で2人まで。営業開始前に採用・解雇可能。");
        for(int i=0;i<4;i++){
            final int id=i;int who=Catalog.guestIndex(SushiWorld.STAFF_NAMES[i]);
            String state=game.staffEmployed[i]?"勤務中":game.level(who)<SushiWorld.PROMOTION_STAFF_LV[i]?"未解放":"待機";
            add(SushiWorld.STAFF_NAMES[i]+"  "+state+"  "+SushiWorld.HOURLY[i]+"円/時",()->{
                if(!game.toggleStaff(id))toast("採用・解雇できない。関係Lv、店舗規模、営業時間を確認。");
                else changed();
            });
        }
        header("季節の大会");
        if(game.wonContests<4)
            caption("次の大会："+SushiWorld.CONTEST_NAMES[game.wonContests]+"。"
                +"\n季節末（日曜）12時開始。事前エントリー費 "+SushiWorld.CONTEST_FEES[game.wonContests]+"円。");
        else caption("全4大会優勝済み。");
        if(game.wonContests<4) add("この季節の大会へエントリー",()->{
            if(!game.joinContest())toast("エントリーは季節最終日12時より前、残高が足りる場合のみ。");
            else changed();
        });
        if(game.contestEntry>=0)add("エントリー済み：大会の調理へ",()->{tab="店内";contestTiming();});
        header("魚の直送契約");
        add("潮さんの故郷の兄と今季35,000円で契約",()->{
            if(!game.contractFisherman())toast("潮さんとの関係や契約済みかどうか、所持金を確認。");
            else changed();
        });
        header("装飾品");
        caption("設置枠：1坪8、3坪10、5坪13。贈り物の受取イベント・配置の個別効果は制作中。");
        for(int i=0;i<13;i++)if(game.receivedDecor[i]){final int id=i;add("装飾品 "+(i+1)+(game.equippedDecor[i]?" 設置中":" 所持"),()->{
            game.equippedDecor[id]=!game.equippedDecor[id];changed();
        });}
        header("4年のスコアと周回");
        caption("現在 "+String.format(Locale.JAPAN,"%,d",game.score())+"点 / 理論最高73,000点"
           +"\n通常客Lv合計＋イベント客Lv合計×5、すべて100倍して表示。"
           +"\nイベント客の常連度と、選んだ装飾品だけ次周へ引き継ぐ。");
        caption("端末内に自動セーブ済み。広告・課金はゲーム本体の完成後に接続する。");
    }
    private void showEnd(){
        header("4年間の経営結果");
        caption("第"+game.run+"周　最終得点 "+
            String.format(Locale.JAPAN,"%,d",game.score())+"点。");
        add("強くてニューゲーム（引継ぎあり）",()->{
            // Only items already received may be chosen; incomplete decoration selection UI marked as such.
            List<Integer> own=new ArrayList<>();for(int i=0;i<13;i++)if(game.receivedDecor[i])own.add(i);
            int limit=Math.min(10,game.run);
            int[] ids=new int[Math.min(limit,own.size())];
            for(int i=0;i<ids.length;i++)ids[i]=own.get(i);
            game.beginNextRun(ids);changed();
        });
        add("この寿司屋で、そのまま営業を続ける",()->{game.continueEndless();changed();});
        caption("周回する場合はイベント限定客の関係値と選んだ装飾品だけを引き継ぐ。");
    }
    private void contestTiming(){
        if(game.contestEntry<0)return;
        int rank=game.contestRank();
        TimingView view=new TimingView(this,rank,result->{
            game.contestJudge(result);
            changed();
            toast("大会の調理結果："+(result==2?"大成功":result==1?"成功":"失敗")
                +"。\n"+game.log+"\n大会課題が残っていれば店内画面から続けられる。");
        });
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("大会の課題 "+(game.contestRound+1)+"/4  ランク"+rank)
            .setView(view).setNegativeButton("中断",null).create();
        view.setOnResultDismiss(()->dialog.dismiss());dialog.show();
    }
    /** Real-time touch timing minigame: moving pointer and distinct great/good windows. */
    private static final class TimingView extends View {
        interface Callback{void outcome(int result);}
        private final Paint paint=new Paint(3);
        private final Callback callback;
        private final int rank;
        private float phase=0;
        private long previous=0;
        private boolean finished=false;
        private Runnable dismiss;
        TimingView(Context c,int rank,Callback cb){super(c);this.rank=rank;this.callback=cb;setMinimumHeight((int)(170*c.getResources().getDisplayMetrics().density));}
        void setOnResultDismiss(Runnable r){dismiss=r;}
        @Override protected void onDraw(Canvas canvas){
            int w=getWidth(),h=getHeight();paint.setColor(0xfff7e9d0);canvas.drawColor(0xfff7e9d0);
            long now=android.os.SystemClock.uptimeMillis();
            if(previous==0)previous=now;
            if(!finished)phase+=(now-previous)*(.00045f+rank*.00021f);
            previous=now;
            float needle=(float)((Math.sin(phase*Math.PI*2)+1)*.5);
            int left=(int)(w*.08f),width=(int)(w*.84f),center=left+width/2;
            float good=Math.max(.075f,.28f-rank*.028f),great=Math.max(.02f,.088f-rank*.009f);
            paint.setColor(0xff2b4e4d);canvas.drawRoundRect(left,h*.45f,left+width,h*.65f,10,10,paint);
            paint.setColor(0xffe0b86f);canvas.drawRect(center-width*good,h*.45f,center+width*good,h*.65f,paint);
            paint.setColor(0xff75bb93);canvas.drawRect(center-width*great,h*.45f,center+width*great,h*.65f,paint);
            paint.setColor(0xffd65e49);float x=left+needle*width;
            canvas.drawRect(x-4,h*.32f,x+4,h*.78f,paint);
            paint.setColor(0xff253f41);paint.setTextSize(h*.105f);paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("中央の緑色で大成功！",w/2,h*.2f,paint);
            canvas.drawText("ここをタッチして判定",w/2,h*.92f,paint);
            if(!finished)postInvalidateDelayed(16);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN||finished)return true;
            finished=true;
            float pos=(float)((Math.sin(phase*Math.PI*2)+1)*.5);
            float dist=Math.abs(pos-.5f);
            float good=Math.max(.075f,.28f-rank*.028f),great=Math.max(.02f,.088f-rank*.009f);
            int result=dist<=great?2:dist<=good?1:0;
            if(dismiss!=null)dismiss.run();callback.outcome(result);return true;
        }
    }
    @Override public void onPause(){stopClock();gameClock.removeCallbacks(clockTick);persist();super.onPause();}
}
