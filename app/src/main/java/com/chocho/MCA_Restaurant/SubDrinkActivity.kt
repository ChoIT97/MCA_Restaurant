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
 * [메뉴 화면 - 음료]
 *
 * 왼쪽 사이드바: 카테고리 이동 (파스타·피자·애피타이저·스테이크) — 현재 카테고리는 흰 글씨로 강조
 * 가운데: 음료 메뉴 카드 2열 그리드 (6종)
 * 오른쪽 아래: 장바구니(🛍) 버튼 → 주문 목록(PaymentListActivity)
 *
 * 메뉴 카드를 누르면 상세 화면으로 "key"(1부터 시작하는 메뉴 순번)를 넘긴다.
 * 상세 화면에서 담으면 Firebase table/21~26 에 저장된다.
 *
 * 다른 카테고리 화면(Sub*Activity)들도 같은 구조다.
 */
class SubDrinkActivity : AppCompatActivity() {

    private val items = mutableListOf<DataClassSubActivity>()
    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2 = 0
    private var sound3 = 0
    private var sound4 = 0

    private lateinit var pastaLinearLayout: LinearLayout
    private lateinit var pizzaLinearLayout: LinearLayout
    private lateinit var appetizerLinearLayout: LinearLayout
    private lateinit var stakeLinearLayout: LinearLayout

    private lateinit var paymentImageView: ImageView

    private lateinit var intentSubPastaActivity: Intent
    private lateinit var intentSubPizzaActivity: Intent
    private lateinit var intentSubStakeActivity: Intent
    private lateinit var intentSubAppetizerActivity: Intent
    private lateinit var intentPaymentListActivity: Intent
    private lateinit var intentSubDrinkItemActivity: Intent

    private lateinit var drinkOrange: String
    private lateinit var drinkLemon: String
    private lateinit var drinkJamong: String
    private lateinit var drinkRed: String
    private lateinit var drinkWhite: String
    private lateinit var drink7star: String

    private lateinit var recyclerView: RecyclerView


    // 화면 생성: 레이아웃을 붙이고 init() 에서 나머지 초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drink)

        init()

    }

    // 뷰 연결, 효과음 준비, 사이드바/장바구니 클릭, 메뉴 그리드 구성
    @SuppressLint("ObsoleteSdkInt")
    private fun init() {

        // 애니매이션
        overridePendingTransition(R.anim.fade_in, R.anim.none)

        //클릭시 소리 사운드
        sound()

        //LinearLayout 변수 선언
        pastaLinearLayout = findViewById(R.id.pastaLinearLayout)
        pizzaLinearLayout = findViewById(R.id.pizzaLinearLayout)
        appetizerLinearLayout = findViewById(R.id.appetizerLinearLayout)
        stakeLinearLayout = findViewById(R.id.stakeLinearLayout)

        //ImageView 변수 선언
        paymentImageView = findViewById(R.id.paymentImageView)

        //Intent 변수 선언
        intentSubPastaActivity = Intent(this, SubPastaActivity::class.java)
        intentSubPizzaActivity = Intent(this, SubPizzaActivity::class.java)
        intentSubStakeActivity = Intent(this, SubStakeActivity::class.java)
        intentSubAppetizerActivity = Intent(this, SubAppetizerActivity::class.java)
        intentPaymentListActivity = Intent(this, PaymentListActivity::class.java)
        intentSubDrinkItemActivity = Intent(this, SubDrinkItemActivity::class.java)

        //리사이클러뷰 변수 선언
        recyclerView = findViewById(R.id.main6)

        //메세지 가져오기
        drinkOrange = this.resources.getString(R.string.Drink_orange)
        drinkLemon = this.resources.getString(R.string.Drink_lemon)
        drinkJamong = this.resources.getString(R.string.Drink_jamong)
        drinkRed = this.resources.getString(R.string.Drink_red)
        drinkWhite = this.resources.getString(R.string.Drink_white)
        drink7star = this.resources.getString(R.string.Drink_7star)



        //목록 추가시
        // 사이드바 카테고리 / 장바구니 버튼 공통 클릭 처리: 눌린 뷰 id 에 맞는 화면으로 이동 (효과음 + 전환 애니메이션 없음)
        val clickListener = View.OnClickListener {
            val intent = when (it.id) {
                R.id.stakeLinearLayout -> intentSubStakeActivity
                R.id.pastaLinearLayout -> intentSubPastaActivity
                R.id.pizzaLinearLayout -> intentSubPizzaActivity
                R.id.appetizerLinearLayout -> intentSubAppetizerActivity
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
        appetizerLinearLayout.setOnClickListener(clickListener)
        paymentImageView.setOnClickListener(clickListener)

        //리사이클러뷰
        val subActivityAdapter = SubActivityAdapter(this, items)
        recyclerView.adapter = subActivityAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        //아이템 추가
        // 그리드에 보여줄 메뉴 목록 (사진, 이름, 단가). 단가는 *ItemActivity / PaymentListAdapter 의 값과 같아야 한다.
        val itemList = listOf(
            DataClassSubActivity(R.drawable.water_orange, Text = drinkOrange, 6900),
            DataClassSubActivity(R.drawable.water_lemon, Text = drinkLemon, 6900),
            DataClassSubActivity(R.drawable.water_jamong, Text = drinkJamong, 6900),
            DataClassSubActivity(R.drawable.water_red, Text = drinkRed, 6900),
            DataClassSubActivity(R.drawable.water_white, Text = drinkWhite, 6900),
            DataClassSubActivity(R.drawable.water_7star, Text = drink7star, 4500)

        )
        items.addAll(itemList)

        //Item 클릭 함수
        // 메뉴 카드 클릭 → 메뉴 이름으로 순번(key)을 정해 상세 화면으로 이동
        subActivityAdapter.itemClick = object : SubActivityAdapter.ItemClick {
            override fun onClick(view: View, position: Int) {
                val itemValue = when (items[position].Text) {
                    drinkOrange -> 1
                    drinkLemon -> 2
                    drinkJamong -> 3
                    drinkRed -> 4
                    drinkWhite -> 5
                    drink7star -> 6

                    else -> return
                }
                intentSubDrinkItemActivity.putExtra("key", itemValue)
                intentSubDrinkItemActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intentSubDrinkItemActivity)
            }
        }
    }

    // 버튼 효과음 준비 (res/raw/push.mp3 를 sound1 으로 사용, 나머지는 로드만 하고 사용하지 않음)
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


