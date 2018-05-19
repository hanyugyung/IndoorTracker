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

    //String placeName;

    String topBSSID;
    String topSSID;
    int topRssi;

    Timer timer;
    TimerTask timerTask = null;

    long now;
    Date date;
    SimpleDateFormat sdfNow;
    String currentTime;


    //사전 조사 결과 미리 등록된 장소와 장소에서 가장 센 AP
    //장소의 이름과 ap 값
    String []placeArr = {"4층 엘레베이터 앞","408호 계단 앞","401호 계단 앞"};
    String []wifiArr = {"50:0f:80:b2:51:62","40:01:7a:de:11:62","18:80:90:c6:7b:22"};
    int []wifiBolume = {-48,-49,-55};

    Vibrator vib;
    TextFileManager textFileManager = new TextFileManager();

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //인텐트 필터에 추가해준 필터, 와이파이매니저가 스캔 결과가 있다고 하면 if문을 실행한다.
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanList = wifiManager.getScanResults();
                getWifiInfo();
                checkProximity();
            }
        }
    };

    // RSSI 값이 가장 큰 AP 정보를 얻기 위한 메소드
    private void getWifiInfo() {


        Log.d(TAG,"스캔결과");
        for(int i=0;i<scanList.size();i++){
            Log.d("TAG",scanList.get(i).BSSID);
        }

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
        now = System.currentTimeMillis();
        date = new Date(now);
        sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        currentTime = sdfNow.format(date);

        //원래의 장소를 unknown 이라 저장해놓는다.
        String currentPlace = "unknown";

        // 미리 등록된 ap와 가장 센 ap를 비교함.
        // 미리 등록된 ap 3개의 목록과 가장 센 ap의 BSSID가 일치하고, 그 세기의 차이가 절댓값으로 13미만이면
        // 그 장소에 있다고 판단한다.
        // 여기서 둘 중 하나라도 아니라면 그 장소는 unknown 장소로 여긴다.
        for(int i = 0; i < 3; i++) {
            if(topBSSID.equals(wifiArr[i]) && Math.abs(topRssi-wifiBolume[i]) < 13) {
                //isProximate = true;
                currentPlace = placeArr[i];
            }
        }

        //진동 알리미
        vib.vibrate(1000);

        Toast.makeText(this, currentTime+" "+currentPlace, Toast.LENGTH_SHORT).show();
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

        //함수가 호출될 때마다 시각을 한번 체크하므로, 메인액티비티에 button 함수 내부에 넣으면
        //딱 한번 처음 정해진 시각만 쓰여진다. 그때 그때의 시각을 나타내고 싶으면 서비스가 시작될 때마다
        //한번씩 호출되는 onCreate 에 있어야 하고, 마찬가지로, 종료시각도 딱 그 시각의 정보를 나타내고 싶다면
        //서비스가 중단될 때마다 한번씩 호출되는 onDestroy 함수 내부에 있어야 한다.
        now = System.currentTimeMillis();
        date = new Date(now);
        sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        currentTime = sdfNow.format(date);
        textFileManager.save("모니터링 시작 - "+currentTime+"\n");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand()");
        timer = new Timer();
        // 주기적으로 wifi scan 수행하기 위한 timer 가동
        startTimerTask();

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
//        Toast.makeText(this, "AlertService 중지", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDestroy()");

        stopTimerTask();
        unregisterReceiver(mReceiver);

        now = System.currentTimeMillis();
        date = new Date(now);
        sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        currentTime = sdfNow.format(date);
        textFileManager.save("모니터링 종료 - "+currentTime+"\n");
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
        // 1분 = 60s = 60000ms
        // 그렇지만 정확히 1분 간격으로 실행되지는 않는데, 그 이유가 디바이스 탓인지 코드 상의 문제인지 모르겠다.
        // 그러나, 코드 상의 문제이면 1분 간격이 아니더라도, 정확한 어떠한 간격으로 실행되어야 하는데
        // 때마다 다른 점을 보면 디바이스 문제인 것 같다.
        // delay 5ms 는 간격을 조금이라도 1분에 맞춰보기 위해 설정해줬지만, 원래는 딜레이가 0인게 맞는 것 같다.
        timer.schedule(timerTask, 5, 60000);
    }

    private void stopTimerTask() {
        // 1. 모든 태스크를 중단한다
        if(timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
