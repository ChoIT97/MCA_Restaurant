package com.chocho.MCA_Restaurant

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


/**
 * [카테고리 메뉴 그리드 어댑터]
 *
 * 각 카테고리 화면(Sub*Activity)의 RecyclerView 에 메뉴 카드(activity_main_item.xml)를 그린다.
 * 카드: 왼쪽 메뉴 사진 / 오른쪽 메뉴 이름 + "￦ 24,800" 형식의 가격
 *
 * 카드 클릭은 [itemClick] 인터페이스로 화면(Activity)에 위임한다.
 */
class SubActivityAdapter(val context: Context, val List: MutableList<DataClassSubActivity>) : RecyclerView.Adapter<SubActivityAdapter.ViewHolder>() {

    private val dec = java.text.DecimalFormat("###,###")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubActivityAdapter.ViewHolder{

        val v = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_item,parent,false)

        return ViewHolder(v)

    }
    //리사이 클러뷰 아이템 클릭 이벤트
    interface ItemClick
    {
        fun onClick(view : View, position:Int)

    }
    var itemClick : ItemClick? = null

    // position 번째 메뉴 데이터를 카드에 연결하고, 클릭 리스너를 등록한다.
    override fun onBindViewHolder(holder : SubActivityAdapter.ViewHolder, position: Int){
        //리사이 클러뷰 아이템 클릭 이벤트
        if (itemClick!=null){
            holder.itemView.setOnClickListener{ v->
                itemClick!!.onClick(v, position)
            }
        }
        holder.bindItems(List[position])
    }
    override fun getItemCount(): Int {
        return List.size
    }

    // 카드 한 장의 뷰 묶음. (orangeImg/orangeText 라는 id 는 처음 음료 화면을 만들 때 이름이 그대로 남은 것)
    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        @SuppressLint("SetTextI18n")
        fun bindItems(item : DataClassSubActivity){
            val orangeImage = itemView.findViewById<ImageView>(R.id.orangeImg)
            val orangeText = itemView.findViewById<TextView>(R.id.orangeText)
            val orangeText2 = itemView.findViewById<TextView>(R.id.orangeText2)


            val money =item.Money
            orangeText.text = item.Text
            orangeText2.text= "￦ " + dec.format(money)
            orangeImage.setImageResource(item.Image)
        }
    }
}