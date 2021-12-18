package com.example.auroomgames.servise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auroomgames.R
import com.example.auroomgames.utils.MyUtils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseActivity: AppCompatActivity() {
    private lateinit var dataBase: DatabaseReference
    var mCustomView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFirebase()
    }

    fun initFirebase() {
        //Подключаемся к базе firebase
        dataBase = FirebaseDatabase.getInstance().getReference("AuroomGames")
        try {

            //Генерируем токен для пушей
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                val token = task.result?.token
                if (token != null) {
                    dataBase.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            //Если база не является пустой
                            if (snapshot.exists()) {
                                //Пробегаемся по базе и сравниваем элементы
                                for (i in snapshot.children) {
                                    //Если токен равен элементу в базе
                                    if (token.toString() == i.value) {
                                        //Удоляем  элемент
                                        dataBase.child(MyUtils.toMyKey(token)).removeValue()

                                    }
                                }
                                //Пробегаемся по базе и подсчитываем элементы
                                for (j in 0..snapshot.childrenCount) {
                                    //Если элемент в базе является последним
                                    if (j >= snapshot.childrenCount) {
                                        //Добавляем новый элемент
                                        dataBase.child(MyUtils.toMyKey(token)).setValue(token)
                                    }
                                }
                            } else {
                                //Если база пуста добавляем элемент
                                dataBase.child(MyUtils.toMyKey(token)).setValue(token)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
                        }
                    })
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show()
        }
    }

    internal inner class myWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return super.shouldOverrideUrlLoading(
                view,
                url
            ) //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    internal inner class myWebChromeClient : WebChromeClient() {
        private var mVideoProgressView: View? = null
        override fun onShowCustomView(
            view: View,
            requestedOrientation: Int,
            callback: CustomViewCallback
        ) {
            //To change body of overridden methods use File | Settings | File Templates.
            onShowCustomView(view, callback)
        }


        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden()
                return
            }
            mCustomView = view
            webView!!.visibility = View.GONE
            customViewContainer!!.visibility = View.VISIBLE
            customViewContainer!!.addView(view)
            customViewCallback = callback
        }

        override fun getVideoLoadingProgressView(): View? {
            if (mVideoProgressView == null) {
                val inflater = LayoutInflater.from(this@BaseActivity)
                mVideoProgressView = inflater.inflate(R.layout.vidio_progres, null)
            }
            return mVideoProgressView
        }


        override fun onHideCustomView() {
            super.onHideCustomView() //To change body of overridden methods use File | Settings | File Templates.
            if (mCustomView == null) return
            webView!!.visibility = View.VISIBLE
            customViewContainer!!.visibility = View.GONE

            // Hide the custom view.
            mCustomView!!.visibility = View.GONE

            // Remove the custom view from its container.
            customViewContainer!!.removeView(mCustomView)
            customViewCallback!!.onCustomViewHidden()
            mCustomView = null
        }
    }
}