package com.geostruct.field;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * GeoTools shell.
 *
 * The whole field application lives in app/src/main/assets and is served over a
 * real secure origin (https://appassets.androidplatform.net/) through
 * WebViewAssetLoader. That is what makes the motion sensors, geolocation and
 * canvas exports work; the same files opened from file:// are rejected by the
 * WebView with NotAllowedError.
 *
 * Native services exposed to the web layer as window.AndroidExport:
 *   save(name, mime, base64)  write a file into the public Downloads folder
 *   theme("light"|"dark")     recolour the system bars to match the UI theme
 *   keepAwake(boolean)        hold the screen on during a measuring session
 *   appVersion()              versionName shown in Settings
 */
public class MainActivity extends Activity {

    private static final String ORIGIN = "https://appassets.androidplatform.net/assets/index.html";
    private static final int REQ_LOCATION = 42;
    private static final int REQ_FILE = 77;

    private WebView web;
    private ValueCallback<Uri[]> filePicker;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePicker != null) filePicker.onReceiveValue(null);
                filePicker = cb;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    filePicker = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidExport");
        setContentView(web);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }

        web.loadUrl(ORIGIN);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE) {
            Uri[] out = null;
            if (res == RESULT_OK && data != null && data.getData() != null) {
                out = new Uri[]{data.getData()};
            }
            if (filePicker != null) filePicker.onReceiveValue(out);
            filePicker = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    /** Hardware back button is routed to the web router first. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.GT && GT.onBack) ? GT.onBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String handled) {
                if (!"true".equals(handled)) finish();
            }
        });
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class Bridge {

        @JavascriptInterface
        public void save(final String name, final String mime, final String base64) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.IS_PENDING, 1);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) throw new IllegalStateException("MediaStore refused the file");
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(data);
                    os.flush();
                    os.close();
                    cv.clear();
                    cv.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(uri, cv, null, null);
                } else {
                    File dir = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    FileOutputStream fo = new FileOutputStream(new File(dir, name));
                    fo.write(data);
                    fo.flush();
                    fo.close();
                }
                toast("Saved in Downloads: " + name);
            } catch (Exception e) {
                toast("Export failed: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void theme(final String mode) {
            final boolean dark = "dark".equals(mode);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int bg = dark ? 0xFF12171A : 0xFFFFFFFF;
                    getWindow().setStatusBarColor(bg);
                    getWindow().setNavigationBarColor(bg);
                    View d = getWindow().getDecorView();
                    int f = d.getSystemUiVisibility();
                    int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    d.setSystemUiVisibility(dark ? (f & ~light) : (f | light));
                }
            });
        }

        @JavascriptInterface
        public void keepAwake(final boolean on) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }

        @JavascriptInterface
        public String appVersion() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "?";
            }
        }
    }
}
