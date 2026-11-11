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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Animated, tappable pixel-art game scene, not a text menu.
 * A 360x252 low-resolution scene uses nearest-neighbor scaling.
 */
public final class PixelShopView extends View {
    public interface TapListener {
        void onGuest(SushiWorld.Customer customer);
        void onCounter();
        void onDoor();
    }
    private static final int W=360,H=252;
    private static final int INK=0xff1c292d, LIGHT=0xfff2e5c2, DARK=0xff223d40, WOOD=0xff955d42;
    private final Bitmap bitmap=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
    private final Canvas pixel=new Canvas(bitmap);
    private final Paint p=new Paint();
    private final Map<Integer,Long> arrival=new HashMap<>();
    private final Set<Integer> known=new HashSet<>();
    private SushiWorld world;
    private TapListener listener;
    private boolean outside=false;
    private long flashAt=0;
    private int flashColor=0;
    public PixelShopView(Context context,SushiWorld game){
        super(context);world=game;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(true);
    }
    public void setTapListener(TapListener tap){listener=tap;}
    public void setOutside(boolean value){outside=value;invalidate();}
    public void update(SushiWorld game,int mode){
        world=game;outside=mode==1;
        Set<Integer> now=new HashSet<>();
        long t=SystemClock.uptimeMillis();
        for(SushiWorld.Customer c:world.seated){
            now.add(c.index);
            if(!known.contains(c.index))arrival.put(c.index,t);
        }
        known.clear();known.addAll(now);
        arrival.keySet().retainAll(now);
        invalidate();
    }
    public void pulse(boolean success){
        flashColor=success?0xff71d6a2:0xffed9e74;
        flashAt=SystemClock.uptimeMillis();invalidate();
    }
    private void r(int col,int x,int y,int w,int h){
        p.setColor(col);p.setStyle(Paint.Style.FILL);pixel.drawRect(x,y,x+w,y+h,p);
    }
    private void border(int x,int y,int w,int h,int col){
        r(INK,x-2,y-2,w+4,h+4);r(col,x,y,w,h);
    }
    private void word(String s,int x,int y,int size,int color){
        p.setAntiAlias(true);p.setColor(color);p.setTextSize(size);
        p.setTypeface(android.graphics.Typeface.create("sans-serif",1));
        pixel.drawText(s,x,y,p);
        p.setAntiAlias(false);
    }
    private void window(int x,int y){
        border(x,y,48,36,0xffc8a67f);
        r(0xff355c64,x+4,y+3,40,29);
        r(0xff709796,x+8,y+5,6,11);
        r(0xff284450,x+7,y+21,32,10);
        r(0xff7a4b36,x+24,y+3,3,30);
        r(0xff7a4b36,x+3,y+16,42,3);
    }
    private void lantern(int x,int y,long ms){
        int sway=(int)Math.round(Math.sin(ms/610.0+x)*2);
        r(INK,x+13,y-9,2,12);
        r(0xffefb36b,x+2+sway,y+2,24,7);
        r(0xffbe4b40,x+sway,y+9,28,22);
        r((ms/600+x)%3==0?0xfff2be78:0xffe99557,x+3+sway,y+11,22,15);
        r(0xffca5f49,x+2+sway,y+28,25,5);
        r(0xff50332c,x+8+sway,y+13,4,10);
        r(0xff50332c,x+17+sway,y+13,4,10);
    }
    private void noren(int x,int y){
        r(0xff355a5b,x,y,92,13);
        r(0xff477b72,x+2,y+8,88,25);
        for(int i=0;i<4;i++)r(0xff477b72,x+i*23+2,y+23,20,20);
        word("一坪寿司屋",x+13,y+22,12,LIGHT);
    }
    private void sushi(int x,int y,int i,long ms){
        int[] palette={0xffd66759,0xffddad79,0xffdfb062,0xffb3a674,0xffe28670};
        int fill=palette[Math.abs(i)%5],bounce=(int)(Math.sin(ms/260.0+i)*1.2);
        r(INK,x-2,y+9,24,4);
        r(0xfff5e9d8,x,y+5+bounce,20,8);
        r(fill,x+1,y+2+bounce,18,6);
        r(0xfff6d1a1,x+4,y+3+bounce,11,2);
    }
    private void person(int id,int x,int y,boolean chef,boolean sitting,long ms){
        int wave=(int)Math.round(Math.sin(ms/370.0+(id*4))*2);
        int blink=((ms/850+id*7)%7==0)?1:0;
        int skin=new int[]{0xfff0c29b,0xffe5b58c,0xffd7a17e,0xffeccbaa}[Math.abs(id)%4];
        int hair=new int[]{0xff282d33,0xff684332,0xff704b40,0xff444b4a,0xff855b42}[Math.abs(id)%5];
        int coat=chef?0xfff5ebd6:new int[]{0xff9c6263,0xff4d7c7d,0xff77794d,0xff9875a1,0xffc68c59,0xffbb7155}[Math.abs(id)%6];
        int height=sitting?27:37,y0=y+wave;
        r(INK,x-2,y0-2,23,height+6);
        r(coat,x,y0+13,19,height-13);
        r(skin,x+3,y0+4,13,12);
        r(hair,x+1,y0,17,5);
        r(hair,x+1,y0+4,3,7);
        r(INK,x+6,y0+9,2,blink+1);r(INK,x+13,y0+9,2,blink+1);
        if(chef){
            r(0xfff6f3e9,x-2,y0-9,23,8);
            r(0xfff6f3e9,x+1,y0-13,16,7);
            r(0xffefe8dc,x+3,y0+16,13,18);
            r(0xff7b9d94,x+9,y0+16,2,18);
            int arm=wave+2;
            r(skin,x+20,y0+20+arm,8,5);
            r(0xffd7dce0,x+27,y0+21+arm,15,2);
        }else{
            if(id%6==0)r(0xffa99b70,x,y0-4,20,4);
            r(skin,x,y0+23,3,6);r(skin,x+17,y0+23,3,6);
        }
    }
    private void interior(long ms){
        r(0xffc4a683,0,0,W,H);
        for(int x=0;x<W;x+=21)for(int y=134;y<H;y+=21)
            r(((x+y)/21)%2==0?0xffbfa085:0xffcbaf90,x,y,20,20);
        r(0xff6c4a3b,0,0,W,126);
        for(int y=12;y<126;y+=25)r(0xff8b5c45,0,y,W,3);
        for(int x=0;x<W;x+=52)r(0xff594139,x,0,3,126);
        r(DARK,0,0,W,24);word("横丁  一坪寿司屋",119,17,13,LIGHT);
        window(15,44);window(295,44);
        noren(134,28);lantern(97,49,ms);lantern(246,49,ms);
        border(64,43,33,27,0xffe1caa0);
        r(0xff4b7f7b,70,51,22,9);
        r(0xffe0eee2,79,53,6,2);
        r(0xff49332b,10,105,340,10);r(0xffb98755,10,105,340,5);
        for(int i=0;i<15;i++)r(new int[]{0xffb7a57e,0xff426c67,0xffb86f54}[i%3],21+i*22,92,9,13);
        // Glass case with changing sushi stock
        border(12,137,336,38,0xff496f74);
        r(0xff91b8b2,17,140,326,23);
        for(int i=0;i<9;i++)sushi(21+i*36,149,i,ms);
        // chef behind glass
        int beat=(int)Math.round(Math.sin(ms/240.0)*2);
        person(0,168,98+beat,true,false,ms);
        r(0xffe4b382,195,141,34,4);
        r(0xfff4eee2,204,139,15,3);
        // L-shaped counter
        r(INK,10,170,338,49);
        r(0xffd0a277,11,173,336,20);
        r(0xff7f4c37,12,194,336,27);
        r(0xffa66d4c,12,194,336,5);
        r(INK,10,167,70,7);r(0xffba8659,10,168,66,4);
        for(int i=0;i<6;i++){
            int x=26+i*54;
            r(INK,x,224,40,11);r(0xff8e6348,x+3,223,34,8);
            r(0xff634b40,x+8,232,5,18);r(0xff634b40,x+29,232,5,18);
        }
        // customer movement from the entrance to seat
        int j=0;
        for(SushiWorld.Customer c:world.seated){
            if(j>=6)break;
            int target=35+j*54;
            long started=arrival.containsKey(c.index)?arrival.get(c.index):0L;
            float fraction=started==0?1f:Math.min(1f,(ms-started)/900f);
            int xx=(int)(335+(target-335)*fraction);
            int yy=fraction<1?159:192;
            person(c.index,xx,yy,false,fraction>=1,ms);
            if(fraction>=1){
                r(0xfff1dbab,xx-6,177,34,12);
                word(c.servedPieces==0?"注文":"食事中",xx-3,186,8,INK);
            }
            j++;
        }
        // subtle steam from tea
        for(int i=0;i<4;i++){
            int x=53+i*77, drift=(int)((ms/125+i*8)%12);
            r(0xffe3efdc,x+2,166-drift,2,3);
            r(0xff4d8d82,x,175,8,4);
        }
        r(0xff234445,0,H-22,W,22);
        word("客 "+world.seated.size()+"/"+world.seats()+"     行列 "+world.queue.size()
            +"     次の来店は15分ごと",12,H-7,12,LIGHT);
        if(world.seated.size()==0){
            word(world.minute<world.restaurantStart?"仕入れをして、暖簾を出す":"お客を待っています…",80,158,15,0xfff4e8ce);
        }else word("お客をタップして注文",113,33,12,0xffe9d8a4);
    }
    private void street(long ms){
        r(0xff233c4e,0,0,W,H);
        for(int i=0;i<44;i++){
            int x=(i*41+17)%W,y=(i*19+3)%80;
            r(0xffbbd1c4,x,y,2,2);
        }
        r(0xff657060,0,94,W,158);
        r(0xffc3a688,0,119,W,90);
        for(int y=132;y<206;y+=17)r(0xffa7896e,0,y,W,2);
        border(0,74,74,129,0xff8a7360);
        r(0xff664c41,0,76,74,20);
        word("たばこ",12,92,13,LIGHT);
        window(10,113);
        border(281,73,79,130,0xff756452);
        r(0xff564638,281,74,79,23);
        word("旧そば",293,91,13,LIGHT);
        window(292,114);
        // main shop
        border(75,55,210,149,0xffa7724d);
        r(0xff553e35,73,53,213,19);
        r(0xffc5a078,79,73,200,35);
        noren(136,97);
        border(123,134,116,68,0xff294c4e);
        r(0xff537b78,129,141,104,53);
        r(0xff4c5c5a,135,147,92,41);
        r(0xffb3875e,137,174,90,12);
        for(int i=0;i<4;i++)sushi(140+i*21,162,i,ms);
        lantern(92,100,ms);lantern(244,100,ms);
        // the store's sliding door
        r(0xffc2a47d,182,135,4,67);
        r(0xffe1bf94,177,159,6,8);
        // street and people, animated walking
        r(0xff504d43,0,207,W,45);r(0xffb5a68a,0,221,W,3);
        int passer=(int)((ms/40)%440)-40;
        person(8,passer,188,false,false,ms);
        // bicycle
        for(int wheel:new int[]{93,144}){
            r(INK,wheel-14,231,28,3);r(0xffafc0b0,wheel-12,226,24,14);
            r(0xff384b4d,wheel-8,230,17,8);
        }
        r(0xff7e8c7e,104,213,4,18);r(0xff8e9a8b,108,220,26,3);
        word("横 丁 の 小 さ な 寿 司 屋",74,43,15,0xffe5c991);
        word("店をタップすると店内へ",98,H-7,12,LIGHT);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(world==null)return;
        long ms=SystemClock.uptimeMillis();
        p.setDither(false);p.setFilterBitmap(false);p.setAntiAlias(false);
        if(outside)street(ms);else interior(ms);
        if(ms-flashAt<400)r(flashColor,0,0,W,4);
        p.setFilterBitmap(false);
        c.drawBitmap(bitmap,new Rect(0,0,W,H),new RectF(0,0,getWidth(),getHeight()),p);
        if(isAttachedToWindow())postInvalidateDelayed(75);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        if(listener==null||world==null)return true;
        float x=e.getX()/getWidth()*W,y=e.getY()/getHeight()*H;
        if(outside){listener.onDoor();return true;}
        if(y<96){listener.onDoor();return true;}
        if(y>=154){
            int n=Math.min(6,world.seated.size());
            for(int i=0;i<n;i++){
                int seat=35+i*54;
                if(Math.abs(x-seat)<28){
                    listener.onGuest(world.seated.get(i));return true;
                }
            }
        }
        listener.onCounter();
        return true;
    }
}
