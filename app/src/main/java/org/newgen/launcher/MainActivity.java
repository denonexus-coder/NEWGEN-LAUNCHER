package org.newgen.launcher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    private static final String TAG = "NEWGEN_LAUNCHER";
    private static final int STORAGE_PERMISSION_CODE = 1001;

    private WebView webView;

    private final File baseDir = new File(Environment.getExternalStorageDirectory(), "NEWGEN");
    private final File gameDir = new File(baseDir, "game");
    private final File gameIndex = new File(gameDir, "index.html");
    private final File gameZip = new File(baseDir, "game.zip");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Safe window fullscreen configuration
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            initWebView();
            applyImmersiveMode();
            checkAndRequestPermissions();
        } catch (Throwable t) {
            Log.e(TAG, "Fatal error on onCreate", t);
        }
    }

    private void applyImmersiveMode() {
        try {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } catch (Exception e) {
            Log.w(TAG, "Could not set immersive mode", e);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        try {
            webView = new WebView(this);
            setContentView(webView);

            // Hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception e) {
                        Log.e(TAG, "Permission request error", e);
                    }
                }

                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d(TAG, "JS: " + consoleMessage.message());
                    return super.onConsoleMessage(consoleMessage);
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Error initializing WebView", t);
        }
    }

    private void checkAndRequestPermissions() {
        try {
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, STORAGE_PERMISSION_CODE);
                    } catch (Exception e) {
                        Intent fallbackIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(fallbackIntent, STORAGE_PERMISSION_CODE);
                    }
                    return;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, STORAGE_PERMISSION_CODE);
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Permission check failure", e);
        }

        loadGame();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            loadGame();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            loadGame();
        }
    }

    private void loadGame() {
        try {
            if (gameZip.exists()) {
                unzipGame();
            }

            if (gameIndex.exists()) {
                Toast.makeText(this, "Carregando NEWGEN de /sdcard/NEWGEN/game...", Toast.LENGTH_SHORT).show();
                webView.loadUrl("file://" + gameIndex.getAbsolutePath());
            } else {
                webView.loadUrl("file:///android_asset/fallback.html");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in loadGame", t);
            if (webView != null) {
                webView.loadUrl("file:///android_asset/fallback.html");
            }
        }
    }

    private void unzipGame() {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(gameZip))) {
            if (!gameDir.exists()) {
                gameDir.mkdirs();
            }
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(gameDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            gameZip.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error unzipping game", e);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
