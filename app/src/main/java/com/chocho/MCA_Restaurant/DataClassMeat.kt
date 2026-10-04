package com.chocho.MCA_Restaurant

/**
 * [장바구니 항목 데이터]
 *
 * Firebase Realtime DB 의 table/{메뉴번호} 와 master/{n} 에 저장되는 한 줄의 주문 정보.
 * 이름이 Meat 이지만 모든 메뉴(음료 포함)에 공통으로 쓰인다.
 *
 * @property meatMenu   메뉴 이름 (strings.xml 의 한글 메뉴명)
 * @property meatNumber 수량 (1~10)
 * @property meatValue  해당 메뉴의 합계 금액 (단가 × 수량)
 *
 * Firebase 가 객체로 변환하려면 기본값(빈 생성자)이 필요해서 모든 필드에 기본값을 둔다.
 */
data class DataClassMeat(
    val meatMenu: String = "오렌지 에이드",
    val meatNumber: Int = 1,
    val meatValue: Int ?= null
)




