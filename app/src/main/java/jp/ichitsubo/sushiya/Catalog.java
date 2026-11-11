package jp.ichitsubo.sushiya;

import java.util.ArrayList;
import java.util.List;

/** The complete approved item/guest numerical catalogs (game specification chapters 20-23). */
public final class Catalog {
    private Catalog() {}
    public static final class Fish {
        public final int number,rank,price,buy,seasonMask;
        public final String name;
        Fish(String[] p){number=Integer.parseInt(p[0]);name=p[1];rank=Integer.parseInt(p[2]);price=Integer.parseInt(p[3]);buy=Integer.parseInt(p[4]);seasonMask=Integer.parseInt(p[5]);}
        public boolean inSeason(int s) {return (seasonMask & (1<<s))!=0;}
    }
    public static final class Guest {
        public final String name,festival;
        public final int weekdayMask,favorite,source,sourceLevel,season;
        public final boolean event;
        Guest(String[] p,boolean special) {
            name=p[0];weekdayMask=Integer.parseInt(p[1]);favorite=Integer.parseInt(p[2]);
            event=special;source=special?-1:Integer.parseInt(p[4]);
            sourceLevel=special?0:Integer.parseInt(p[5]);festival=special?p[3]:"";
            season=special?Integer.parseInt(p[4]):-1;
        }
    }
    public static final Fish[] FISH;
    public static final Guest[] REGULAR;
    public static final Guest[] SPECIAL;
    static {
        List<Fish> fish=new ArrayList<>();
        String fishRows="26|イワシ|1|200|570|6;60|玉子|1|150|0|0;69|まさご|1|200|570|0;81|かっぱ巻|1|200|570|2;85|かんぴょう巻|1|200|570|0;86|お新香巻|1|200|570|0;87|納豆巻|1|200|570|0;88|梅しそ巻|1|200|570|0;89|山ごぼう巻|1|200|570|0;98|涙巻（わさび）|1|200|570|0;6|カツオ|2|300|860|5;7|炙りカツオ|2|350|1000|5;8|サーモン|2|300|860|0;9|炙りサーモン|2|350|1000|0;11|ブリ|2|350|1000|8;12|ハマチ|2|300|860|6;19|スズキ|2|350|1000|2;22|アジ|2|250|710|2;23|サバ|2|300|860|12;24|しめサバ|2|350|1000|12;25|コハダ|2|300|860|6;27|サンマ|2|350|1000|4;52|赤エビ|2|350|1000|0;55|イカ|2|250|710|0;59|タコ|2|300|860|2;63|ネギトロ|2|350|1000|0;67|釜揚げしらす|2|250|710|7;68|とびこ|2|300|860|0;82|鉄火巻|2|350|1000|8;84|ネギトロ巻|2|350|1000|0;91|えびきゅう巻|2|350|1000|2;99|太巻き|2|350|1000|0;1|マグロ赤身|3|450|1290|8;4|漬けマグロ|3|500|1430|8;10|トロサーモン|3|450|1290|0;13|カンパチ|3|500|1430|6;14|ヒラマサ|3|500|1430|3;15|真鯛|3|500|1430|1;16|炙り鯛|3|550|1570|1;17|ヒラメ|3|550|1570|8;20|イサキ|3|450|1290|2;28|サワラ|3|450|1290|9;29|炙りサワラ|3|500|1430|9;32|メバル|3|450|1290|1;33|カサゴ|3|450|1290|9;34|ホウボウ|3|500|1430|9;37|タチウオ|3|450|1290|6;38|アナゴ|3|550|1570|2;40|サヨリ|3|400|1140|1;41|ホタテ|3|500|1430|8;43|つぶ貝|3|500|1430|9;45|ホッキ貝|3|550|1570|9;48|青柳|3|550|1570|1;51|甘エビ|3|550|1570|9;54|シャコ|3|500|1430|5;56|ヤリイカ|3|450|1290|9;58|コウイカ|3|450|1290|12;64|カニ味噌|3|550|1570|8;66|生しらす|3|450|1290|7;70|数の子|3|450|1290|0;74|ホタルイカ沖漬け|3|500|1430|1;2|中トロ|4|750|2140|8;5|炙り中トロ|4|800|2290|8;18|エンガワ|4|700|2000|8;21|シマアジ|4|750|2140|6;30|キンメダイ|4|850|2430|8;35|カワハギ|4|750|2140|12;39|ウナギ|4|750|2140|6;42|赤貝|4|700|2000|8;47|トリ貝|4|800|2290|1;49|煮ハマグリ|4|750|2140|9;53|車エビ|4|700|2000|2;57|アオリイカ|4|700|2000|5;61|イクラ|4|800|2290|4;65|カニ身|4|850|2430|8;71|あん肝|4|650|1860|8;72|白子|4|650|1860|8;83|トロ鉄火巻|4|850|2430|8;3|大トロ|5|1400|4000|8;31|ノドグロ|5|1500|4290|12;36|フグ|5|1300|3710|8;44|ミル貝|5|1000|2860|3;46|アワビ|5|1300|3710|2;50|ボタンエビ|5|1100|3140|9;62|ウニ|5|1800|5140|3";
        for(String row:fishRows.split(";"))fish.add(new Fish(row.split("\\|",-1)));
        FISH=fish.toArray(new Fish[0]);
        List<Guest> people=new ArrayList<>();
        String peopleRows="課長代理|31|4|1|-1|0;タバちゃん|31|25|1|-1|0;サブちゃん|96|22|1|-1|0;豚骨|31|7|1|-1|0;よっちゃん|112|41|1|-1|0;ヴェンさん|21|38|1|-1|0;お嬢|32|8|1|-1|0;校長|2|15|1|-1|0;花姐|112|40|1|-1|0;センセ|31|67|1|-1|0;すずさん|31|52|0|0|4;カンちゃん|64|1|0|10|5;マゴちゃん|21|91|0|1|3;お松|8|24|0|12|4;潮さん|96|34|0|2|6;熊さん|21|82|0|3|3;なべやん|14|56|0|15|3;大ママ|112|64|0|4|4;ヤスさん|112|6|0|17|6;トキさん|4|85|0|5|5;マッキー|16|88|0|19|3;座長|112|9|0|6|2;源さん|1|26|0|7|4;おふじ|64|51|0|22|3;おきぬさん|14|59|0|8|4;ユウくん|64|55|0|24|3;隊長|21|11|0|9|6;ジロー|96|42|0|2|7;イチロー|96|30|0|27|7;チチロー|96|23|0|28|7;イッペー坊|14|84|0|3|6;お千代|11|54|0|17|5;たっつぁん|26|27|0|13|5";
        for(String row:peopleRows.split(";"))people.add(new Guest(row.split("\\|",-1),false));
        REGULAR=people.toArray(new Guest[0]);
        List<Guest> special=new ArrayList<>();
        String events="ぶんちゃん|1|47|卒業式|0;ボブ|32|44|GW|0;嫁ちゃん|2|46|梅雨|1;おふくろさん|64|39|お盆|1;コスモ|4|31|お月見|2;ジジィ|16|35|ハロウィン|2;三田さん|8|72|クリスマス|3;おみき|32|50|お正月|3";
        for(String row:events.split(";"))special.add(new Guest(row.split("\\|",-1),true));
        SPECIAL=special.toArray(new Guest[0]);
    }
    public static Fish fish(int number){for(Fish f:FISH)if(f.number==number)return f;return null;}
    public static int fishIndex(int number){for(int i=0;i<FISH.length;i++)if(FISH[i].number==number)return i;return -1;}
    public static int guestIndex(String name){for(int i=0;i<REGULAR.length;i++)if(REGULAR[i].name.equals(name))return i;return -1;}
}
