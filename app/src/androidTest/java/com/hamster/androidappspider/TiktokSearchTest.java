package com.hamster.androidappspider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TiktokSearchTest {
    private static final String LOG_TAG = "TiktokSearchTest";

    private static final String TARGET_PACKAGE = "com.ss.android.ugc.aweme";
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int LAUNCH_TIMEOUT = 5000;

    private UiDevice mDevice;
    private int height;
    private int width;

    private OkHttpClient client = new OkHttpClient();
    private static final String KEY_SERVER = "http://10.143.15.226:4396";
    private Random random = new Random();


    @Before
    public void startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(getInstrumentation());
        height = mDevice.getDisplayHeight();
        width = mDevice.getDisplayWidth();

        mDevice.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        launchApp();
        launchAppFirstTime();
    }

    /**
     * 输入查询词，点击搜索并滚屏
     * <p>
     * Resource Id表：
     * dy3 呼出搜索按钮
     * aia 搜索框
     * kig 取消搜索按钮/搜索按钮
     * qw 清空搜索框
     */
    @Test
    public void walkAround() {
        String key = null;
        int failCount = 0;
        int hangoutCountdown = 2;
        while (true) {
            if (failCount == 0) {
                key = getSearchKey();
            }
            if (key == null) {
                break;
            }
            boolean isSucceed = doSearch(key);
            if (!isSucceed) {
                failCount++;
                if (failCount >= 3) {
                    throw new RuntimeException("DeviceScrapped");
                }
                restartApp();
            }
            failCount = 0;
            hangoutCountdown--;
            if (hangoutCountdown <= 0) {
                doHangOut();
                hangoutCountdown = 1 + random.nextInt(1);
            }
        }
    }

    private boolean doSearch(String key) {
        UiObject2 searchInput = mDevice.findObject(By.res(TARGET_PACKAGE, "aia"));
        if (searchInput == null) {
            mDevice.wait(Until.findObject(By.res(TARGET_PACKAGE, "dy3")), DEFAULT_TIMEOUT).click();
            searchInput = mDevice.wait(Until.findObject(By.res(TARGET_PACKAGE, "aia")), DEFAULT_TIMEOUT);
        }
        UiObject2 searchInputClearBtn = mDevice.findObject(By.res(TARGET_PACKAGE, "qw"));
        if (searchInputClearBtn != null) {
            searchInputClearBtn.click();
            SystemClock.sleep(100 + random.nextInt(400));
        }
        searchInput.click();
        searchInput.setText(key);
        SystemClock.sleep(100 + random.nextInt(100));
        mDevice.findObject(By.res(TARGET_PACKAGE, "kig")).click();

        // select scrollable video node that has valid content/child
        mDevice.wait(Until.hasObject(By.res(TARGET_PACKAGE, "exk")), 10000);
        UiObject2 searchResult = mDevice.findObject(By.res(TARGET_PACKAGE, "exk").hasChild(By.pkg(TARGET_PACKAGE)));
        if (searchResult != null) {
            for (int i = 0; i < 3; i++) {
                searchResult.scroll(Direction.DOWN, 2 + random.nextFloat() * 2, 800 + random.nextInt(400) * 160);
                SystemClock.sleep(100 + random.nextInt(200));
            }
            return true;
        } else {
            return false;
        }
    }

    private void doHangOut() {
        for (int i = 0; i < 3; i++) {
            UiObject2 goSearchBtn = mDevice.findObject(By.res(TARGET_PACKAGE, "dy3"));
            if (goSearchBtn != null) {
                break;
            }
            SystemClock.sleep(500);
            UiObject2 cancelSearchBtn = mDevice.findObject(By.res(TARGET_PACKAGE, "kig"));
            if (cancelSearchBtn != null) {
                cancelSearchBtn.click();
            }
        }
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            scrollRecommend();
            SystemClock.sleep(1000 + random.nextInt(1000));
        }
    }

    @After
    public void cleanUp() {
        forceCloseApp();
    }

    private String getSearchKey() {
        Request request = new Request.Builder().url(KEY_SERVER).build();
        try {
            Response response = client.newCall(request).execute();
            if (response.code() != 200) {
                return null;
            }
            return response.body().string();
        } catch (IOException e) {
        }
        return null;
    }

    private void launchApp() {
        Context context = getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(TARGET_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    /**
     * 处理应用首次启动时，权限确认弹窗
     * bco 个人信息保护指引
     * kls 上滑查看更多视频
     * eeh 儿童、青少年使用须知
     * ejw 检测更新，以后再说
     * com.android.packageinstaller:id/permission_allow_button 系统权限申请弹窗
     */
    private void launchAppFirstTime() {
        BySelector[] selectors = {
                By.res(TARGET_PACKAGE, "l__"),
                By.res(TARGET_PACKAGE, "bco"),
                By.res(TARGET_PACKAGE, "eeh"),
//                By.res(TARGET_PACKAGE, "kls"),
                By.res(TARGET_PACKAGE, "ejw"),
                By.res("com.android.packageinstaller", "permission_allow_button")
        };
        // 等待视频或弹窗，标志启动完成
        Log.i(LOG_TAG, "detect app boot finish: " + waitOne(selectors, 60000, 200, 0));
        // 如果有视频，暂停视频来提速
        pauseRecommendVideo();

        BySelector[] popUpSelectors = Arrays.copyOfRange(selectors, 1, selectors.length);
        while (waitOne(popUpSelectors, 3000, 200, 500)) {
            Log.i(LOG_TAG, "found popup");
            UiObject2 uiObject2;
            uiObject2 = mDevice.findObject(By.res(TARGET_PACKAGE, "bco"));
            if (uiObject2 != null) {
                uiObject2.click();
                continue;
            }
            uiObject2 = mDevice.findObject(By.res(TARGET_PACKAGE, "eeh"));
            if (uiObject2 != null) {
                uiObject2.click();
                continue;
            }
            uiObject2 = mDevice.findObject(By.res(TARGET_PACKAGE, "ejw"));
            if (uiObject2 != null) {
                uiObject2.click();
                continue;
            }
            uiObject2 = mDevice.findObject(By.res("com.android.packageinstaller", "permission_allow_button"));
            if (uiObject2 != null) {
                uiObject2.click();
                continue;
            }
        }
        // always swipe up
        scrollRecommend();
        SystemClock.sleep(1000);
        scrollRecommend();
    }

    private void restartApp() {
        forceCloseApp();
        SystemClock.sleep(1000);
        launchApp();
        launchAppFirstTime();
    }

    private void forceCloseApp() {
        try {
            mDevice.executeShellCommand("am force-stop " + TARGET_PACKAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseRecommendVideo() {
        if (!mDevice.hasObject(By.res(TARGET_PACKAGE, "fee").hasChild(By.clazz("android.widget.ImageView")))) {
            mDevice.click(width / 2, height / 2);
        }
    }

    private void scrollRecommend() {
        int startR = height / 16 - random.nextInt(height / 8);
        int endR = height / 16 - random.nextInt(height / 8);
        mDevice.swipe(width / 2, height / 4 * 3 + startR, width / 2, height / 4 + endR, 6 + random.nextInt(4));
    }

    /**
     * Uses package manager to find the package name of the device launcher. Usually this package
     * is "com.android.launcher" but can be different at times. This is a generic solution which
     * works on all platforms.`
     */
    private String getLauncherPackageName() {
        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Use PackageManager to get the launcher package name
        PackageManager pm = getApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    private boolean waitOne(BySelector[] selectors, long timeout, long interval, long minWait) {
        long startTime = SystemClock.uptimeMillis();

        boolean hasOne = false;
        for (long elapsedTime = 0; !hasOne; elapsedTime = SystemClock.uptimeMillis() - startTime) {
            if (elapsedTime >= timeout) {
                break;
            }

            SystemClock.sleep(interval);
            for (int i = 0; i < selectors.length; i++) {
                if (mDevice.hasObject(selectors[i])) {
                    hasOne = true;
                    break;
                }
            }
        }

        long cost = SystemClock.uptimeMillis() - startTime;
        if (cost < minWait) {
            SystemClock.sleep(minWait - cost);
        }
        return hasOne;
    }
}
