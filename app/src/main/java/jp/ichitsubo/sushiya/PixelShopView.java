package jp.ichitsubo.sushiya;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Crisp nearest-neighbor pixel art drawn entirely on device; no network or image service. */
public final class PixelShopView extends View {
    private final Bitmap bitmap=Bitmap.createBitmap(360,236,Bitmap.Config.ARGB_8888);
    private final Canvas art=new Canvas(bitmap);
    private final Paint p=new Paint();
    private SushiWorld world;
    private int mode=0;
    private boolean cooking=false;
    private final Map<Integer,Long> entering=new HashMap<>();
    private final Map<Integer,Integer> previousSeats=new HashMap<>();
    private final List<Departure> departing=new ArrayList<>();
    private CustomerTapListener tapListener;
    private static final int INK=0xff14232c, NIGHT=0xff182d3a, WOOD=0xff965332;
    private static final long WALK_MS=2200L, LEAVE_MS=1900L;
    public interface CustomerTapListener { void onCustomerTap(SushiWorld.Customer customer); }
    private static final class Departure {
        final int id,startingX;final long when;
        Departure(int id,int start,long time){this.id=id;startingX=start;when=time;}
    }
    public PixelShopView(Context c,SushiWorld w){super(c);world=w;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void setCustomerTapListener(CustomerTapListener cb){tapListener=cb;setClickable(true);}
    public void setCooking(boolean value){cooking=value;invalidate();}
    public void update(SushiWorld w,int newMode){
        world=w;mode=newMode;
        syncCustomers(SystemClock.uptimeMillis());
        invalidate();
    }
    private void syncCustomers(long now){
        Set<Integer> actual=new HashSet<>();
        for(int j=0;j<world.seated.size();j++){
            SushiWorld.Customer c=world.seated.get(j);
            actual.add(c.index);
            if(!entering.containsKey(c.index))entering.put(c.index,now);
            previousSeats.put(c.index,j);
        }
        Iterator<Map.Entry<Integer,Long>> it=entering.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer,Long> item=it.next();
            if(!actual.contains(item.getKey())){
                int slot=previousSeats.containsKey(item.getKey())?previousSeats.get(item.getKey()):0;
                departing.add(new Departure(item.getKey(),48+slot*54,now));
                previousSeats.remove(item.getKey());
                it.remove();
            }
        }
        for(Iterator<Departure> d=departing.iterator();d.hasNext();)
            if(now-d.next().when>LEAVE_MS)d.remove();
    }
    private float progress(long elapsed,long duration){
        return Math.max(0f,Math.min(1f,elapsed/(float)duration));
    }
    private int walkStep(long now){return (int)((now/205L)%2L);}

