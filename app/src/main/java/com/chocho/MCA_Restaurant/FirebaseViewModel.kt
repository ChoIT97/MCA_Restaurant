package com.chocho.MCA_Restaurant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * [주문 목록 ViewModel]
 *
 * FirebaseActivityRepo 의 LiveData 를 화면(PaymentListActivity)에 전달하는 중간 계층.
 */
class FirebaseViewModel : ViewModel() {

    private val repo = FirebaseActivityRepo()
    private val mutableData = MutableLiveData<MutableList<DataClassMeat>>()

    /**
     * 장바구니 목록 LiveData 를 반환한다. 화면에서 observe 해서 RecyclerView 를 갱신한다.
     */
    fun fetchData(): LiveData<MutableList<DataClassMeat>> {

        repo.getData().observeForever {

            mutableData.value = it

        }

        return mutableData

    }

}