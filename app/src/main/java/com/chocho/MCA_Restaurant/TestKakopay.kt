package com.chocho.MCA_Restaurant

import android.content.Intent
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * [테스트용 - 현재 사용하지 않음]
 *
 * 카카오페이 연동을 처음 실험하던 화면. AndroidManifest 에 등록되어 있지 않아 실행되지 않는다.
 * 실제 결제 로직은 PaymentListActivity 로 옮겨졌다.
 */
class TestKakopay : AppCompatActivity() {

    private val client = OkHttpClient()
    private var receivedIntent: Intent? = null
    val executor = Executors.newSingleThreadScheduledExecutor()

    private val database = Firebase.database
    private val tableDatabase = database.getReference("table")
    private val masterDatabase = database.getReference("master")
    private val live: MutableList<Any> = mutableListOf()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

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

        val button = findViewById<Button>(R.id.button)
        val toastString = "결제 창으로 넘어 갑니다"
        val toastDuration = Toast.LENGTH_SHORT
        val toastPayment = Toast.makeText(this, toastString, toastDuration)

        button.setOnClickListener {
            toastPayment.show()
        }

        //파이어베이스 table 가격값 가져오기
        tableDatabase.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(tableSnapshot: DataSnapshot) {
                var sum = 0
                val dec = DecimalFormat("###,###")
                if (tableSnapshot.exists()) {

                    for (tableMeatValue in tableSnapshot.children) {

                        sum += tableMeatValue.child("meatValue").getValue(Int::class.java)!!

                        // 카카오 페이 결제 버튼
                        val kakaoPay = findViewById<ImageButton>(R.id.paybtn)
                        kakaoPay.setOnClickListener {
                            kakaoPayReady("1번 테이블",sum)
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        //파이어베이스 가격값 master 에 저장






    }


    // 0.5초 주기로 결제 상태 확인
    fun startPollingPaymentStatus(tid: String) {
        val pollingTask = object : Runnable {
            override fun run() {
                requestPaymentStatus(tid)
            }
        }
        executor.scheduleAtFixedRate(pollingTask, 0, 1, TimeUnit.SECONDS)
    }

    // 결제 완료 시 폴링 상태 해제
    fun stopPollingPaymentStatus() {
        executor.shutdown()
    }

    // 결제 준비 단계
    private fun kakaoPayReady( name:String, pay:Int ) {
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
        params["approval_url"] = "http://192.168.0.33:8080/"
        params["fail_url"] = "http://192.168.0.33:8080/"
        params["cancel_url"] = "http://192.168.0.33:8080/"

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
            .addHeader("Authorization", "KakaoAK 471ce1832263a9c17fbec652e3ec12b9")
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
    fun requestPaymentStatus(tid: String) {
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
            .addHeader("Authorization", "KakaoAK 471ce1832263a9c17fbec652e3ec12b9")
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // 새로운 Intent를 받으면 receivedIntent 변수에 저장
        receivedIntent = intent
    }

    // 결제 승인 요청
    private fun kakaoPayApprove(pg_token: String, tid: String) {
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
            .addHeader("Authorization", "KakaoAK 471ce1832263a9c17fbec652e3ec12b9")
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

                    //결제 결과 master에 넘김
                    tableDatabase.addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            live.clear()
                            for (messageData in snapshot.children) {
                                val getData = messageData.getValue(DataClassMeat::class.java)
                                live.add(getData!!)
                                Log.d("궁금해", live.toString())
                                masterDatabase.setValue(live)
                                tableDatabase.removeValue()
                            }


                        }


                        override fun onCancelled(error: DatabaseError) {
                        }

                    })



                }

            }
        })
    }
}