package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
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

            dialog.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setDimAmount(0.65f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }

            val density = activity.resources.displayMetrics.density
            fun dpToPx(dp: Int): Int = (dp * density).toInt()

            val layout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(28), dpToPx(24), dpToPx(28), dpToPx(24))

                val shape = GradientDrawable().apply {
                    cornerRadius = dpToPx(20).toFloat()
                    setColor(Color.parseColor("#0F172A")) // Deep Slate Navy
                    setStroke(dpToPx(1), Color.parseColor("#334155")) // Elegant Subtle Border
                }
                background = shape
            }

            val progressBar = ProgressBar(activity).apply {
                indeterminateDrawable.setColorFilter(Color.parseColor("#00C3FD"), android.graphics.PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            }

            val titleView = TextView(activity).apply {
                text = "Ad is Loading..."
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dpToPx(14), 0, 0)
                gravity = Gravity.CENTER
            }

            val subtitleView = TextView(activity).apply {
                text = "Please wait a moment..."
                setTextColor(Color.parseColor("#94A3B8")) // Slate 400
                textSize = 12.5f
                setPadding(0, dpToPx(4), 0, 0)
                gravity = Gravity.CENTER
            }

            layout.addView(progressBar)
            layout.addView(titleView)
            layout.addView(subtitleView)
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
     * Shows a brief "Ad is Loading..." dialog to confirm ad loading to the user before displaying the ad.
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

        // Wait 600ms so user clearly sees the "Ad is Loading..." confirmation before the ad opens
        Handler(Looper.getMainLooper()).postDelayed({
            dismissDialog(dialog)
            onComplete()
        }, 600L)
    }
}