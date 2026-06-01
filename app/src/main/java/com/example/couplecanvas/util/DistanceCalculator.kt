package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.LocationShareState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceCalculator {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
        val fromLat = Math.toRadians(fromLatitude)
        val toLat = Math.toRadians(toLatitude)
        val deltaLat = Math.toRadians(toLatitude - fromLatitude)
        val deltaLon = Math.toRadians(toLongitude - fromLongitude)
        val haversine = sin(deltaLat / 2).pow(2.0) + cos(fromLat) * cos(toLat) * sin(deltaLon / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    fun distanceText(localUid: String, shares: List<LocationShareState>): String {
        val local = shares.firstOrNull { it.uid == localUid }
        val partner = shares.firstOrNull { it.uid != localUid }
        return distanceText(local, partner)
    }

    fun distanceText(local: LocationShareState?, partner: LocationShareState?): String {
        if (local?.enabled != true) return "위치 공유가 꺼져 있어요"
        if (partner?.enabled != true) return "상대방도 동의하면 거리만 표시돼요"

        val localLat = local.latitude
        val localLon = local.longitude
        val partnerLat = partner.latitude
        val partnerLon = partner.longitude
        if (localLat == null || localLon == null || partnerLat == null || partnerLon == null) {
            return "서로 동의했어요. 현재 위치를 1회 공유해주세요"
        }

        return "마지막 공유 기준 ${formatDistance(distanceMeters(localLat, localLon, partnerLat, partnerLon))} 떨어져 있어요"
    }

    private fun formatDistance(meters: Double): String =
        if (meters < 1_000) "${meters.toInt().coerceAtLeast(0)}m" else "%.1fkm".format(meters / 1_000)
}
