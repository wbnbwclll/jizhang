package com.wbnbwclll.voiceledger.model

/**
 * 固定分类枚举。
 * 名称写入数据库，因此新增分类只能追加，不能改名（改名需要数据迁移）。
 */
enum class Category(val displayName: String, val direction: Direction) {
    FOOD("餐饮", Direction.EXPENSE),
    TRANSPORT("交通", Direction.EXPENSE),
    SHOPPING("购物", Direction.EXPENSE),
    HOUSING("居住", Direction.EXPENSE),
    ENTERTAINMENT("娱乐", Direction.EXPENSE),
    MEDICAL("医疗", Direction.EXPENSE),
    EDUCATION("教育", Direction.EXPENSE),
    SOCIAL("人情", Direction.EXPENSE),
    OTHER_EXPENSE("其他", Direction.EXPENSE),

    SALARY("工资", Direction.INCOME),
    PART_TIME("兼职", Direction.INCOME),
    RED_PACKET("红包", Direction.INCOME),
    INVESTMENT("理财", Direction.INCOME),
    OTHER_INCOME("其他", Direction.INCOME);

    companion object {
        fun of(direction: Direction): List<Category> = entries.filter { it.direction == direction }

        fun fromNameOrNull(name: String?): Category? = entries.firstOrNull { it.name == name }
    }
}
