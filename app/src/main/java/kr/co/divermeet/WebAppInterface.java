package kr.co.divermeet;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.firebase.messaging.FirebaseMessaging;

public class WebAppInterface {
    private Context mContext;
    private WebView mWebView;
    private String fcmToken;

    WebAppInterface(Context c, WebView webView) {
        mContext = c;
        mWebView = webView;
        fetchFcmToken();
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    this.fcmToken = task.getResult();
                });
    }

    /**
     * 웹에서 푸시 토큰을 요청할 때 호출됩니다.
     * @return FCM 토큰
     */
    @JavascriptInterface
    public String getPushToken() {
        return fcmToken;
    }

    /**
     * 웹에서 위치 정보 전송을 시작하도록 요청할 때 호출됩니다.
     */
    @JavascriptInterface
    public void requestLocation() {
        Intent serviceIntent = new Intent(mContext, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(serviceIntent);
        } else {
            mContext.startService(serviceIntent);
        }
    }
}