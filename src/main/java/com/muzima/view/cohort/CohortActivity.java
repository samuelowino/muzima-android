package com.muzima.view.cohort;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPagerAdapter;
import com.muzima.service.DataSyncService;
import com.muzima.utils.Fonts;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.customViews.PagerSlidingTabStrip;
import com.muzima.view.patients.MuzimaFragmentActivity;
import com.muzima.view.preferences.SettingsActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_COHORTS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_OBSERVATIONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_PATIENTS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_STATUS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR;

public class CohortActivity extends MuzimaFragmentActivity {
    private static final String TAG = "CohortActivity";
    private ViewPager viewPager;
    private CohortPagerAdapter cohortPagerAdapter;
    private PagerSlidingTabStrip pagerTabsLayout;
    private MenuItem menubarLoadButton;
    private boolean syncInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        initPager();
        initPagerIndicator();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.cohort_list_menu, menu);
        menubarLoadButton = menu.findItem(R.id.menu_load);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if(syncInProgress){
                    Toast.makeText(this, "Already fetching cohorts, ignored the request", Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncCohortsInBackgroundService();
                return true;

            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(SYNC_STATUS, UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(SYNC_TYPE, -1);

        if(syncType == SYNC_COHORTS){
            hideProgressbar();
            syncInProgress = false;
            if(syncStatus == SUCCESS){
                cohortPagerAdapter.onCohortDownloadFinish();
            }
        }else if(syncType == SYNC_PATIENTS){
            if(syncStatus == SUCCESS){
                cohortPagerAdapter.onPatientsDownloadFinish();
            }
        }else if(syncType == SYNC_OBSERVATIONS){
            hideProgressbar();
        }
    }


    public void hideProgressbar() {
        menubarLoadButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarLoadButton.setActionView(R.layout.refresh_menuitem);
    }

    private void initPager() {
        viewPager = (ViewPager) findViewById(R.id.pager);
        cohortPagerAdapter = new CohortPagerAdapter(getApplicationContext(), getSupportFragmentManager());
        cohortPagerAdapter.initPagerViews();
        viewPager.setAdapter(cohortPagerAdapter);
    }

    private void initPagerIndicator() {
        pagerTabsLayout = (PagerSlidingTabStrip) findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(viewPager);
        viewPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
    }

    private void syncCohortsInBackgroundService() {
        Intent intent = new Intent(this, DataSyncService.class);
        intent.putExtra(SYNC_TYPE, SYNC_COHORTS);
        intent.putExtra(CREDENTIALS, getCredentials());
        syncInProgress = true;
        cohortPagerAdapter.onCohortDownloadStart();
        showProgressBar();
        startService(intent);
    }
}
