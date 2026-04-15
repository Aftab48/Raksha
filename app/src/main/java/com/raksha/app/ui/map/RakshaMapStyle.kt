package com.raksha.app.ui.map

import com.google.android.gms.maps.model.MapStyleOptions

object RakshaMapStyle {
    private val styleJson = """
        [
          {"elementType":"geometry","stylers":[{"color":"#050f0d"}]},
          {"elementType":"labels.text.fill","stylers":[{"color":"#6b9e92"}]},
          {"elementType":"labels.text.stroke","stylers":[{"color":"#050f0d"}]},
          {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1a3830"}]},
          {"featureType":"water","elementType":"geometry","stylers":[{"color":"#051510"}]},
          {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#0a1f18"}]},
          {"featureType":"transit","stylers":[{"visibility":"off"}]},
          {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#1a3830"}]}
        ]
    """.trimIndent()

    val mapStyleOptions: MapStyleOptions?
        get() = runCatching { MapStyleOptions(styleJson) }.getOrNull()
}
