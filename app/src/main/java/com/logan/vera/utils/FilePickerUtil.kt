package com.logan.vera.utils

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

fun Context.openFilePicker() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/epub+zip"
    }
    (this as? android.app.Activity)?.startActivityForResult(intent, 1)
}
