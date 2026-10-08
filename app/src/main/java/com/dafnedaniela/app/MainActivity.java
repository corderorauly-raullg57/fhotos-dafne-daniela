package com.dafnedaniela.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/** Generado por APK Studio. Envuelve tu web en una app Android nativa. */
public class MainActivity extends Activity {
    private static final String START_URL = "file:///android_asset/www/index.html";
    private static final boolean URL_MODE = false;
    private static final boolean FULLSCREEN = false;
    private static final boolean KEEP_SCREEN_ON = true;
    private static final boolean EXTERNAL_LINKS_IN_BROWSER = true;
    private static final boolean ALLOW_ZOOM = true;
    private static final boolean ASK_NOTIFICATIONS = true;
    private static final String STATUS_COLOR = "#800000";
    private static final String BG_COLOR = "#0000FF";
    private static final int REQ_FILE = 1001;
    private static final int REQ_PERMS = 1002;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingWebPerm;
    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try { StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build()); } catch (Exception e) { }
        Window w = getWindow();
        if (KEEP_SCREEN_ON) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (FULLSCREEN) {
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            try { w.setStatusBarColor(Color.parseColor(STATUS_COLOR)); } catch (Exception e) { }
        }

        web = new WebView(this);
        try { web.setBackgroundColor(Color.parseColor(BG_COLOR)); } catch (Exception e) { }
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setGeolocationEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(ALLOW_ZOOM);
        s.setBuiltInZoomControls(ALLOW_ZOOM);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (URL_MODE && failingUrl != null && failingUrl.equals(view.getUrl())) {
                    view.loadDataWithBaseURL(null, "<html><body style='font-family:sans-serif;text-align:center;padding:40px;background:" + BG_COLOR + ";color:#fff'><h2>Sin conexión</h2><p>Comprueba tu internet.</p><button style='padding:14px 22px;border-radius:12px;border:0;font-size:16px' onclick='location.href=\"" + START_URL + "\"'>Reintentar</button></body></html>", "text/html", "UTF-8", null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { handleWebPermission(request); }
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION) || hasPerm(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoOrigin = origin;
                    pendingGeoCb = callback;
                    ask(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION});
                }
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent i = params.createIntent();
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    if (params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(Intent.createChooser(i, "Elegir archivo"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                if (url.startsWith("blob:") || url.startsWith("data:")) {
                    Toast.makeText(MainActivity.this, "Este archivo no se puede descargar desde la app", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String name = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                    r.setMimeType(mimeType);
                    r.addRequestHeader("User-Agent", userAgent);
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(r);
                    Toast.makeText(MainActivity.this, "Descargando " + name, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    openExternal(url);
                }
            }
        });

        if (ASK_NOTIFICATIONS && Build.VERSION.SDK_INT >= 33) ask(new String[]{"android.permission.POST_NOTIFICATIONS"});

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START_URL);
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        if (u.startsWith("file:") || u.startsWith("about:") || u.startsWith("data:") || u.startsWith("blob:") || u.startsWith("javascript:")) return false;
        if (u.startsWith("http://") || u.startsWith("https://")) {
            if (URL_MODE && sameSite(url)) return false;
            if (!EXTERNAL_LINKS_IN_BROWSER && !u.contains("wa.me") && !u.contains("whatsapp.com") && !u.contains("maps.google") && !u.contains("goo.gl/maps")) return false;
            openExternal(url);
            return true;
        }
        if (u.startsWith("intent:")) {
            try {
                Intent i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                try { startActivity(i); }
                catch (ActivityNotFoundException e) {
                    String fb = i.getStringExtra("browser_fallback_url");
                    if (fb != null) web.loadUrl(fb);
                }
            } catch (Exception e) { }
            return true;
        }
        openExternal(url); // tel:, mailto:, sms:, whatsapp:, geo:, market:...
        return true;
    }

    private boolean sameSite(String url) {
        try {
            String a = Uri.parse(url).getHost();
            String b = Uri.parse(START_URL).getHost();
            if (a == null || b == null) return true;
            if (a.startsWith("www.")) a = a.substring(4);
            if (b.startsWith("www.")) b = b.substring(4);
            return a.equals(b) || a.endsWith("." + b) || b.endsWith("." + a);
        } catch (Exception e) { return true; }
    }

    private void openExternal(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No hay ninguna app para abrir este enlace", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasPerm(String p) {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    private void ask(String[] perms) {
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(perms, REQ_PERMS);
    }

    private void handleWebPermission(PermissionRequest r) {
        ArrayList<String> need = new ArrayList<String>();
        for (String res : r.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && !hasPerm(android.Manifest.permission.CAMERA)) need.add(android.Manifest.permission.CAMERA);
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && !hasPerm(android.Manifest.permission.RECORD_AUDIO)) need.add(android.Manifest.permission.RECORD_AUDIO);
        }
        if (need.isEmpty()) { r.grant(r.getResources()); return; }
        pendingWebPerm = r;
        ask(need.toArray(new String[0]));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (pendingWebPerm != null) {
            ArrayList<String> ok = new ArrayList<String>();
            for (String res : pendingWebPerm.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && hasPerm(android.Manifest.permission.CAMERA)) ok.add(res);
                else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && hasPerm(android.Manifest.permission.RECORD_AUDIO)) ok.add(res);
            }
            if (ok.isEmpty()) pendingWebPerm.deny();
            else pendingWebPerm.grant(ok.toArray(new String[0]));
            pendingWebPerm = null;
        }
        if (pendingGeoCb != null) {
            boolean g = hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION) || hasPerm(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            pendingGeoCb.invoke(pendingGeoOrigin, g, false);
            pendingGeoCb = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] result = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        result = new Uri[n];
                        for (int i = 0; i < n; i++) result[i] = data.getClipData().getItemAt(i).getUri();
                    } else if (data.getData() != null) {
                        result = new Uri[]{data.getData()};
                    }
                }
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    @Override
    protected void onPause() { super.onPause(); if (web != null) web.onPause(); }

    @Override
    protected void onResume() { super.onResume(); if (web != null) web.onResume(); }

    /** Guarda bytes en la galería (Android 10+) o en la carpeta de la app y devuelve su Uri */
    private Uri saveBytes(String b64, String name, String mime) {
        try {
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            String safe = (name == null ? "archivo" : name).replaceAll("[^A-Za-z0-9._-]", "_");
            String m = (mime == null || mime.length() == 0) ? "application/octet-stream" : mime;
            String folder = getApplicationInfo().loadLabel(getPackageManager()).toString().replaceAll("[^A-Za-z0-9 ._-]", "").trim();
            if (folder.length() == 0) folder = "App";
            String fname = System.currentTimeMillis() + "_" + safe;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, fname);
                v.put(MediaStore.MediaColumns.MIME_TYPE, m);
                Uri col;
                if (m.startsWith("image/")) {
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + folder);
                    col = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if (m.startsWith("video/")) {
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + folder);
                    col = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else {
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + folder);
                    col = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
                Uri uri = getContentResolver().insert(col, v);
                if (uri == null) return null;
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os == null) return null;
                os.write(data);
                os.close();
                return uri;
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (dir == null) dir = getCacheDir();
                File f = new File(dir, fname);
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(data);
                fo.close();
                return Uri.fromFile(f);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Funciones nativas disponibles desde JavaScript como window.Android */
    public class Bridge {
        @JavascriptInterface
        public void toast(final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }
            });
        }

        @JavascriptInterface
        public void share(String text) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            Intent c = Intent.createChooser(i, "Compartir");
            c.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(c);
        }

        @JavascriptInterface
        public void vibrate(long ms) {
            try {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) v.vibrate(ms);
            } catch (Exception e) { }
        }

        @JavascriptInterface
        public void open(String url) { openExternal(url); }

        @JavascriptInterface
        public boolean saveFile(String b64, String name, String mime) {
            return saveBytes(b64, name, mime) != null;
        }

        @JavascriptInterface
        public void shareFile(String b64, String name, final String mime) {
            final Uri u = saveBytes(b64, name, mime);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (u == null) {
                        Toast.makeText(MainActivity.this, "No se pudo compartir", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType(mime);
                        i.putExtra(Intent.EXTRA_STREAM, u);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Intent c = Intent.createChooser(i, "Compartir");
                        c.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(c);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "No hay apps para compartir", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
