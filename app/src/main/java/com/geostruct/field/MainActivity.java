package com.geostruct.field;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
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
 * GneissTools shell.
 *
 * The whole application is app/src/main/assets/index.html, served from a real
 * secure origin (https://appassets.androidplatform.net/) through
 * WebViewAssetLoader. That is what makes the motion sensors, geolocation and
 * canvas exports work; the same files opened from file:// are rejected by the
 * WebView with NotAllowedError.
 *
 * Native services exposed to the web layer as window.AndroidExport:
 *   save(name, mime, base64)  write a file into the public Downloads folder
 *   theme("light"|"dark")     recolour the system bars to match the UI theme
 *   keepAwake(boolean)        hold the screen on during a measuring session
 *   appVersion()              versionName shown in About
 *
 * The file chooser also offers the camera, so the field map tool can store
 * photo samples taken on the spot.
 */
public class MainActivity extends Activity {

    private static final String ORIGIN = "https://appassets.androidplatform.net/assets/index.html";
    private static final int REQ_LOCATION = 42;
    private static final int REQ_FILE = 77;

    private WebView web;
    private ValueCallback<Uri[]> filePicker;
    private Uri captureUri;
    private GeolocationPermissions.Callback geoCb;
    private String geoOrigin;

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
        s.setDatabaseEnabled(true);
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
                if (hasLocation()) {
                    cb.invoke(origin, true, false);
                    return;
                }
                /* The OS permission is still missing, usually because the page asked
                   for a position while the system dialog was on screen. Hold the web
                   callback, ask for the permission and answer once it is decided. */
                geoCb = cb;
                geoOrigin = origin;
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePicker != null) filePicker.onReceiveValue(null);
                filePicker = cb;
                captureUri = null;

                Intent chooser = Intent.createChooser(params.createIntent(), "Photo sample");
                if (wantsImage(params)) {
                    Intent cam = buildCaptureIntent();
                    if (cam != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
                }
                try {
                    startActivityForResult(chooser, REQ_FILE);
                } catch (Exception e) {
                    filePicker = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidExport");
        setContentView(web);

        if (!hasLocation()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }

        web.loadUrl(ORIGIN);
    }

    private boolean wantsImage(WebChromeClient.FileChooserParams params) {
        String[] types = params.getAcceptTypes();
        if (types == null) return false;
        for (String t : types) {
            if (t != null && t.toLowerCase().startsWith("image/")) return true;
        }
        return false;
    }

    /** Camera intent that writes straight into the gallery, so the result is a readable Uri. */
    private Intent buildCaptureIntent() {
        try {
            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cam.resolveActivity(getPackageManager()) == null) return null;
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "gneisstools_" + System.currentTimeMillis() + ".jpg");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            captureUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            if (captureUri == null) return null;
            cam.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
            cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return cam;
        } catch (Exception e) {
            captureUri = null;
            return null;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE) {
            Uri[] out = null;
            if (res == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    out = new Uri[]{data.getData()};
                } else if (captureUri != null) {
                    out = new Uri[]{captureUri};
                }
            }
            if (out == null && captureUri != null) {
                try {
                    getContentResolver().delete(captureUri, null, null);
                } catch (Exception ignored) {
                }
            }
            captureUri = null;
            if (filePicker != null) filePicker.onReceiveValue(out);
            filePicker = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** The page starts its watch while the permission dialog is still up, so it
     *  has to be told when the permission finally arrives. */
    private void tellWebGpsReady() {
        if (web == null) return;
        web.evaluateJavascript("window.gpsPermissionGranted && window.gpsPermissionGranted()", null);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_LOCATION) {
            boolean ok = false;
            for (int r : res) if (r == PackageManager.PERMISSION_GRANTED) ok = true;
            if (geoCb != null) {
                geoCb.invoke(geoOrigin, ok, false);
                geoCb = null;
                geoOrigin = null;
            }
            if (ok) tellWebGpsReady();
            else toast("Location refused: the field map cannot show your position");
            return;
        }
        super.onRequestPermissionsResult(req, perms, res);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocation()) tellWebGpsReady();
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
                    if (os == null) throw new IllegalStateException("no output stream");
                    os.write(data);
                    os.close();
                    cv.clear();
                    cv.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(uri, cv, null, null);
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("no Downloads folder");
                    FileOutputStream fos = new FileOutputStream(new File(dir, name));
                    fos.write(data);
                    fos.close();
                }
                toast("Saved to Downloads: " + name);
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
                    Window w = getWindow();
                    int c = dark ? 0xFF000000 : 0xFFFFFFFF;
                    w.setStatusBarColor(c);
                    w.setNavigationBarColor(c);
                    View d = w.getDecorView();
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
                PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
                return p.versionName + " (build " + p.versionCode + ")";
            } catch (Exception e) {
                return "unknown";
            }
        }
    }
}
