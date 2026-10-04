package com.chocho.MCA_Restaurant

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * [테스트용 - 현재 사용하지 않음]
 *
 * 외부 웹페이지(mcapay.dothome.co.kr)를 WebView 로 띄워 결제를 흉내내던 초기 버전.
 * Manifest 에는 등록되어 있지만 다른 화면에서 이 화면으로 이동하는 코드는 없다.
 * 버튼을 누르면 table 을 master/1번 테이블 로 옮기고 시작 화면으로 돌아간다.
 */
class TestWebView : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview)

        val webview = findViewById<WebView>(R.id.webview)
        val Button = findViewById<Button>(R.id.button)

        val PayRemoveButton = findViewById<Button>(R.id.PayRemove)
        PayRemoveButton.setOnClickListener {
            Toast.makeText(this, "결제가 취소되었습니다.", Toast.LENGTH_SHORT).show()
            val payIntent = Intent(this, SubPastaActivity::class.java)
            startActivity(payIntent)
        }

        webview.apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
        }

        webview.loadUrl("http://mcapay.dothome.co.kr/new.html")
//        webview.loadUrl("https://online-pay.kakao.com/mockup/v1/e1c2ef3c9cc10c4bd4586da3d6465f85b185b75e230d502f49fe3eb3b7bd4b21/info")

        val Ref = Firebase.database.getReference("vible")
        val mRe = Firebase.database.getReference("table")

        Button.setOnClickListener {
            Ref.addValueEventListener(object : ValueEventListener {
                val live: MutableList<Any> = mutableListOf()
                override fun onDataChange(snapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    val value = snapshot.getValue()
                    live.add(value.toString())


                    val firebaseIntent = Intent(this@TestWebView, MainActivity::class.java)


                    val mDatabase = FirebaseDatabase.getInstance()
                    val kyky = mDatabase.getReference("master").child("1번 테이블")
                    val live: MutableList<Any> = mutableListOf()

                    mRe.addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            live.clear()
                            for (messageData in snapshot.children) {
                                val getData = messageData.getValue(DataClassMeat::class.java)
                                live.add(getData!!)
                                Log.d("궁금해", live.toString())
                                kyky.setValue(live)
                            }


                        }


                        override fun onCancelled(error: DatabaseError) {
                        }

                    })

                    startActivity(firebaseIntent)
                    mRe.removeValue()
                    Ref.removeValue()


                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Failed to read value.", error.toException())
                }

            })

        }



    }

    //백키를 눌렀을때
    override fun onBackPressed() {}

}