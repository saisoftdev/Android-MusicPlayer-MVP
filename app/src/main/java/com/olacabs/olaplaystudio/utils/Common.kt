package com.olacabs.olaplaystudio.utils

import com.olacabs.olaplaystudio.OlaApplication
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*


fun Date.formatDate(): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
    return dateFormat.format(this)
}

fun Date.getSimpleTime(): String {
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    return dateFormat.format(this)
}

fun <E> MutableCollection<E>.clearAndAddAll(replace: Collection<E>) {
    clear()
    addAll(replace)
}

fun Any.showAsToast() {
    EventBus.getDefault().post(OlaApplication.Companion.ShowToastEvent(this.toString()))
}

fun EventBus.regOnce(subscriber: Any) {
    if (!isRegistered(subscriber)) register(subscriber)
}

fun EventBus.unRegOnce(subscriber: Any) {
    if (isRegistered(subscriber)) unregister(subscriber)
}
