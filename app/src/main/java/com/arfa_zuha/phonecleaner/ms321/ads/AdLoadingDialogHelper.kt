package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

object AdLoadingDialogHelper {

    fun showLoadingDialog(activity: Activity): Dialog? {
        if (activity.isFinishing || activity.isDestroyed) {
            return null
        }
        return try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val layout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(60, 60, 60, 60)

                val shape = android.graphics.drawable.GradientDrawable()
                shape.cornerRadius = 32f
                shape.setColor(Color.parseColor("#CC000000"))
                background = shape
            }

            val progressBar = ProgressBar(activity).apply {
                @Suppress("DEPRECATION")
                indeterminateDrawable.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
            }

            val textView = TextView(activity).apply {
                text = "Ad is loading..."
                setTextColor(Color.WHITE)
                textSize = 15f
                setPadding(0, 30, 0, 0)
                gravity = Gravity.CENTER
            }

            layout.addView(progressBar)
            layout.addView(textView)
            dialog.setContentView(layout)

            if (!activity.isFinishing && !activity.isDestroyed) {
                dialog.show()
                dialog
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun dismissDialog(dialog: Dialog?) {
        try {
            if (dialog != null && dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (e: Exception) {
            // Ignore window token exceptions
        }
    }

    /**
     * Shows a brief "Loading Ad..." dialog for a fraction of a second before showing the actual ad.
     * Prevents window token leaks and crashes by validating activity state.
     */
    fun showLoadingAndThen(activity: Activity, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        val dialog = showLoadingDialog(activity)
        if (dialog == null) {
            onComplete()
            return
        }

        // Wait 400ms to break user's clicking momentum, then show ad
        Handler(Looper.getMainLooper()).postDelayed({
            dismissDialog(dialog)
            onComplete()
        }, 400L)
    }
}
