package com.chocho.MCA_Restaurant


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * [주문 목록 / 결제 화면]
 *
 * 1. Firebase "table" 노드(장바구니)를 실시간 구독해서 목록과 총 금액을 보여준다.
 * 2. [전체삭제] → table 노드 삭제 후 메뉴 화면으로
 * 3. [주문] → 카카오페이 단건결제(테스트 CID: TC0ONETIME) 진행
 *    ready(결제 준비) → 브라우저에서 결제 → approval_url 페이지가 myapp:// 딥링크로 앱을 다시 열어줌
 *    → onNewIntent 로 pg_token 수신 → approve(결제 승인)
 * 4. 승인되면 장바구니를 "master" 노드로 복사(주방/서빙 로봇 쪽에서 읽음)하고 table 을 비운 뒤 시작 화면으로
 *
 * Manifest 에서 launchMode="singleTop" + myapp 스킴 intent-filter 로 딥링크를 받는다.
 */
class PaymentListActivity : AppCompatActivity() {

    private val dec = java.text.DecimalFormat("###,###")
    private val toastShort = Toast.LENGTH_SHORT

    //database
    private val database = Firebase.database
    private val tableDatabase = database.getReference("table")
    private val masterDatabase = database.getReference("master")
    private val table: MutableList<Any> = mutableListOf()

    //kakopay
    // 카카오 Admin 키. 주의: 소스에 키가 그대로 노출되어 있으므로 실제 배포 시에는 서버로 옮기고 키를 재발급해야 한다.
    private val admin = "KakaoAK 471ce1832263a9c17fbec652e3ec12b9"
    // 결제 성공/실패/취소 후 이동할 주소. 로컬 PC 웹서버(같은 Wi-Fi)에서 myapp:// 딥링크로 앱을 다시 열어주는 페이지를 띄운다.
    private val approvalUrl = "http://192.168.0.6:8080/"
    private val failUrl = "http://192.168.0.6:8080/"
    private val cancelUrl = "http://192.168.0.6:8080/"

    private val client = OkHttpClient()
    private var receivedIntent: Intent? = null
    // 결제 상태를 1초마다 조회하기 위한 스케줄러
    private val executor = Executors.newSingleThreadScheduledExecutor()

    //리사이클러뷰 변수
    private lateinit var adapter: PaymentListAdapter
    private val viewModel by lazy { ViewModelProvider(this).get(FirebaseViewModel::class.java) }

    //총 합
    // 장바구니 총 금액 (table 의 meatValue 합계)
    private var totalAmountSum = 0

    private lateinit var backButton: ImageButton
    private lateinit var paymentButton: ImageButton
    private lateinit var removeButton: ImageButton

    private lateinit var totalAmount: TextView

    private lateinit var intentSubPastaActivity: Intent

    private lateinit var paymentListRecyclerView: RecyclerView

    private lateinit var deleteMessage: String
    private lateinit var notPayListMessage: String
    private lateinit var notDeleteListMessage: String

