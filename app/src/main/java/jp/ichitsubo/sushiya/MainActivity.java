package jp.ichitsubo.sushiya;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;

/** Native Android development console for the first verified simulation slice. */
public final class MainActivity extends Activity {
    private GameState state;
    private GameEngine engine;
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        state=load(); engine=new GameEngine(state,new Random());
        buildUi(); update();
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(251,248,238));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,32,28,32);
        scroll.addView(root);
        TextView heading=new TextView(this); heading.setText("一坪寿司屋"); heading.setTextSize(27);heading.setTextColor(Color.rgb(35,55,48)); heading.setGravity(Gravity.CENTER);root.addView(heading);
        TextView caveat=new TextView(this);
        caveat.setText("開発中のゲームロジック確認画面です。企画書の全機能を実装した完成版ではありません。");
        caveat.setTextColor(Color.rgb(138,66,42));caveat.setPadding(0,18,0,18);root.addView(caveat);
        status=new TextView(this); status.setTextSize(19);status.setTextColor(Color.rgb(25,36,35));status.setPadding(0,12,0,24);root.addView(status);
        button(root,"15分進める（来客抽選を検証）",()->step(15));
        button(root,"1時間進める",()->step(60));
        button(root,"24時間進める（季節会計を検証）",()->step(1440));
        button(root,"セーブ",()->{save();state.lastEvent="端末に保存しました。";update();});
        button(root,"ロード",()->{state=load();engine=new GameEngine(state,new Random());update();});
        TextView notes=new TextView(this);notes.setText("現在の確認範囲：四季の暦・15分抽選・季節維持費・スコア計算・端末保存。\n未実装：本格接客、寿司調理、41人の会話、85品目の仕入れ、イベント、周回UI、装飾、画像、広告など。広告設定・課金接続は本体完成後。");
        notes.setTextColor(Color.DKGRAY);notes.setPadding(0,28,0,20);root.addView(notes);
        setContentView(scroll);
    }
    private void button(LinearLayout parent,String text,Runnable action) {
        Button b=new Button(this); b.setText(text);parent.addView(b);
        b.setOnClickListener(v->{action.run();});
    }
    private void step(int minutes) {engine.advanceMinutes(minutes);save();update();}
    private void update() {
        status.setText("第"+state.run+"周 / "+state.dateLabel()+"\n"
          +String.format(java.util.Locale.JAPAN,"時刻 %02d:%02d",state.minuteOfDay/60,state.minuteOfDay%60)
          +"\n店舗 1坪→3坪→5坪の第"+(state.shopTier+1)+"段階（現在 "+state.seats()+"席）"
          +"\n所持金 "+String.format(java.util.Locale.JAPAN,"%,d",state.yen)+"円"
          +"\n現時点の関係Lv得点 "+String.format(java.util.Locale.JAPAN,"%,d",state.score())+"点"
          +"\n\n"+state.lastEvent);
    }
    private void save() {
        try {
            JSONObject o=new JSONObject();
            o.put("run",state.run);o.put("day",state.day);o.put("minute",state.minuteOfDay);o.put("yen",state.yen);
            o.put("shopTier",state.shopTier);o.put("fridgeTier",state.fridgeTier);o.put("fishCaseTier",state.fishCaseTier);
            o.put("totalSeasonalStaffYen",state.totalSeasonalStaffYen);o.put("startMinutes",state.startMinutes);o.put("endMinutes",state.endMinutes);
            o.put("fishermanSubscribed",state.fishermanSubscribed);o.put("endedRun",state.endedRun);
            o.put("regularLevels",array(state.regularLevels));o.put("eventLevels",array(state.eventLevels));
            o.put("staff",array(state.staff));o.put("lastEvent",state.lastEvent);
            getPreferences(MODE_PRIVATE).edit().putString("state",o.toString()).apply();
        } catch(Exception ex){state.lastEvent="セーブ失敗："+ex.getMessage();}
    }
    private JSONArray array(int[] a){JSONArray j=new JSONArray();for(int v:a) j.put(v);return j;}
    private JSONArray array(boolean[] a){JSONArray j=new JSONArray();for(boolean v:a) j.put(v);return j;}
    private GameState load() {
        GameState s=new GameState();String raw=getPreferences(MODE_PRIVATE).getString("state",null);if(raw==null)return s;
        try {
            JSONObject o=new JSONObject(raw);
            s.run=o.getInt("run");s.day=o.getInt("day");s.minuteOfDay=o.getInt("minute");s.yen=o.getInt("yen");
            s.shopTier=o.getInt("shopTier");s.fridgeTier=o.getInt("fridgeTier");s.fishCaseTier=o.getInt("fishCaseTier");
            s.totalSeasonalStaffYen=o.getInt("totalSeasonalStaffYen");s.startMinutes=o.getInt("startMinutes");s.endMinutes=o.getInt("endMinutes");
            s.fishermanSubscribed=o.getBoolean("fishermanSubscribed");s.endedRun=o.getBoolean("endedRun");
            JSONArray a=o.getJSONArray("regularLevels");for(int i=0;i<s.regularLevels.length;i++)s.regularLevels[i]=a.getInt(i);
            a=o.getJSONArray("eventLevels");for(int i=0;i<s.eventLevels.length;i++)s.eventLevels[i]=a.getInt(i);
            a=o.getJSONArray("staff");for(int i=0;i<s.staff.length;i++)s.staff[i]=a.getBoolean(i);
            s.lastEvent=o.optString("lastEvent","復元しました。");
        } catch(Exception ex){return new GameState();}
        return s;
    }
}
