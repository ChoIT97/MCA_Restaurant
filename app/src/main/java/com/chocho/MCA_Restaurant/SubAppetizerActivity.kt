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
 * [메뉴 화면 - 애피타이저]
 *
 * 왼쪽 사이드바: 카테고리 이동 (파스타·피자·스테이크·음료) — 현재 카테고리는 흰 글씨로 강조
 * 가운데: 애피타이저 메뉴 카드 2열 그리드 (4종)
 * 오른쪽 아래: 장바구니(🛍) 버튼 → 주문 목록(PaymentListActivity)
 *
 * 메뉴 카드를 누르면 상세 화면으로 "key"(1부터 시작하는 메뉴 순번)를 넘긴다.
 * 상세 화면에서 담으면 Firebase table/13~16 에 저장된다.
 *
 * 다른 카테고리 화면(Sub*Activity)들도 같은 구조다.
 */
class SubAppetizerActivity : AppCompatActivity() {

    private val items = mutableListOf<DataClassSubActivity>()
    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2 = 0
    private var sound3 = 0
    private var sound4 = 0

    private lateinit var pastaLinearLayout: LinearLayout
    private lateinit var pizzaLinearLayout: LinearLayout
    private lateinit var stakeLinearLayout: LinearLayout
    private lateinit var waterLinearLayout: LinearLayout

    private lateinit var paymentImageView: ImageView

    private lateinit var intentSubPastaActivity: Intent
    private lateinit var intentSubPizzaActivity: Intent
    private lateinit var intentSubStakeActivity: Intent
    private lateinit var intentSubDrinkActivity: Intent
    private lateinit var intentPaymentListActivity: Intent
    private lateinit var intentSubAppetizerItemActivity: Intent

    private lateinit var appetizerCaesar: String
    private lateinit var appetizerGarlic: String
    private lateinit var appetizerKale: String
    private lateinit var appetizerShrimp: String

    private lateinit var recyclerView: RecyclerView


    // 화면 생성: 레이아웃을 붙이고 init() 에서 나머지 초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appetizer)

        init()

    }

    // 뷰 연결, 효과음 준비, 사이드바/장바구니 클릭, 메뉴 그리드 구성
    @SuppressLint("ObsoleteSdkInt")
    private fun init() {

        // 애니매이션
        overridePendingTransition(R.anim.fade_in, R.anim.none)

        //LinearLayout 변수 선언
        pastaLinearLayout = findViewById(R.id.pastaLinearLayout)
        pizzaLinearLayout = findViewById(R.id.pizzaLinearLayout)
        stakeLinearLayout = findViewById(R.id.stakeLinearLayout)
        waterLinearLayout = findViewById(R.id.drinkLinearLayout)

        //ImageView 변수 선언
        paymentImageView = findViewById(R.id.paymentImageView)

        //Intent 변수 선언
        intentSubPastaActivity = Intent(this, SubPastaActivity::class.java)
        intentSubPizzaActivity = Intent(this, SubPizzaActivity::class.java)
        intentSubStakeActivity = Intent(this, SubStakeActivity::class.java)
        intentSubDrinkActivity = Intent(this, SubDrinkActivity::class.java)
        intentPaymentListActivity = Intent(this, PaymentListActivity::class.java)
        intentSubAppetizerItemActivity = Intent(this, SubAppetizerItemActivity::class.java)

        //리사이클러뷰 변수 선언
        recyclerView = findViewById(R.id.main4)

        //메세지 가져오기
        appetizerCaesar = this.resources.getString(R.string.Appetizer_Caesar)
        appetizerGarlic = this.resources.getString(R.string.Appetizer_Garlic)
        appetizerKale = this.resources.getString(R.string.Appetizer_Kale)
        appetizerShrimp = this.resources.getString(R.string.Appetizer_Shrimp)

        //클릭시 소리 사운드
        // 버튼 효과음 준비 (res/raw/push.mp3 를 sound1 으로 사용, 나머지는 로드만 하고 사용하지 않음)
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            SoundPool.Builder().setMaxStreams(6).setAudioAttributes(audioAttributes).build()
        } else {
            SoundPool(6, AudioManager.STREAM_MUSIC, 0)
        }
        sound1 = soundPool!!.load(this, R.raw.push, 1)
        sound2 = soundPool!!.load(this, R.raw.sound2, 1)
        sound3 = soundPool!!.load(this, R.raw.sound3, 1)
        sound4 = soundPool!!.load(this, R.raw.sound4, 1)


        // 사이드바 카테고리 / 장바구니 버튼 공통 클릭 처리: 눌린 뷰 id 에 맞는 화면으로 이동 (효과음 + 전환 애니메이션 없음)
        val clickListener = View.OnClickListener {
            val intent = when (it.id) {
                R.id.stakeLinearLayout -> intentSubStakeActivity
                R.id.pastaLinearLayout -> intentSubPastaActivity
                R.id.pizzaLinearLayout -> intentSubPizzaActivity
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
        stakeLinearLayout.setOnClickListener(clickListener)
        waterLinearLayout.setOnClickListener(clickListener)
        paymentImageView.setOnClickListener(clickListener)

        // 그리드에 보여줄 메뉴 목록 (사진, 이름, 단가). 단가는 *ItemActivity / PaymentListAdapter 의 값과 같아야 한다.
        val itemList = listOf(
            DataClassSubActivity(R.drawable.appetizer_sizer, Text = appetizerCaesar, 19500),
            DataClassSubActivity(R.drawable.appetizer_tawer, Text = appetizerGarlic, 19900),
            DataClassSubActivity(R.drawable.appetizer_kail, Text = appetizerKale, 19900),
            DataClassSubActivity(R.drawable.appetizer_kasulella, Text = appetizerShrimp, 25900)
        )

        items.addAll(itemList)


        val subActivityAdapter = SubActivityAdapter(this, items)
        recyclerView.adapter = subActivityAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        //Item 클릭 함수
        // 메뉴 카드 클릭 → 메뉴 이름으로 순번(key)을 정해 상세 화면으로 이동
        subActivityAdapter.itemClick = object : SubActivityAdapter.ItemClick {
            override fun onClick(view: View, position: Int) {
                val itemValue = when (items[position].Text) {
                    appetizerCaesar -> 1
                    appetizerGarlic -> 2
                    appetizerKale -> 3
                    appetizerShrimp -> 4
                    else -> return
                }
                intentSubAppetizerItemActivity.putExtra("key", itemValue)
                intentSubAppetizerItemActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intentSubAppetizerItemActivity)
            }
        }
    }

    //백키를 눌렀을 때
    override fun onBackPressed() {}

}