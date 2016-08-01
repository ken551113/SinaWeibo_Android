/*
 * Copyright (c) 2016 Nick Guo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nick.dev.sina.app.content;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseIntArray;

import com.nick.scalpel.Scalpel;
import com.nick.scalpel.annotation.binding.FindView;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

import java.util.ArrayList;
import java.util.List;

import dev.nick.logger.Logger;
import nick.dev.sina.R;
import nick.dev.sina.app.annotation.RetrieveLogger;
import nick.dev.sina.app.widget.ColorUtils;
import nick.dev.sina.sdk.AuthHelper;

public class NavigatorActivity extends AppCompatActivity implements TransactionManager {

    final List<TransactionListener> transactionListeners = new ArrayList<>();

    BottomBar mBottomBar;

    @FindView(id = R.id.toolbar)
    Toolbar mToolbar;

    @FindView(id = R.id.coordinator)
    CoordinatorLayout mCoordinator;

    int[] mColors = new int[]{
            R.color.tab_1,
            R.color.tab_2,
            R.color.tab_3,
            R.color.tab_4,
            R.color.tab_5,
    };

    FragmentController mController;

    SparseIntArray idMap = new SparseIntArray();

    @RetrieveLogger
    Logger mLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.sessionValid(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_navigator);

        Scalpel.getInstance().wire(this);

        setSupportActionBar(mToolbar);

        initUI(savedInstanceState);
    }

    void initUI(Bundle savedInstanceState) {

        initPages();

        mBottomBar = BottomBar.attachShy(mCoordinator, findViewById(R.id.container), savedInstanceState);
        mBottomBar.noTopOffset();
        mBottomBar.setItems(R.menu.navigator);
        mBottomBar.setOnMenuTabClickListener(new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                TransactionSafeFragment from = mController.getCurrent();
                int toId = idMap.get(menuItemId);
                mController.setCurrent(toId);
                TransactionSafeFragment to = mController.getCurrent();

                synchronized (transactionListeners) {
                    for (TransactionListener listener : transactionListeners) {
                        listener.onFragmentTransaction(from, to);
                    }
                }

                int themedColor = ContextCompat.getColor(NavigatorActivity.this, mColors[idMap.get(menuItemId)]);
                mToolbar.setBackgroundColor(themedColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(ColorUtils.colorBurn(themedColor));
                }
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {

            }
        });

        mapColorForTab(R.id.nav_status, 0);
        mapColorForTab(R.id.nav_message, 1);
        mapColorForTab(R.id.nav_hot, 2);
        mapColorForTab(R.id.nav_me, 3);
        mapColorForTab(R.id.nav_config, 4);
    }

    void mapColorForTab(int id, int index) {
        idMap.put(id, index);
        mBottomBar.mapColorForTab(index, ContextCompat.getColor(this, mColors[index]));
    }

    private void initPages() {
        List<TransactionSafeFragment> fragments = new ArrayList<>(4);
        fragments.add(new StatusFragment());
        fragments.add(new StatusFragment());
        fragments.add(new StatusFragment());
        fragments.add(new StatusFragment());
        fragments.add(new StatusFragment());
        mController = new FragmentController(getSupportFragmentManager(), fragments);
    }


    @Override
    public void registerTransactionListener(TransactionListener listener) {
        mLogger.debug(listener);
        synchronized (transactionListeners) {
            transactionListeners.add(listener);
        }
    }

    @Override
    public void unRegisterTransactionListener(TransactionListener listener) {
        mLogger.debug(listener);
        synchronized (transactionListeners) {
            transactionListeners.remove(listener);
        }
    }
}
