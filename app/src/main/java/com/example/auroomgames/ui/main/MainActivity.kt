package com.example.auroomgames.ui.main

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.auroomcasino.ui.fragment.ExistingBottomFragment
import com.example.auroomgames.R
import com.example.auroomgames.servise.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var mWebChromeClient: myWebChromeClient
    private lateinit var mWebViewClient: myWebViewClient
    private var visibility = 0
    private var downloadProgress = 0
    private lateinit var linearImage: ConstraintLayout
    private var mAuth: FirebaseAuth? = null
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private var codeNumber = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadingView!!.start()
        firebaseToServer(true)
    }

    private fun firebaseToServer(boolean: Boolean) {
        val configSettings = FirebaseRemoteConfigSettings.Builder().build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetch(0).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                remoteConfig.fetchAndActivate()
                if (boolean) {
                    val urlApi = remoteConfig.getString("url_$codeNumber")
                    servesApi(urlApi)
                } else {
                    codeNumber += 1
                    val urlApi = remoteConfig.getString("url_$codeNumber")
                    if (urlApi.isNotEmpty()) {
                        servesApi(urlApi)
                    } else {
                        errorFragment()
                    }
                }
            } else {
                errorFragment()
            }
        }
    }

    private fun errorFragment(){
        val bottomSheetDialogFragment = ExistingBottomFragment()
        bottomSheetDialogFragment.isCancelable = false;
        bottomSheetDialogFragment.show(supportFragmentManager, bottomSheetDialogFragment.tag)
    }

    private fun servesApi(urlApi: String){
        CoroutineScope(Dispatchers.IO).launch {
            if (isServerOperation(urlApi)) {
                runOnUiThread { initView(urlApi) }
            }
        }
    }

    private fun isServerOperation(urlApi: String): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        if (netInfo != null && netInfo.isConnected) {
            try {
                val url = URL(urlApi)
                val urlc = url.openConnection() as HttpURLConnection
                urlc.connectTimeout = 3000
                urlc.connect()
                if (urlc.responseCode == 200) {
                    return true
                }
                // also check different code for down or the site is blocked, example
                if (urlc.responseCode == 521) {
                    // Web server of the site is down
                    return false
                }
            } catch (e1: MalformedURLException) {
                e1.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        firebaseToServer(false)
        return false
    }

    private fun initView(urlApi: String) {
        customViewContainer = findViewById<View>(R.id.customViewContainer) as FrameLayout
        webView = findViewById<View>(R.id.webView) as WebView
        linearImage = findViewById(R.id.linear_image)

        initAnim()

        //Приоретет к стялям приложения
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
            if (Build.VERSION.SDK_INT >= 21) {
                this.supportActionBar?.show()
                window.statusBarColor = resources.getColor(R.color.black);
            }
        }

        //Опредиляет размеры дисплея
        val width: Int = Resources.getSystem().displayMetrics.widthPixels
        val height: Int = Resources.getSystem().displayMetrics.heightPixels

        //Ключи связки webView
        val webSettings = webView.settings
        mWebViewClient = myWebViewClient()
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.databaseEnabled = true
        webView.webViewClient = mWebViewClient
        mWebChromeClient = myWebChromeClient()
        webView.webChromeClient = mWebChromeClient
        webView.settings.setAppCacheEnabled(true)
        webView.settings.setAppCacheEnabled(true)
        webView.settings.saveFormData = true
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        //Скармливаю url сайта
        webView.loadUrl(urlApi)

        //Если размер дисплея ниже заданных параметров размер зайта 14 sp
        if (width <= 1080 && height <= 1920) {
            webSettings.defaultFontSize = 14
        }

        // Огроничение для выхода в системный браузер
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (url.startsWith("tel:") || url.startsWith("viber:")) {
                    try {
                        view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                } else if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url)
                    true
                } else {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                }
            }

            //Слушатель на первичную загрузку сайта
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (visibility != 1) {
                    linearImage.visibility = View.VISIBLE
                    downloadProgress = 1
                }
            }

            //Слушатель на повторную загрузку сайта
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (downloadProgress == 1) {
                    loadingView!!.stop()
                    linearImage.visibility = View.GONE
                    visibility = 1
                }
            }
        }
    }

    private fun initAnim() {
        loadingView!!.start()
    }

    fun inCustomView(): Boolean {
        return mCustomView != null
    }

    private fun hideCustomView() {
        mWebChromeClient.onHideCustomView()
    }

    override fun onStart() {
        super.onStart()
        mAuth = FirebaseAuth.getInstance()

        mAuth!!.signInWithEmailAndPassword("auroom@mail.ru", "aurom1994")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    initFirebase()
                }
            }
    }

    override fun onStop() {
        super.onStop()
        //To change body of overridden methods use File | Settings | File Templates.
        if (inCustomView()) {
            hideCustomView()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inCustomView()) {
                hideCustomView()
                return true
            }
            if (mCustomView == null && webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}