package com.example.hanyugyeong.test05;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AlertService extends Service {
    private static final String TAG = "AlertService";

    WifiManager wifiManager;
    List<ScanResult> scanList;

    String placeName;

    String topBSSID;
    String topSSID;
    int topRssi = -100;

    Timer timer = new Timer();
    TimerTask timerTask = null;

    //사전 조사 결과 미리 등록된 장소와 장소에서 가장 센 AP
    //장소의 이름과 ap 값
    String []placeArr = {"4층 엘레베이터 앞","408호 계단 앞","401호 계단 앞"};
    String []wifiArr = {"50:0f:80:b2:51:61","64:e5:99:23:f6:cc","40:01:7a:de:11:32"};
    int []wifiBolume = {-81,-70,-60};

    Vibrator vib;
    TextFileManager textFileManager;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWifiInfo();
                checkProximity();
            }
        }
    };

    // RSSI 값이 가장 큰 AP 정보를 얻기 위한 메소드
    private void getWifiInfo() {
        scanList = wifiManager.getScanResults();


        // 신호 세기가 가장 센 AP의 정보를 저장하기 위한 변수를 scanResultList의 첫번째 결과로 초기화
        // top1rssi: AP의 RSSI 값, top1BSSID: AP의 BSSID 값, top1SSID: AP의 SSID 값
        topRssi = scanList.get(0).level;
        topBSSID = scanList.get(0).BSSID;
        topSSID = scanList.get(0).SSID;

        // RSSI 크기가 가장 큰 것의 BSSID, SSID, RSSI 값을 얻음
        for(int i = 1; i < scanList.size(); i++) {
            ScanResult result = scanList.get(i);
            if (topRssi <= result.level) {
                topRssi = result.level;
                topBSSID = result.BSSID;
                topSSID = result.SSID;
            }
        }


    }

    // 등록 장소 근접 여부를 판단하고 그에 따라 알림을 주기 위한 메소드
    private void checkProximity() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String currentTime = sdfNow.format(date);

        String currentPlace = "unknown";

        boolean isProximate = false;
        // 미리 등록된 ap와 가장 센 ap를 비교함.
        // 등록된 장소 근처에 있는 것으로 판단
        for(int i = 0; i < 3; i++) {
            if(topSSID.equals(placeArr[i])){
                if(topBSSID.equals(wifiArr[i]) && topRssi-wifiBolume[i] < 10){
                    isProximate = true;
                    currentPlace = placeArr[i];
                }
            }
        }

        if(isProximate) {
            // 진동 패턴
            // 0초 후에 시작 => 바로 시작, 200ms 동안 진동, 100ms 동안 쉼, 200ms 동안 진동, 100ms 동안 쉼, 200ms 동안 진동
            long[] pattern = {0, 200, 100, 200, 100, 200};
            // pattern 변수로 지정된 방식으로 진동한다, -1: 반복 없음. 한번의 진동 패턴 수행 후 완료
            vib.vibrate(pattern, -1);
            Toast.makeText(this, currentTime+" "+currentPlace, Toast.LENGTH_SHORT).show();

        } else {
            // 동작 확인용
            vib.vibrate(200);
            Toast.makeText(this, currentTime+" "+currentPlace, Toast.LENGTH_SHORT).show();
        }
        //외부 파일에 쓰기
        textFileManager.save(currentTime+" "+currentPlace+"\n");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 주기적으로 wifi scan 수행하기 위한 timer 가동
        startTimerTask();

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
//        Toast.makeText(this, "AlertService 중지", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDestroy()");

        stopTimerTask();
        unregisterReceiver(mReceiver);
    }

    private void startTimerTask() {
        // TimerTask 생성한다
        timerTask = new TimerTask() {
            @Override
            public void run() {
                wifiManager.startScan();
            }
        };

        // TimerTask를 Timer를 통해 실행시킨다
        // 1초 후에 타이머를 구동하고 1분마다 반복한다
        // 1초 = 1000ms
        // 1분 = 3600초 = 3600000ms
        timer.schedule(timerTask, 1000, 3600000);
    }

    private void stopTimerTask() {
        // 1. 모든 태스크를 중단한다
        if(timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
