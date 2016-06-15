package ru.start_car.newrlock.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import ru.start_car.newrlock.R;
import ru.start_car.newrlock.common.aids.EventHandler;
import ru.start_car.newrlock.ui.fragments.CarLocationFragment;
import ru.start_car.newrlock.ui.fragments.GeneralFragment;
import ru.start_car.newrlock.ui.fragments.InfoFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_SIGNUP = 0;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private static int AuthState = 0;
    private static final Handler handler = new Handler();

    /**
     * Callback on disconnect event happened
     */
    private class DisconnectedEvent implements EventHandler {
        @Override
        public void invoke(final Object arg) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(getApplicationContext(), "Tost", Toast.LENGTH_SHORT);
                    } catch (Exception e) {
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Activity onCreate");
        setContentView(R.layout.activity_main);


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(4);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        if (AuthState == 0) restartAuth();
        else {
            ConnectionSingletone.getInstance().setDisconnectedEventHandler(new DisconnectedEvent());
        }

    }


    private void restartAuth() {
        AuthState = 0;
        ConnectionSingletone.getInstance().stop();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_SIGNUP);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_home_page:
                mPager.setCurrentItem(0);
                break;
            case R.id.nav_map_page:
                mPager.setCurrentItem(1);
                break;
            case R.id.nav_car_info:
                mPager.setCurrentItem(2);
                break;
            case R.id.nav_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_log_out:
                restartAuth();
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new GeneralFragment();
                    break;
                case 1:
                    fragment = new CarLocationFragment();
                    break;
                case 2:
                    fragment= new InfoFragment();
                    break;
                default:
            }
            return fragment;
        }


        @Override
        public int getCount() {
            return 3;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_SIGNUP) && (resultCode == RESULT_OK)) {
            AuthState = 1;
            mPager.setCurrentItem(0);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
