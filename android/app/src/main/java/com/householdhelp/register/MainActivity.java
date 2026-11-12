package com.householdhelp.register;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> chooser;
    private static final int PICK = 11, LOC = 12;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if ("appassets.androidplatform.net".equals(r.getUrl().getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC);
                cb.invoke(origin, true, false);
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try { startActivityForResult(p.createIntent(), PICK); }
                catch (Exception e) { chooser = null; return false; }
                return true;
            }
        });
        web.addJavascriptInterface(new AuthBridge(), "AndroidAuth");
        web.loadUrl("https://appassets.androidplatform.net/index.html");
    }

    // ---- Fingerprint / screen-lock unlock, called from the web app ----
    private class AuthBridge {
        @JavascriptInterface public boolean available() { return canAuth(); }
        @JavascriptInterface public void verify(final String id) { runOnUiThread(() -> startAuth(id)); }
    }

    private boolean canAuth() {
        if (Build.VERSION.SDK_INT >= 30) {
            BiometricManager bm = getSystemService(BiometricManager.class);
            return bm != null && bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
        }
        KeyguardManager km = getSystemService(KeyguardManager.class);
        return km != null && km.isDeviceSecure();
    }

    private void finishAuth(String id, String result) {
        web.evaluateJavascript("window.__authDone(" + JSONObject.quote(id) + "," + JSONObject.quote(result) + ")", null);
    }

    private void startAuth(final String id) {
        if (!canAuth()) {
            finishAuth(id, "Set up a fingerprint or screen lock in your phone's Settings first.");
            return;
        }
        BiometricPrompt.Builder b = new BiometricPrompt.Builder(this)
            .setTitle("Unlock Help Register")
            .setSubtitle("Use your fingerprint");
        if (Build.VERSION.SDK_INT >= 30) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else {
            b.setDeviceCredentialAllowed(true);
        }
        b.build().authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) { finishAuth(id, "ok"); }
            @Override public void onAuthenticationError(int code, CharSequence msg) { finishAuth(id, ""); }
        });
    }

    // Tell the web app when the app goes to the background, so "lock after" works reliably.
    @Override protected void onStop() { super.onStop(); web.evaluateJavascript("window.__appHide&&__appHide()", null); }
    @Override protected void onStart() { super.onStart(); web.evaluateJavascript("window.__appShow&&__appShow()", null); }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        } else super.onActivityResult(req, res, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