    private void rect(int color,float x,float y,float w,float h){p.setColor(color);p.setStyle(Paint.Style.FILL);art.drawRect(x,y,x+w,y+h,p);}
    private void edge(int x,int y,int w,int h,int col){rect(INK,x-2,y-2,w+4,h+4);rect(col,x,y,w,h);}
    private void text(String value,int color,int size,float x,float y){
        p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));
        p.setAntiAlias(true);p.setColor(color);p.setTextSize(size);p.setShadowLayer(1,1,1,INK);
        art.drawText(value,x,y,p);p.clearShadowLayer();p.setAntiAlias(false);
    }
    private void window(int x,int y){
        edge(x,y,48,36,0xffd5b275);rect(0xff355869,x+4,y+3,40,30);
        rect(0xff55757a,x+9,y+5,8,10);rect(0xff203c54,x+4,y+18,40,15);
        rect(0xff8b5837,x+24,y+3,3,30);rect(0xff8b5837,x+3,y+16,42,3);
    }
    private void lantern(int x,int y){
        rect(0xff382c2c,x+10,y-12,3,13);
        rect(0xffe5a44e,x+2,y+1,25,6);rect(0xffd46948,x,y+7,29,23);
        rect(0xfff19c5f,x+4,y+10,21,14);rect(0xffca533b,x+2,y+28,25,5);
        rect(0xff37242a,x+7,y+12,3,11);rect(0xff37242a,x+16,y+12,3,11);
        rect(0xfff3c26a,x+13,y+33,4,4);
    }
    private void noren(int x,int y){
        rect(0xff30565c,x,y,80,33);
        rect(0xff477e80,x+4,y+4,72,22);
        for(int i=0;i<4;i++){rect(0xff477e80,x+i*20,y+24,17,18);rect(0xff31595d,x+i*20+16,y+24,3,12);}
        text("一坪寿司屋",0xfff8e7cb,11,x+8,y+20);
    }
    private void sushi(int x,int y,int which){
        int c=new int[]{0xffdc655b,0xffe7a165,0xfff2ba71,0xffb0a16b,0xffd77f7e}[Math.abs(which)%5];
        rect(0xff203039,x-2,y+8,26,9);
        rect(0xfff7e9ca,x,y+4,22,8);
        rect(0xffe2cbac,x+3,y+10,16,3);
        rect(c,x+1,y,20,7);rect(0xfff2ceb0,x+4,y+1,11,2);
    }
    private void person(int index,int x,int y,boolean chef,boolean sitting){
        person(index,x,y,chef,sitting,0);
    }
    private void person(int index,int x,int y,boolean chef,boolean sitting,int step){
        int skin=(index%4==0)?0xffeab591:(index%4==1)?0xffd9a17f:0xfff1c3a4;
        int hair=new int[]{0xff272c32,0xff55403a,0xff7d4a35,0xff3c4849,0xff815847}[Math.abs(index)%5];
        int cloth=chef?0xfff4ecdc:new int[]{0xff9b5e59,0xff436d7a,0xff6f7650,0xff8d719d,0xffbb8e4f,0xffb76744}[Math.abs(index)%6];
        int height=sitting?25:32;
        rect(INK,x-2,y-2,22,height+5);
        rect(cloth,x,y+13,18,height-13);
        rect(0xffdecdbb,x+6,y+17,6,14);
        rect(skin,x+2,y+3,14,12);
        rect(hair,x,y,18,5);
        rect(hair,x,y+3,3,8);
        rect(INK,x+5,y+9,2,2);rect(INK,x+13,y+9,2,2);
        if(chef){
            rect(0xfff6f4e8,x,y-6,18,7);rect(0xfff6f4e8,x+3,y-11,12,8);
            rect(0xfff3f0e4,x+3,y+12,12,16);rect(0xffb8c9be,x+8,y+17,2,12);
        }else if(index%5==0){rect(0xffd9ba77,x,y-3,18,3);}
        rect(0xfff5ddbc,x+1,y+23+(step==1?2:0),3,5);
        rect(0xfff5ddbc,x+16,y+23+(step==0?2:0),3,5);
        if(step>0&&!sitting&&!chef){
            rect(0xff252d30,x+1,y+height-2,6,6);
            rect(0xff252d30,x+11,y+height+1,6,4);
        }
    }
    private void tiles(){
        rect(0xff3a3d3e,0,0,360,236);
        for(int x=0;x<360;x+=18)for(int y=0;y<236;y+=18){
            int col=((x/18+y/18)%2==0)?0xffb99b76:0xffc9ab87;
            rect(col,x,y,17,17);
        }
        rect(0xff6c4539,0,0,360,112);
        for(int y=12;y<115;y+=24)rect(0xff82533e,0,y,360,2);
        for(int x=0;x<360;x+=54)rect(0xff614034,x,0,3,112);
    }
    private void drawInterior(long now){
        tiles();
        rect(0xff293f41,0,0,360,21);
        text("ICHITSUBO  ·  SUSHI",0xffe4cda0,12,87,15);
        window(15,33);window(291,33);
        noren(138,20);lantern(101,35);lantern(241,35);
        if((now/680)%2==0)rect(0x66ffe4a2,108,52,14,18);
        if((now/910)%2==0)rect(0x66ffe4a2,249,52,14,18);
        // wall fish art
        edge(60,36,36,23,0xffe0c7a2);rect(0xff567573,65,42,27,13);
        rect(0xff8ac0b3,68,48,21,5);rect(0xffd2ede6,82,46,4,3);
        edge(267,35,24,24,0xffc8ac87);rect(0xff345d54,272,39,14,15);
        // back shelf
        rect(0xff44302f,12,88,336,8);rect(0xff9f6a44,15,90,330,4);
        for(int i=0;i<14;i++){rect(new int[]{0xff6c8b72,0xffa96f55,0xffdcb87d}[i%3],19+i*24,76,9,12);}
        // fish cabinet and counter
        rect(INK,15,111,330,60);
        rect(0xff719295,17,113,326,29);
        rect(0xffb5d7cb,20,115,320,4);
        for(int i=0;i<9;i++)sushi(25+i*35,121,i);
        rect(0xffcba17b,12,143,336,21);
        rect(0xff7c4b34,12,164,336,29);
        rect(0xffb77b51,16,166,328,6);
        for(int i=0;i<9;i++)rect(0xff654235,17+i*38,172,24,16);
        // small tea cups and plates
        for(int i=0;i<4;i++){rect(0xffe4ddbd,48+i*83,155,10,6);rect(0xff3f746e,50+i*83,155,6,6);}
        int chefBob=(int)((now/300)%2);
        person(18,174,95+chefBob,true,false);
        if(cooking||((now/600)%3==0)){
            rect(0xfff0ca9b,196,122+chefBob,15,5);
            rect(0xfff1ddb3,202,127+chefBob,18,4);
        } else rect(0xfff1ddb3,196,127,24,5);
        // foreground stools
        for(int i=0;i<Math.min(6,world.seats());i++){
            int x=37+i*54;
            rect(INK,x+1,199,40,11);
            rect(0xff754e3b,x+4,198,34,8);
            rect(0xff4c3c37,x+9,207,5,25);rect(0xff4c3c37,x+30,207,5,25);
        }
        int j=0;
        for(SushiWorld.Customer c:world.seated){
            if(j>=Math.min(6,world.seats()))break;
            float p=progress(now-entering.getOrDefault(c.index,now),WALK_MS);
            int targetX=48+j*54;
            if(p<1f){
                int x=(int)(334+(targetX-334)*p);
                int y=(int)(194+(173-194)*p);
                person(c.index,x,y,false,false,walkStep(now));
            }else{
                int breathe=(int)((now/430+c.index)%2);
                person(c.index,targetX,173+breathe,false,true);
                if(c.servedPieces>0){rect(0xfff8e2b0,targetX-5,194,29,4);sushi(targetX-3,185,c.index);}
                else if((now/600)%2==0){rect(0xfff5eccf,targetX+9,151,3,9);rect(0xfff5eccf,targetX+5,154,11,3);}
            }
            j++;
        }
        for(Departure d:departing){
            float p=progress(now-d.when,LEAVE_MS);
            int x=(int)(d.startingX+(335-d.startingX)*p);
            int y=(int)(173+(194-173)*p);
            person(d.id,x,y,false,false,walkStep(now));
        }
        rect(0xaa172b30,11,210,338,22);
        text("来店 "+world.seated.size()+"人     行列 "+world.queue.size()+"人     ケース "+world.visibleCaseCount()+"種",0xfff4e8cc,11,19,225);
    }
    private void drawStreet(){
        rect(0xff193a4c,0,0,360,236);
        for(int k=0;k<46;k++){int x=(k*47+23)%360,y=(k*19+7)%76;rect(0xffb8c5a3,x,y,2,2);}
        rect(0xff5f6c61,0,90,360,146);rect(0xff546052,0,187,360,49);
        rect(0xffbb9d77,0,120,360,72);
        for(int i=0;i<9;i++)rect(0xff87765f,0,132+i*7,360,2);
        // left tobacco store, right former soba shop
        edge(0,83,74,111,0xff8e7861);edge(281,80,79,114,0xff6d665b);
        rect(0xff654f48,0,84,75,21);rect(0xff66564c,281,81,79,23);
        text("たばこ",0xfff3dcae,13,10,100);text("旧そば",0xffe0cfb1,12,292,98);
        window(11,116);window(295,113);
        // shop body
        edge(77,64,202,128,0xffa96845);
        rect(0xff613e34,75,59,205,17);
        rect(0xffbb9b73,80,78,195,31);
        noren(139,94);
        rect(0xff27474b,127,122,105,70);
        rect(0xffc6aa7f,132,127,95,60);
        rect(0xff35585b,135,130,89,54);
        rect(0xff254044,139,135,81,48);
        // translucent window glass view into counter
        rect(0xff9e7153,144,160,72,12);
        for(int i=0;i<3;i++)sushi(145+i*23,151,i);
        lantern(96,97);lantern(243,97);
        rect(0xff4b453f,0,191,360,45);
        rect(0xffb9a98a,0,211,360,3);
        // Bicycle
        for(int c:new int[]{107,164}){
            rect(INK,c-15,214,30,4);rect(0xffa7b3a5,c-13,205,27,18);
            rect(0xff3f4f51,c-8,210,17,8);
        }
        rect(0xff8b8c71,120,193,3,18);rect(0xff969a8c,140,200,26,3);
        rect(0xffa66b55,137,192,12,5);rect(0xffc6b69f,128,194,21,3);
        text("横 丁 の 小 さ な 寿 司 屋",0xffecd6a5,13,83,46);
    }
    @Override public void onDraw(Canvas c){
        super.onDraw(c);
        long now=SystemClock.uptimeMillis();
        p.setAntiAlias(false); p.setFilterBitmap(false);p.setDither(false);
        if(mode==1)drawStreet();else drawInterior(now);
        Rect src=new Rect(0,0,360,236);
        RectF dst=new RectF(0,0,getWidth(),getHeight());
        c.drawBitmap(bitmap,src,dst,p);
        if(getWindowVisibility()==VISIBLE)postInvalidateDelayed(65);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        if(mode!=0||tapListener==null||world==null)return true;
        float x=e.getX()*360f/Math.max(1,getWidth());
        float y=e.getY()*236f/Math.max(1,getHeight());
        if(y<157||y>213)return true;
        for(int j=0;j<world.seated.size()&&j<6;j++){
            int target=48+j*54;
            if(Math.abs(x-(target+9))<=26){
                tapListener.onCustomerTap(world.seated.get(j));
                break;
            }
        }
        return true;
    }
}
