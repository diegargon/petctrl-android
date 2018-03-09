package net.envigo.petctrl;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //private static final String TAG_RET_OVERVIEW = "OverviewFragment";

    //private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    protected SharedPreferences settings = null;

    //private ArrayList<Fragment> TabFragmentList;
    public ArrayList<String> tabTitles = new ArrayList<>();

    private List<Fragment> tabFragments = new ArrayList<>();
    int OverViewPos;

    protected OverviewFragment OvFrag;




    Context context;


    protected MyFragmentPageAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private WifiUtils wifiUtils;
    protected TabLayout mTabLayout;

    private String admin_password = null;
    private String ap_name = null;



    private boolean ShowWelcome = true;

    protected ArrayList<PetClients> PetClientList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Log", "MainActivity onCreate called" + this);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        admin_password = settings.getString("admin_password", null);
        ap_name = settings.getString("ap_name", null);
        ShowWelcome = settings.getBoolean("checkWelcome", true);

        if (checkConfig(true)) {
            wifiUtils = new WifiUtils(context);
            wifiUtils.cfgAP(ap_name, admin_password);

            Log.d("Log", "Main, configuring user ap mode");
        }
        if (savedInstanceState != null) {

            //ashOn = savedInstanceState.getBoolean("isFlashOn");
        }
        //prefs = getSharedPreferences(My_Prefs, MODE_PRIVATE);
        //AutoOn = prefs.getBoolean("AutoOn", true);

        setupTabs();


        requestPermissions();

    }

    public boolean checkConfig(boolean msg) {
        if (admin_password != null && ap_name != null && admin_password.length() >= 6 && ap_name.length() >= 4) {
            return true;
        }
        if (msg) {
            Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
        }
        return false;
    }
    public void getClientList() {
        if(wifiUtils == null)
            return;

        wifiUtils.getClientList(true, 300, new iScanListener() {
            @Override
            public void onFinishScan(ArrayList<PetClients> clients) {

                /* Trabajamos sobre _tmp para poder asignar la nueva lista para que no de error
                al abrir addnewtab y detectar nuevas clientes
                 */
                ArrayList<PetClients> PetClientList_tmp = PetClientList;

                //PetClientList = clients;

                for (PetClients clientScanResult : clients) {

                    if(PetClientList_tmp.size() > 0) {

                        boolean coincidence = false;
                        for (PetClients result : PetClientList_tmp) {

                            if (result.getHWAddr().equals(clientScanResult.getHWAddr())) {

                                coincidence = true;

                            } else {
                                //Log.d("Log", "Petclientlist exists " );
                            }
                        }
                        if (!coincidence) {
                            //Log.d("Log", "Petclientlist not exists ");
                            PetClientList.add(clientScanResult);
                            Log.d("Log", "assgined slot client" + PetClientList.size());
                            get_client_info(clientScanResult);

                        }
                    } else {
                        //Log.d("Log", "Petclientlist first ");
                        PetClientList.add(clientScanResult);
                        Log.d("Log", "assgined slot client" + PetClientList.size());
                        get_client_info(clientScanResult);

                    }
                    newClient();
                }

            }

        });
    }

    public void get_client_info(final PetClients client) {
        HashMap<String, String> conn_details = new HashMap<>();
        HashMap<String, String> conn_data = new HashMap<>();

        conn_details.put("method", "POST");
        conn_details.put("url", "http://" + client.getIpAddr() + "/client_info");

        conn_data.put("admin_password", admin_password);

        ConnRest conn = new ConnRest(new iConnResult() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (jsonObject == null) {
                    Log.d("Log", "Success without response");

                } else {
                    Log.d("Log", "Success" +  jsonObject.toString() );
                    try {
                        if (jsonObject.getString("status").equals("ok")) {
                            client.setName(jsonObject.getString("name"));
                            client.setChip(jsonObject.getString("chipid"));
                            client.setPhoneNumber(jsonObject.getString("phone"));
                            String RSSI = jsonObject.getString("RSSI");
                            client.setRSSI(Integer.parseInt(RSSI));

                            addClientTab(client.getName(), PetClientList.size() -1);

                        } else {
                            Toast.makeText(context, jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }



                }

            }

            @Override
            public void onFailure(Exception e) {
                Log.d("Log", "Conn OnFail: " + e.getMessage());
                //TODO: Borrar PetclientList item failed que metimos en getclientlist si la conexion falla
            }
        });

        conn.execute(conn_details, conn_data);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent modifySettings=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(modifySettings);
            return true;
        }

        if (id == R.id.action_exit) {
            wifiUtils.disableAP();
            finish();
            return true;
        }
        if (id == R.id.action_pairdevices) {
            if (checkConfig(true) == true ) {
                showPairTab();
                Toast.makeText(context, R.string.closeToContinue, Toast.LENGTH_SHORT).show();
            }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }



    public class MyFragmentPageAdapter extends FragmentPagerAdapter {

        //TODO: Ver para que funciona actualmente pos, por que no sirve para la posicion actual
        public  int pos = 0;

        public List<Fragment> myFragments = new ArrayList<>();
        private ArrayList<String> categories = new ArrayList<>();
        private Context context;

        //public MyFragmentPageAdapter(Context context, FragmentManager fm, List<Fragment> myFrags, ArrayList<String> cats) {
        public MyFragmentPageAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.context = context;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //return super.instantiateItem(container, position);

            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

            if (createdFragment instanceof OverviewFragment) {
                OverviewFragment Overfrag = (OverviewFragment) createdFragment;
                Overfrag.update();
            }
            if (createdFragment instanceof  ClientFragment) {
                ClientFragment Clientfrag = (ClientFragment) createdFragment;
                Clientfrag.update();
            }

            Log.d("Log", "instantiateItem called, position " + position + "->" + createdFragment);
            myFragments.set(position, createdFragment);
            tabFragments.set(position, createdFragment);
            return createdFragment;

        }

        @Override
        public Fragment getItem(int position) {
            Log.d("Log", "getItem adapter called " + position);

            return myFragments.get(position);

        }

        @Override
        public int getCount() {
            //Log.d("Log", "getCount adapter called->" + myFragments.size());
            return myFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return super.getPageTitle(position);
            //Log.d("Log", "getPageTitle adapter called" + position);
            setPos(position);
            return categories.get(position);

        }

        public int getPos() {
            return pos;
        }

        public void addFragment(Fragment fragment, String title) {
            Log.d("Log", "Frag: " +getCount() + " " + fragment);
            myFragments.add( fragment);
            tabFragments.add(fragment);
            categories.add( title);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
        public void removeFragment(int position) {
            myFragments.remove(position);
            tabFragments.remove(position);
            categories.remove(position);
            mSectionsPagerAdapter.notifyDataSetChanged();

        }

        public void setPos(int pos) {
            mSectionsPagerAdapter.pos = pos;
        }
    }


    void setupTabs() {

//        tabTitles.add("Emparejar");

        //tabFragments = buildFragments();
        //Tabs

//        mSectionsPagerAdapter = new MyFragmentPageAdapter(this ,getSupportFragmentManager(), tabFragments, tabTitles);
        mSectionsPagerAdapter = new MyFragmentPageAdapter(this ,getSupportFragmentManager());
        if (ShowWelcome) {
            //tabTitles.add("Welcome");
            showWelcomeTab();
        }

        showOverviewTab();
        //mSectionsPagerAdapter.addFragment(new WelcomeFragment(), "one");
        //mSectionsPagerAdapter.addFragment(new PetSettings(), "two");
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);


    }

    /*
    private List<Fragment> buildFragments() {
        List<Fragment> fragments = new ArrayList<>();
        for(int i = 0; i<tabTitles.size(); i++) {
            Log.d("Log", "Tabsize:" + tabTitles.size() + "buildFragment loop->" + i);
            Bundle b = new Bundle();
            b.putInt("position", i);
            if(i == 0) {
                fragments.add(Fragment.instantiate(this, WelcomeFragment.class.getName(), b));
            } else {
                fragments.add(Fragment.instantiate(this, PetSettings.class.getName(), b));
            }
        }

        return fragments;
    }
    */

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Log", "Main onStop called");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* TODO; Al salir de config tambien lo desabilita
        if (wifiUtils != null) {
            wifiUtils.disableAP();
        }
        */

        Log.d("Log", "Main activity on destroy called");
    }

    public void addClientTab(String title, int PetClient_pos) {
        Log.d("Log", "addclienttab:" + PetClient_pos);
        //mSectionsPagerAdapter.addFragment(new ClientFragment(), title);
        ClientFragment PetClientFrag = ClientFragment.newInstance(PetClient_pos);
        mSectionsPagerAdapter.addFragment(PetClientFrag, title);
        PetClientFrag.setupClient();
        overviewText();
    }

    private void showPairTab() {
        //TODO: al dar dos veces salen dos pantallas
        //PetSettings pf = new PetSettings();
        //mSectionsPagerAdapter.addFragment(pf, "Emparejar");
        mSectionsPagerAdapter.addFragment(PetSettings.newInstance(), "Emparejar");
        mTabLayout.getTabAt(mSectionsPagerAdapter.getCount() ).select();

    }
    private void showWelcomeTab() {
        WelcomeFragment wf = new WelcomeFragment();
        mSectionsPagerAdapter.addFragment(wf, "Bienvenido");
    }
    private void showOverviewTab() {
        //Log.d("Log", "showOverviewTab called " + this);
/*
        if(OverviewFragment == null) {
            OverviewFragment = OverviewFragment.newInstance();
            Log.d("Log", "Newinstance : " + OverviewFragment);
        }
*/
        OverviewFragment of = new OverviewFragment();

        //OverviewFragment = new OverviewFragment();

        //mSectionsPagerAdapter.addFragment(OverviewFragment.newInstance(), getString(R.string.overview));
        OverViewPos = tabFragments.size();
        mSectionsPagerAdapter.addFragment(of, getString(R.string.overview));
    }
    void requestPermissions() {
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},  PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Cambiar ajustes
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);

            }

        }


    }

    public void newClient() {
        overviewText();
    }

    public void overviewText () {
        Log.d("Log","overviewText main");
        String text = "Clientes: " + PetClientList.size() + "\n";

        for (PetClients clientScanResult : PetClientList) {
            text += "Nombre: " + clientScanResult.getName();
            text += " Online: ";
            text += clientScanResult.isReachable() ? "Si" : "No";
            text += "\n";
        }

        OverviewFragment fr = (OverviewFragment) tabFragments.get(OverViewPos);
        fr.setText(text);

    }

    protected String getAdminPassword() {
        return admin_password;
    }

    public void closeCurrentTab() {
        int CurrentPos =  mSectionsPagerAdapter.getPos();
        Log.d("Log", "Close tab called " + CurrentPos);
//        mTabLayout.getTabAt(position -1 ).select();
        mTabLayout.removeTabAt(CurrentPos);
        mSectionsPagerAdapter.removeFragment(CurrentPos);

        overviewText();

    }
}
