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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST = 1;

    //여기선 와이파이가 꺼져있을 때 켜주기 위해
    WifiManager wifiManager;
    Button start, stop;
    ImageView record;

    Boolean isStated = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setTitle("Indoor Tracker");
        setContentView(R.layout.activity_main);


        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        record = findViewById(R.id.record);

        //와이파이가 꺼져있으면 킨다
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        //런타임 퍼미션 요청
        myPermissionCheck();


        //버튼 이벤트처리 함수
        button();
    }



    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG,"onDestroy");

        //어플 실행을 완전히 종료시키면 실행중인 서비스를 종료시킨다
        if(isStated) {
            isStated = false;
            stopService(new Intent(this, AlertService.class));
        }
    }


    public void button() {

        //스타트 버튼을 눌렀을 때의 이벤트 처리 - 서비스 시작
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isStated) {
                    Toast.makeText(getApplicationContext(), "Indoor Track 탐지 시작!!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), AlertService.class);
                    startService(intent);
                    isStated = true;
                }
                else{
                    Toast.makeText(getApplicationContext(), "이미 탐지 중...", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //스탑 버튼을 눌렀을 때의 이벤트 처리 - 서비스 중단
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isStated) {
                    Toast.makeText(getApplicationContext(), "Indoor Track 탐지 종료!!", Toast.LENGTH_SHORT).show();
                    stopService(new Intent(getApplicationContext(), AlertService.class));
                    isStated = false;
                }else{
                    Toast.makeText(getApplicationContext(), "이미 탐지 종료됨...", Toast.LENGTH_SHORT).show();
                }
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

    //앱이 실행될 때 런타임 퍼미션 체크하는 함수, 외장 파일 사용 퍼미션과 위치 정보 사용 퍼미션을 체크한다.
    public void myPermissionCheck(){
        //퍼미션 코드는 교수님의 자료를 퍼와서 수정함.
        //런타임 퍼미션 체크
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){

            // 퍼미션에 대한 설명을 해줘야하니? - 네
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)&&
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)){

            }

            //퍼미션에 대한 설명 필요없으면, 바로 권한 부여
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST);



        } else {
            //허용되었을 때
            isPermitted = true;
        }
    }

    //호출 순서 : on create -> myPermissionCheck -> requestPermissions -> onRequestPermissionsResult
    //런타임 퍼미션 얻기
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isPermitted = true;
                } else {
                    isPermitted = false;
                    finish();
                }
                return;
            }
        }
    }


}