    private lateinit var deleteToast: Toast
    private lateinit var notPayListToast: Toast
    private lateinit var notDeleteListToast: Toast


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_list)

        init()

    }

    private fun init() {
        // 애니매이션
        overridePendingTransition(R.anim.fade_in, R.anim.none)

        //이미지버튼 변수
        backButton = findViewById(R.id.backButton)
        paymentButton = findViewById(R.id.payment)
        removeButton = findViewById(R.id.removeButton)

        //리사이클러뷰 변수
        paymentListRecyclerView = findViewById(R.id.paymentListRecyclerView)

        //텍스트 뷰 변수
        totalAmount = findViewById(R.id.totalAmount)

        //intent 변수
        intentSubPastaActivity = Intent(this, SubPastaActivity::class.java)


        //메세지 가져오기
        deleteMessage = this.resources.getString(R.string.deleteMessage)
        notPayListMessage = this.resources.getString(R.string.notPayListMessage)
        notDeleteListMessage = this.resources.getString(R.string.notDeleteListMessage)

        //toast 메세지
        deleteToast = Toast.makeText(this, deleteMessage, toastShort)
        notPayListToast = Toast.makeText(this, notPayListMessage, toastShort)
        notDeleteListToast = Toast.makeText(this, notDeleteListMessage, toastShort)

        //뒤로가기 버튼
        backButton.setOnClickListener {
            intentSubPastaActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intentSubPastaActivity)
        }

        //리사이클러뷰 파이어베이스 연결
        adapter = PaymentListAdapter(this)

        //RecyclerView 같은 경우 매니져를 설정해 줘야한다.
        paymentListRecyclerView.layoutManager = LinearLayoutManager(this)
        paymentListRecyclerView.setHasFixedSize(true)
        paymentListRecyclerView.adapter = adapter

        observerData()

        //테이블 데이터베이스에서 가격 정보 가져오기
        // 장바구니가 바뀔 때마다 총 금액을 다시 계산하고, 장바구니가 비었는지에 따라 버튼 동작을 바꾼다.
        tableDatabase.addValueEventListener(object : ValueEventListener {

            @SuppressLint("SetTextI18n")
            override fun onDataChange(tableSnapshot: DataSnapshot) {
                totalAmountSum = 0  //totalAmountSum 초기화 안해주면 -누르거나 삭제했을때 계속 값이 추가됨

                if (tableSnapshot.exists()) { //존재할 때
                    //각 meatValue 값 (각 주문한 음식의 가격) 을 다 더함
                    for (tableMeatValue in tableSnapshot.children) {

                        totalAmountSum += tableMeatValue.child("meatValue").getValue(Int::class.java)!!

                    }

                    //결제 버튼(카카오페이)
                    paymentButton.setOnClickListener {

                        kakaoPayReady("1번 테이블", totalAmountSum)

                    }

                    //전체 삭제 버튼
                    removeButton.setOnClickListener {

                        tableDatabase.removeValue()

                        deleteToast.show()

                        startActivity(intentSubPastaActivity)
                    }


                } else { //구매목록이 없을때

                    totalAmountSum = 0

                    paymentButton.setOnClickListener {

                        notPayListToast.show()

                        startActivity(intentSubPastaActivity)

                    }
                    removeButton.setOnClickListener {

                        notDeleteListToast.show()

                    }
                }

                totalAmount.text = "￦ " + dec.format(totalAmountSum)

            }

            override fun onCancelled(error: DatabaseError) {
                val errorMessage = "Database operation cancelled: ${error.message}"
                Toast.makeText(this@PaymentListActivity, errorMessage, Toast.LENGTH_SHORT).show()

            }
        })
    }

    // ViewModel 의 장바구니 LiveData 를 구독해서 RecyclerView 를 갱신한다.
    @SuppressLint("NotifyDataSetChanged")
    private fun observerData() {
        viewModel.fetchData().observe(this) {

            adapter.setListData(it)
            adapter.notifyDataSetChanged()

        }

    }

    /**
     * 결제창을 띄운 뒤 1초마다 결제 상태를 조회한다.
     * 딥링크로 pg_token 이 들어오면 requestPaymentStatus 안에서 승인 단계로 넘어간다.
     */
    fun startPollingPaymentStatus(tid: String) {
        val pollingTask = Runnable { requestPaymentStatus(tid) }
        executor.scheduleAtFixedRate(pollingTask, 0, 1, TimeUnit.SECONDS)
    }

    // 결제 완료 시 폴링 상태 해제
    // 결제 상태 조회를 멈춘다. (executor 를 shutdown 하므로 같은 화면에서 재시작은 불가)
    fun stopPollingPaymentStatus() {
        executor.shutdown()
    }

    // 결제 준비 단계
    /**
     * [1단계] 카카오페이 결제 준비 API 호출
     *
     * @param name 상품명 (현재 "1번 테이블" 고정)
     * @param pay  결제 금액
     * 응답의 next_redirect_pc_url 을 브라우저로 열고, tid(결제 고유번호)로 상태 조회를 시작한다.
     */
    private fun kakaoPayReady(name: String, pay: Int) {
        val readyUrl = "https://kapi.kakao.com/v1/payment/ready"
        // 파라미터 설정
        val params = HashMap<String, Any>()
        params["cid"] = "TC0ONETIME"
        params["partner_order_id"] = "123"
        params["partner_user_id"] = "123"
        params["item_name"] = name
        params["quantity"] = "1"
        params["total_amount"] = "$pay"
        params["vat_amount"] = "200"
        params["tax_free_amount"] = "0"
        params["approval_url"] = approvalUrl
        params["fail_url"] = failUrl
        params["cancel_url"] = cancelUrl

        // form 형태의 데이터 구성
        val formBody = FormBody.Builder()

        val httpBuilder = Uri.Builder()
        val set: Set<*> = params.keys
        val iterator = set.iterator()
        while (iterator.hasNext()) {

            // body에 데이터 추가
            val key = iterator.next() as String
            formBody.add(key, params[key].toString())
        }

        val request = Request.Builder()
            .addHeader("Authorization", admin)
            .addHeader("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
            .url(readyUrl + httpBuilder.toString())
            .post(formBody.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("ready", "실패")
            }

            // TODO [응답을 받은 경우]
            override fun onResponse(call: Call, response: Response) {
                // 카카오페이 결제 요청 결과 처리 부분
                val jsonObject = JSONObject(response.body!!.string())
                Log.d("ready", "" + jsonObject)
                val nextRedirectPcUrl = jsonObject.getString("next_redirect_pc_url")
                val tid = jsonObject.getString("tid")

                // 웹 브라우저로 열기
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nextRedirectPcUrl))
                startActivity(intent)

                startPollingPaymentStatus(tid)
            }
        })
    }

    // 결제 상태 조회
    /**
     * [2단계] 결제 상태 조회 (폴링)
     *
     * 딥링크(onNewIntent)로 받은 Intent 에 pg_token 이 있으면 폴링을 멈추고 승인 요청으로 넘어간다.
     */
    private fun requestPaymentStatus(tid: String) {
        val orderUrl = "https://kapi.kakao.com/v1/payment/order"
        // 요청 바디에 담을 파라미터
        val params = HashMap<String, Any>()
        params["cid"] = "TC0ONETIME"
        params["tid"] = tid

        // form 형태의 데이터 구성
        val formBody = FormBody.Builder()

        val httpBuilder = Uri.Builder()
        val set: Set<*> = params.keys
        val iterator = set.iterator()
        while (iterator.hasNext()) {
            // body에 데이터 추가
            val key = iterator.next() as String
            formBody.add(key, params[key].toString())
        }

        // API 요청
        val request = Request.Builder()
            .addHeader("Authorization", admin)
            .url(orderUrl + httpBuilder)
            .post(formBody.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("request", "실패")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                // API 요청 성공 시 처리 로직
                if (response.isSuccessful && responseBody != null) {
                    // 결과 JSON 파싱
                    val jsonObject = JSONObject(responseBody)

                    Log.d("success", "" + jsonObject)

                    // pg_token 값을 추출
                    if (receivedIntent != null) {
                        val pgToken: String? = receivedIntent?.data?.getQueryParameter("pg_token")
                        Log.d("token", "" + pgToken)
                        if (pgToken != null) {
                            stopPollingPaymentStatus()
                            kakaoPayApprove(pgToken, tid)
                        }
                    }
                }
            }
        })
    }

    // 결제 완료 후 myapp:// 딥링크로 이 화면이 다시 열릴 때 호출된다. (singleTop 이라 새로 만들지 않고 Intent 만 전달됨)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // 새로운 Intent 받으면 receivedIntent 변수에 저장
        receivedIntent = intent
    }

    // 결제 승인 요청
    /**
     * [3단계] 카카오페이 결제 승인 API 호출
     *
     * 승인 성공 시 장바구니(table)를 master 노드로 복사하고 table 을 삭제한 뒤 시작 화면으로 이동한다.
     * master 는 서빙 로봇 태블릿 앱(MCA_Third_project)이 읽어서 서빙할 주문으로 표시한다.
     */
    private fun kakaoPayApprove(pg_token: String, tid: String) {

        val intentMainActivity = Intent(this@PaymentListActivity, MainActivity::class.java)

        val approveUrl = "https://kapi.kakao.com/v1/payment/approve"
        val params = HashMap<String, Any>()
        params["cid"] = "TC0ONETIME"
        params["tid"] = tid
        params["partner_order_id"] = "123"
        params["partner_user_id"] = "123"
        params["pg_token"] = pg_token

        // form 형태의 데이터 구성
        val formBody = FormBody.Builder()

        val httpBuilder = Uri.Builder()
        val set: Set<*> = params.keys
        val iterator = set.iterator()
        while (iterator.hasNext()) {
            // body에 데이터 추가
            val key = iterator.next() as String
            formBody.add(key, params[key].toString())
        }

        val request = Request.Builder()
            .addHeader("Authorization", admin)
            .url(approveUrl + httpBuilder.toString())
            .post(formBody.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("approve", "실패")
            }

            override fun onResponse(call: Call, response: Response) {
                // 결제 승인 요청 결과 처리
                val responseBody = response.body?.string()
                // API 요청 성공 시 처리 로직
                if (response.isSuccessful && responseBody != null) {
                    // 결과 JSON 파싱
                    val jsonObject = JSONObject(responseBody)
                    Log.d("finish", "" + jsonObject)

                    //결제 결과 master 에 넘김
                    tableDatabase.addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(tableSnapshot: DataSnapshot) {

                            table.clear()

                            for (tableMeatValue in tableSnapshot.children) {

                                val tableMeatValueDataDataClass = tableMeatValue.getValue(DataClassMeat::class.java)

                                table.add(tableMeatValueDataDataClass!!)

                                masterDatabase.setValue(table)

                                tableDatabase.removeValue()

                                startActivity(intentMainActivity)

                            }

                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })


                }

            }
        })
    }

    //백키를 눌렀을 때
    override fun onBackPressed() {}

}


//리사이클러뷰 코틀린 설명 잘 되어있는 사이트
//https://blog.yena.io/studynote/2017/12/06/Android-Kotlin-RecyclerView1.html
//리사이클러뷰 파이어베이스 리얼타임베이스
//https://gloria94.tistory.com/19
/** 키해시
try {
val information =
packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
val signatures = information.signingInfo.apkContentsSigners
val md = MessageDigest.getInstance("SHA")
for (signature in signatures) {
val md: MessageDigest = MessageDigest.getInstance("SHA")
md.update(signature.toByteArray())
var hashcode = String(Base64.encode(md.digest(), 0))
Log.d("hashcode", "" + hashcode)
}
} catch (e: Exception) {
Log.d("hashcode", "에러::" + e.toString())

}
 **/






