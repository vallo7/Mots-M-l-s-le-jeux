package com.motsmeles.game.platform

import android.app.Activity
import java.lang.ref.WeakReference

/** Référence faible vers l'activité visible (nécessaire pour afficher une publicité plein écran). */
class ActivityHolder {
    private var ref: WeakReference<Activity>? = null
    var current: Activity?
        get() = ref?.get()
        set(value) { ref = value?.let { WeakReference(it) } }
}
