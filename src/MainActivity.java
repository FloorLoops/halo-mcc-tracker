package four.parliament.halotracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.media.ToneGenerator;
import android.media.AudioManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    static final boolean PREMIUM = false; // public free build
    java.util.List<String[]> METAS = new java.util.ArrayList<String[]>();
    java.util.HashSet<String> metas = new java.util.HashSet<String>();
    java.util.HashSet<String> visitedGames = new java.util.HashSet<String>();
    java.util.HashMap<String,Integer> detailOpens = new java.util.HashMap<String,Integer>();
    FrameLayout overlay;
    int titleTaps=0, footerTaps=0, chipTaps=0; String chipLast="";
    long egChkMs=0; String egChkId=""; java.util.ArrayList<Long> egRecent=new java.util.ArrayList<Long>(); String lastToggleId="";
    long lastCheckMs=0; String lastCheckedId=""; int checkBurst=0;
    boolean bulkUnlock=false;
    java.util.HashMap<String,String> unlockTimes = new java.util.HashMap<String,String>();

    static final int BG=0xFF0A0E13, BG2=0xFF0D1117, CARD=0xFF151C26, CARD2=0xFF1C2533, LINE=0xFF21303F;
    static final int CYAN=0xFF00B8E8, GREEN=0xFF39D353, GOLD=0xFFFFD54F, ORANGE=0xFFFF8A50, PURPLE=0xFFCE93D8;
    static final int T1=0xFFD6E2EE, T2=0xFF8B9AB0, T3=0xFF55677A;
    static final String[][] RANKS={{"0","Recruit","🟫","Private at 10%"},{"10","Private","🔵","Corporal at 25%"},
        {"25","Corporal","🟡","Sergeant at 40%"},{"40","Sergeant","🟠","Staff Sergeant at 55%"},
        {"55","Staff Sergeant","🔴","Lieutenant at 65%"},{"65","Lieutenant","🟣","Captain at 75%"},
        {"75","Captain","⚫","ODST at 85%"},{"85","ODST Operative","🪖","Spartan at 93%"},
        {"93","Spartan","🟢","Noble Spartan at 99%"},{"99","Noble Spartan","🌟","Master Chief at 100%"},
        {"100","Master Chief","🎖️","Collection complete!"}};
    // v1.2 — alternate rank ladders (choose your style)
    static final String[][] RANKS_H3={
        {"0","Recruit","🟫","Apprentice at 8%"},{"8","Apprentice","⬜","Private at 16%"},
        {"16","Private","🔵","Corporal at 25%"},{"25","Corporal","🟦","Sergeant at 34%"},
        {"34","Sergeant","🟡","Gunnery Sergeant at 43%"},{"43","Gunnery Sergeant","🟨","Lieutenant at 52%"},
        {"52","Lieutenant","🟠","Captain at 61%"},{"61","Captain","🟧","Major at 70%"},
        {"70","Major","🔴","Commander at 78%"},{"78","Commander","🟥","Colonel at 85%"},
        {"85","Colonel","🟣","Brigadier at 91%"},{"91","Brigadier","🟪","General at 96%"},
        {"96","General","⭐","Field Marshal at 99%"},{"99","Field Marshal","🎖️","Legend at 100%"},
        {"100","Legend","🏆","Halo 3 legend!"}};
    static final String[][] RANKS_REACH={
        {"0","Recruit","🟫","Private at 7%"},{"7","Private","🔵","Corporal at 14%"},
        {"14","Corporal","🟦","Sergeant at 21%"},{"21","Sergeant","🟡","Warrant Officer at 30%"},
        {"30","Warrant Officer","🟨","Captain at 39%"},{"39","Captain","🟠","Major at 48%"},
        {"48","Major","🟧","Lt. Colonel at 57%"},{"57","Lt. Colonel","🔴","Colonel at 65%"},
        {"65","Colonel","🟥","Brigadier at 72%"},{"72","Brigadier","🟣","General at 79%"},
        {"79","General","🟪","Field Marshal at 85%"},{"85","Field Marshal","⭐","Hero at 90%"},
        {"90","Hero","🌟","Legend at 94%"},{"94","Legend","🎖️","Mythic at 97%"},
        {"97","Mythic","👑","Inheritor at 100%"},{"100","Inheritor","🏆","Reach pinnacle!"}};
    static final String[] RANK_STYLE_NAMES={"MCC","Halo 3","Reach"};
    static final String[] TYPES={"all","story","skull","terminal","speed","legendary","laso","multiplayer","firefight","spartan_ops","collectible","meta"};

    Map<String,JSONObject> games=new LinkedHashMap<String,JSONObject>();
    List<JSONObject> all=new ArrayList<JSONObject>();
    Set<String> done=new HashSet<String>(); Set<String> pins=new HashSet<String>();
    SharedPreferences prefs;
    int totalGs=0;
    String tab="home", curGame="ce", fStatus="ALL", fType="all", query="", fMission="", fSort="default"; // v1.6 fSort
    long sessionBase=0; int sessionChecks=0;
    LinearLayout root, content; AchAdapter adapter;
    TextView[] navBtns=new TextView[4];

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("halo",MODE_PRIVATE);
        loadSet(done,"done"); loadSet(pins,"pins");
        try{
            InputStream in=getAssets().open("data.json");
            ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int r;
            while((r=in.read(buf))>0) bo.write(buf,0,r); in.close();
            JSONObject rt=new JSONObject(new String(bo.toByteArray(),StandardCharsets.UTF_8));
            JSONObject g=rt.getJSONObject("games"); Iterator<String> it=g.keys();
            while(it.hasNext()){String k=it.next(); games.put(k,g.getJSONObject(k));}
            JSONArray a=rt.getJSONArray("achievements");
            for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i); all.add(o); totalGs+=o.optInt("gs");}
            try{ JSONArray ex=new JSONArray(prefs.getString("extraAch","[]")); for(int i=0;i<ex.length();i++){ JSONObject o=ex.getJSONObject(i); all.add(o); totalGs+=o.optInt("gs"); } }catch(Exception e){}
        }catch(Exception e){}
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{BG2,BG})); // v1.2.5 depth
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1f));
        root.addView(content);
        root.addView(buildNav());
        overlay=new FrameLayout(this);
        overlay.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(overlay);
        loadSet(metas,"metas"); loadCsv(visitedGames,"vgames"); addAllMetas();
        try{ JSONObject ut=new JSONObject(prefs.getString("ut","{}")); java.util.Iterator<String> it=ut.keys(); while(it.hasNext()){ String k=it.next(); unlockTimes.put(k,ut.optString(k)); } }catch(Exception e){}
        int h=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if(h>=3 && h<4) unlockMeta("egg_cryo");
        String lastVer=prefs.getString("lastVer",""); String curVer=appVer();
        boolean updated = lastVer.length()>0 && !lastVer.equals(curVer);
        show("home"); checkMetas();
        if(updated){ bulkUnlock=true; int before=metas.size(); checkMetas(); bulkUnlock=false; final int gained=metas.size()-before; final String fv=lastVer, tv=curVer;
            root.postDelayed(new Runnable(){ public void run(){ showUpdateReview(fv,tv,gained); } },800); }
        prefs.edit().putString("lastVer",curVer).apply();
        if(PREMIUM && !metas.contains("meta_king")){ metas.add("meta_king"); saveSet(metas,"metas"); root.postDelayed(new Runnable(){ public void run(){ grandUnlock("👑","King of the Hill","You own this. The Parliament bows."); } }, updated?2600:1200); }
    }

    void loadSet(Set<String> s,String key){ String v=prefs.getString(key,""); if(v.length()>0) for(String x:v.split(",")) s.add(x); }
    void loadCsv(java.util.HashSet<String> s,String key){ String v=prefs.getString(key,""); if(v.length()>0) for(String x:v.split(",")) s.add(x); }
    void saveCsv(java.util.HashSet<String> s,String key){ StringBuilder sb=new StringBuilder(); for(String x:s){ if(sb.length()>0) sb.append(','); sb.append(x);} prefs.edit().putString(key,sb.toString()).apply(); }
    void saveSet(Set<String> s,String key){ StringBuilder sb=new StringBuilder(); for(String x:s){ if(sb.length()>0) sb.append(','); sb.append(x);} prefs.edit().putString(key,sb.toString()).apply(); }
    void buzz(){ try{((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(16,140));}catch(Exception e){} }
    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density); }
    GradientDrawable box(int fill,int stroke,int rad){ GradientDrawable g=new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(rad)); g.setStroke(dp(1),stroke); return g; }
    TextView text(String s,float sz,int c,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sz); t.setTextColor(c); t.setTypeface(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL); return t; }
    LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setBackground(box(CARD,LINE,8)); c.setPadding(dp(14),dp(12),dp(14),dp(13)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); c.setLayoutParams(lp); return c; }
    ProgressBar bar(int pct,int color){ ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); pb.setMax(100); pb.setProgress(pct);
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        pb.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF223040));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(7)); lp.topMargin=dp(8); pb.setLayoutParams(lp); return pb; }
    /* ===== v1.2.5-v1.5 helpers ===== */
    GradientDrawable glow(int top,int bottom,int stroke,int rad){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
        g.setCornerRadius(dp(rad)); g.setStroke(dp(1),stroke); return g; }
    LinearLayout glowCard(int accent){ LinearLayout c=card(); c.setBackground(glow(CARD2,CARD,accent,9)); return c; }
    View rule(int color){ View v=new View(this); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(2)); lp.topMargin=dp(7); lp.bottomMargin=dp(3); v.setLayoutParams(lp);
        v.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{color,0x00000000})); return v; }
    static String gameIcon(String g){
        if("ce".equals(g)) return "🔆"; if("h2".equals(g)) return "⚔️"; if("h3".equals(g)) return "🪖";
        if("odst".equals(g)) return "🌃"; if("reach".equals(g)) return "🌌"; if("h4".equals(g)) return "🌐";
        return "🎖️"; }
    boolean sfxOn(){ return prefs.getBoolean("sfxOn",true); }
    boolean notifOn(){ return prefs.getBoolean("notifOn",true); }
    void playTick(){ if(!sfxOn()) return; POOL.execute(new Runnable(){ public void run(){ try{
        ToneGenerator tg=new ToneGenerator(AudioManager.STREAM_MUSIC,60);
        tg.startTone(ToneGenerator.TONE_PROP_BEEP,55); Thread.sleep(80); tg.release(); }catch(Exception e){} } }); }
    void playNotify(){ if(!notifOn()) return; POOL.execute(new Runnable(){ public void run(){ try{
        ToneGenerator tg=new ToneGenerator(AudioManager.STREAM_MUSIC,95);
        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,170); Thread.sleep(190);
        tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L,240); Thread.sleep(260);
        tg.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L,300); Thread.sleep(320); tg.release(); }catch(Exception e){} } }); }
    String fullRes(String thumb){ if(thumb==null||thumb.length()==0) return thumb;
        try{ java.util.regex.Matcher m=java.util.regex.Pattern.compile("(.*/images)/thumb/(.+?)/[0-9]+px-[^/]+$").matcher(thumb);
            if(m.find()) return m.group(1)+"/"+m.group(2); }catch(Exception e){} return thumb; }
    int[] count(String gid){ int n=0,dn=0,gs=0,gsd=0;
        for(JSONObject o:all){ if(gid!=null&&!gid.equals(o.optString("game"))) continue; n++; gs+=o.optInt("gs");
            if(done.contains(o.optString("id"))){dn++; gsd+=o.optInt("gs");}} return new int[]{n,dn,gs,gsd}; }
    /* v1.2 rank system */
    String[][] ranks(){ int s=prefs.getInt("rankStyle",0); return s==1?RANKS_H3:(s==2?RANKS_REACH:RANKS); }
    String rankStyleName(){ int s=prefs.getInt("rankStyle",0); return RANK_STYLE_NAMES[s<0||s>2?0:s]; }
    String[] rank(int pct){ String[][] L=ranks(); String[] cur=L[0]; for(String[] r:L) if(pct>=Integer.parseInt(r[0])) cur=r; return cur; }
    // v1.2 XP-weighted completion: gamerscore earned / total (heavier achievements count more)
    int xpPct(){ int[] t=count(null); return t[2]==0?0:100*t[3]/t[2]; }
    boolean xpBasis(){ return prefs.getBoolean("rankXp",true); }
    int rankPct(){ int[] t=count(null); int ap=t[0]==0?0:100*t[1]/t[0]; return xpBasis()?xpPct():ap; }
    // v1.2 focus mode: best gamerscore for least time among undone achievements
    double focusScore(JSONObject o){ double h=estHrs(o); if(h<=0) h=0.4;
        double g=Math.max(o.optInt("gs"),5); double s=g/h;
        if(o.optBoolean("missable")) s*=1.3; String d=o.optString("diff","").toLowerCase();
        if(d.contains("easy")) s*=1.25; else if(d.contains("hard")||d.contains("legend")) s*=0.7; return s; }
    java.util.List<JSONObject> focusPicks(int n){
        java.util.List<JSONObject> todo=new java.util.ArrayList<JSONObject>();
        for(JSONObject o:all) if(!done.contains(o.optString("id"))) todo.add(o);
        java.util.Collections.sort(todo,new java.util.Comparator<JSONObject>(){
            public int compare(JSONObject a,JSONObject b){ double va=focusScore(a),vb=focusScore(b); return va>vb?-1:(va<vb?1:0); } });
        return todo.subList(0,Math.min(n,todo.size())); }
    String gameName(String gid){ JSONObject g=games.get(gid); return g==null?gid:g.optString("name",gid); }

    /* ===== bottom nav ===== */
    View buildNav(){
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(BG2); nav.setPadding(0,dp(6),0,dp(8));
        String[][] items={{"home","⛨","HOME"},{"games","🗂","GAMES"},{"pins","📌","PINS"},{"more","⚙","MORE"}};
        for(int i=0;i<4;i++){ final String id=items[i][0];
            TextView t=text(items[i][1]+"\n"+items[i][2],11,T3,true);
            t.setGravity(Gravity.CENTER); t.setLineSpacing(0,1.1f);
            t.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
            t.setPadding(0,dp(6),0,dp(4)); navBtns[i]=t;
            t.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ show(id); } });
            nav.addView(t); }
        return nav;
    }
    void restyleNav(){ String[] ids={"home","games","pins","more"};
        for(int i=0;i<4;i++) navBtns[i].setTextColor(ids[i].equals(tab)?CYAN:T3); }

    void show(String t){ tab=t; restyleNav();
        if(sessionMin()>=45) unlockMeta("egg_endure");
        checkMetas();
        content.removeAllViews();
        if(t.equals("home")) content.addView(buildHome());
        else if(t.equals("games")) content.addView(buildGames());
        else if(t.equals("pins")) content.addView(buildPins());
        else content.addView(buildMore());
        content.setAlpha(0f); content.animate().alpha(1f).setDuration(220).start(); } // v1.4 screen transition

    /* ===== HOME ===== */
    View buildHome(){
        ScrollView sv=new ScrollView(this);
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14),dp(16),dp(14),dp(20)); sv.addView(col);
        final TextView title=text("⛨ UNSC TERMINAL",20,CYAN,true); title.setLetterSpacing(0.16f);
        title.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(++titleTaps>=7){ titleTaps=0; unlockMeta("egg_bloom"); } } });
        col.addView(title);
        col.addView(text("MCC ACHIEVEMENT DATABASE · CLASSIFIED",9.5f,T3,false));
        col.addView(rule(CYAN)); // v1.2.5

        int[] t=count(null); int pct=t[0]==0?0:100*t[1]/t[0];
        int rpct=rankPct(); String[] rk=rank(rpct);
        LinearLayout rc=card(); rc.setBackground(glow(CARD2,CARD,CYAN,9)); // v1.2.5 glow
        LinearLayout rrow=new LinearLayout(this); rrow.setOrientation(LinearLayout.HORIZONTAL); rrow.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic=text(rk[2],30,T1,false); ic.setPadding(0,0,dp(12),0); rrow.addView(ic);
        LinearLayout rcol=new LinearLayout(this); rcol.setOrientation(LinearLayout.VERTICAL); rcol.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        rcol.addView(text("RANK · "+rankStyleName().toUpperCase()+" STYLE",9,T3,true));
        rcol.addView(text(rk[1],19,CYAN,true));
        rcol.addView(text("▸ "+rk[3],10.5f,T2,false));
        rrow.addView(rcol);
        TextView ladderHint=text("▸",20,T3,true); rrow.addView(ladderHint);
        rc.addView(rrow);
        // v1.2 — XP-weighted vs achievement basis (tap to toggle), and the rank %
        final TextView basis=text((xpBasis()?"● XP-weighted "+xpPct()+"%":"○ XP-weighted "+xpPct()+"%")+"   ·   "+(xpBasis()?"○ achievements "+pct+"%":"● achievements "+pct+"%"),10,GOLD,false);
        LinearLayout.LayoutParams blp2=new LinearLayout.LayoutParams(-1,-2); blp2.topMargin=dp(8); basis.setLayoutParams(blp2);
        basis.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ prefs.edit().putBoolean("rankXp",!xpBasis()).apply(); show("home"); } });
        rc.addView(basis);
        rc.addView(text("XP-weighted: heavier achievements (more G) lift your rank more · tap above to switch · long-press to change style",8.5f,T3,false));
        rc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(PREMIUM) showRankLadder(); else upsell("Full rank ladder"); } });
        rc.setOnLongClickListener(new View.OnLongClickListener(){ public boolean onLongClick(View v){ int s=(prefs.getInt("rankStyle",0)+1)%3; prefs.edit().putInt("rankStyle",s).apply(); Toast.makeText(MainActivity.this,"Rank style: "+RANK_STYLE_NAMES[s],Toast.LENGTH_SHORT).show(); show("home"); return true; } });
        col.addView(rc);
        // time-to-100 (premium)
        if(PREMIUM){
            LinearLayout tc=card();
            tc.addView(text("⏳ ESTIMATED TIME TO 100%",9.5f,T2,true));
            double[] est=timeLeft();
            tc.addView(text(fmtHours(est[0])+" remaining",20,GOLD,true));
            tc.addView(text(fmtHours(est[1])+" total campaign · "+fmtHours(est[1]-est[0])+" done",10.5f,T2,false));
            tc.addView(text("difficulty-weighted estimate — LASO playlists & legendary runs counted heavy, not 1h each",8.5f,T3,false));
            col.addView(tc);
        }

        LinearLayout oc=card(); oc.setBackground(glow(CARD2,CARD,GREEN,9)); // v1.2.5 glow
        oc.addView(text("OVERALL PROGRESS · ALL ACHIEVEMENTS",9.5f,T2,true));
        oc.addView(text(t[1]+" / "+t[0]+"  ·  "+pct+"%",23,GREEN,true));
        oc.addView(text("GAMERSCORE  "+t[3]+" / "+t[2]+" G",12,GOLD,false));
        oc.addView(bar(pct,GREEN));
        oc.addView(text("complete official database · 700 achievements / 7,000G · icons bundled offline",8.5f,T3,false));
        col.addView(oc);

        // v1.2 — FOCUS MODE: smart "what should I do next" + best-value targets
        if(t[1]<t[0]){
            LinearLayout fc=card();
            fc.addView(text("🎯 FOCUS MODE · BEST NEXT TARGETS",9.5f,T2,true));
            fc.addView(text("highest gamerscore for the least time — tap any to see its guide",8.5f,T3,false));
            for(final JSONObject o:focusPicks(5)){
                LinearLayout fr=new LinearLayout(this); fr.setOrientation(LinearLayout.HORIZONTAL); fr.setGravity(Gravity.CENTER_VERTICAL);
                fr.setPadding(0,dp(6),0,dp(6));
                TextView fi=text(o.optString("icon","🎯"),17,T1,false); fi.setPadding(0,0,dp(10),0); fr.addView(fi);
                LinearLayout fcol=new LinearLayout(this); fcol.setOrientation(LinearLayout.VERTICAL); fcol.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
                fcol.addView(text(o.optString("name"),12.5f,T1,true));
                String tm=o.optString("time",""); String gm=gameName(o.optString("game")).replace("Halo: ","").replace("Halo ","H");
                fcol.addView(text(gm+" · "+o.optInt("gs")+"G"+(tm.length()>0?" · ~"+tm:"")+(o.optBoolean("missable")?" · ⚠ missable":""),9.5f,T2,false));
                fr.addView(fcol);
                fr.addView(text("▸",14,CYAN,true));
                fr.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ showDetail(null,o); } });
                fc.addView(fr);
            }
            // smart breakdown — closest game + easiest type to clear
            String closeG=null; int closeP=-1, closeLeft=0;
            for(Map.Entry<String,JSONObject> e:games.entrySet()){ int[] c=count(e.getKey()); if(c[0]==0) continue; int gp=100*c[1]/c[0];
                if(gp<100 && gp>closeP){ closeP=gp; closeG=e.getValue().optString("name"); closeLeft=c[0]-c[1]; } }
            String bestTy=null; int bestLeft=Integer.MAX_VALUE;
            for(String ty:TYPES){ if(ty.equals("all")||ty.equals("meta")) continue; int left=countType(ty); if(left>0 && left<bestLeft){ bestLeft=left; bestTy=ty; } }
            LinearLayout sb=new LinearLayout(this); sb.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sblp=new LinearLayout.LayoutParams(-1,-2); sblp.topMargin=dp(8); sb.setLayoutParams(sblp);
            sb.setBackground(box(CARD2,LINE,6)); sb.setPadding(dp(10),dp(8),dp(10),dp(8));
            sb.addView(text("📈 SMART BREAKDOWN",9,GOLD,true));
            if(closeG!=null) sb.addView(text("closest game: "+closeG+" — "+closeP+"% ("+closeLeft+" left)",10,T1,false));
            if(bestTy!=null) sb.addView(text("easiest category to finish: "+bestTy.replace("_"," ")+" ("+bestLeft+" left)",10,T1,false));
            sb.addView(text("gamerscore earned: "+t[3]+" / "+t[2]+" G ("+xpPct()+"%)",10,GOLD,false));
            fc.addView(sb);
            col.addView(fc);
        }

        // v1.6 — campaign ledger: 2-up game grid (half the scrolling, per-game accent glow)
        TextView glabel=text("CAMPAIGN LEDGER",9.5f,T2,true);
        LinearLayout.LayoutParams gllp=new LinearLayout.LayoutParams(-1,-2); gllp.topMargin=dp(14); glabel.setLayoutParams(gllp);
        col.addView(glabel);
        java.util.List<String> gids=new java.util.ArrayList<String>();
        for(Map.Entry<String,JSONObject> e:games.entrySet()){ int[] c=count(e.getKey()); if(c[0]>0) gids.add(e.getKey()); }
        LinearLayout grow=null;
        for(int gi=0; gi<gids.size(); gi++){
            final String gid=gids.get(gi); JSONObject g=games.get(gid);
            int[] c=count(gid); int gp=100*c[1]/c[0];
            int accent=Color.parseColor(g.optString("color","#00b8e8"));
            if(gi%2==0){ grow=new LinearLayout(this); grow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams grlp=new LinearLayout.LayoutParams(-1,-2); grlp.topMargin=dp(9); grow.setLayoutParams(grlp); col.addView(grow); }
            LinearLayout gc=new LinearLayout(this); gc.setOrientation(LinearLayout.VERTICAL);
            gc.setBackground(gp==100?box(CARD,GREEN,9):glow(CARD2,CARD,accent,9));
            gc.setPadding(dp(12),dp(10),dp(12),dp(11));
            LinearLayout.LayoutParams celp=new LinearLayout.LayoutParams(0,-2,1f); if(gi%2==1) celp.leftMargin=dp(9); gc.setLayoutParams(celp);
            gc.addView(text(gameIcon(gid)+" "+g.optString("name").replace("Halo: ","").replace("Halo ","H"),12.5f,T1,true));
            gc.addView(text(gp==100?"✔ 100%":gp+"%",21,gp==100?GREEN:accent,true));
            gc.addView(text(c[1]+"/"+c[0]+" · "+c[3]+"/"+c[2]+"G",9.5f,T2,false));
            gc.addView(bar(gp,gp==100?GREEN:accent));
            gc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ curGame=gid; fMission=""; visitGame(gid); show("games"); } });
            grow.addView(gc); }
        if(gids.size()%2==1 && grow!=null){ View sp=new View(this);
            LinearLayout.LayoutParams splp=new LinearLayout.LayoutParams(0,dp(1),1f); splp.leftMargin=dp(9); sp.setLayoutParams(splp); grow.addView(sp); }
        final TextView foot=text("\n◇ FOR PERSONAL GLORY ◇",9.5f,T3,false); foot.setGravity(Gravity.CENTER);
        foot.setOnClickListener(new View.OnClickListener(){ long w=0; public void onClick(View v){ long now=System.currentTimeMillis(); if(now-w>1600){ footerTaps=0; } w=now; if(++footerTaps>=4 && pins.size()==4){ footerTaps=0; unlockMeta("egg_parliament"); } } });
        col.addView(foot);
        return sv;
    }

    void upsell(String feat){
        new AlertDialog.Builder(this).setTitle("✦ "+feat)
            .setMessage(feat+" is part of UNSC Terminal Plus.\n\nUnlock Xbox Live sync, time-to-100%, full rank ladder, and detailed stats.")
            .setPositiveButton("OK",null).show();
    }

    double parseHrs(String t){ if(t==null) return 0; t=t.trim().toLowerCase();
        try{ if(t.endsWith("h")) return Double.parseDouble(t.substring(0,t.length()-1));
            if(t.endsWith("m")) return Double.parseDouble(t.substring(0,t.length()-1))/60.0;
            if(t.contains("h")) return Double.parseDouble(t.substring(0,t.indexOf("h")));
        }catch(Exception e){} return 0; }
    // v1.2.1 — difficulty/type-weighted hour estimate. Uses the explicit "time" tag when present,
    // otherwise estimates from type + difficulty so a LASO playlist reads as 20h+, not a flat 0.4h.
    double estHrs(JSONObject o){
        double h=parseHrs(o.optString("time","")); if(h>0) return h;
        String ty=o.optString("type","").toLowerCase(); String d=o.optString("diff","").toLowerCase();
        double base=0.5;
        if(ty.contains("laso")) base=20;
        else if(ty.contains("legendary")) base=4;
        else if(ty.contains("speed")) base=1.5;
        else if(ty.contains("multiplayer")||ty.contains("firefight")||ty.contains("spartan_ops")) base=2.5;
        else if(ty.contains("story")) base=1.2;
        else if(ty.contains("skull")||ty.contains("terminal")||ty.contains("collectible")) base=0.6;
        double mult=1.0;
        if(d.contains("very hard")||d.contains("extreme")||d.contains("brutal")) mult=2.5;
        else if(d.contains("hard")) mult=1.7;
        else if(d.contains("medium")||d.contains("moderate")) mult=1.2;
        else if(d.contains("easy")) mult=0.7;
        return base*mult; }
    double[] timeLeft(){ double rem=0,tot=0;
        for(JSONObject o:all){ double h=estHrs(o);
            tot+=h; if(!done.contains(o.optString("id"))) rem+=h; }
        return new double[]{rem,tot}; }
    String fmtHours(double h){ if(h>=1) return (h>=10?Math.round(h):Math.round(h*10)/10.0)+"h"; return Math.round(h*60)+"m"; }

    void showRankLadder(){
        ScrollView sv=new ScrollView(this); LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8),dp(4),dp(8),dp(8)); sv.addView(col);
        int rpct=rankPct();
        col.addView(text("UNSC RANK LADDER · "+rankStyleName().toUpperCase()+" · you're at "+rpct+"% ("+(xpBasis()?"XP-weighted":"achievements")+")",10.5f,CYAN,true));
        // style switcher chips
        LinearLayout chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,-2); clp.topMargin=dp(8); clp.bottomMargin=dp(4); chips.setLayoutParams(clp);
        final AlertDialog[] dref=new AlertDialog[1];
        for(int i=0;i<3;i++){ final int si=i; boolean on=prefs.getInt("rankStyle",0)==i;
            TextView chip=text(RANK_STYLE_NAMES[i],11,on?CYAN:T2,on); chip.setGravity(Gravity.CENTER);
            chip.setBackground(box(on?CARD2:CARD,on?CYAN:LINE,14)); chip.setPadding(dp(12),dp(7),dp(12),dp(7));
            LinearLayout.LayoutParams chlp=new LinearLayout.LayoutParams(0,-2,1f); if(i>0) chlp.leftMargin=dp(6); chip.setLayoutParams(chlp);
            chip.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ prefs.edit().putInt("rankStyle",si).apply(); if(dref[0]!=null) dref[0].dismiss(); showRankLadder(); } });
            chips.addView(chip); }
        col.addView(chips);
        for(String[] r:ranks()){ int rp=Integer.parseInt(r[0]); boolean reached=rpct>=rp; boolean curr=r==rank(rpct);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(box(curr?CARD2:CARD, curr?CYAN:LINE, 6)); row.setPadding(dp(12),dp(10),dp(12),dp(10));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(6); row.setLayoutParams(lp);
            TextView ic=text(r[2],22,reached?T1:T3,false); ic.setPadding(0,0,dp(12),0); row.addView(ic);
            LinearLayout c2=new LinearLayout(this); c2.setOrientation(LinearLayout.VERTICAL); c2.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
            c2.addView(text(r[1],14,reached?(curr?CYAN:GREEN):T2,curr));
            c2.addView(text("unlocks at "+rp+"%",9.5f,T3,false)); row.addView(c2);
            row.addView(text(reached?"✔":"",15,GREEN,true)); col.addView(row); }
        dref[0]=new AlertDialog.Builder(this).setView(sv).setPositiveButton("CLOSE",null).create(); dref[0].show();
    }

    /* ===== GAMES ===== */
    View buildGames(){
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10),dp(12),dp(10),0);
        try{ int acc=Color.parseColor(games.get(curGame).optString("color","#00b8e8"));
            col.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{(acc&0x00FFFFFF)|0x2E000000,BG})); }catch(Exception e){}
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); hs.addView(chips);
        for(Map.Entry<String,JSONObject> e:games.entrySet()){
            final String gid=e.getKey(); int[] c=count(gid); if(c[0]==0) continue;
            boolean on=gid.equals(curGame);
            TextView ch=text(gameIcon(gid)+" "+e.getValue().optString("name").replace("Halo: ","").replace("Halo ","H")+" "+(100*c[1]/c[0])+"%",12,on?CYAN:T2,on);
            ch.setBackground(box(on?CARD2:CARD,on?CYAN:LINE,16)); ch.setPadding(dp(14),dp(8),dp(14),dp(8));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.rightMargin=dp(7); ch.setLayoutParams(lp);
            ch.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(gid.equals(chipLast)){ if(++chipTaps>=7) unlockMeta("egg_madrigal"); } else { chipLast=gid; chipTaps=1; } curGame=gid; fMission=""; visitGame(gid); show("games"); } });
            chips.addView(ch); }
        col.addView(hs);

        EditText search=new EditText(this);
        search.setHint("🔎 search all 700 achievements…"); search.setText(query); // v1.6 global
        search.setHintTextColor(T3); search.setTextColor(T1); search.setTextSize(13);
        search.setTypeface(Typeface.MONOSPACE); search.setBackground(box(BG2,LINE,8));
        search.setPadding(dp(12),dp(10),dp(12),dp(10)); search.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.topMargin=dp(9); search.setLayoutParams(slp);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){}
            public void afterTextChanged(Editable s){ query=s.toString().toLowerCase(); if(adapter!=null) adapter.refilter(); if(query.length()>=2) unlockMeta("ftsearch"); } });
        col.addView(search);

        LinearLayout frow=new LinearLayout(this); frow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,-2); flp.topMargin=dp(8); frow.setLayoutParams(flp);
        int[] cg=count(curGame); // v1.6 — live counts on the status chips
        String[] fs={"ALL","TODO","DONE"}; int[] fn={cg[0],cg[0]-cg[1],cg[1]};
        for(int i=0;i<3;i++){ final String f=fs[i]; boolean on=f.equals(fStatus);
            TextView fb=text(f+" "+fn[i],11.5f,on?CYAN:T2,true); fb.setGravity(Gravity.CENTER);
            fb.setBackground(box(on?CARD2:CARD,on?CYAN:LINE,6)); fb.setPadding(0,dp(8),0,dp(8));
            LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,-2,1f); if(i>0) blp.leftMargin=dp(6); fb.setLayoutParams(blp);
            fb.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ fStatus=f; show("games"); } });
            frow.addView(fb); }
        col.addView(frow);
        // v1.6 — sort row: recommended order · biggest G first · quickest first · A–Z
        LinearLayout sortRow=new LinearLayout(this); sortRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams solp=new LinearLayout.LayoutParams(-1,-2); solp.topMargin=dp(7); sortRow.setLayoutParams(solp);
        String[][] sorts={{"default","ORDER"},{"gs","TOP G"},{"time","QUICKEST"},{"az","A–Z"}};
        for(int i=0;i<sorts.length;i++){ final String sid=sorts[i][0]; boolean on=sid.equals(fSort);
            TextView sb2=text(sorts[i][1],10,on?GOLD:T3,on); sb2.setGravity(Gravity.CENTER);
            sb2.setBackground(box(on?CARD2:BG2,on?GOLD:LINE,6)); sb2.setPadding(0,dp(7),0,dp(7));
            LinearLayout.LayoutParams sblp=new LinearLayout.LayoutParams(0,-2,1f); if(i>0) sblp.leftMargin=dp(6); sb2.setLayoutParams(sblp);
            sb2.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ fSort=sid; show("games"); } });
            sortRow.addView(sb2); }
        col.addView(sortRow);

        HorizontalScrollView ts=new HorizontalScrollView(this); ts.setHorizontalScrollBarEnabled(false);
        LinearLayout trow=new LinearLayout(this); trow.setOrientation(LinearLayout.HORIZONTAL); ts.addView(trow);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2); tlp.topMargin=dp(7); tlp.bottomMargin=dp(8); ts.setLayoutParams(tlp);
        for(final String ty:TYPES){ boolean on=ty.equals(fType);
            TextView tb=text(ty.toUpperCase().replace("_"," "),10,on?GOLD:T3,on);
            tb.setBackground(box(on?CARD2:BG2,on?GOLD:LINE,14)); tb.setPadding(dp(11),dp(6),dp(11),dp(6));
            LinearLayout.LayoutParams lp2=new LinearLayout.LayoutParams(-2,-2); lp2.rightMargin=dp(6); tb.setLayoutParams(lp2);
            tb.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ fType=ty; show("games"); } });
            trow.addView(tb); }
        col.addView(ts);

        // mission focus row: "what can I complete in THIS mission?"
        java.util.LinkedHashMap<String,int[]> missions = new java.util.LinkedHashMap<String,int[]>();
        for(JSONObject o:all){ if(!curGame.equals(o.optString("game"))) continue; String ms=o.optString("mission",""); if(ms.length()==0) continue;
            int[] c=missions.get(ms); if(c==null){ c=new int[]{0,0}; missions.put(ms,c); } c[1]++; if(done.contains(o.optString("id"))) c[0]++; }
        if(!missions.isEmpty()){
            HorizontalScrollView ms=new HorizontalScrollView(this); ms.setHorizontalScrollBarEnabled(false);
            LinearLayout mrow=new LinearLayout(this); mrow.setOrientation(LinearLayout.HORIZONTAL); ms.addView(mrow);
            LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,-2); mlp.bottomMargin=dp(8); ms.setLayoutParams(mlp);
            boolean allOn=fMission.equals("");
            TextView allc=text("\ud83c\udfaf ALL MISSIONS",10,allOn?CYAN:T3,allOn); allc.setBackground(box(allOn?CARD2:BG2,allOn?CYAN:LINE,14)); allc.setPadding(dp(11),dp(6),dp(11),dp(6));
            LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-2,-2); alp.rightMargin=dp(6); allc.setLayoutParams(alp);
            allc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ fMission=""; show("games"); } });
            mrow.addView(allc);
            for(java.util.Map.Entry<String,int[]> e:missions.entrySet()){ final String mn=e.getKey(); int[] c=e.getValue(); boolean on=mn.equals(fMission);
                TextView mb=text(mn+" "+c[0]+"/"+c[1],10,on?CYAN:T3,on); mb.setBackground(box(on?CARD2:BG2,on?CYAN:LINE,14)); mb.setPadding(dp(11),dp(6),dp(11),dp(6));
                LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-2,-2); blp.rightMargin=dp(6); mb.setLayoutParams(blp);
                mb.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ fMission=mn; show("games"); } });
                mrow.addView(mb); }
            col.addView(text("\ud83c\udfaf FOCUS \u2014 what can I knock out in one mission?",9,T3,false));
            col.addView(ms);
        }

        ListView lv=new ListView(this); lv.setDivider(null); lv.setDividerHeight(dp(7));
        lv.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1f));
        lv.setFastScrollEnabled(true);
        adapter=new AchAdapter(false); lv.setAdapter(adapter);
        wireList(lv,adapter);
        col.addView(lv);
        adapter.refilter();
        return col;
    }

    void wireList(ListView lv,final AchAdapter ad){
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> p,View v,int pos,long id){
                JSONObject o=ad.items.get(pos); String aid=o.optString("id");
                if(done.contains(aid)){ done.remove(aid);
                    if(aid.equals(egChkId) && System.currentTimeMillis()-egChkMs<2500) unlockMeta("egg_easy"); }
                else { done.add(aid); buzz(); playTick(); sessionChecks++;
                    long now=System.currentTimeMillis(); egChkId=aid; egChkMs=now;
                    egRecent.add(now); while(egRecent.size()>3) egRecent.remove(0);
                    if(egRecent.size()==3 && now-egRecent.get(0)<=10000) unlockMeta("egg_grunt");
                    String gcg=o.optString("game"); int[] gcc=count(gcg);
                    if(gcc[0]>0 && gcc[1]==gcc[0]){ playNotify(); Toast.makeText(MainActivity.this,"🎖️ "+gameName(gcg)+" — 100% COMPLETE",Toast.LENGTH_LONG).show(); } }
                lastToggleId=aid;
                saveSet(done,"done"); ad.refilter(); } });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            public boolean onItemLongClick(AdapterView<?> p,View v,int pos,long id){
                showDetail(ad,ad.items.get(pos)); return true; } });
    }

    void showDetail(final AchAdapter ad,final JSONObject o){
        final String aid=o.optString("id");
        { Integer c=detailOpens.get(aid); int n=(c==null?0:c)+1; detailOpens.put(aid,n); if(n>=5) unlockMeta("egg_iwhbyd"); }
        if(o.optString("name").toLowerCase().contains("343 guilty spark")) unlockMeta("egg_343");
        String extra="Difficulty: "+o.optString("diff","—")+"   Time: "+o.optString("time","—")
            +"\nType: "+o.optString("type","—")+"   Mode: "+o.optString("mode","—")
            +(o.optString("mission","").length()>0?"\nMission: "+o.optString("mission"):"")
            +(o.optBoolean("missable")?"\n⚠ MISSABLE":"")+(o.optBoolean("coop")?"\n👥 CO-OP friendly":"")
            +(unlockTimes.containsKey(aid)?"\n🕒 Unlocked: "+fmtDate(unlockTimes.get(aid)):"");
        new AlertDialog.Builder(this)
            .setTitle(o.optString("icon")+" "+o.optString("name")+" · "+o.optInt("gs")+"G")
            .setMessage(o.optString("desc")+"\n\n📖 GUIDE\n"+o.optString("guide","No guide yet.")+"\n\n"+extra)
            .setNeutralButton("ART & GUIDES ▸",new android.content.DialogInterface.OnClickListener(){
                public void onClick(android.content.DialogInterface d,int w){
                    final String[] opts={"🖼 View full artwork","📚 Halopedia page","🏆 TrueAchievements","▶️ YouTube solutions"};
                    new AlertDialog.Builder(MainActivity.this).setTitle(o.optString("name"))
                        .setItems(opts,new android.content.DialogInterface.OnClickListener(){
                            public void onClick(android.content.DialogInterface dd,int which){
                                if(which==0){ showArtwork(o); return; }
                                String u;
                                if(which==1) u=o.optString("wiki","https://www.halopedia.org");
                                else if(which==2) u="https://www.trueachievements.com/searchresults.aspx?search="+java.net.URLEncoder.encode(o.optString("name"));
                                else u="https://www.youtube.com/results?search_query="+java.net.URLEncoder.encode("Halo MCC "+o.optString("name")+" achievement guide");
                                try{ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u))); }catch(Exception e){}
                            } }).show(); } })
            .setPositiveButton(pins.contains(aid)?"UNPIN 📌":"PIN 📌",new android.content.DialogInterface.OnClickListener(){
                public void onClick(android.content.DialogInterface d,int w){
                    if(pins.contains(aid)) pins.remove(aid); else pins.add(aid);
                    saveSet(pins,"pins"); if(ad!=null) ad.refilter();
                    if(pins.size()>=7) unlockMeta("egg_marathon"); checkMetas();
                    Toast.makeText(MainActivity.this,pins.contains(aid)?"📌 pinned":"unpinned",Toast.LENGTH_SHORT).show(); } })
            .setNegativeButton("CLOSE",null).show();
    }

    /* ===== v1.3.5 full-resolution achievement artwork viewer ===== */
    void showArtwork(final JSONObject o){
        final FrameLayout fx=new FrameLayout(this); fx.setBackgroundColor(0xF20A0E13);
        overlay.addView(fx,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams clp=new FrameLayout.LayoutParams(-1,-1); clp.leftMargin=dp(18); clp.rightMargin=dp(18); col.setLayoutParams(clp);
        col.addView(textC(o.optString("icon")+"  "+o.optString("name"),16,GOLD,true));
        col.addView(textC(o.optInt("gs")+"G · "+gameName(o.optString("game"))+(o.optString("mission","").length()>0?" · "+o.optString("mission"):""),10.5f,T2,false));
        final android.widget.ImageView iv=new android.widget.ImageView(this);
        int sz=Math.min(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels)-dp(90);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(sz,sz); ilp.topMargin=dp(14); ilp.bottomMargin=dp(10); iv.setLayoutParams(ilp);
        iv.setAdjustViewBounds(true); iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        iv.setBackground(box(BG2,LINE,8)); col.addView(iv);
        col.addView(textC("full-res from Halopedia · tap anywhere to close",9.5f,T3,false));
        fx.addView(col);
        loadFull(fullRes(o.optString("img","")),iv);
        col.setAlpha(0f); col.setScaleX(0.94f); col.setScaleY(0.94f);
        col.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280).start();
        fx.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ try{ overlay.removeView(fx); }catch(Exception e){} } });
    }
    void loadFull(final String url,final android.widget.ImageView iv){
        if(url==null||url.length()==0) return;
        android.graphics.Bitmap c=memCache.get("full:"+url); if(c!=null){ iv.setImageBitmap(c); return; }
        final int target=Math.min(getResources().getDisplayMetrics().widthPixels,1200);
        POOL.execute(new Runnable(){ public void run(){ try{
            String fn="full_"+Integer.toHexString(url.hashCode());
            java.io.File f=new java.io.File(getCacheDir(),fn);
            if(!(f.exists()&&f.length()>0)){
                java.net.HttpURLConnection c2=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
                c2.setConnectTimeout(8000); c2.setReadTimeout(12000); c2.setRequestProperty("User-Agent","UNSC-Terminal-personal/1.0");
                java.io.InputStream in=c2.getInputStream(); java.io.FileOutputStream fo=new java.io.FileOutputStream(f);
                byte[] b=new byte[8192]; int r; while((r=in.read(b))>0) fo.write(b,0,r); fo.close(); in.close(); }
            android.graphics.BitmapFactory.Options op=new android.graphics.BitmapFactory.Options(); op.inJustDecodeBounds=true;
            android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(),op);
            int s=1; while((op.outWidth/s>target||op.outHeight/s>target)) s*=2;
            android.graphics.BitmapFactory.Options o2=new android.graphics.BitmapFactory.Options(); o2.inSampleSize=s;
            final android.graphics.Bitmap bm=android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(),o2);
            if(bm!=null) runOnUiThread(new Runnable(){ public void run(){ if(memCache.size()>120) memCache.clear(); memCache.put("full:"+url,bm); iv.setImageBitmap(bm); } });
        }catch(Exception e){} } }); }

    void visitGame(String gid){ visitedGames.add(gid); saveCsv(visitedGames,"vgames"); int g=0; for(java.util.Map.Entry<String,JSONObject> e:games.entrySet()){ int[] c=count(e.getKey()); if(c[0]>0) g++; } if(visitedGames.size()>=g) unlockMeta("egg_library"); }

    /* ===== PINS ===== */
    View buildPins(){
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10),dp(14),dp(10),0);
        TextView h=text("📌 PINNED TARGETS",15,CYAN,true); h.setPadding(dp(4),0,0,dp(8)); col.addView(h);
        if(pins.isEmpty()){
            TextView empty=text("\nNo pins yet.\n\nLong-press any achievement →\nPIN to keep it here.",13,T3,false);
            empty.setGravity(Gravity.CENTER); empty.setPadding(0,dp(40),0,0); col.addView(empty); return col; }
        ListView lv=new ListView(this); lv.setDivider(null); lv.setDividerHeight(dp(7));
        lv.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1f));
        final AchAdapter ad=new AchAdapter(true); lv.setAdapter(ad); wireList(lv,ad);
        col.addView(lv); ad.refilter();
        return col;
    }

    /* ===== MORE ===== */
    View buildMore(){
        ScrollView sv=new ScrollView(this);
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14),dp(14),dp(14),dp(20)); sv.addView(col);
        col.addView(text("⚙ COMMAND",15,CYAN,true));

        LinearLayout sc=card(); sc.addView(text("⏱ SESSION",9.5f,T2,true));
        final Chronometer ch=new Chronometer(this); ch.setTextColor(CYAN); ch.setTextSize(24); ch.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        if(sessionBase>0){ ch.setBase(sessionBase); ch.start(); } else ch.setBase(SystemClock.elapsedRealtime());
        sc.addView(ch);
        final TextView scount=text("checks this session: "+sessionChecks,10.5f,T2,false); sc.addView(scount);
        LinearLayout brow=new LinearLayout(this); brow.setOrientation(LinearLayout.HORIZONTAL);
        TextView go=text("START",12,GREEN,true); go.setBackground(box(CARD2,GREEN,6)); go.setPadding(dp(18),dp(8),dp(18),dp(8));
        TextView st=text("RESET",12,ORANGE,true); st.setBackground(box(CARD2,ORANGE,6)); st.setPadding(dp(18),dp(8),dp(18),dp(8));
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-2,-2); blp.topMargin=dp(8); blp.rightMargin=dp(8);
        go.setLayoutParams(blp); st.setLayoutParams(blp);
        go.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            sessionBase=SystemClock.elapsedRealtime(); sessionChecks=0; ch.setBase(sessionBase); ch.start();
            scount.setText("checks this session: 0"); buzz(); } });
        st.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            sessionBase=0; sessionChecks=0; ch.stop(); ch.setBase(SystemClock.elapsedRealtime()); scount.setText("checks this session: 0"); } });
        brow.addView(go); brow.addView(st); sc.addView(brow); col.addView(sc);

        LinearLayout xs=card(); xs.addView(text("⚡ XBOX LIVE SYNC",9.5f,T2,true));
        final EditText key=new EditText(this); key.setHint("OpenXBL API key (xbl.io — free)");
        key.setText(prefs.getString("xblKey","")); key.setHintTextColor(T3); key.setTextColor(T1); key.setTextSize(12);
        key.setTypeface(Typeface.MONOSPACE); key.setBackground(box(BG2,LINE,6)); key.setPadding(dp(10),dp(9),dp(10),dp(9));
        LinearLayout.LayoutParams klp=new LinearLayout.LayoutParams(-1,-2); klp.topMargin=dp(7); key.setLayoutParams(klp);
        xs.addView(key);
        TextView save=text("SAVE KEY",12,CYAN,true); save.setBackground(box(CARD2,CYAN,6)); save.setPadding(dp(18),dp(8),dp(18),dp(8));
        LinearLayout.LayoutParams svlp=new LinearLayout.LayoutParams(-2,-2); svlp.topMargin=dp(8); save.setLayoutParams(svlp);
        save.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            prefs.edit().putString("xblKey",key.getText().toString().trim()).apply();
            Toast.makeText(MainActivity.this,"✓ key saved — live sync ships in v2.1",Toast.LENGTH_SHORT).show(); } });
        LinearLayout xrow=new LinearLayout(this); xrow.setOrientation(LinearLayout.HORIZONTAL);
        xrow.addView(save);
        TextView syncB=text("⚡ SYNC NOW",12,GOLD,true); syncB.setBackground(box(CARD2,GOLD,6)); syncB.setPadding(dp(18),dp(8),dp(18),dp(8));
        LinearLayout.LayoutParams xlp=new LinearLayout.LayoutParams(-2,-2); xlp.topMargin=dp(8); xlp.leftMargin=dp(8); syncB.setLayoutParams(xlp);
        syncB.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            prefs.edit().putString("xblKey",key.getText().toString().trim()).apply(); if(PREMIUM) xboxSync(); else upsell("Xbox Live auto-sync"); } });
        xrow.addView(syncB); xs.addView(xrow);
        xs.addView(text("Pulls your real unlock state from Xbox Live via OpenXBL (free key at xbl.io). Matches by achievement name.",9,T3,false));
        col.addView(xs);

        LinearLayout gk=card(); gk.addView(text("📊 CAREER STATS KEY",9.5f,T2,true));
        final EditText gkey=new EditText(this); gkey.setHint("Grunt API key (gruntapi.com)");
        gkey.setText(prefs.getString("gruntKey","")); gkey.setHintTextColor(T3); gkey.setTextColor(T1); gkey.setTextSize(12);
        gkey.setTypeface(Typeface.MONOSPACE); gkey.setBackground(box(BG2,LINE,6)); gkey.setPadding(dp(10),dp(9),dp(10),dp(9));
        LinearLayout.LayoutParams gklp=new LinearLayout.LayoutParams(-1,-2); gklp.topMargin=dp(7); gkey.setLayoutParams(gklp); gk.addView(gkey);
        TextView gsave=text("SAVE KEY",12,CYAN,true); gsave.setBackground(box(CARD2,CYAN,6)); gsave.setPadding(dp(18),dp(8),dp(18),dp(8));
        LinearLayout.LayoutParams gslp=new LinearLayout.LayoutParams(-2,-2); gslp.topMargin=dp(8); gsave.setLayoutParams(gslp);
        gsave.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            prefs.edit().putString("gruntKey",gkey.getText().toString().trim()).apply();
            Toast.makeText(MainActivity.this,"✓ Grunt API key saved — feeds your Career Dossier",Toast.LENGTH_SHORT).show(); } });
        gk.addView(gsave);
        gk.addView(text("Career Dossier (More ▸ 🎖️) is live. Paste your Grunt API key + sync Xbox to layer live stats on top; deeper combat stats (medals, kills, accuracy, playtime) populate as that source returns them.",9,T3,false));
        col.addView(gk);

        int[] t=count(null);
        LinearLayout stc=card(); stc.addView(text("📊 STATS",9.5f,T2,true));
        stc.addView(text("unlocked  "+t[1]+" / "+t[0],12.5f,T1,false));
        stc.addView(text("gamerscore  "+t[3]+" / "+t[2]+" G",12.5f,GOLD,false));
        stc.addView(text("pinned  "+pins.size()+"   ·   missables left  "+countFlag("missable"),12.5f,T1,false));
        stc.addView(text("LASO left  "+countType("laso")+"   ·   skulls left  "+countType("skull"),12.5f,PURPLE,false));
        double[] est2=timeLeft(); stc.addView(text("est. time to 100%  "+fmtHours(est2[0]),12.5f,GOLD,false));
        col.addView(stc);
        col.addView(careerCard()); // v1.3 career dossier
        if(PREMIUM){
        LinearLayout tyc=card(); tyc.addView(text("📂 BY TYPE (left to do)",9.5f,T2,true));
        String[] tys={"story","skull","terminal","speed","legendary","laso","multiplayer","firefight","spartan_ops","collectible"};
        for(String ty:tys){ int left=countType(ty); int tot=0; for(JSONObject o:all) if(ty.equals(o.optString("type"))) tot++;
            if(tot==0) continue;
            LinearLayout rr=new LinearLayout(this); rr.setOrientation(LinearLayout.HORIZONTAL);
            TextView nl=text(ty.replace("_"," "),11.5f,left==0?GREEN:T1,false); nl.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f)); rr.addView(nl);
            rr.addView(text(left==0?"✔ done":(tot-left)+"/"+tot,11.5f,left==0?GREEN:T2,false)); tyc.addView(rr); }
        col.addView(tyc); }

        LinearLayout ac=card();
        int unlocked=metas.size(), totalShown=METAS.size();
        ac.addView(text("🏆 APP ACHIEVEMENTS",9.5f,T2,true));
        ac.addView(text(unlocked+" / "+totalShown+"   ·   "+appRank(),16,GOLD,true));
        ac.addView(text("secrets found: "+eggsFound()+" / "+EGG_IDS.length,10.5f,T2,false));
        ac.addView(bar(totalShown==0?0:100*unlocked/totalShown,GOLD));
        final LinearLayout grid=new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL); grid.setVisibility(View.GONE);
        LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(-1,-2); glp.topMargin=dp(8); grid.setLayoutParams(glp);
        final TextView toggle=text("▸ show list",11,CYAN,true); toggle.setPadding(0,dp(8),0,0);
        toggle.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            if(grid.getChildCount()==0){
                for(String[] m:METAS){ boolean got=metas.contains(m[0]); boolean egg=m[1].equals("egg");
                    if(m[0].equals("egg_parliament") && !got) continue;
                    LinearLayout r=new LinearLayout(MainActivity.this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0,dp(5),0,dp(5));
                    String ic=got?m[2]:(egg?"🔒":"🔘");
                    TextView ti=text(ic,17,got?T1:T3,false); ti.setPadding(0,0,dp(10),0); r.addView(ti);
                    LinearLayout cc=new LinearLayout(MainActivity.this); cc.setOrientation(LinearLayout.VERTICAL); cc.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
                    String nm = got ? m[3] : (egg ? "??? (secret)" : m[3]);
                    cc.addView(text(nm,12.5f,got?(egg?GOLD:T1):T3,got));
                    if(got||!egg) cc.addView(text(egg&&!got?"hidden — keep playing":m[4],10,got?T3:T2,false));
                    r.addView(cc); r.addView(text(got?"▶":"",13,got?CYAN:GREEN,true));
                    if(got){ final String[] mm=m; r.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ replayMeta(mm); } }); }
                    grid.addView(r); }
                toggle.setText("▾ hide list"); grid.setVisibility(View.VISIBLE);
            } else { boolean vis=grid.getVisibility()==View.VISIBLE; grid.setVisibility(vis?View.GONE:View.VISIBLE); toggle.setText(vis?"▸ show list":"▾ hide list"); }
        } });
        ac.addView(toggle); ac.addView(grid);
        ac.addView(text("tap any unlocked achievement to replay its unlock · rank is now XP-weighted — heavier achievements lift it more (v1.2)",8.5f,T3,false));
        col.addView(ac);

        LinearLayout ex=card(); ex.addView(text("💾 DATA",9.5f,T2,true));
        TextView cp=text("COPY PROGRESS BACKUP",12,GREEN,true); cp.setBackground(box(CARD2,GREEN,6)); cp.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-2,-2); clp.topMargin=dp(8); cp.setLayoutParams(clp);
        cp.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("halo",prefs.getString("done","")));
            unlockMeta("ftback"); Toast.makeText(MainActivity.this,"✓ progress copied — paste anywhere safe",Toast.LENGTH_SHORT).show(); } });
        ex.addView(cp);
        // v1.2.2 — undo accidental checks + reset to Xbox-synced state (manual check/uncheck always still works)
        TextView undo=text("↩ UNDO LAST CHECK",12,GOLD,true); undo.setBackground(box(CARD2,GOLD,6)); undo.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams ulp=new LinearLayout.LayoutParams(-2,-2); ulp.topMargin=dp(8); undo.setLayoutParams(ulp);
        undo.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            if(lastToggleId.length()==0){ Toast.makeText(MainActivity.this,"nothing to undo",Toast.LENGTH_SHORT).show(); return; }
            if(done.contains(lastToggleId)) done.remove(lastToggleId); else done.add(lastToggleId);
            saveSet(done,"done"); Toast.makeText(MainActivity.this,"↩ undone",Toast.LENGTH_SHORT).show(); lastToggleId=""; show(tab); } });
        ex.addView(undo);
        TextView rs=text("↻ RESET CHECKS TO MY XBOX SYNC",12,CYAN,true); rs.setBackground(box(CARD2,CYAN,6)); rs.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-2,-2); rlp.topMargin=dp(8); rs.setLayoutParams(rlp);
        rs.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            final String sd=prefs.getString("syncedDone","");
            if(sd.length()==0){ Toast.makeText(MainActivity.this,"Sync with Xbox first (paste your key + tap Sync) — then this can restore your exact account state.",Toast.LENGTH_LONG).show(); return; }
            new AlertDialog.Builder(MainActivity.this).setTitle("Reset to Xbox sync?")
                .setMessage("Clears any manual/accidental checks and restores exactly what your Xbox account has unlocked. You can re-check anything by hand afterward.")
                .setPositiveButton("RESET",new android.content.DialogInterface.OnClickListener(){ public void onClick(android.content.DialogInterface d,int w){
                    done.clear(); for(String x:sd.split(",")) if(x.length()>0) done.add(x); saveSet(done,"done");
                    Toast.makeText(MainActivity.this,"✓ reset to your Xbox-synced achievements",Toast.LENGTH_SHORT).show(); show(tab); } })
                .setNegativeButton("CANCEL",null).show(); } });
        ex.addView(rs);
        ex.addView(text("Tap any achievement to check/uncheck it yourself anytime — even when synced. “Reset” only undoes accidental checks back to your Xbox state.",8.5f,T3,false));
        ex.addView(text("Database: the complete official set — 700 achievements / 7,000G (Halopedia-sourced, cross-verified against Xbox Live; 14 wiki gamerscore errors corrected). All 700 icons ship inside the app. Xbox sync still adds anything new Microsoft ever ships.",9,T3,false));
        col.addView(ex);

        LinearLayout fxc=card(); fxc.addView(text("🔊 SOUND & FX",9.5f,T2,true));
        final TextView sfxBtn=text((sfxOn()?"🔊":"🔇")+" SFX  "+(sfxOn()?"ON":"OFF"),12,sfxOn()?GREEN:T3,true);
        sfxBtn.setBackground(box(CARD2,sfxOn()?GREEN:LINE,6)); sfxBtn.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams sflp=new LinearLayout.LayoutParams(-2,-2); sflp.topMargin=dp(8); sfxBtn.setLayoutParams(sflp);
        sfxBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ prefs.edit().putBoolean("sfxOn",!sfxOn()).apply(); if(sfxOn()) playTick(); show("more"); } });
        fxc.addView(sfxBtn);
        final TextView nBtn=text((notifOn()?"🔔":"🔕")+" Mission-complete chime  "+(notifOn()?"ON":"OFF"),12,notifOn()?GREEN:T3,true);
        nBtn.setBackground(box(CARD2,notifOn()?GREEN:LINE,6)); nBtn.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(-2,-2); nlp.topMargin=dp(8); nBtn.setLayoutParams(nlp);
        nBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ prefs.edit().putBoolean("notifOn",!notifOn()).apply(); if(notifOn()) playNotify(); show("more"); } });
        fxc.addView(nBtn);
        fxc.addView(text("Original UNSC-style tones: a tick on every check, a fanfare on unlocks, and a chime when you 100% a game. Mute either anytime.",9,T3,false));
        col.addView(fxc);

        LinearLayout fbc=card(); fbc.addView(text("✉ FEEDBACK · FEATURE REQUEST",9.5f,T2,true));
        TextView fbB=text("✉ EMAIL THE DEV",12,CYAN,true); fbB.setBackground(box(CARD2,CYAN,6)); fbB.setPadding(dp(16),dp(8),dp(16),dp(8));
        LinearLayout.LayoutParams fblp=new LinearLayout.LayoutParams(-2,-2); fblp.topMargin=dp(8); fbB.setLayoutParams(fblp);
        fbB.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            try{ Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:floorloops@parliamentfour.com"));
                i.putExtra(Intent.EXTRA_SUBJECT,"UNSC Terminal v"+appVer()+" — feature request");
                i.putExtra(Intent.EXTRA_TEXT,"What I'd love to see in a future version:\n\n");
                startActivity(Intent.createChooser(i,"Send feedback")); }
            catch(Exception e){ Toast.makeText(MainActivity.this,"No email app found",Toast.LENGTH_SHORT).show(); } } });
        fbc.addView(fbB);
        fbc.addView(text("Ideas land with the dev and get built into future versions. (You can also drop them in the companion app's idea inbox.)",9,T3,false));
        col.addView(fbc);

        LinearLayout rm=card(); rm.addView(text("🗺️ ROADMAP",9.5f,T2,true));
        String[][] RM={
            {"1","v1.0","Native app · ~690-achievement database (Halopedia) · real icons · guides"},
            {"1","v1.1.5","Xbox Live sync (+ unlock dates) · 100+ in-app achievements: animated banners, sounds, replay, app-rank, secrets · rank ladder · time-to-100% · per-type stats · in-app roadmap"},
            {"1","v1.2","XP-weighted ranking overhaul · choose rank style: MCC / Halo 3 / Reach · focus mode (best next targets) · smart breakdowns"},
            {"1","v1.2.1","Difficulty-weighted time-to-completion (LASO ≈ 20h+, not 1h) · Xbox sync fills in any achievements your account has that the DB is missing"},
            {"1","v1.2.2","Fixed 2 dead easter-egg triggers (all 11 unlockable now) · undo accidental checks + reset-to-Xbox-sync (manual checking always works)"},
            {"1","v1.2.3","Exact 700/7000 database — full official set baked in, verified against live Xbox data (10 missing Halo 3 achievements added, 14 wiki gamerscore errors fixed)"},
            {"1","v1.2.5","Native UI glow-up — HUD gradients, glow cards, accent rules, per-game icons & themed backdrops"},
            {"1","v1.3","Career Dossier (service record, medals, time invested + live Xbox stats) · per-game icons · in-app feedback button → emails floorloops@parliamentfour.com"},
            {"1","v1.3.5","Per-mission / per-map filter (campaign achievements) · full-res achievement artwork viewer · image-cache optimization"},
            {"1","v1.4","Mutable UNSC-style SFX (check tick + unlock fanfare) · screen transitions & animations"},
            {"1","v1.5","Mission-complete chime when you 100% a game · sound toggles · polish"},
            {"1","v1.6","Premium pass — all 700 icons bundled offline (instant, zero network) · global search across every game · sort modes (Top G / Quickest / A–Z) · live filter counts · 2-up home grid"},
            {"0","v1.6.5","Home-screen widgets"},
            {"0","v1.7","General tips & pointers (YouTube/Halopedia/TA)"},
            {"0","v1.8","Walkthroughs · solution videos · screenshots"},
            {"0","v1.9","Optimal completion-order + LASO routing"},
        };
        for(String[] r:RM){ boolean done=r[0].equals("1");
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0,dp(4),0,dp(4));
            TextView dot=text(done?"✅":"◻️",12,done?GREEN:T3,false); dot.setPadding(0,0,dp(8),0); row.addView(dot);
            LinearLayout c2=new LinearLayout(this); c2.setOrientation(LinearLayout.VERTICAL); c2.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
            c2.addView(text(r[1],12,done?GREEN:CYAN,true));
            c2.addView(text(r[2],10.5f,done?T3:T2,false)); row.addView(c2); rm.addView(row); }
        rm.addView(text("submit ideas via the companion app — they get built into future versions",8.5f,T3,false));
        col.addView(rm);

        TextView ab=text("\nUNSC TERMINAL v1.6 · native\n© 2026 Parliament Four · for personal glory",9,T3,false);
        ab.setGravity(Gravity.CENTER); col.addView(ab);
        return sv;
    }
    int countFlag(String f){ int n=0; for(JSONObject o:all) if(o.optBoolean(f)&&!done.contains(o.optString("id"))) n++; return n; }
    int countType(String ty){ int n=0; for(JSONObject o:all) if(ty.equals(o.optString("type"))&&!done.contains(o.optString("id"))) n++; return n; }

    /* ===== v1.3 CAREER DOSSIER ===== */
    LinearLayout careerCard(){
        LinearLayout c=glowCard(GOLD);
        c.addView(text("🎖️ CAREER DOSSIER",9.5f,GOLD,true));
        int[] t=count(null); int pct=t[0]==0?0:100*t[1]/t[0];
        c.addView(text("SERVICE RECORD",9,T3,true));
        c.addView(text(rank(rankPct())[1]+"  ·  "+t[1]+"/"+t[0]+" ops cleared  ·  "+pct+"%",13,T1,true));
        c.addView(text("gamerscore  "+t[3]+" / "+t[2]+" G",11.5f,GOLD,false));
        double inv=0; for(JSONObject o:all) if(done.contains(o.optString("id"))) inv+=estHrs(o);
        c.addView(text("time invested (est)  "+fmtHours(inv)+"   ·   "+fmtHours(timeLeft()[0])+" to 100%",11,T2,false));
        c.addView(rule(GOLD));
        c.addView(text("CAMPAIGN MEDALS",9,T3,true));
        LinearLayout med=new LinearLayout(this); med.setOrientation(LinearLayout.HORIZONTAL); med.setPadding(0,dp(5),0,dp(2));
        for(java.util.Map.Entry<String,JSONObject> e:games.entrySet()){ int[] gc=count(e.getKey()); if(gc[0]==0) continue;
            int gp=100*gc[1]/gc[0]; String md=gp==100?"🥇":(gp>=50?"🥈":(gp>0?"🥉":"▫️"));
            med.addView(text(gameIcon(e.getKey())+md+"  ",13.5f,T1,false)); }
        c.addView(med);
        c.addView(text("🥇 100%   🥈 50%+   🥉 started",8.5f,T3,false));
        String cs=prefs.getString("careerStats","");
        if(cs.length()>0){ c.addView(rule(CYAN)); c.addView(text("⚡ LIVE XBOX STATS",9,CYAN,true)); c.addView(text(cs,11,T2,false)); }
        else c.addView(text("\n⚡ Paste your Grunt API key above + run an Xbox sync to layer live stats (gamertag, synced gamerscore) onto your dossier. Deeper combat stats — medals, kills, accuracy, playtime — light up here as that data source returns them for your gamertag.",9,T3,false));
        return c;
    }


    /* ===== icon loader ===== */
    static final java.util.concurrent.ExecutorService POOL = java.util.concurrent.Executors.newFixedThreadPool(4);
    final java.util.HashMap<String, android.graphics.Bitmap> memCache = new java.util.HashMap<String, android.graphics.Bitmap>();
    void loadIcon(final String aid, final String url, final android.widget.ImageView iv) {
        final String key = (aid != null && aid.length() > 0) ? "a:" + aid : url;
        iv.setTag(key);
        android.graphics.Bitmap c = memCache.get(key);
        if (c != null) { iv.setImageBitmap(c); return; }
        iv.setImageBitmap(null);
        POOL.execute(new Runnable() { public void run() {
            try {
                android.graphics.Bitmap bm = null;
                // v1.6 — all 700 icons ship inside the APK (assets/icons/<id>.png): instant, offline, zero network
                if (aid != null && aid.length() > 0) {
                    try { java.io.InputStream ain = getAssets().open("icons/" + aid + ".png");
                        bm = android.graphics.BitmapFactory.decodeStream(ain); ain.close(); } catch (Exception e) {}
                }
                if (bm == null && url != null && url.length() > 0) {
                    String fn = "ic_" + Integer.toHexString(url.hashCode());
                    java.io.File f = new java.io.File(getCacheDir(), fn);
                    if (f.exists() && f.length() > 0) bm = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
                    if (bm == null) {
                        java.net.HttpURLConnection c2 = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                        c2.setConnectTimeout(8000); c2.setReadTimeout(10000);
                        c2.setRequestProperty("User-Agent", "UNSC-Terminal-personal/1.0");
                        java.io.InputStream in = c2.getInputStream();
                        java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                        byte[] b = new byte[8192]; int r;
                        while ((r = in.read(b)) > 0) fo.write(b, 0, r);
                        fo.close(); in.close();
                        bm = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
                    }
                }
                final android.graphics.Bitmap fb = bm;
                if (fb != null) runOnUiThread(new Runnable() { public void run() {
                    if (memCache.size() > 120) memCache.clear(); // v1.3.5 cap memory cache
                    memCache.put(key, fb);
                    if (key.equals(iv.getTag())) iv.setImageBitmap(fb); } });
            } catch (Exception e) {}
        } });
    }

    /* ===== xbox live sync (OpenXBL) ===== */
    void xboxSync() {
        final String key = prefs.getString("xblKey", "");
        if (key.length() == 0) { Toast.makeText(this, "save an OpenXBL key first (xbl.io)", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "\u26a1 syncing with Xbox Live\u2026", Toast.LENGTH_SHORT).show();
        POOL.execute(new Runnable() { public void run() {
            String err = null; String gt = null;
            final java.util.List<JSONObject> newAch = new java.util.ArrayList<JSONObject>();
            final java.util.List<String> achievedIds = new java.util.ArrayList<String>();
            final java.util.HashMap<String,String> times = new java.util.HashMap<String,String>();
            try {
                String[] acc = apiGet("https://xbl.io/api/v2/account", key);
                if (!acc[0].equals("200")) { err = "key rejected (HTTP " + acc[0] + "). Check the OpenXBL key at xbl.io."; }
                else {
                    java.util.regex.Matcher gm = java.util.regex.Pattern.compile("\"gamertag\"\\s*:\\s*\"([^\"]+)\"").matcher(acc[1]);
                    if (gm.find()) gt = gm.group(1);
                    String titleId = null;
                    String[] th = apiGet("https://xbl.io/api/v2/player/titleHistory", key);
                    if (th[0].equals("200")) { try {
                        JSONObject root = new JSONObject(th[1]); JSONArray titles = root.optJSONArray("titles");
                        if (titles != null) for (int i = 0; i < titles.length(); i++) { JSONObject t = titles.getJSONObject(i);
                            if (t.optString("name","").toLowerCase().contains("master chief")) { titleId = t.optString("titleId"); break; } }
                    } catch (Exception e) {} }
                    if (titleId == null) titleId = "1144039928";
                    String[] ar = apiGet("https://xbl.io/api/v2/achievements/title/" + titleId, key);
                    if (!ar[0].equals("200")) { err = "couldn't load MCC achievements (HTTP " + ar[0] + "). Play MCC once on this account, then retry."; }
                    else {
                        JSONObject root = new JSONObject(ar[1]); JSONArray arr = null;
                        JSONObject content = root.optJSONObject("content");
                        if (content != null) arr = content.optJSONArray("achievements");
                        if (arr == null) arr = root.optJSONArray("achievements");
                        if (arr == null) arr = root.optJSONArray("titleAchievements");
                        if (arr == null) { err = "no achievements returned \u2014 body: " + ar[1].substring(0, Math.min(140, ar[1].length())); }
                        else {
                            java.util.HashMap<String, String> byName = new java.util.HashMap<String, String>();
                            for (JSONObject o : all) byName.put(o.optString("name").trim().toLowerCase(), o.optString("id"));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject a = arr.getJSONObject(i);
                                String nm = a.optString("name").trim(); if (nm.length()==0) continue;
                                String nkey = nm.toLowerCase();
                                String id = byName.get(nkey);
                                if (id == null) {
                                    String nid = "xbl_" + Integer.toHexString(nkey.hashCode());
                                    int gs = 0; JSONArray rw = a.optJSONArray("rewards");
                                    if (rw != null) for (int k=0;k<rw.length();k++){ JSONObject rr=rw.optJSONObject(k); if(rr!=null && "Gamerscore".equalsIgnoreCase(rr.optString("type"))) { try{ gs=Integer.parseInt(rr.optString("value","0")); }catch(Exception e){} } }
                                    JSONObject n = new JSONObject();
                                    n.put("id", nid); n.put("name", nm); n.put("game", "mcc"); n.put("gs", gs);
                                    n.put("type", "meta"); n.put("icon", "\ud83c\udf96\ufe0f");
                                    n.put("desc", a.optString("description", a.optString("lockedDescription","")));
                                    newAch.add(n); byName.put(nkey, nid); id = nid;
                                }
                                boolean got = "Achieved".equalsIgnoreCase(a.optString("progressState")) || a.optInt("unlocked",0)==1 || a.optBoolean("isUnlocked",false);
                                if (got) {
                                    String tu = a.optString("timeUnlocked","");
                                    if (tu.length()==0) { JSONObject pg=a.optJSONObject("progression"); if(pg!=null) tu=pg.optString("timeUnlocked",""); }
                                    if (tu.length()>0) times.put(id, tu);
                                    achievedIds.add(id);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) { err = "error: " + e; }
            final String fe = err; final String fgt = gt;
            runOnUiThread(new Runnable() { public void run() {
                if (fe != null) { new AlertDialog.Builder(MainActivity.this).setTitle("Sync failed").setMessage(fe + (fgt!=null?"\n\n(key works \u2014 signed in as "+fgt+")":"")).setPositiveButton("OK",null).show(); return; }
                // apply ALL mutations on the UI thread (thread-safe)
                if (!newAch.isEmpty()) {
                    JSONArray extra = new JSONArray();
                    try { extra = new JSONArray(prefs.getString("extraAch","[]")); } catch (Exception e) {}
                    for (JSONObject n : newAch) { all.add(n); totalGs += n.optInt("gs"); extra.put(n); }
                    prefs.edit().putString("extraAch", extra.toString()).apply();
                }
                int fm = 0;
                for (String id : achievedIds) { if (!done.contains(id)) { done.add(id); fm++; } }
                // v1.2.2 — remember the Xbox-authoritative done set so accidental manual checks can be reset away
                { StringBuilder sb=new StringBuilder(); for(String id:achievedIds){ if(sb.length()>0) sb.append(','); sb.append(id);} prefs.edit().putString("syncedDone",sb.toString()).apply(); }
                for (java.util.Map.Entry<String,String> e : times.entrySet()) unlockTimes.put(e.getKey(), e.getValue());
                try{ JSONObject ut=new JSONObject(); for(java.util.Map.Entry<String,String> e:unlockTimes.entrySet()) ut.put(e.getKey(),e.getValue()); prefs.edit().putString("ut",ut.toString()).apply(); }catch(Exception e){}
                bulkUnlock=true; saveSet(done, "done");
                prefs.edit().putString("careerStats","Gamertag  "+(fgt!=null?fgt:"—")+"\nSynced gamerscore  "+gsDone()+" G\nSynced unlocks  "+done.size()).apply();
                int beforeM=metas.size(); unlockMeta("ftsync"); checkMetas(); bulkUnlock=false;
                int gainedM=metas.size()-beforeM;
                Toast.makeText(MainActivity.this, "\u2714 "+(fgt!=null?fgt+": ":"")+"+" + fm + " synced \u00b7 +" + gainedM + " app achievements", Toast.LENGTH_LONG).show();
                show(tab); } });
        } });
    }
    String[] apiGet(String url, String key) {
        try {
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            c.setConnectTimeout(10000); c.setReadTimeout(20000);
            c.setRequestProperty("X-Authorization", key);
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            java.io.InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int r;
            if (in != null) while ((r = in.read(b)) > 0) bo.write(b, 0, r);
            if (in != null) in.close();
            return new String[]{ String.valueOf(code), new String(bo.toByteArray(), StandardCharsets.UTF_8) };
        } catch (Exception e) { return new String[]{ "0", "exception: " + e }; }
    }


    /* ===== in-app achievements ===== */
    void metaDef(String id,String cond,String icon,String title,String desc,boolean secret){
        METAS.add(new String[]{id,cond,icon,title,desc,secret?"1":"0"}); }
    void addAllMetas(){
        metaDef("d1","done:1","🩸","First Blood","Unlock any 1 MCC achievements",false);
        metaDef("d5","done:5","🥾","Boot Camp","Unlock any 5 MCC achievements",false);
        metaDef("d10","done:10","🔟","Double Digits","Unlock any 10 MCC achievements",false);
        metaDef("d25","done:25","📈","Making Progress","Unlock any 25 MCC achievements",false);
        metaDef("d50","done:50","🎯","Half-Centurion","Unlock any 50 MCC achievements",false);
        metaDef("d75","done:75","⛽","Three-Quarter","Unlock any 75 MCC achievements",false);
        metaDef("d100","done:100","💯","Centurion","Unlock any 100 MCC achievements",false);
        metaDef("d150","done:150","🎖️","Seasoned","Unlock any 150 MCC achievements",false);
        metaDef("d200","done:200","🪖","Veteran","Unlock any 200 MCC achievements",false);
        metaDef("d250","done:250","⚔️","Battle-Hardened","Unlock any 250 MCC achievements",false);
        metaDef("d300","done:300","🔥","Relentless","Unlock any 300 MCC achievements",false);
        metaDef("d400","done:400","🚀","Unstoppable","Unlock any 400 MCC achievements",false);
        metaDef("d450","done:450","🤖","The Machine","Unlock any 450 MCC achievements",false);
        metaDef("d500","done:500","🌟","Legend in Making","Unlock any 500 MCC achievements",false);
        metaDef("d600","done:600","🧗","Almost There","Unlock any 600 MCC achievements",false);
        metaDef("d650","done:650","🏁","Home Stretch","Unlock any 650 MCC achievements",false);
        metaDef("d690","done:690","👑","Completionist","Unlock any 690 MCC achievements",false);
        metaDef("d700","done:700","🏆","The Full Seven Hundred","Unlock ALL 700 MCC achievements — total victory",false);
        metaDef("gs250","gs:250","🪙","Pocket Change","Earn 250G of Gamerscore",false);
        metaDef("gs500","gs:500","💰","Coin Collector","Earn 500G of Gamerscore",false);
        metaDef("gs1000","gs:1000","🎮","GS Grinder","Earn 1000G of Gamerscore",false);
        metaDef("gs2000","gs:2000","🎯","Point Blank","Earn 2000G of Gamerscore",false);
        metaDef("gs3000","gs:3000","📊","Score Surge","Earn 3000G of Gamerscore",false);
        metaDef("gs4000","gs:4000","🎲","High Roller","Earn 4000G of Gamerscore",false);
        metaDef("gs5000","gs:5000","🎖️","GS General","Earn 5000G of Gamerscore",false);
        metaDef("gs6000","gs:6000","🔢","Number Cruncher","Earn 6000G of Gamerscore",false);
        metaDef("gs7000","gs:7000","🆙","Maxed Out","Earn 7000G of Gamerscore",false);
        metaDef("gs7110","gs:7000","🏆","Perfect Score","Earn the maximum 7,000G — every point MCC has",false);
        metaDef("g1ce","g1:ce","🎬","First Step: CE Anniversary","Unlock your first CE Anniversary achievement",false);
        metaDef("g1h2","g1:h2","🎬","First Step: Halo 2","Unlock your first Halo 2 achievement",false);
        metaDef("g1h3","g1:h3","🎬","First Step: Halo 3","Unlock your first Halo 3 achievement",false);
        metaDef("g1odst","g1:odst","🎬","First Step: ODST","Unlock your first ODST achievement",false);
        metaDef("g1reach","g1:reach","🎬","First Step: Reach","Unlock your first Reach achievement",false);
        metaDef("g1h4","g1:h4","🎬","First Step: Halo 4","Unlock your first Halo 4 achievement",false);
        metaDef("g1mcc","g1:mcc","🎬","First Step: cross-game","Unlock your first cross-game achievement",false);
        metaDef("g100ce","g100:ce","🏅","CE Anniversary Complete","100% every CE Anniversary achievement",false);
        metaDef("g100h2","g100:h2","🏅","Halo 2 Complete","100% every Halo 2 achievement",false);
        metaDef("g100h3","g100:h3","🏅","Halo 3 Complete","100% every Halo 3 achievement",false);
        metaDef("g100odst","g100:odst","🏅","ODST Complete","100% every ODST achievement",false);
        metaDef("g100reach","g100:reach","🏅","Reach Complete","100% every Reach achievement",false);
        metaDef("g100h4","g100:h4","🏅","Halo 4 Complete","100% every Halo 4 achievement",false);
        metaDef("g100mcc","g100:mcc","🏅","cross-game Complete","100% every cross-game achievement",false);
        metaDef("g50ce","g50:ce","▶️","CE Anniversary Halfway","Reach 50% in CE Anniversary",false);
        metaDef("g50h2","g50:h2","▶️","Halo 2 Halfway","Reach 50% in Halo 2",false);
        metaDef("g50h3","g50:h3","▶️","Halo 3 Halfway","Reach 50% in Halo 3",false);
        metaDef("g50odst","g50:odst","▶️","ODST Halfway","Reach 50% in ODST",false);
        metaDef("g50reach","g50:reach","▶️","Reach Halfway","Reach 50% in Reach",false);
        metaDef("g50h4","g50:h4","▶️","Halo 4 Halfway","Reach 50% in Halo 4",false);
        metaDef("g50mcc","g50:mcc","▶️","cross-game Halfway","Reach 50% in cross-game",false);
        metaDef("tfstory","tf:story","📖","First Story","Unlock any Story achievement — beat campaign missions",false);
        metaDef("tfskull","tf:skull","💀","First Skull","Unlock any Skull achievement — find/use every skull",false);
        metaDef("tfterminal","tf:terminal","📟","First Terminal","Unlock any Terminal achievement — find every terminal",false);
        metaDef("tfspeed","tf:speed","⏱️","First Speed","Unlock any Speed achievement — beat par-time challenges",false);
        metaDef("tflegendary","tf:legendary","🔴","First Legendary","Unlock any Legendary achievement — beat missions on legendary",false);
        metaDef("tflaso","tf:laso","☠️","First LASO","Unlock any LASO achievement — legendary all skulls on — the hardest grind",false);
        metaDef("tfmultiplayer","tf:multiplayer","🎮","First Multiplayer","Unlock any Multiplayer achievement — win multiplayer challenges",false);
        metaDef("tffirefight","tf:firefight","🔫","First Firefight","Unlock any Firefight achievement — survive firefight",false);
        metaDef("tfspartan_ops","tf:spartan_ops","🛰️","First Spartan Ops","Unlock any Spartan Ops achievement — clear spartan ops",false);
        metaDef("tfcollectible","tf:collectible","🧩","First Collectible","Unlock any Collectible achievement — grab every collectible",false);
        metaDef("tastory","ta:story","📖","Story Master","Unlock EVERY Story achievement (beat campaign missions)",false);
        metaDef("taskull","ta:skull","💀","Skull Master","Unlock EVERY Skull achievement (find/use every skull)",false);
        metaDef("taterminal","ta:terminal","📟","Terminal Master","Unlock EVERY Terminal achievement (find every terminal)",false);
        metaDef("taspeed","ta:speed","⏱️","Speed Master","Unlock EVERY Speed achievement (beat par-time challenges)",false);
        metaDef("talegendary","ta:legendary","🔴","Legendary Master","Unlock EVERY Legendary achievement (beat missions on legendary)",false);
        metaDef("talaso","ta:laso","☠️","LASO Master","Unlock EVERY LASO achievement (legendary all skulls on — the hardest grind)",false);
        metaDef("tamultiplayer","ta:multiplayer","🎮","Multiplayer Master","Unlock EVERY Multiplayer achievement (win multiplayer challenges)",false);
        metaDef("tafirefight","ta:firefight","🔫","Firefight Master","Unlock EVERY Firefight achievement (survive firefight)",false);
        metaDef("taspartan_ops","ta:spartan_ops","🛰️","Spartan Ops Master","Unlock EVERY Spartan Ops achievement (clear spartan ops)",false);
        metaDef("tacollectible","ta:collectible","🧩","Collectible Master","Unlock EVERY Collectible achievement (grab every collectible)",false);
        metaDef("rk0","rank:0","🟫","Rank: Recruit","Reach 0% overall completion",false);
        metaDef("rk10","rank:10","🔵","Rank: Private","Reach 10% overall completion",false);
        metaDef("rk25","rank:25","🟡","Rank: Corporal","Reach 25% overall completion",false);
        metaDef("rk40","rank:40","🟠","Rank: Sergeant","Reach 40% overall completion",false);
        metaDef("rk55","rank:55","🔴","Rank: Staff Sergeant","Reach 55% overall completion",false);
        metaDef("rk65","rank:65","🟣","Rank: Lieutenant","Reach 65% overall completion",false);
        metaDef("rk75","rank:75","⚫","Rank: Captain","Reach 75% overall completion",false);
        metaDef("rk85","rank:85","🪖","Rank: ODST Operative","Reach 85% overall completion",false);
        metaDef("rk93","rank:93","🟢","Rank: Spartan","Reach 93% overall completion",false);
        metaDef("rk99","rank:99","🌟","Rank: Noble Spartan","Reach 99% overall completion",false);
        metaDef("rk100","rank:100","🎖️","Rank: Master Chief","Reach 100% overall completion",false);
        metaDef("pin1","pins:1","📌","Pin It","Pin any achievement (long-press → PIN)",false);
        metaDef("pin5","pins:5","📌","Target List","Pin 5 achievements at once",false);
        metaDef("pin10","pins:10","📌","Hit List","Pin 10 achievements",false);
        metaDef("pin25","pins:25","📌","Master Strategist","Pin 25 achievements",false);
        metaDef("ses10","sess:10","⏱️","Warming Up","Keep a session timer running 10 minutes",false);
        metaDef("ses30","sess:30","🎯","In the Zone","Keep a session timer running 30 minutes",false);
        metaDef("ses60","sess:60","🏃","Marathon Session","Keep a session timer running 60 minutes",false);
        metaDef("ftsync","feat:sync","⚡","Linked Up","Sync with Xbox Live (paste an OpenXBL key)",false);
        metaDef("ftback","feat:backup","💾","Safe Keeping","Copy a progress backup",false);
        metaDef("ftsearch","feat:search","🔎","Detective","Use the achievement search",false);
        metaDef("egg_cryo","egg","❄️","Wake Me When You Need Me","???",true);
        metaDef("egg_easy","egg","🗡️","Were It So Easy","???",true);
        metaDef("egg_grunt","egg","🎉","Grunt Birthday Party","???",true);
        metaDef("egg_iwhbyd","egg","😈","I Would Have Been Your Daddy","???",true);
        metaDef("egg_marathon","egg","🏃","Marathon Man","???",true);
        metaDef("egg_endure","egg","🛡️","Endure","???",true);
        metaDef("egg_library","egg","📚","The Library","???",true);
        metaDef("egg_madrigal","egg","🎵","Siege of Madrigal","???",true);
        metaDef("egg_343","egg","💡","343 Guilty Spark","???",true);
        metaDef("egg_bloom","egg","🌸","Bloom","???",true);
        metaDef("egg_parliament","egg","🦉","A Parliament of Four","???",true);
        metaDef("meta_allsecrets","allsecrets","🔓","Conspiracy Theorist","Discover all 11 hidden easter eggs scattered through the app",false);
        if(PREMIUM) metaDef("meta_king","king","👑","King of the Hill","Own the app. (You built this.)",false);
    }
    String[] metaById(String id){ for(String[] m:METAS) if(m[0].equals(id)) return m; return null; }
    int gsDone(){ int g=0; for(JSONObject o:all) if(done.contains(o.optString("id"))) g+=o.optInt("gs"); return g; }
    int[] gameDoneCount(String gid){ int n=0,d=0; for(JSONObject o:all) if(gid.equals(o.optString("game"))){ n++; if(done.contains(o.optString("id"))) d++; } return new int[]{d,n}; }
    int[] typeDoneCount(String ty){ int n=0,d=0; for(JSONObject o:all) if(ty.equals(o.optString("type"))){ n++; if(done.contains(o.optString("id"))) d++; } return new int[]{d,n}; }
    long sessionMin(){ if(sessionBase<=0) return 0; return (SystemClock.elapsedRealtime()-sessionBase)/60000; }

    boolean condMet(String cond){
        try{
            String[] p=cond.split(":"); String k=p[0];
            if(k.equals("done")) return done.size()>=Integer.parseInt(p[1]);
            if(k.equals("gs")) return gsDone()>=Integer.parseInt(p[1]);
            if(k.equals("g1")){ int[] c=gameDoneCount(p[1]); return c[0]>=1; }
            if(k.equals("g100")){ int[] c=gameDoneCount(p[1]); return c[1]>0 && c[0]>=c[1]; }
            if(k.equals("g50")){ int[] c=gameDoneCount(p[1]); return c[1]>0 && c[0]*2>=c[1]; }
            if(k.equals("tf")){ int[] c=typeDoneCount(p[1]); return c[0]>=1; }
            if(k.equals("ta")){ int[] c=typeDoneCount(p[1]); return c[1]>0 && c[0]>=c[1]; }
            if(k.equals("rank")){ int[] t=count(null); int pct=t[0]==0?0:100*t[1]/t[0]; return pct>=Integer.parseInt(p[1]); }
            if(k.equals("pins")) return pins.size()>=Integer.parseInt(p[1]);
            if(k.equals("sess")) return sessionMin()>=Integer.parseInt(p[1]);
        }catch(Exception e){}
        return false;
    }
    void checkMetas(){
        for(String[] m:METAS){ String cond=m[1];
            if(cond.equals("egg")||cond.startsWith("feat")) continue;
            if(!metas.contains(m[0]) && condMet(cond)) unlockMeta(m[0]); }
    }
    void unlockMeta(String id){
        if(metas.contains(id)) return; String[] m=metaById(id); if(m==null) return;
        metas.add(id); saveSet(metas,"metas");
        if(bulkUnlock){ if(id.equals("meta_king")) grandUnlock(m[2],m[3],"The Parliament bows. Were it so easy."); return; }
        if(id.equals("meta_king")){ grandUnlock(m[2],m[3],"The Parliament bows. Were it so easy."); return; }
        boolean egg = m[1].equals("egg");
        playUnlock(egg); buzz();
        showAchievementBanner(m[2],m[3],egg);
        if(egg){ flash(); if(allEggsFound()){ megaCelebration(); unlockMeta("meta_allsecrets"); } }
    }
    static final String[] EGG_IDS={"egg_cryo","egg_easy","egg_grunt","egg_iwhbyd","egg_marathon","egg_endure","egg_library","egg_madrigal","egg_343","egg_bloom","egg_parliament"};
    boolean allEggsFound(){ for(String e:EGG_IDS) if(!metas.contains(e)) return false; return true; }
    int eggsFound(){ int n=0; for(String e:EGG_IDS) if(metas.contains(e)) n++; return n; }
    void megaCelebration(){
        runOnUiThread(new Runnable(){ public void run(){
            final FrameLayout fx=new FrameLayout(MainActivity.this); fx.setBackgroundColor(0xCC0A0E13);
            overlay.addView(fx,new FrameLayout.LayoutParams(-1,-1));
            final String[] emo={"🎉","🎆","🛡️","🎖️","⭐","💥","🦉","☠️","💀","🚀","🔥","👑"};
            final java.util.Random rnd=new java.util.Random();
            final int W=getResources().getDisplayMetrics().widthPixels;
            final int H=getResources().getDisplayMetrics().heightPixels;
            for(int i=0;i<48;i++){ TextView e=text(emo[rnd.nextInt(emo.length)],22+rnd.nextInt(26),T1,false);
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2); lp.leftMargin=rnd.nextInt(W); lp.topMargin=-100-rnd.nextInt(400); e.setLayoutParams(lp);
                fx.addView(e); e.animate().translationY(H+300).rotationBy(rnd.nextInt(720)-360).setDuration(1800+rnd.nextInt(1600)).setStartDelay(rnd.nextInt(700)).start(); }
            LinearLayout card=new LinearLayout(MainActivity.this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
            card.setBackground(box(0xFF0D1117,GOLD,14)); card.setPadding(dp(26),dp(24),dp(26),dp(24));
            FrameLayout.LayoutParams clp=new FrameLayout.LayoutParams(-2,-2); clp.gravity=Gravity.CENTER; clp.leftMargin=dp(24); clp.rightMargin=dp(24); card.setLayoutParams(clp);
            card.addView(textC("🦉🛡️🎖️",40)); card.addView(textC("ALL SECRETS FOUND",18,GOLD,true));
            card.addView(textC("You found every easter egg.",13,T1,false));
            card.addView(textC("\nThe Parliament salutes you.\nWere it so easy.",11,T2,false));
            card.setAlpha(0f); card.setScaleX(0.6f); card.setScaleY(0.6f);
            fx.addView(card); card.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(400).setDuration(600).start();
            playUnlock(true);
            fx.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ overlay.removeView(fx); } });
            fx.postDelayed(new Runnable(){ public void run(){ try{ overlay.removeView(fx); }catch(Exception e){} } }, 7000);
        } });
    }
    TextView textC(String s,float sz){ return textC(s,sz,T1,false); }
    TextView textC(String s,float sz,int c,boolean b){ TextView t=text(s,sz,c,b); t.setGravity(Gravity.CENTER); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(4); t.setLayoutParams(lp); return t; }
    String appRank(){
        int n=metas.size();
        if(n>=100) return "🎖️ Reclaimer";
        if(n>=80) return "🌟 Mythic";
        if(n>=60) return "🟢 Spartan";
        if(n>=40) return "🪖 ODST";
        if(n>=25) return "⚫ Captain";
        if(n>=12) return "🟠 Sergeant";
        if(n>=5) return "🔵 Private";
        return "🟫 Recruit";
    }

    void playUnlock(final boolean egg){
        if(!sfxOn()) return; // v1.4 mute respects SFX toggle
        POOL.execute(new Runnable(){ public void run(){ try{
            ToneGenerator tg=new ToneGenerator(AudioManager.STREAM_MUSIC,90);
            if(egg){ tg.startTone(ToneGenerator.TONE_PROP_BEEP,120); Thread.sleep(140);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2,160); Thread.sleep(180);
                tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L,220); Thread.sleep(260); }
            else { tg.startTone(ToneGenerator.TONE_PROP_ACK,140); Thread.sleep(160); }
            tg.release();
        }catch(Exception e){} } });
    }
    void flash(){
        runOnUiThread(new Runnable(){ public void run(){
            final View f=new View(MainActivity.this); f.setBackgroundColor(0x5500B8E8);
            overlay.addView(f,new FrameLayout.LayoutParams(-1,-1));
            f.animate().alpha(0f).setDuration(650).withEndAction(new Runnable(){ public void run(){ overlay.removeView(f); } }).start();
        } });
    }
    void showAchievementBanner(final String icon,final String title,final boolean egg){
        runOnUiThread(new Runnable(){ public void run(){
            LinearLayout b=new LinearLayout(MainActivity.this); b.setOrientation(LinearLayout.HORIZONTAL); b.setGravity(Gravity.CENTER_VERTICAL);
            b.setBackground(box(0xFF0D1117, egg?GOLD:CYAN, 10)); b.setPadding(dp(14),dp(12),dp(16),dp(12));
            TextView ic=text(icon,26,T1,false); ic.setPadding(0,0,dp(14),0); b.addView(ic);
            LinearLayout c=new LinearLayout(MainActivity.this); c.setOrientation(LinearLayout.VERTICAL);
            c.addView(text(egg?"✦ SECRET UNLOCKED":"ACHIEVEMENT UNLOCKED",9.5f,egg?GOLD:CYAN,true));
            c.addView(text(title,15,T1,true)); b.addView(c);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2); lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; lp.topMargin=dp(38); lp.leftMargin=dp(14); lp.rightMargin=dp(14);
            b.setLayoutParams(lp); b.setTranslationY(dp(-160)); b.setAlpha(0f);
            overlay.addView(b);
            b.animate().translationY(0).alpha(1f).setDuration(420).start();
            b.postDelayed(new Runnable(){ public void run(){
                b.animate().translationY(dp(-160)).alpha(0f).setDuration(380).withEndAction(new Runnable(){ public void run(){ overlay.removeView(b); } }).start();
            } }, egg?3200:2400);
        } });
    }

    String fmtDate(String iso){ try{ if(iso==null||iso.length()<10) return iso; String d=iso.substring(0,10); String t=iso.length()>=16?iso.substring(11,16):""; return d+(t.length()>0?" "+t:""); }catch(Exception e){ return iso; } }
    String appVer(){ try{ return getPackageManager().getPackageInfo(getPackageName(),0).versionName; }catch(Exception e){ return "?"; } }

    void replayMeta(String[] m){
        if(m[0].equals("meta_king")){ grandUnlock(m[2],m[3],"You own this. The Parliament bows."); return; }
        if(m[0].equals("meta_allsecrets")||allEggsFound()&&false){ }
        boolean egg=m[1].equals("egg");
        showAchievementBanner(m[2],m[3],egg); playUnlock(egg); buzz(); if(egg) flash();
    }
    void grandUnlock(final String icon,final String title,final String sub){
        runOnUiThread(new Runnable(){ public void run(){
            final FrameLayout fx=new FrameLayout(MainActivity.this); fx.setBackgroundColor(0xDD0A0E13);
            overlay.addView(fx,new FrameLayout.LayoutParams(-1,-1));
            final String[] emo={"👑","🛡️","🎖️","⭐","💛","✨","🔥","🦉"};
            final java.util.Random rnd=new java.util.Random();
            final int W=getResources().getDisplayMetrics().widthPixels, H=getResources().getDisplayMetrics().heightPixels;
            for(int i=0;i<60;i++){ TextView e=text(emo[rnd.nextInt(emo.length)],20+rnd.nextInt(30),GOLD,false);
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2); lp.leftMargin=rnd.nextInt(W); lp.topMargin=-120-rnd.nextInt(500); e.setLayoutParams(lp);
                fx.addView(e); e.animate().translationY(H+300).rotationBy(rnd.nextInt(900)-450).setDuration(2000+rnd.nextInt(1800)).setStartDelay(rnd.nextInt(900)).start(); }
            LinearLayout card=new LinearLayout(MainActivity.this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
            card.setBackground(box(0xFF0D1117,GOLD,16)); card.setPadding(dp(28),dp(26),dp(28),dp(26));
            FrameLayout.LayoutParams clp=new FrameLayout.LayoutParams(-2,-2); clp.gravity=Gravity.CENTER; clp.leftMargin=dp(22); clp.rightMargin=dp(22); card.setLayoutParams(clp);
            card.addView(textC(icon,52)); card.addView(textC("✦ ✦ ✦",16,GOLD,true));
            card.addView(textC(title,22,GOLD,true)); card.addView(textC(sub,12,T1,false));
            card.setAlpha(0f); card.setScaleX(0.4f); card.setScaleY(0.4f);
            fx.addView(card); card.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(300).setDuration(700).start();
            playUnlock(true); buzz();
            fx.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ overlay.removeView(fx); } });
            fx.postDelayed(new Runnable(){ public void run(){ try{ overlay.removeView(fx); }catch(Exception e){} } }, 6500);
        } });
    }

    void showUpdateReview(String fromV,String toV,int gained){
        String msg = "Updated to v"+toV+".\n\n";
        if(gained>0) msg += "While you were away, "+gained+" new app achievement"+(gained==1?"":"s")+" were added that you ALREADY qualify for — they’ve been unlocked silently (no banner spam).\n\n";
        else msg += "New content may be live.\n\n";
        msg += "Take a moment: More → 🏆 App Achievements to review everything new — tap any unlocked one to REPLAY its unlock animation.";
        new AlertDialog.Builder(this).setTitle("✨ What’s New").setMessage(msg).setPositiveButton("REVIEW LATER",null).show();
    }

    /* ===== adapter ===== */
    class AchAdapter extends BaseAdapter {
        boolean pinMode; List<JSONObject> items=new ArrayList<JSONObject>();
        AchAdapter(boolean pinMode){ this.pinMode=pinMode; }
        void refilter(){ items.clear();
            boolean global = !pinMode && query.length()>0; // v1.6 — typing searches ALL games, not just the open one
            for(JSONObject o:all){
                String aid=o.optString("id");
                if(pinMode){ if(!pins.contains(aid)) continue; }
                else {
                    if(!global && !curGame.equals(o.optString("game"))) continue;
                    boolean d=done.contains(aid);
                    if(fStatus.equals("TODO")&&d) continue;
                    if(fStatus.equals("DONE")&&!d) continue;
                    if(!fType.equals("all")&&!fType.equals(o.optString("type"))) continue;
                    if(!global && fMission.length()>0&&!fMission.equals(o.optString("mission",""))) continue;
                    if(query.length()>0&&!(o.optString("name")+" "+o.optString("desc")).toLowerCase().contains(query)) continue;
                }
                items.add(o); }
            if(!pinMode && !fSort.equals("default")){ // v1.6 sort modes
                java.util.Collections.sort(items,new java.util.Comparator<JSONObject>(){
                    public int compare(JSONObject a,JSONObject b){
                        if(fSort.equals("gs")){ int r=b.optInt("gs")-a.optInt("gs"); return r!=0?r:a.optString("name").compareToIgnoreCase(b.optString("name")); }
                        if(fSort.equals("time")){ double x=estHrs(a),y=estHrs(b); return x<y?-1:(x>y?1:a.optString("name").compareToIgnoreCase(b.optString("name"))); }
                        return a.optString("name").compareToIgnoreCase(b.optString("name")); } });
            }
            notifyDataSetChanged(); }
        public int getCount(){ return items.size(); }
        public Object getItem(int i){ return items.get(i); }
        public long getItemId(int i){ return i; }
        public View getView(int pos,View cv,ViewGroup parent){
            JSONObject o=items.get(pos);
            boolean d=done.contains(o.optString("id"));
            LinearLayout row; TextView chk,nm,ds,gs; android.widget.ImageView img;
            if(cv instanceof LinearLayout && cv.getTag()!=null){
                row=(LinearLayout)cv; View[] h=(View[])cv.getTag();
                chk=(TextView)h[0]; nm=(TextView)h[1]; ds=(TextView)h[2]; gs=(TextView)h[3]; img=(android.widget.ImageView)h[4];
            } else {
                row=new LinearLayout(MainActivity.this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10),dp(9),dp(12),dp(9));
                chk=text("",17,T3,true); chk.setPadding(0,0,dp(8),0); row.addView(chk);
                img=new android.widget.ImageView(MainActivity.this);
                LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(44),dp(44)); ilp.rightMargin=dp(10);
                img.setLayoutParams(ilp); img.setBackground(box(BG2,LINE,5)); img.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP); img.setClipToOutline(true); row.addView(img);
                LinearLayout mid=new LinearLayout(MainActivity.this); mid.setOrientation(LinearLayout.VERTICAL);
                mid.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
                nm=text("",13.5f,T1,true); mid.addView(nm);
                ds=text("",11,T2,false); mid.addView(ds);
                row.addView(mid);
                gs=text("",12.5f,GOLD,true); gs.setPadding(dp(10),0,0,0); row.addView(gs);
                row.setTag(new View[]{chk,nm,ds,gs,img});
            }
            row.setBackground(box(d?0xFF11231A:CARD,d?0xFF1E4D2E:LINE,7));
            chk.setText(d?"✔":"☐"); chk.setTextColor(d?GREEN:T3);
            String badges=(pins.contains(o.optString("id"))?" 📌":"")
                +("speed".equals(o.optString("type"))?" ⏱":(o.optBoolean("missable")?" ⚠":""))
                +(o.optBoolean("coop")?" 👥":"");
            nm.setText(o.optString("icon")+" "+o.optString("name")+badges); nm.setTextColor(d?T2:T1);
            boolean tagGame = pinMode || query.length()>0; // v1.6 — global search results show their game
            ds.setText((tagGame?("["+gameName(o.optString("game")).replace("Halo: ","").replace("Halo ","H")+"]  "):"")+o.optString("desc")); ds.setTextColor(d?T3:T2);
            gs.setText(o.optInt("gs")+"G"); gs.setTextColor(d?GREEN:GOLD);
            String iu=o.optString("img","");
            if(iu.length()>0||o.optString("id").length()>0){
                loadIcon(o.optString("id"),iu,img);
                if(d){ img.clearColorFilter(); img.setImageAlpha(255); }
                else { android.graphics.ColorMatrix cm=new android.graphics.ColorMatrix(); cm.setSaturation(0f);
                    img.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm)); img.setImageAlpha(135); }
                img.setVisibility(View.VISIBLE);
            } else img.setVisibility(View.GONE);
            return row;
        }
    }

    @Override public void onBackPressed(){ if(!tab.equals("home")) show("home"); else super.onBackPressed(); }
}
