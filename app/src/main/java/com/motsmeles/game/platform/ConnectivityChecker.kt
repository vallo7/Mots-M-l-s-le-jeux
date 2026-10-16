package com.motsmeles.game.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Détection simple de connexion : sert uniquement à expliquer l'indisponibilité d'une publicité. */
class ConnectivityChecker(private val context: Context) {
    fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
