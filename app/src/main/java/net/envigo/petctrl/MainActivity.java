package net.envigo.petctrl;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    protected SharedPreferences settings = null;

    //private ArrayList<Fragment> TabFragmentList;
    public ArrayList<String> tabTitles = new ArrayList<>();
    private List<Fragment> tabFragments = new ArrayList<>();

    private Conn conn;

    Context context;

    protected MyFragmentPageAdapter mSectionsPagerAdapter;
    //private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private WifiUtils wifiUtils;
    protected TabLayout mTabLayout;

    private String admin_password;
    private String ap_name;



    private boolean ShowWelcome = true;

    private ArrayList<PetClients> PetClientList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        admin_password = settings.getString("admin_password", null);
        ap_name = settings.getString("ap_name", null);
        ShowWelcome = settings.getBoolean("checkWelcome", true);

        if (admin_password != null && ap_name != null  && admin_password.length() >= 6 && ap_name.length() >= 4) {
            wifiUtils = new WifiUtils(context);
            wifiUtils.cfgAP(ap_name, admin_password);

            Log.d("Log", "Main, configuring user ap mode");
        } else {
            Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
        }
        if (savedInstanceState != null) {
            //ashOn = savedInstanceState.getBoolean("isFlashOn");
        }
        //prefs = getSharedPreferences(My_Prefs, MODE_PRIVATE);
        //AutoOn = prefs.getBoolean("AutoOn", true);

        setupTabs();

        requestPermissions();

    }

    public void getClientList() {

        wifiUtils.getClientList(true, 300, new iScanListener() {
            @Override
            public void onFinishScan(ArrayList<PetClients> clients) {
                ArrayList<String> petList = new ArrayList<>();

                //PetClients clientScanResult
                PetClientList = clients;

                //for (PetClients clientScanResult : clients) {
                for (PetClients clientScanResult : clients) {
                    addNewTab(clientScanResult.getIpAddr());
                }
                //((MainActivity)getActivity()).addNewTab();

            }
        });

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
            showPairTab();
            Toast.makeText(context, R.string.closeToContinue, Toast.LENGTH_SHORT).show();
        }


        return super.onOptionsItemSelected(item);
    }


    public class MyFragmentPageAdapter extends FragmentPagerAdapter {

        public  int pos = 0;

        private List<Fragment> myFragments = new ArrayList<>();
        private ArrayList<String> categories = new ArrayList<>();
        private Context context;

        public MyFragmentPageAdapter(Context context, FragmentManager fm, List<Fragment> myFrags, ArrayList<String> cats) {
            super(fm);
            myFragments = myFrags;
            this.categories = cats;
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("Log", "getItem adapter called");

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
            Log.d("Log", "getPageTitle adapter called");
            setPos(position);
            return categories.get(position);

        }

        public int getPos() {
            return pos;
        }

        //public void add(Class<Fragment> c, String title, Bundle b) {
        /*
        public void add(Class c, String title, Bundle b) {
            Log.d("Log", "Called add");

            myFragments.add(Fragment.instantiate(context,c.getName(), b));

            categories.add(title);
        }
        */

        public void addFragment(Fragment fragment, String title) {
            myFragments.add( fragment);
            categories.add( title);
        }
        public void removeFragment(int position) {
            myFragments.remove(position);
            categories.remove(position);
        }

        public void setPos(int pos) {
            mSectionsPagerAdapter.pos = pos;
        }
    }


    void setupTabs() {

//        tabTitles.add("Emparejar");

        tabFragments = buildFragments();
        //Tabs
        //mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter = new MyFragmentPageAdapter(this ,getSupportFragmentManager(), tabFragments, tabTitles);

        if (ShowWelcome) {
            //tabTitles.add("Welcome");
            showWelcomeTab();
        }

        //mSectionsPagerAdapter.addFragment(new WelcomeFragment(), "one");
        //mSectionsPagerAdapter.addFragment(new PetSettings(), "two");
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);


    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifiUtils.disableAP();
    }

    public void addNewTab(String title) {
        mSectionsPagerAdapter.addFragment(new WelcomeFragment(), title);
        mSectionsPagerAdapter.notifyDataSetChanged();

    }

    private void showPairTab() {
        
        mSectionsPagerAdapter.addFragment(new PetSettings(), "Emparejar");
        mSectionsPagerAdapter.notifyDataSetChanged();
        mTabLayout.getTabAt(mSectionsPagerAdapter.getCount() -1).select();

    }
    private void showWelcomeTab() {
        mSectionsPagerAdapter.addFragment(new WelcomeFragment(), "Bienvenido");
        mSectionsPagerAdapter.notifyDataSetChanged();


    }
    void requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},  PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);

            // Cambiar ajustes
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);

            }

        }


    }

    static public class Conn extends ConnRest {

        public Conn(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

        }
    }

}
