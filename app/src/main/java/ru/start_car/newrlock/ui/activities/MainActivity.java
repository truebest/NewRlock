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
import android.view.MenuItem;
import android.widget.Toast;

import ru.start_car.newrlock.R;
import ru.start_car.newrlock.common.aids.EventHandler;
import ru.start_car.newrlock.ui.fragments.CarLocationFragment;
import ru.start_car.newrlock.ui.fragments.GeneralFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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
                        Toast.makeText(getApplicationContext(),"Tost",Toast.LENGTH_SHORT);
                    } catch(Exception e) { }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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


        if (AuthState == 0) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, REQUEST_SIGNUP);
        }
        else {
            ConnectionSingletone.getInstance().setDisconnectedEventHandler(new DisconnectedEvent());
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_home_page:
                mPager.setCurrentItem(0);
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
                default:
            }
            return fragment;
        }


        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_SIGNUP) && (resultCode == RESULT_OK)) {
            AuthState = 1;
        }
    }
}
