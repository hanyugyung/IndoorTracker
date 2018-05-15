package com.example.hanyugyeong.test05;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;


    String topAPId;         //가장 센 ap의 이름과 주소
    int topRssi = -100;     //가장 센 ap의 신호 크기

    WifiManager wifiManager;
    List<ScanResult> scanResultList;    //스캔된 ap 리스트

    Button start, stop, record;

    //사전 조사 결과 미리 등록된 장소와 장소에서 가장 센 AP
    //장소의 이름과 ap 값
    String []placeArr = {"4층 엘레베이터 앞","408호 계단 앞","401호 계단 앞"};
    String []wifiArr = {"50:0f:80:b2:51:61","64:e5:99:23:f6:cc","40:01:7a:de:11:32"};



    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                getWifiInfo();
        }
    };

    // RSSI 값이 가장 큰 AP 정보를 얻기 위한 메소드
    private void getWifiInfo() {
        scanResultList = wifiManager.getScanResults();
        String topBSSID;
        String topSSID;

        // 신호 세기가 가장 센 AP의 정보를 저장하기 위한 변수를 scanResultList의 첫번째 결과로 초기화
        // top1rssi: AP의 RSSI 값, top1BSSID: AP의 BSSID 값, top1SSID: AP의 SSID 값
        topRssi = scanResultList.get(0).level;
        topBSSID = scanResultList.get(0).BSSID;
        topSSID = scanResultList.get(0).SSID;

        // RSSI 크기가 가장 큰 것의 BSSID, SSID, RSSI 값을 얻음
        for(int i = 1; i < scanResultList.size(); i++) {
            ScanResult result = scanResultList.get(i);
            if (topRssi <= result.level) {
                topRssi = result.level;
                topBSSID = result.BSSID;
                topSSID = result.SSID;
            }
        }
        // 화면의 TextView에 SSID와 BSSID를 이어붙여서 텍스트로 표시
        topAPId = topSSID +" "+ topBSSID;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setTitle("Indoor Tracker");
        setContentView(R.layout.activity_main);


        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);

        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        //런타임 퍼미션 요청
        requestRuntimePermission();

        //버튼 이벤트처리 함수
        button();

    }

    @Override
    protected void onStart(){
        super.onStart();
        // wifi scan 결과 수신을 위한 BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // wifi scan 결과 수신용 BroadcastReceiver 등록 해제
        unregisterReceiver(mReceiver);
    }


    public void button() {
        //스타트 버튼을 눌렀을 때의 이벤트 처리 - 서비스 시작
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Indoor Track 탐지 시작!!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), AlertService.class);
                intent.putExtra("AP", topAPId);
                intent.putExtra("RSSI", topRssi);
                startService(intent);
            }
        });
        //스탑 버튼을 눌렀을 때의 이벤트 처리 - 서비스 중단
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Indoor Track 탐지 중단!!", Toast.LENGTH_SHORT).show();
                stopService(new Intent(getApplicationContext(), AlertService.class));
            }
        });

        //외부 파일로 저장된 기록을 확인할 수 있는 액티비티로 이동
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RecordedActivity.class);
                startActivity(intent);
            }
        });
    }

    private void requestRuntimePermission() {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            // ACCESS_FINE_LOCATION 권한이 있는 것
            isPermitted = true;
        }
        //*********************************************************************
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                    isPermitted = false;

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
