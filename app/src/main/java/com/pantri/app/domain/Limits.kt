package com.pantri.app.domain

/** Longest text we accept for each kind of field, so a paste of a whole web page cannot bloat the database. */
object Limits {
    const val NAME = 80
    const val UNIT = 16
    const val NOTE = 200
    const val STEPS = 5_000
    const val MAX_STORES = 30
    const val MAX_PRICE_CENTAVOS = 99_999_999_900L
    const val MAX_QUANTITY = 999_999.0
}
