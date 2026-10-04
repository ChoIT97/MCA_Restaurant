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
 * [메뉴 화면 - 파스타]
 *
 * 왼쪽 사이드바: 카테고리 이동 (피자·애피타이저·스테이크·음료) — 현재 카테고리는 흰 글씨로 강조
 * 가운데: 파스타 메뉴 카드 2열 그리드 (7종)
 * 오른쪽 아래: 장바구니(🛍) 버튼 → 주문 목록(PaymentListActivity)
 *
 * 메뉴 카드를 누르면 상세 화면으로 "key"(1부터 시작하는 메뉴 순번)를 넘긴다.
 * 상세 화면에서 담으면 Firebase table/1~7 에 저장된다.
 *
 * 다른 카테고리 화면(Sub*Activity)들도 같은 구조다.
 */
class SubPastaActivity : AppCompatActivity() {

    private val items = mutableListOf<DataClassSubActivity>()
    private var soundPool: SoundPool? = null
    private var sound1 = 0
    private var sound2 = 0
    private var sound3 = 0
    private var sound4 = 0

    private lateinit var pizzaLinearLayout: LinearLayout
    private lateinit var stakeLinearLayout: LinearLayout
    private lateinit var waterLinearLayout: LinearLayout
    private lateinit var appetizerLinearLayout: LinearLayout

    private lateinit var paymentImageView: ImageView

    private lateinit var intentSubPizzaActivity: Intent
    private lateinit var intentSubAppetizerActivity: Intent
    private lateinit var intentSubStakeActivity: Intent
    private lateinit var intentSubDrinkActivity: Intent
    private lateinit var intentPaymentListActivity: Intent
    private lateinit var intentSubPastaItemActivity: Intent

    private lateinit var pastaGarlic: String
    private lateinit var pastaTriple: String
    private lateinit var pastaCarbonara: String
    private lateinit var pastaArrabbiata: String
    private lateinit var pastaSpicy: String
    private lateinit var pastaOcean: String
    private lateinit var pastaCrab: String

    private lateinit var recyclerView: RecyclerView


    // 화면 생성: 레이아웃을 붙이고 init() 에서 나머지 초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasta)

        init()

    }

    // 뷰 연결, 효과음 준비, 사이드바/장바구니 클릭, 메뉴 그리드 구성
    private fun init() {

        // 애니매이션
        overridePendingTransition(R.anim.fade_in, R.anim.none)

        //LinearLayout 변수 선언
        pizzaLinearLayout = findViewById(R.id.pizzaLinearLayout)
        appetizerLinearLayout = findViewById(R.id.appetizerLinearLayout)
        stakeLinearLayout = findViewById(R.id.stakeLinearLayout)
        waterLinearLayout = findViewById(R.id.drinkLinearLayout)

        //ImageView 변수 선언
        paymentImageView = findViewById(R.id.paymentImageView)

        //Intent 변수 선언
        intentSubPizzaActivity = Intent(this, SubPizzaActivity::class.java)
        intentSubAppetizerActivity = Intent(this, SubAppetizerActivity::class.java)
        intentSubStakeActivity = Intent(this, SubStakeActivity::class.java)
        intentSubDrinkActivity = Intent(this, SubDrinkActivity::class.java)
        intentPaymentListActivity = Intent(this, PaymentListActivity::class.java)
        intentSubPastaItemActivity = Intent(this, SubPastaItemActivity::class.java)

        //리사이클러뷰 변수 선언
        recyclerView = findViewById(R.id.main2)

        //메세지 가져오기
        pastaGarlic = this.resources.getString(R.string.Pasta_garlic)
        pastaTriple = this.resources.getString(R.string.Pasta_triple)
        pastaCarbonara = this.resources.getString(R.string.Pasta_carbonara)
        pastaArrabbiata = this.resources.getString(R.string.Pasta_arrabbiata)
        pastaSpicy = this.resources.getString(R.string.Pasta_spicy)
        pastaOcean = this.resources.getString(R.string.Pasta_ocean)
        pastaCrab = this.resources.getString(R.string.Pasta_crab)

        //클릭시 소리 사운드
        sound()

        // 사이드바 카테고리 / 장바구니 버튼 공통 클릭 처리: 눌린 뷰 id 에 맞는 화면으로 이동 (효과음 + 전환 애니메이션 없음)
        val clickListener = View.OnClickListener {
            val intent = when (it.id) {
                R.id.stakeLinearLayout -> intentSubStakeActivity
                R.id.appetizerLinearLayout -> intentSubAppetizerActivity
                R.id.pizzaLinearLayout -> intentSubPizzaActivity
                R.id.drinkLinearLayout -> intentSubDrinkActivity
                R.id.paymentImageView -> intentPaymentListActivity
                else -> return@OnClickListener
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            soundPool?.play(sound1, 0.5f, 0.5f, 0, 0, 1f)
            startActivity(intent)
        }

        pizzaLinearLayout.setOnClickListener(clickListener)
        appetizerLinearLayout.setOnClickListener(clickListener)
        stakeLinearLayout.setOnClickListener(clickListener)
        waterLinearLayout.setOnClickListener(clickListener)
        paymentImageView.setOnClickListener(clickListener)

        // 그리드에 보여줄 메뉴 목록 (사진, 이름, 단가). 단가는 *ItemActivity / PaymentListAdapter 의 값과 같아야 한다.
        val itemList = listOf(

            DataClassSubActivity(R.drawable.pasta_garlic, Text = pastaGarlic, 24800),
            DataClassSubActivity(R.drawable.pasta_triple, Text = pastaTriple, 24800),
            DataClassSubActivity(R.drawable.pasta_carbonara, Text = pastaCarbonara, 25500),
            DataClassSubActivity(R.drawable.pasta_arrabbiata, Text = pastaArrabbiata, 25500),
            DataClassSubActivity(R.drawable.pasta_spicy, Text = pastaSpicy, 26200),
            DataClassSubActivity(R.drawable.pasta_ocean, Text = pastaOcean, 26200),
            DataClassSubActivity(R.drawable.pasta_crab, Text = pastaCrab, 27500)

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
                    pastaGarlic -> 1
                    pastaTriple -> 2
                    pastaCarbonara -> 3
                    pastaArrabbiata -> 4
                    pastaSpicy -> 5
                    pastaOcean -> 6
                    pastaCrab -> 7
                    else -> return
                }
                intentSubPastaItemActivity.putExtra("key", itemValue)
                intentSubPastaItemActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intentSubPastaItemActivity)
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