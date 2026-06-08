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
        setContentView(root);
        show("home");
    }

    void loadSet(Set<String> s,String key){ String v=prefs.getString(key,""); if(v.length()>0) for(String x:v.split(",")) s.add(x); }
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

    void show(String t){ tab=t; restyleNav(); content.removeAllViews();
        if(t.equals("home")) content.addView(buildHome());
        else if(t.equals("games")) content.addView(buildGames());
        else if(t.equals("pins")) content.addView(buildPins());
        else content.addView(buildMore()); }

    /* ===== HOME ===== */
    View buildHome(){
        ScrollView sv=new ScrollView(this);
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14),dp(16),dp(14),dp(20)); sv.addView(col);
        TextView title=text("⛨ UNSC TERMINAL",20,CYAN,true); title.setLetterSpacing(0.16f); col.addView(title);
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
            gc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ curGame=gid; show("games"); } });
            col.addView(gc); }
        TextView foot=text("\n◇ FOR PERSONAL GLORY ◇",9.5f,T3,false); foot.setGravity(Gravity.CENTER); col.addView(foot);
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
            ch.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ curGame=gid; show("games"); } });
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
            public void afterTextChanged(Editable s){ query=s.toString().toLowerCase(); if(adapter!=null) adapter.refilter(); } });
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
        String extra="Difficulty: "+o.optString("diff","—")+"   Time: "+o.optString("time","—")
            +"\nType: "+o.optString("type","—")+"   Mode: "+o.optString("mode","—")
            +(o.optString("mission","").length()>0?"\nMission: "+o.optString("mission"):"")
            +(o.optBoolean("missable")?"\n⚠ MISSABLE":"")+(o.optBoolean("coop")?"\n👥 CO-OP friendly":"");
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
                    Toast.makeText(MainActivity.this,pins.contains(aid)?"📌 pinned":"unpinned",Toast.LENGTH_SHORT).show(); } })
            .setNegativeButton("CLOSE",null).show();
    }

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

        LinearLayout ex=card(); ex.addView(text("💾 DATA",9.5f,T2,true));
        TextView cp=text("COPY PROGRESS BACKUP",12,GREEN,true); cp.setBackground(box(CARD2,GREEN,6)); cp.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-2,-2); clp.topMargin=dp(8); cp.setLayoutParams(clp);
        cp.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("halo",prefs.getString("done","")));
            Toast.makeText(MainActivity.this,"✓ progress copied — paste anywhere safe",Toast.LENGTH_SHORT).show(); } });
        ex.addView(cp);
        ex.addView(text("Database: 690 achievements / 7,110G imported from Halopedia (live icons + wiki links). Exact-700 reconciliation vs TrueAchievements: next update.",9,T3,false));
        col.addView(ex);

        TextView ab=text("\nUNSC TERMINAL v2.0 · native\n© 2026 Parliament Four · for personal glory",9,T3,false);
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
                            if (id != null && !done.contains(id)) { done.add(id); matched++; }
                        }
                    }
                }
            } catch (Exception e) { err = String.valueOf(e); }
            final int fm = matched; final String fe = err;
            runOnUiThread(new Runnable() { public void run() {
                if (fe != null) { Toast.makeText(MainActivity.this, "sync failed: " + fe, Toast.LENGTH_LONG).show(); return; }
                saveSet(done, "done");
                Toast.makeText(MainActivity.this, "✔ Xbox sync: +" + fm + " unlocked", Toast.LENGTH_LONG).show();
                show(tab); } });
        } });
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
