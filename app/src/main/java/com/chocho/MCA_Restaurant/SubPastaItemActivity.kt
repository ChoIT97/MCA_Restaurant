package com.chocho.MCA_Restaurant

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * [메뉴 상세 화면 - 파스타]
 *
 * SubPastaActivity 에서 넘겨준 "key"(메뉴 순번)에 맞는 레이아웃(activity_sub_*.xml)을 띄운다.
 * 화면: 큰 배너 사진 / 영문 메뉴명·설명·가격 / [-] 수량 [+] / [담기] / 뒤로가기
 *
 * - 수량은 1~10. +/- 를 길게 누르면 0.1초마다 연속으로 증감한다.
 * - [담기] → Firebase table/{메뉴번호} 에 (메뉴명, 수량, 수량×단가) 저장 후 SubPastaActivity 로 복귀
 *   (파스타 메뉴번호: 1~7. 같은 메뉴를 다시 담으면 수량이 더해지지 않고 새 값으로 덮어쓴다)
 *
 * 다른 *ItemActivity 들도 같은 구조다.
 */
class SubPastaItemActivity : AppCompatActivity() {

    private lateinit var plusButton: ImageButton
    private lateinit var minusButton: ImageButton
    private lateinit var bringButton: ImageButton
    private lateinit var backButtonWhite: ImageButton

    private lateinit var textNumber: TextView

    //string 가져오기
    private lateinit var pastaGarlic: String
    private lateinit var pastaTriple: String
    private lateinit var pastaCarbonara: String
    private lateinit var pastaArrabbiata: String
    private lateinit var pastaSpicy: String
    private lateinit var pastaOcean: String
    private lateinit var pastaCrab: String

    private lateinit var itemName: String
    private var itemPrice = 0
    private var number = 1

    //database
    // Firebase Realtime DB 의 "table" 노드 = 현재 테이블의 장바구니
    private val database = Firebase.database
    private val tableDatabase = database.getReference("table")

    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2 = 0
    private var sound3 = 0
    private var sound4 = 0

