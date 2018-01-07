package com.olacabs.olaplaystudio.utils

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.InputMethodManager


fun Float.pxToDp(): Float {
    val densityDpi = Resources.getSystem().displayMetrics.densityDpi.toFloat()
    return this / (densityDpi / 160f)
}

fun Int.dpToPx(): Int {
    val density = Resources.getSystem().displayMetrics.density
    return Math.round(this * density)
}

fun View.hideKeyboard() {
    val input = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    input.hideSoftInputFromWindow(this.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
}

fun View.showKeyboard() {
    val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.visible(b: Boolean = true) {
    if (b) show() else hide()
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}