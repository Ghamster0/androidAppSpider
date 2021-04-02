package com.hamster.androidappspider;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

import okhttp3.OkHttpClient;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TestTest {
    private UiDevice mDevice;
    private int height;
    private int width;

    @Before
    public void startMainActivityFromHomeScreen() throws InterruptedException {
        mDevice = UiDevice.getInstance(getInstrumentation());
        height = mDevice.getDisplayHeight();
        width = mDevice.getDisplayWidth();
    }

    @Test
    public void run() {
//        try {
//            logMac();
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//        throw new RuntimeException("over");
        scrollFirstPage();
    }

    @After
    public void logDone() {
        Log.e("Mac", "Yeah Done!");
    }

    private void scrollFirstPage() {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int startR = height / 16 - random.nextInt(height / 8);
            int endR = height / 16 - random.nextInt(height / 8);
            mDevice.swipe(width / 2, height / 4 * 3 + startR, width / 2, height / 4 + endR, 6 + random.nextInt(4));
            SystemClock.sleep(2000);
        }
    }

    private void logMac() throws SocketException {
        if (Build.VERSION.SDK_INT >= 23) {
            Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) enumeration.nextElement();
                //获取硬件地址，一般是MAC
                byte[] arrayOfByte = networkInterface.getHardwareAddress();
                if (arrayOfByte == null || arrayOfByte.length == 0) {
                    continue;
                }

                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : arrayOfByte) {
                    //格式化为：两位十六进制加冒号的格式，若是不足两位，补0
                    stringBuilder.append(String.format("%02X:", new Object[]{Byte.valueOf(b)}));
                }
                if (stringBuilder.length() > 0) {
                    //删除后面多余的冒号
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                String str = stringBuilder.toString();
                Log.e("Mac", networkInterface.getName() + " " + str);
            }
        } else {
            WifiManager wifiManager = (WifiManager) getInstrumentation().getContext().getSystemService("wifi");
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null)
                Log.e("Mac", wifiInfo.getMacAddress());
        }
    }
}