    // (사용되지 않음) +/- 겸용 연속 증감 Runnable. 실제로는 아래 handler1_up / handler1_down 을 쓴다.
    private val handler: Handler = Handler()
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            soundPool!!.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            val pmText = findViewById<TextView>(R.id.textNumber)
            number += if (isIncreasing) 1 else -1
            number = number.coerceIn(1, 10)
            pmText.text = number.toString()
            handler.postDelayed(this, 100)
        }
    }

    private var isIncreasing: Boolean = false

    // [+] 버튼을 길게 누르는 동안 0.1초마다 수량 +1 (최대 10)
    private val handler1_up: Handler = Handler()
    private val runnable1_up: Runnable = object : Runnable {
        override fun run() {
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            val pmText = findViewById<TextView>(R.id.textNumber)
            // Print out your letter here...
            number++
            if (number >= 10) {
                number = 10
            }
            // Call the runnable again
            handler1_up.postDelayed(this, 100)
            pmText.text = number.toString()
        }
    }
    // [-] 버튼을 길게 누르는 동안 0.1초마다 수량 -1 (최소 1)
    private val handler1_down: Handler = Handler()
    private val runnable1_down: Runnable = object : Runnable {
        override fun run() {
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            val pmText = findViewById<TextView>(R.id.textNumber)
            // Print out your letter here...
            number--
            if (number <= 1) {
                number = 1
            }

            // Call the runnable again
            handler1_down.postDelayed(this, 100)
            pmText.text = number.toString()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()

    }


    @SuppressLint("ClickableViewAccessibility")
    private fun init(){

        //전 화면에서 값이 넘어 오는것 받기
        // 목록 화면에서 넘어온 메뉴 순번 (1부터). 이 값으로 레이아웃과 저장할 메뉴번호를 고른다.
        val itemValue = intent.getIntExtra("key", 0)

        //애니매이션
        overridePendingTransition(R.anim.fade_in, R.anim.none)

        //소리
        sound()

        //값 넘어오는 것에 따라 activity 선택
        when (itemValue) {
            1 -> setContentView(R.layout.activity_sub_pasta_garlic)
            2 -> setContentView(R.layout.activity_sub_pasta_triple)
            3 -> setContentView(R.layout.activity_sub_pasta_carbonara)
            4 -> setContentView(R.layout.activity_sub_pasta_arrabbiata)
            5 -> setContentView(R.layout.activity_sub_pasta_spicy)
            6 -> setContentView(R.layout.activity_sub_pasta_ocean)
            7 -> setContentView(R.layout.activity_sub_pasta_crab)

        }

        //변수 선언
        textNumber = findViewById(R.id.textNumber)
        plusButton = findViewById(R.id.plusButton)
        minusButton = findViewById(R.id.minusButton)
        bringButton = findViewById(R.id.bringButton)
        backButtonWhite = findViewById(R.id.backButtonWhite)

        //메세지 가져오기
        pastaGarlic = this.resources.getString(R.string.Pasta_garlic)
        pastaTriple = this.resources.getString(R.string.Pasta_triple)
        pastaCarbonara = this.resources.getString(R.string.Pasta_carbonara)
        pastaArrabbiata = this.resources.getString(R.string.Pasta_arrabbiata)
        pastaSpicy = this.resources.getString(R.string.Pasta_spicy)
        pastaOcean = this.resources.getString(R.string.Pasta_ocean)
        pastaCrab = this.resources.getString(R.string.Pasta_crab)

        //plus minus button
        // 길게 누르면 연속 증감 시작. false 를 반환해서 아래 터치 리스너도 함께 동작하게 한다.
        plusButton.setOnLongClickListener {
            handler1_up.post(runnable1_up)
            false
        }

        minusButton.setOnLongClickListener {
            handler1_down.post(runnable1_down)
            false
        }

        // 누르는 순간 1 증감, 손을 떼면 연속 증감 중지
        plusButton.setOnTouchListener { _, event -> handleButtonTouchEvent(event, isPlusButton = true) }

        minusButton.setOnTouchListener { _, event -> handleButtonTouchEvent(event, isPlusButton = false) }

        // [담기]: 선택한 메뉴/수량/금액을 Firebase table/{메뉴번호} 에 저장하고 목록 화면으로 돌아간다.
        bringButton.setOnClickListener {
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)

            when (itemValue) {
                1 -> {
                    itemName = pastaGarlic
                    itemPrice = number * 24800
                    tableDatabase.child("1").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                2 -> {
                    itemName = pastaTriple
                    itemPrice = number * 24800
                    tableDatabase.child("2").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                3 -> {
                    itemName = pastaCarbonara
                    itemPrice = number * 25500
                    tableDatabase.child("3").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                4 -> {
                    itemName = pastaArrabbiata
                    itemPrice = number * 25500
                    tableDatabase.child("4").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                5 -> {
                    itemName = pastaSpicy
                    itemPrice = number * 26200
                    tableDatabase.child("5").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                6 -> {
                    itemName = pastaOcean
                    itemPrice = number * 26200
                    tableDatabase.child("6").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                7 -> {
                    itemName = pastaCrab
                    itemPrice = number * 27500
                    tableDatabase.child("7").setValue(DataClassMeat(itemName, number, itemPrice))
                }
                else -> return@setOnClickListener
            }

            intent = Intent(this, SubPastaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
        }

        // 배너 왼쪽 위 뒤로가기 → 저장하지 않고 목록 화면으로
        backButtonWhite.setOnClickListener {
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            intent = Intent(this, SubPastaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
        }

    }

    /**
     * +/- 버튼 터치 처리
     *
     * ACTION_DOWN: 효과음 + 수량 1 증감(1~10 범위)
     * ACTION_UP  : 길게 누르기로 시작된 연속 증감을 멈춘다
     * @return false (다른 리스너도 이벤트를 받도록)
     */
    private fun handleButtonTouchEvent(event: MotionEvent, isPlusButton: Boolean): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            if (isPlusButton) {
                number++
                if (number >= 10) {
                    number = 10
                }
            } else {
                number--
                if (number <= 1) {
                    number = 1
                }
            }
            textNumber.text = number.toString()
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (isPlusButton) {
                handler1_up.removeCallbacks(runnable1_up)
            } else {
                handler1_down.removeCallbacks(runnable1_down)
            }
        }
        return false
    }

    // 버튼 효과음 준비 (res/raw/push.mp3 를 sound1 으로 사용)
    @SuppressLint("ObsoleteSdkInt")
    private fun sound(){
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()
        } else { SoundPool(6, AudioManager.STREAM_MUSIC, 0) }
        sound1 = soundPool!!.load(this, R.raw.push, 1)
        sound2 = soundPool!!.load(this, R.raw.sound2, 1)
        sound3 = soundPool!!.load(this, R.raw.sound3, 1)
        sound4 = soundPool!!.load(this, R.raw.sound4, 1)
    }

    //백키를 눌렀을 때
    override fun onBackPressed() {}
}