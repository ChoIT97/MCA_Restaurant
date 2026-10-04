package com.chocho.MCA_Restaurant


import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


/**
 * [주문 목록(장바구니) 어댑터]
 *
 * 주문 목록 화면의 각 줄(activity_bag_item.xml)을 그린다.
 * 한 줄: 메뉴 이름 / 금액 / [-] 수량 [+] / [X 삭제]
 *
 * +/- 나 삭제를 누르면 Firebase table/{메뉴번호} 를 직접 수정하고,
 * 그 변경이 FirebaseViewModel 을 통해 다시 화면에 반영된다. (화면 숫자를 직접 바꾸지 않음)
 *
 * 메뉴번호 규칙 (각 *ItemActivity 와 동일해야 함)
 *   1~7   파스타   /  8~12  피자    / 13~16 애피타이저
 *  17~20  스테이크 / 21~26  음료
 */
@Suppress("NAME_SHADOWING")
class PaymentListAdapter(private val context: Context) : RecyclerView.Adapter<PaymentListAdapter.ViewHolder>() {

    //decimalFormat
    private val dec = java.text.DecimalFormat("###,###")

    //database
    private val database = Firebase.database
    private val tableDatabase = database.getReference("table")

    //메세지 가져오기
    private var pastaGarlic: String = context.resources.getString(R.string.Pasta_garlic)
    private var pastaTriple: String = context.resources.getString(R.string.Pasta_triple)
    private var pastaCarbonara: String = context.resources.getString(R.string.Pasta_carbonara)
    private var pastaArrabbiata: String = context.resources.getString(R.string.Pasta_arrabbiata)
    private var pastaSpicy: String = context.resources.getString(R.string.Pasta_spicy)
    private var pastaOcean: String = context.resources.getString(R.string.Pasta_ocean)
    private var pastaCrab: String = context.resources.getString(R.string.Pasta_crab)

    private var pizzaSpicy: String = context.resources.getString(R.string.pizza_spicy)
    private var pizzaJola: String = context.resources.getString(R.string.pizza_jola)
    private var pizzaSnowing: String = context.resources.getString(R.string.pizza_snowing)
    private var pizzaBurata: String = context.resources.getString(R.string.pizza_burata)
    private var pizzaGolden: String = context.resources.getString(R.string.pizza_golden)

    private var appetizerCaesar: String = context.resources.getString(R.string.Appetizer_Caesar)
    private var appetizerGarlic: String = context.resources.getString(R.string.Appetizer_Garlic)
    private var appetizerKale: String = context.resources.getString(R.string.Appetizer_Kale)
    private var appetizerShrimp: String = context.resources.getString(R.string.Appetizer_Shrimp)

    private var stakeGarlic: String = context.resources.getString(R.string.stake_garlic)
    private var stakeHub: String = context.resources.getString(R.string.stake_hub)
    private var stakeHug: String = context.resources.getString(R.string.stake_hug)
    private var stakeTwist: String = context.resources.getString(R.string.stake_twist)

    private var drinkOrange: String = context.resources.getString(R.string.Drink_orange)
    private var drinkLemon: String = context.resources.getString(R.string.Drink_lemon)
    private var drinkJamong: String = context.resources.getString(R.string.Drink_jamong)
    private var drinkRed: String = context.resources.getString(R.string.Drink_red)
    private var drinkWhite: String = context.resources.getString(R.string.Drink_white)
    private var drink7star: String = context.resources.getString(R.string.Drink_7star)

    private var dataClassMeatList = mutableListOf<DataClassMeat>()

    // ViewModel 에서 받은 최신 장바구니 목록으로 교체한다. (호출 후 notifyDataSetChanged 필요)
    fun setListData(data: MutableList<DataClassMeat>) {
        dataClassMeatList = data
    }

    //화면을 최초 로딩하여 만들어진 View 가 없는 경우,xml 퍼알을 inflate 하여 ViewHolder 를 생성한다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentListAdapter.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.activity_bag_item, parent, false)

