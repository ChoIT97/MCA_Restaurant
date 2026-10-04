package com.chocho.MCA_Restaurant

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * [메뉴 화면 - 스테이크]
 *
 * 왼쪽 사이드바: 카테고리 이동 (파스타·피자·애피타이저·음료) — 현재 카테고리는 흰 글씨로 강조
 * 가운데: 스테이크 메뉴 카드 2열 그리드 (4종)
 * 오른쪽 아래: 장바구니(🛍) 버튼 → 주문 목록(PaymentListActivity)
 *
 * 메뉴 카드를 누르면 상세 화면으로 "key"(1부터 시작하는 메뉴 순번)를 넘긴다.
 * 상세 화면에서 담으면 Firebase table/17~20 에 저장된다.
 *
 * 다른 카테고리 화면(Sub*Activity)들도 같은 구조다.
 */
class SubStakeActivity : AppCompatActivity() {

    private val items = mutableListOf<DataClassSubActivity>()
    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2 = 0
    private var sound3 = 0
    private var sound4 = 0

    private lateinit var pastaLinearLayout: LinearLayout
    private lateinit var pizzaLinearLayout: LinearLayout
    private lateinit var appetizerLinearLayout: LinearLayout
    private lateinit var drinkLinearLayout: LinearLayout

    private lateinit var paymentImageView: ImageView

    private lateinit var intentSubPastaActivity: Intent
    private lateinit var intentSubPizzaActivity: Intent
    private lateinit var intentSubDrinkActivity: Intent
    private lateinit var intentSubAppetizerActivity: Intent
    private lateinit var intentPaymentListActivity: Intent
    private lateinit var intentSubStakeItemActivity: Intent

    private lateinit var stakeGarlic: String
    private lateinit var stakeHub: String
    private lateinit var stakeHug: String
    private lateinit var stakeTwist: String

    private lateinit var recyclerView: RecyclerView


    // 화면 생성: 레이아웃을 붙이고 init() 에서 나머지 초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stake)

        init()

    }

    // 뷰 연결, 효과음 준비, 사이드바/장바구니 클릭, 메뉴 그리드 구성
    private fun init() {

        // 애니매이션
        overridePendingTransition(R.anim.fade_in, R.anim.none)

        //클릭시 소리 사운드
        sound()

        //LinearLayout 변수 선언
        pastaLinearLayout = findViewById(R.id.pastaLinearLayout)
        pizzaLinearLayout = findViewById(R.id.pizzaLinearLayout)
        appetizerLinearLayout = findViewById(R.id.appetizerLinearLayout)
        drinkLinearLayout = findViewById(R.id.drinkLinearLayout)

        //ImageView 변수 선언
        paymentImageView = findViewById(R.id.paymentImageView)

        //Intent 변수 선언
        intentSubPastaActivity = Intent(this, SubPastaActivity::class.java)
        intentSubPizzaActivity = Intent(this, SubPizzaActivity::class.java)
        intentSubDrinkActivity = Intent(this, SubStakeActivity::class.java)
        intentSubAppetizerActivity = Intent(this, SubAppetizerActivity::class.java)
        intentSubDrinkActivity = Intent(this, SubDrinkActivity::class.java)
        intentPaymentListActivity = Intent(this, PaymentListActivity::class.java)
        intentSubStakeItemActivity = Intent(this, SubStakeItemActivity::class.java)

        //리사이클러뷰 변수 선언
        recyclerView = findViewById(R.id.main5)

        //메세지 가져오기
        stakeGarlic = this.resources.getString(R.string.stake_garlic)
        stakeHub = this.resources.getString(R.string.stake_hub)
        stakeHug = this.resources.getString(R.string.stake_hug)
        stakeTwist = this.resources.getString(R.string.stake_twist)

        //목록 추가시
        // 사이드바 카테고리 / 장바구니 버튼 공통 클릭 처리: 눌린 뷰 id 에 맞는 화면으로 이동 (효과음 + 전환 애니메이션 없음)
        val clickListener = View.OnClickListener {
            val intent = when (it.id) {
                R.id.pastaLinearLayout -> intentSubPastaActivity
                R.id.pizzaLinearLayout -> intentSubPizzaActivity
                R.id.appetizerLinearLayout -> intentSubAppetizerActivity
                R.id.drinkLinearLayout -> intentSubDrinkActivity
                R.id.paymentImageView -> intentPaymentListActivity
                else -> return@OnClickListener
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            startActivity(intent)
        }
        pastaLinearLayout.setOnClickListener(clickListener)
        pizzaLinearLayout.setOnClickListener(clickListener)
        appetizerLinearLayout.setOnClickListener(clickListener)
        drinkLinearLayout.setOnClickListener(clickListener)
        paymentImageView.setOnClickListener(clickListener)

        //리사이클러뷰
        val subActivityAdapter = SubActivityAdapter(this, items)
        recyclerView.adapter = subActivityAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        //아이템 추가
        // 그리드에 보여줄 메뉴 목록 (사진, 이름, 단가). 단가는 *ItemActivity / PaymentListAdapter 의 값과 같아야 한다.
        val itemList = listOf(
            DataClassSubActivity(R.drawable.stake_garlic, Text = stakeGarlic, 49500),
            DataClassSubActivity(R.drawable.stake_hub, Text = stakeHub, 49500),
            DataClassSubActivity(R.drawable.stake_hug, Text = stakeHug, 49900),
            DataClassSubActivity(R.drawable.stake_twist, Text = stakeTwist, 49500),
        )
        items.addAll(itemList)

        //Item 클릭 함수
        // 메뉴 카드 클릭 → 메뉴 이름으로 순번(key)을 정해 상세 화면으로 이동
        subActivityAdapter.itemClick = object : SubActivityAdapter.ItemClick {
            override fun onClick(view: View, position: Int) {
                val itemValue = when (items[position].Text) {
                    stakeGarlic -> 1
                    stakeHub -> 2
                    stakeHug -> 3
                    stakeTwist -> 4
                    else -> return
                }
                intentSubStakeItemActivity.putExtra("key", itemValue)
                intentSubStakeItemActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intentSubStakeItemActivity)
            }
        }

    }

    // 버튼 효과음 준비 (res/raw/push.mp3 를 sound1 으로 사용, 나머지는 로드만 하고 사용하지 않음)
    @SuppressLint("ObsoleteSdkInt")
    private fun sound() {

        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            SoundPool(6, AudioManager.STREAM_MUSIC, 0)
        }
        sound1 = soundPool!!.load(this, R.raw.push, 1)
        sound2 = soundPool!!.load(this, R.raw.sound2, 1)
        sound3 = soundPool!!.load(this, R.raw.sound3, 1)
        sound4 = soundPool!!.load(this, R.raw.sound4, 1)

    }

    //백키를 눌렀을 때
    override fun onBackPressed() {}

}