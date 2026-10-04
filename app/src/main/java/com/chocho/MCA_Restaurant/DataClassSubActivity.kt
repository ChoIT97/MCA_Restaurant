package com.chocho.MCA_Restaurant

/**
 * [메뉴 카드 데이터]
 *
 * 카테고리 화면(Sub*Activity)의 2열 그리드에 표시되는 메뉴 한 개.
 *
 * @property Image 메뉴 사진 drawable 리소스 ID
 * @property Text  메뉴 이름
 * @property Money 단가(원)
 */
data class DataClassSubActivity(
    val Image: Int,
    val Text: String ="",
    val Money: Int

)