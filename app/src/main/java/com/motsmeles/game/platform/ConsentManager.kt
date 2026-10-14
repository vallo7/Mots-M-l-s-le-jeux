package com.motsmeles.game.platform

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Consentement publicitaire (UMP) : demandé au lancement, requis dans certaines régions avant toute annonce. */
class ConsentManager(appContext: Context) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(appContext)

    val canRequestAds: Boolean get() = info.canRequestAds()

    val privacyOptionsRequired: Boolean
        get() = info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Met à jour le statut puis affiche le formulaire si nécessaire. [onReady] reçoit le droit de demander des annonces. */
    fun gatherConsent(activity: Activity, onReady: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onReady(info.canRequestAds()) }
            },
            { onReady(info.canRequestAds()) },
        )
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }
}
