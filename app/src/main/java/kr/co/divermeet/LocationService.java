package kr.co.divermeet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData; // LiveData를 위해 추가

// LocalBroadcastManager import 삭제

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String CHANNEL_ID = "LocationServiceChannel";

    // 위치 업데이트를 위한 public static LiveData
    public static final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // LiveData에 위치 정보 게시
                    locationLiveData.postValue(location);

                    // LocalBroadcastManager 관련 코드 삭제:
                    // Intent intent = new Intent("location-update");
                    // intent.putExtra("latitude", location.getLatitude());
                    // intent.putExtra("longitude", location.getLongitude());
                    // LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Divermeet 실행 중")
                .setContentText("현재 위치를 확인 및 전송 중입니다.")
                .setSmallIcon(R.mipmap.ic_launcher) // TODO: 적절한 아이콘으로 변경
                .build();

        startForeground(1, notification);
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10초 간격
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000) // 최소 5초 간격
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: 권한이 없을 경우, 서비스를 중단하는 것 외에 LiveData 등을 통해 오류 상태를 UI에 알리는 것을 고려
            stopSelf();
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 이 서비스는 바인딩을 사용하지 않습니다.
    }
}