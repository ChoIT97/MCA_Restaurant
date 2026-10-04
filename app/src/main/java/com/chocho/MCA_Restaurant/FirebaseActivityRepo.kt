package com.chocho.MCA_Restaurant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
/**
 * [장바구니 데이터 저장소]
 *
 * Firebase 의 "table" 노드(현재 테이블의 장바구니)를 실시간으로 구독해서
 * LiveData 로 내보낸다. 주문 목록 화면(PaymentListActivity)에서 사용한다.
 */
class FirebaseActivityRepo {

    private val database = Firebase.database
    private val tableDatabase = database.getReference("table")

    private val mutableData = MutableLiveData<MutableList<DataClassMeat>>()
    private val listData: MutableList<DataClassMeat> = mutableListOf()
    /**
     * table 노드를 구독하고, 값이 바뀔 때마다 전체 목록을 다시 만들어 LiveData 에 넣는다.
     *
     * 주의: table 이 완전히 비면 snapshot.exists() 가 false 라서 LiveData 가 갱신되지 않는다.
     * (전체삭제 후에는 화면 이동으로 목록이 초기화되므로 실사용에는 문제 없음)
     */
    fun getData(): LiveData<MutableList<DataClassMeat>> {

        tableDatabase.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(tableSnapshot: DataSnapshot) {

                if (tableSnapshot.exists()) {

                    listData.clear()

                    for (tableMeatValue in tableSnapshot.children) {

                        val getData = tableMeatValue.getValue(DataClassMeat::class.java)

                        listData.add(getData!!)

                        mutableData.value = listData

                    }

                }

            }

            override fun onCancelled(error: DatabaseError) {}

        })

        return mutableData

    }
}