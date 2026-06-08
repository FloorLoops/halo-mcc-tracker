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
    static final String[] TYPES={"all","story","skull","terminal","speed","legendary","laso","multiplayer","firefight","spartan_ops","collectible","meta"};

    Map<String,JSONObject> games=new LinkedHashMap<String,JSONObject>();
    List<JSONObject> all=new ArrayList<JSONObject>();
    Set<String> done=new HashSet<String>(); Set<String> pins=new HashSet<String>();
    SharedPreferences prefs;
    int totalGs=0;
    String tab="home", curGame="ce", fStatus="ALL", fType="all", query="";
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
        }catch(Exception e){}
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
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
    int[] count(String gid){ int n=0,dn=0,gs=0,gsd=0;
        for(JSONObject o:all){ if(gid!=null&&!gid.equals(o.optString("game"))) continue; n++; gs+=o.optInt("gs");
            if(done.contains(o.optString("id"))){dn++; gsd+=o.optInt("gs");}} return new int[]{n,dn,gs,gsd}; }
    String[] rank(int pct){ String[] cur=RANKS[0]; for(String[] r:RANKS) if(pct>=Integer.parseInt(r[0])) cur=r; return cur; }

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
        else content.addView(buildMore()); }

    /* ===== HOME ===== */
    View buildHome(){
        ScrollView sv=new ScrollView(this);
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14),dp(16),dp(14),dp(20)); sv.addView(col);
        final TextView title=text("⛨ UNSC TERMINAL",20,CYAN,true); title.setLetterSpacing(0.16f);
        title.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(++titleTaps>=7){ titleTaps=0; unlockMeta("egg_bloom"); } } });
        col.addView(title);
        col.addView(text("MCC ACHIEVEMENT DATABASE · CLASSIFIED",9.5f,T3,false));

        int[] t=count(null); int pct=t[0]==0?0:100*t[1]/t[0];
        String[] rk=rank(pct);
        LinearLayout rc=card(); rc.setBackground(box(CARD,CYAN,8));
        LinearLayout rrow=new LinearLayout(this); rrow.setOrientation(LinearLayout.HORIZONTAL); rrow.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic=text(rk[2],30,T1,false); ic.setPadding(0,0,dp(12),0); rrow.addView(ic);
        LinearLayout rcol=new LinearLayout(this); rcol.setOrientation(LinearLayout.VERTICAL);
        rcol.addView(text("RANK",9,T3,true));
        rcol.addView(text(rk[1],19,CYAN,true));
        rcol.addView(text("▸ "+rk[3],10.5f,T2,false));
        rrow.addView(rcol);
        TextView ladderHint=text("▸",20,T3,true); rrow.addView(ladderHint);
        rc.addView(rrow);
        rc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(PREMIUM) showRankLadder(); else upsell("Full rank ladder"); } });
        col.addView(rc);
        // time-to-100 (premium)
        if(PREMIUM){
            LinearLayout tc=card();
            tc.addView(text("⏳ ESTIMATED TIME TO 100%",9.5f,T2,true));
            double[] est=timeLeft();
            tc.addView(text(fmtHours(est[0])+" remaining",20,GOLD,true));
            tc.addView(text(fmtHours(est[1])+" total campaign · "+fmtHours(est[1]-est[0])+" done",10.5f,T2,false));
            tc.addView(text("rough estimate from per-achievement time tags (where known)",8.5f,T3,false));
            col.addView(tc);
        }

        LinearLayout oc=card();
        oc.addView(text("CAMPAIGN PROGRESS",9.5f,T2,true));
        oc.addView(text(t[1]+" / "+t[0]+"  ·  "+pct+"%",23,GREEN,true));
        oc.addView(text("GAMERSCORE  "+t[3]+" / "+t[2]+" G",12,GOLD,false));
        oc.addView(bar(pct,GREEN));
        oc.addView(text("official database · 690 achievements · Halopedia import",8.5f,T3,false));
        col.addView(oc);

        for(Map.Entry<String,JSONObject> e:games.entrySet()){
            final String gid=e.getKey(); JSONObject g=e.getValue();
            int[] c=count(gid); if(c[0]==0) continue; int gp=100*c[1]/c[0];
            int accent=Color.parseColor(g.optString("color","#00b8e8"));
            LinearLayout gc=card(); if(gp==100) gc.setBackground(box(CARD,GREEN,8));
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView nm=text(g.optString("name"),15,T1,true); nm.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f)); row.addView(nm);
            row.addView(text(gp==100?"✔ 100%":gp+"%",14,gp==100?GREEN:accent,true));
            gc.addView(row);
            gc.addView(text(g.optString("year")+" · "+c[1]+"/"+c[0]+" · "+c[3]+"/"+c[2]+" G",10,T2,false));
            gc.addView(bar(gp,accent));
            gc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ curGame=gid; visitGame(gid); show("games"); } });
            col.addView(gc); }
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
    double[] timeLeft(){ double rem=0,tot=0;
        for(JSONObject o:all){ double h=parseHrs(o.optString("time","")); if(h<=0) h=0.4;
            tot+=h; if(!done.contains(o.optString("id"))) rem+=h; }
        return new double[]{rem,tot}; }
    String fmtHours(double h){ if(h>=1) return (h>=10?Math.round(h):Math.round(h*10)/10.0)+"h"; return Math.round(h*60)+"m"; }

    void showRankLadder(){
        ScrollView sv=new ScrollView(this); LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8),dp(4),dp(8),dp(8)); sv.addView(col);
        int[] t=count(null); int pct=t[0]==0?0:100*t[1]/t[0];
        col.addView(text("UNSC RANK LADDER · you're at "+pct+"%",11,CYAN,true));
        for(String[] r:RANKS){ int rp=Integer.parseInt(r[0]); boolean reached=pct>=rp; boolean curr=r==rank(pct);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(box(curr?CARD2:CARD, curr?CYAN:LINE, 6)); row.setPadding(dp(12),dp(10),dp(12),dp(10));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(6); row.setLayoutParams(lp);
            TextView ic=text(r[2],22,reached?T1:T3,false); ic.setPadding(0,0,dp(12),0); row.addView(ic);
            LinearLayout c2=new LinearLayout(this); c2.setOrientation(LinearLayout.VERTICAL); c2.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
            c2.addView(text(r[1],14,reached?(curr?CYAN:GREEN):T2,curr));
            c2.addView(text("unlocks at "+rp+"%",9.5f,T3,false)); row.addView(c2);
            row.addView(text(reached?"✔":"",15,GREEN,true)); col.addView(row); }
        new AlertDialog.Builder(this).setView(sv).setPositiveButton("CLOSE",null).show();
    }

    /* ===== GAMES ===== */
    View buildGames(){
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10),dp(12),dp(10),0);
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); hs.addView(chips);
        for(Map.Entry<String,JSONObject> e:games.entrySet()){
            final String gid=e.getKey(); int[] c=count(gid); if(c[0]==0) continue;
            boolean on=gid.equals(curGame);
            TextView ch=text(e.getValue().optString("name").replace("Halo: ","").replace("Halo ","H")+" "+(100*c[1]/c[0])+"%",12,on?CYAN:T2,on);
            ch.setBackground(box(on?CARD2:CARD,on?CYAN:LINE,16)); ch.setPadding(dp(14),dp(8),dp(14),dp(8));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.rightMargin=dp(7); ch.setLayoutParams(lp);
            ch.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(gid.equals(chipLast)){ if(++chipTaps>=7) unlockMeta("egg_madrigal"); } else { chipLast=gid; chipTaps=1; } curGame=gid; visitGame(gid); show("games"); } });
            chips.addView(ch); }
        col.addView(hs);

        EditText search=new EditText(this);
        search.setHint("🔎 search achievements…"); search.setText(query);
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
        String[] fs={"ALL","TODO","DONE"};
        for(int i=0;i<3;i++){ final String f=fs[i]; boolean on=f.equals(fStatus);
            TextView fb=text(f,11.5f,on?CYAN:T2,true); fb.setGravity(Gravity.CENTER);
            fb.setBackground(box(on?CARD2:CARD,on?CYAN:LINE,6)); fb.setPadding(0,dp(8),0,dp(8));
            LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,-2,1f); if(i>0) blp.leftMargin=dp(6); fb.setLayoutParams(blp);
            fb.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ fStatus=f; show("games"); } });
            frow.addView(fb); }
        col.addView(frow);

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
                if(done.contains(aid)) done.remove(aid); else { done.add(aid); buzz(); sessionChecks++; }
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
            .setNeutralButton("GUIDES ▸",new android.content.DialogInterface.OnClickListener(){
                public void onClick(android.content.DialogInterface d,int w){
                    final String[] opts={"📚 Halopedia page","🏆 TrueAchievements","▶️ YouTube solutions"};
                    new AlertDialog.Builder(MainActivity.this).setTitle(o.optString("name"))
                        .setItems(opts,new android.content.DialogInterface.OnClickListener(){
                            public void onClick(android.content.DialogInterface dd,int which){
                                String u;
                                if(which==0) u=o.optString("wiki","https://www.halopedia.org");
                                else if(which==1) u="https://www.trueachievements.com/searchresults.aspx?search="+java.net.URLEncoder.encode(o.optString("name"));
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
            Toast.makeText(MainActivity.this,"✓ Grunt API key saved — career stats arrive in v1.3",Toast.LENGTH_SHORT).show(); } });
        gk.addView(gsave);
        gk.addView(text("Get ahead of v1.3: paste your Grunt API key now and career stats (medals, headshots, kills, accuracy, playtime) will light up automatically when that update lands.",9,T3,false));
        col.addView(gk);

        int[] t=count(null);
        LinearLayout stc=card(); stc.addView(text("📊 STATS",9.5f,T2,true));
        stc.addView(text("unlocked  "+t[1]+" / "+t[0],12.5f,T1,false));
        stc.addView(text("gamerscore  "+t[3]+" / "+t[2]+" G",12.5f,GOLD,false));
        stc.addView(text("pinned  "+pins.size()+"   ·   missables left  "+countFlag("missable"),12.5f,T1,false));
        stc.addView(text("LASO left  "+countType("laso")+"   ·   skulls left  "+countType("skull"),12.5f,PURPLE,false));
        double[] est2=timeLeft(); stc.addView(text("est. time to 100%  "+fmtHours(est2[0]),12.5f,GOLD,false));
        col.addView(stc);
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
        ac.addView(text("tap any unlocked achievement to replay its unlock · rank climbs as you earn more (XP overhaul in v1.2)",8.5f,T3,false));
        col.addView(ac);

        LinearLayout ex=card(); ex.addView(text("💾 DATA",9.5f,T2,true));
        TextView cp=text("COPY PROGRESS BACKUP",12,GREEN,true); cp.setBackground(box(CARD2,GREEN,6)); cp.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-2,-2); clp.topMargin=dp(8); cp.setLayoutParams(clp);
        cp.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("halo",prefs.getString("done","")));
            unlockMeta("ftback"); Toast.makeText(MainActivity.this,"✓ progress copied — paste anywhere safe",Toast.LENGTH_SHORT).show(); } });
        ex.addView(cp);
        ex.addView(text("Database: 690 achievements / 7,110G imported from Halopedia (live icons + wiki links). Exact-700 reconciliation vs TrueAchievements: next update.",9,T3,false));
        col.addView(ex);

        LinearLayout rm=card(); rm.addView(text("🗺️ ROADMAP",9.5f,T2,true));
        String[][] RM={
            {"1","v1.0","Native app · 690-achievement database · real icons · Xbox sync · guides"},
            {"1","v1.1","Icons crop-to-fit · rank ladder · time-to-100% · per-type stats"},
            {"1","v1.1.1","100 in-app achievements · animated banners · sounds · app-rank · secrets"},
            {"1","v1.1.2","In-app roadmap · sync no longer storms banners"},
            {"0","v1.1.x","Exact-700 reconciliation · smart weighted time-to-completion"},
            {"0","v1.2","XP-weighted ranking overhaul · Halo 3 rank icons · smart breakdowns & focus mode"},
            {"0","v1.2.5","Native UI glow-up (match the web version)"},
            {"0","v1.3","Career stats (medals, headshots…) · per-game icons · design pass"},
            {"0","v1.3.5","Achievement artwork viewer (HQ images)"},
            {"0","v1.4","Halo SFX & animations"},
            {"0","v1.5","Notification sound · tweaks"},
            {"0","v1.6","Home-screen widgets"},
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

        TextView ab=text("\nUNSC TERMINAL v1.1.5 · native (final pre-v1.2)\n© 2026 Parliament Four · for personal glory",9,T3,false);
        ab.setGravity(Gravity.CENTER); col.addView(ab);
        return sv;
    }
    int countFlag(String f){ int n=0; for(JSONObject o:all) if(o.optBoolean(f)&&!done.contains(o.optString("id"))) n++; return n; }
    int countType(String ty){ int n=0; for(JSONObject o:all) if(ty.equals(o.optString("type"))&&!done.contains(o.optString("id"))) n++; return n; }


    /* ===== icon loader ===== */
    static final java.util.concurrent.ExecutorService POOL = java.util.concurrent.Executors.newFixedThreadPool(4);
    final java.util.HashMap<String, android.graphics.Bitmap> memCache = new java.util.HashMap<String, android.graphics.Bitmap>();
    void loadIcon(final String url, final android.widget.ImageView iv) {
        iv.setTag(url);
        android.graphics.Bitmap c = memCache.get(url);
        if (c != null) { iv.setImageBitmap(c); return; }
        iv.setImageBitmap(null);
        POOL.execute(new Runnable() { public void run() {
            try {
                String fn = "ic_" + Integer.toHexString(url.hashCode());
                java.io.File f = new java.io.File(getCacheDir(), fn);
                android.graphics.Bitmap bm = null;
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
                final android.graphics.Bitmap fb = bm;
                if (fb != null) runOnUiThread(new Runnable() { public void run() {
                    memCache.put(url, fb);
                    if (url.equals(iv.getTag())) iv.setImageBitmap(fb); } });
            } catch (Exception e) {}
        } });
    }

    /* ===== xbox live sync (OpenXBL) ===== */
    void xboxSync() {
        final String key = prefs.getString("xblKey", "");
        if (key.length() == 0) { Toast.makeText(this, "save an OpenXBL key first (xbl.io)", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "⚡ syncing with Xbox Live…", Toast.LENGTH_SHORT).show();
        POOL.execute(new Runnable() { public void run() {
            int matched = 0; String err = null;
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL("https://xbl.io/api/v2/achievements/title/717072975").openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(20000);
                c.setRequestProperty("X-Authorization", key);
                c.setRequestProperty("Accept", "application/json");
                java.io.InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
                ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int r;
                while ((r = in.read(b)) > 0) bo.write(b, 0, r); in.close();
                if (c.getResponseCode() >= 400) { err = "HTTP " + c.getResponseCode(); }
                else {
                    JSONObject root = new JSONObject(new String(bo.toByteArray(), StandardCharsets.UTF_8));
                    JSONArray arr = root.optJSONArray("achievements");
                    if (arr == null) err = "no achievements in response — key OK?";
                    else {
                        java.util.HashMap<String, String> byName = new java.util.HashMap<String, String>();
                        for (JSONObject o : all) byName.put(o.optString("name").trim().toLowerCase(), o.optString("id"));
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject a = arr.getJSONObject(i);
                            String st = a.optString("progressState");
                            if (!"Achieved".equalsIgnoreCase(st)) continue;
                            String id = byName.get(a.optString("name").trim().toLowerCase());
                            if (id != null) {
                                String tu = a.optString("timeUnlocked","");
                                if (tu.length()==0) { JSONObject pg=a.optJSONObject("progression"); if(pg!=null) tu=pg.optString("timeUnlocked",""); }
                                if (tu.length()>0) unlockTimes.put(id, tu);
                                if (!done.contains(id)) { done.add(id); matched++; }
                            }
                        }
                    }
                }
            } catch (Exception e) { err = String.valueOf(e); }
            final int fm = matched; final String fe = err;
            runOnUiThread(new Runnable() { public void run() {
                if (fe != null) { Toast.makeText(MainActivity.this, "sync failed: " + fe, Toast.LENGTH_LONG).show(); return; }
                try{ JSONObject ut=new JSONObject(); for(java.util.Map.Entry<String,String> e:unlockTimes.entrySet()) ut.put(e.getKey(),e.getValue()); prefs.edit().putString("ut",ut.toString()).apply(); }catch(Exception e){}
                bulkUnlock=true; saveSet(done, "done");
                int beforeM=metas.size(); unlockMeta("ftsync"); checkMetas(); bulkUnlock=false;
                int gainedM=metas.size()-beforeM;
                Toast.makeText(MainActivity.this, "✔ Xbox sync: +" + fm + " achievements · +" + gainedM + " app achievements (silent)", Toast.LENGTH_LONG).show();
                show(tab); } });
        } });
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
        metaDef("gs250","gs:250","🪙","Pocket Change","Earn 250G of Gamerscore",false);
        metaDef("gs500","gs:500","💰","Coin Collector","Earn 500G of Gamerscore",false);
        metaDef("gs1000","gs:1000","🎮","GS Grinder","Earn 1000G of Gamerscore",false);
        metaDef("gs2000","gs:2000","🎯","Point Blank","Earn 2000G of Gamerscore",false);
        metaDef("gs3000","gs:3000","📊","Score Surge","Earn 3000G of Gamerscore",false);
        metaDef("gs4000","gs:4000","🎲","High Roller","Earn 4000G of Gamerscore",false);
        metaDef("gs5000","gs:5000","🎖️","GS General","Earn 5000G of Gamerscore",false);
        metaDef("gs6000","gs:6000","🔢","Number Cruncher","Earn 6000G of Gamerscore",false);
        metaDef("gs7000","gs:7000","🆙","Maxed Out","Earn 7000G of Gamerscore",false);
        metaDef("gs7110","gs:7110","🏆","Perfect Score","Earn 7110G of Gamerscore",false);
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
        if(gained>0) msg += "While you were away, "+gained+" new app achievement"+(gained==1?"":"s")+" were added that you ALREADY qualify for — they\u2019ve been unlocked silently (no banner spam).\n\n";
        else msg += "New content may be live.\n\n";
        msg += "Take a moment: More \u2192 \ud83c\udfc6 App Achievements to review everything new — tap any unlocked one to REPLAY its unlock animation.";
        new AlertDialog.Builder(this).setTitle("\u2728 What\u2019s New").setMessage(msg).setPositiveButton("REVIEW LATER",null).show();
    }

    /* ===== adapter ===== */
    class AchAdapter extends BaseAdapter {
        boolean pinMode; List<JSONObject> items=new ArrayList<JSONObject>();
        AchAdapter(boolean pinMode){ this.pinMode=pinMode; }
        void refilter(){ items.clear();
            for(JSONObject o:all){
                String aid=o.optString("id");
                if(pinMode){ if(!pins.contains(aid)) continue; }
                else {
                    if(!curGame.equals(o.optString("game"))) continue;
                    boolean d=done.contains(aid);
                    if(fStatus.equals("TODO")&&d) continue;
                    if(fStatus.equals("DONE")&&!d) continue;
                    if(!fType.equals("all")&&!fType.equals(o.optString("type"))) continue;
                    if(query.length()>0&&!(o.optString("name")+" "+o.optString("desc")).toLowerCase().contains(query)) continue;
                }
                items.add(o); }
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
            ds.setText((pinMode?("["+games.get(o.optString("game")).optString("name")+"]  "):"")+o.optString("desc")); ds.setTextColor(d?T3:T2);
            gs.setText(o.optInt("gs")+"G"); gs.setTextColor(d?GREEN:GOLD);
            String iu=o.optString("img","");
            if(iu.length()>0){
                loadIcon(iu,img);
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
