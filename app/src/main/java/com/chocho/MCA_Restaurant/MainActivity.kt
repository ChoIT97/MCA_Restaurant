package com.chocho.MCA_Restaurant

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity


/**
 * [시작 화면]
 *
 * 앱 실행 시 처음 보이는 화면. 주황 배경에 MCA 로고와 [주문하기] 버튼만 있다.
 * 버튼을 누르면 메뉴 화면(파스타 카테고리, SubPastaActivity)으로 이동한다.
 * 결제가 끝나면 PaymentListActivity 가 다시 이 화면으로 돌아온다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mainButton :ImageButton

    private lateinit var intentSubPastaActivity :Intent
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mainButton = findViewById(R.id.mainButton)

        intentSubPastaActivity = Intent(this, SubPastaActivity::class.java)

        mainButton.setOnClickListener { startActivity(intentSubPastaActivity) }
    }
    //백키를 눌렀을 때
    // 키오스크용 앱이라 기기 뒤로가기 키를 막아둔다. (모든 화면 공통)
    override fun onBackPressed() {}
}