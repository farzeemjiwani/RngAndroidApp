package com.androidbelieve.drawerwithswipetabs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

public class NotificationActivity extends AppCompatActivity {
    RecyclerView l1;
    SharedPreferences sharedPref;
    NotificationAdapter c;
    NotificationUpdater n;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_notification);
        l1 = (RecyclerView) findViewById(R.id.rv_notification);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        l1.setLayoutManager(mLayoutManager);
        toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedPref=getSharedPreferences("NOTI",MODE_PRIVATE);
        String strJson = sharedPref.getString("jsondata","0");
        Log.v("Str json",strJson);
        if(!strJson.equals("0"))
        {
            Log.v("Trying to request","kdf");
            try
            {
                c = new NotificationAdapter(getApplicationContext(), new LinkedList<Notification>(),this);
                JSONObject js = new JSONObject(strJson);
                JSONArray jarray = js.getJSONArray("result");
                String notes[] = new String[jarray.length()];
                for (int i = 0; i < jarray.length(); i++)
                {
                    notes[i] = jarray.getJSONObject(i).getString("MESSAGE");
                    String timestamp=jarray.getJSONObject(i).getString("TIMESTAMP");
                    c.updateList(notes[i],jarray.getJSONObject(i).getString("TYPE"),jarray.getJSONObject(i).getString("NID"),timestamp);
                }
                l1.setAdapter(c);
                n= (NotificationUpdater) new NotificationUpdater(AccessToken.getCurrentAccessToken().getUserId()).execute();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            c = new NotificationAdapter(getApplicationContext(), new LinkedList<Notification>(),this);
            l1.setAdapter(c);
            new NotificationUpdater(AccessToken.getCurrentAccessToken().getUserId()).execute();
        }
    }
    void clearNotis()
    {
        sharedPref.edit().putString("jsondata","").commit();

    }
    void removeButton(String nid)
    {
        Log.v("NIDinremovebut",nid);
        n.run=false;
        n.cancel(true);
        String strJson = sharedPref.getString("jsondata","0");
        JSONObject js;
        JSONArray jarray=new JSONArray();
        try {
            if (!strJson.equals("0")) {
                js = new JSONObject(strJson);
                jarray = js.getJSONArray("result");
                JSONArray finaljarray = new JSONArray();

                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject c = jarray.getJSONObject(i);
                    System.out.println("currentjsondata"+c.getString("NID")+"Given"+nid);
                    if (c.getString("NID").equals(nid)) {

                        c.remove("TYPE");
                        c.put("TYPE", "REPLIED");
                        Log.v("inside","inside");

                    }
                Log.v("finaljson",finaljarray.toString());
                    finaljarray.put(c);
                }


                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", finaljarray);
                sharedPref.edit().putString("jsondata", jsonObject.toString()).commit();
                n.run = true;
                n.execute();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    /*@Override
    protected void onPause()
    {
        super.onPause();
        n.run=false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        n.run=false;
    }*/

    class NotificationUpdater extends AsyncTask<String,String, Void> {

        String pid;
        boolean run;
        String Notis[];
        NotificationUpdater(String pid) {
            this.pid = pid;
            run=true;
        }

        @Override
        protected Void doInBackground(String... strings) {
            long time=5000;
            while(run)
            {

                String link = "http://rng.000webhostapp.com/getnoti.php?pid=" + pid;

                Log.v("link", link);
                try {
                    URL url = new URL(link);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);

                    }
                    Log.v("Sbbb",sb.toString());
                    if(!sb.toString().equals("{\"result\":[]}")) {
                        Log.v("New notis found!!","Lol");
                        this.publishProgress(sb.toString());
                    }
                    Thread.sleep(time);
                    time+=time;
                    if(time==5000*6)
                        return null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String...result) {
            Log.v("result", result[0]);
            JSONObject js;
            JSONArray jarray=new JSONArray();

            if (result[0] != null) {
                try {
                    String strJson = sharedPref.getString("jsondata","0");
                    if(!strJson.equals("0")) {
                        js = new JSONObject(strJson);
                        jarray = js.getJSONArray("result");
                    }
                    JSONObject jsonObj = new JSONObject(result[0]);
                    JSONArray notification = jsonObj.getJSONArray("result");
                    String notes[] = new String[notification.length()];
                    for (int i = 0; i < notification.length(); i++) {
                        JSONObject notice = notification.getJSONObject(i);
                        jarray.put(notice);
                        notes[i] = notice.getString("MESSAGE");
                        c.updateList(notes[i],notice.getString("TYPE"),notice.getString("NID"),notice.getString("TIMESTAMP"));
                    }
                    js = new JSONObject();
                    js.put("result", (JSONArray) jarray);
                    sharedPref.edit().putString("jsondata", js.toString()).commit();

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_noti, menu);
        return super.onCreateOptionsMenu(menu);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                return true;
            case R.id.action_clear:
                clearNotis();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

