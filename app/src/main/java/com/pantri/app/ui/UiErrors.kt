package com.pantri.app.ui

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteFullException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow

/** Unexpected storage errors become a message on screen instead of a crash. */
object UiErrors {
    val events = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun report(e: Throwable) {
        events.tryEmit(
            when (e) {
                is SQLiteFullException -> "Your phone is out of storage. Free some space and try again."
                is SQLiteConstraintException -> "That change couldn't be saved because something it needs was removed."
                else -> "Something went wrong. Your data is safe."
            },
        )
    }
}

val safeLaunch = CoroutineExceptionHandler { _, e -> UiErrors.report(e) }