        return ViewHolder(view)
    }

    //onBindViewHolder : 위의 onCreateViewHolder 에서 만든 view 와 실제 입력되는 각각의 데이터를 연결한다.
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PaymentListAdapter.ViewHolder, position: Int) {

        val dataClassMeat: DataClassMeat = dataClassMeatList[position]
        val money = dataClassMeat.meatValue
        var count = dataClassMeat.meatNumber

        holder.textName.text = dataClassMeat.meatMenu
        holder.textNumber.text = count.toString()
        holder.textValue.text = "￦ " + dec.format(money)



        //+-선택창
        /**
         * 이 줄의 메뉴 이름이 [text] 와 같으면 +/- 버튼을 연결한다.
         *
         * @param text  메뉴 이름
         * @param num   Firebase table 의 메뉴번호 키
         * @param value 메뉴 단가 → 금액 = 수량 × 단가 로 다시 계산해서 저장
         * 수량은 1~10 범위로 제한한다.
         */
        fun selectPlusMinus(text: String, num: String, value: Int) {

            if (holder.textName.text == text) {

                holder.plusButton.setOnClickListener {

                    count = (count + 1).coerceAtMost(10) //최대값 10

                    tableDatabase.child(num).setValue(DataClassMeat(text, count, count * value))

                }
                holder.minusButton.setOnClickListener {

                    count = (count - 1).coerceAtLeast(1) //최소값 1

                    tableDatabase.child(num).setValue(DataClassMeat(text, count, count * value))

                }

            }

        }


        //삭제창
        /**
         * 이 줄의 메뉴 이름이 [text] 와 같으면 X 버튼에 "삭제하시겠습니까?" 확인 팝업을 연결한다.
         * Yes → Firebase table/{num} 삭제 + 목록에서 제거
         */
        fun selectDelete(text: String, num: String) {
            if (holder.textName.text == text) {
                val builder = AlertDialog.Builder(context)
                holder.deleteButton.setOnClickListener() {

                    builder.setMessage("선택한 상품을 삭제하시겠습니까?").setCancelable(true)

                        .setPositiveButton("Yes") { _, _ ->
                            // Delete from firebase

                            tableDatabase.child(num).removeValue()

                            dataClassMeatList.removeAt(position)

                            notifyItemRemoved(position)
                        }

                        .setNegativeButton("No") { dialog, _ ->
                            // Dismiss the dialog
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()

                }

            }
        }

        //Plus  Minus 버튼 눌렀을 때
        // 메뉴 이름으로 어떤 메뉴인지 찾아 버튼 동작을 연결한다. (이름, 메뉴번호, 단가)
        selectPlusMinus(pastaGarlic, "1", 24800)
        selectPlusMinus(pastaTriple, "2", 24800)
        selectPlusMinus(pastaCarbonara, "3", 25500)
        selectPlusMinus(pastaArrabbiata, "4", 25500)
        selectPlusMinus(pastaSpicy, "5", 26200)
        selectPlusMinus(pastaOcean, "6", 26200)
        selectPlusMinus(pastaCrab, "7", 27500)
        selectPlusMinus(pizzaSpicy, "8", 24800)
        selectPlusMinus(pizzaJola, "9", 25800)
        selectPlusMinus(pizzaSnowing, "10", 26800)
        selectPlusMinus(pizzaBurata, "11", 27500)
        selectPlusMinus(pizzaGolden, "12", 27800)
        selectPlusMinus(appetizerCaesar, "13", 19500)
        selectPlusMinus(appetizerGarlic, "14", 19900)
        selectPlusMinus(appetizerKale, "15", 19900)
        selectPlusMinus(appetizerShrimp, "16", 25900)
        selectPlusMinus(stakeGarlic, "17", 49500)
        selectPlusMinus(stakeHub, "18", 49500)
        selectPlusMinus(stakeHug, "19", 49900)
        selectPlusMinus(stakeTwist, "20", 49500)
        selectPlusMinus(drinkOrange, "21", 6900)
        selectPlusMinus(drinkLemon, "22", 6900)
        selectPlusMinus(drinkJamong, "23", 6900)
        selectPlusMinus(drinkRed, "24", 6900)
        selectPlusMinus(drinkWhite, "25", 6900)
        selectPlusMinus(drink7star, "26", 4500)

        //삭제 버튼 눌렀을 때
        selectDelete(pastaGarlic, "1")
        selectDelete(pastaTriple, "2")
        selectDelete(pastaCarbonara, "3")
        selectDelete(pastaArrabbiata, "4")
        selectDelete(pastaSpicy, "5")
        selectDelete(pastaOcean, "6")
        selectDelete(pastaCrab, "7")
        selectDelete(pizzaSpicy, "8")
        selectDelete(pizzaJola, "9")
        selectDelete(pizzaSnowing, "10")
        selectDelete(pizzaBurata, "11")
        selectDelete(pizzaGolden, "12")
        selectDelete(appetizerCaesar, "13")
        selectDelete(appetizerGarlic, "14")
        selectDelete(appetizerKale, "15")
        selectDelete(appetizerShrimp, "16")
        selectDelete(stakeGarlic, "17")
        selectDelete(stakeHub, "18")
        selectDelete(stakeHug, "19")
        selectDelete(stakeTwist, "20")
        selectDelete(drinkOrange, "21")
        selectDelete(drinkLemon, "22")
        selectDelete(drinkJamong, "23")
        selectDelete(drinkRed, "24")
        selectDelete(drinkWhite, "25")
        selectDelete(drink7star, "26")

    }

    override fun getItemCount(): Int {
        return dataClassMeatList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textName: TextView = itemView.findViewById(R.id.textName)
        val textValue: TextView = itemView.findViewById(R.id.textValue)
        val textNumber: TextView = itemView.findViewById(R.id.textNumber)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete)
        val plusButton: ImageButton = itemView.findViewById(R.id.plusButton)
        val minusButton: ImageButton = itemView.findViewById(R.id.minusButton)


    }

}

/**
count++
if (count >= 10) {count = 10 } 이 코드를

count = (count + 1).coerceAtMost(10) 이렇게 바꿈

반대의 경우
count = (count - 1).coerceAtLeast(1)

 **/