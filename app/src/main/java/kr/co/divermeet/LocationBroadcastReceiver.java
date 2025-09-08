package kr.co.divermeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "LocationReceiver";
    private WebView webView;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public LocationBroadcastReceiver(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "location-update".equals(intent.getAction())) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);

            executorService.execute(() -> {
                String addressString = "주소를 찾을 수 없습니다.";
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address fetchedAddress = addresses.get(0);
                        addressString = fetchedAddress.getAddressLine(0);
                        if (addressString == null || addressString.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            if (fetchedAddress.getLocality() != null) sb.append(fetchedAddress.getLocality()).append(" ");
                            if (fetchedAddress.getThoroughfare() != null) sb.append(fetchedAddress.getThoroughfare());
                            // 추가적인 주소 필드를 조합할 수 있습니다.
                            addressString = sb.toString().trim();
                             if (addressString.isEmpty()) {
                                addressString = "주소 정보를 가져오지 못했습니다.";
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder IOException: " + e.getMessage(), e);
                    addressString = "주소 변환 중 오류 발생";
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Geocoder IllegalArgumentException: " + e.getMessage() + ". Lat: " + latitude + ", Lng: " + longitude, e);
                    addressString = "잘못된 좌표값입니다.";
                } catch (Exception e) {
                    Log.e(TAG, "An unexpected error occurred during geocoding: " + e.getMessage(), e);
                    addressString = "알 수 없는 오류로 주소 변환 실패";
                }

                final String finalAddressString = addressString;
                mainThreadHandler.post(() -> {
                    if (webView != null) {
                        // JavaScript 문자열 내 특수문자(특히 따옴표) 문제를 방지하기 위해 이스케이프 처리
                        String escapedAddress = finalAddressString.replace("\'", "\\\'").replace("\"", "\\\"");
                        String script = String.format(Locale.US, "javascript:window.__receiveLocationDetailsFromAndroid(%f, %f, '%s');", latitude, longitude, escapedAddress);
                        webView.evaluateJavascript(script, null);
                    }
                });
            });
        }
    }
}
