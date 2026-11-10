package com.pantri.app.domain

import com.pantri.app.data.Item

/** An ingredient on its way to a grocery list (from a recipe or the meal plan). */
data class Incoming(val name: String, val quantity: Double, val unit: String, val category: String)

data class MergePlan(val updates: List<Item>, val inserts: List<Incoming>)

fun normUnit(u: String): String = when (val x = u.trim().lowercase().trimEnd('.')) {
    "pcs", "pc", "piece", "pieces" -> "pc"
    "kgs", "kilo", "kilos", "kg" -> "kg"
    "gram", "grams", "gm", "g" -> "g"
    "liter", "liters", "litre", "litres", "l" -> "l"
    "milliliter", "milliliters", "ml" -> "ml"
    else -> x
}

private fun key(name: String, unit: String) = name.trim().lowercase() + "|" + normUnit(unit)

/**
 * Folds [incoming] into [existing] items: the same name with the same unit adds up,
 * anything else becomes a new line. Checked items are never touched (you already bought those).
 */
fun mergeIngredients(existing: List<Item>, incoming: List<Incoming>): MergePlan {
    val collapsed = LinkedHashMap<String, Incoming>()
    for (i in incoming) {
        if (i.name.isBlank()) continue
        val k = key(i.name, i.unit)
        val prev = collapsed[k]
        collapsed[k] = if (prev == null) i.copy(name = i.name.trim()) else prev.copy(quantity = prev.quantity + i.quantity)
    }
    val open = existing.filter { !it.checked }.associateBy { key(it.name, it.unit) }.toMutableMap()
    val updates = mutableListOf<Item>()
    val inserts = mutableListOf<Incoming>()
    for ((k, inc) in collapsed) {
        val hit = open[k]
        if (hit == null) {
            inserts += inc
        } else {
            val newQty = hit.quantity + inc.quantity
            val newPrice = if (hit.quantity > 0) Math.round(hit.priceCentavos * (newQty / hit.quantity)) else hit.priceCentavos
            updates += hit.copy(quantity = newQty, priceCentavos = newPrice)
        }
    }
    return MergePlan(updates, inserts)
}
