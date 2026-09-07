package ch.rolf.androidweather.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.data.ESRI_BASEMAP
import ch.rolf.androidweather.data.infraredTileUrl
import ch.rolf.androidweather.data.radarTileUrl
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.adaptive.isTablet
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RadarScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadRadar() }
    val catalog = ui.radar
    var frameIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var showIr by remember { mutableStateOf(false) }
    var showLightning by remember { mutableStateOf(false) }
    LaunchedEffect(playing, catalog?.frames?.size) {
        while (playing && (catalog?.frames?.isNotEmpty() == true)) {
            delay(500)
            frameIndex = (frameIndex + 1) % catalog!!.frames.size
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wetterradar", style = MaterialTheme.typography.headlineSmall)
        if (catalog == null) {
            Text("Radar wird geladen…")
            return
        }
        val frames = catalog.frames
        if (frames.isNotEmpty() && frameIndex >= frames.size) frameIndex = frames.lastIndex
        val frame = frames.getOrNull(frameIndex)
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isTablet()) Modifier.weight(1f) else Modifier.height(360.dp)),
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    minZoomLevel = 4.0
                    maxZoomLevel = 7.0
                    controller.setZoom(7.0)
                    controller.setCenter(GeoPoint(ui.place.latitude, ui.place.longitude))
                    setTileSource(esriSource())
                }
            },
            update = { map ->
                map.overlays.removeAll { it is TilesOverlay || it is Marker }
                map.controller.setCenter(GeoPoint(ui.place.latitude, ui.place.longitude))
                if (showIr && catalog.infrared.isNotEmpty()) {
                    val ir = catalog.infrared.last()
                    map.overlays.add(TilesOverlay(tileProvider(map, irSource(catalog.host, ir.path)), map.context).apply {
                        loadingBackgroundColor = AndroidColor.TRANSPARENT
                    })
                } else if (frame != null) {
                    map.overlays.add(TilesOverlay(tileProvider(map, rainSource(catalog.host, frame.path)), map.context).apply {
                        loadingBackgroundColor = AndroidColor.TRANSPARENT
                    })
                }
                if (showLightning) {
                    map.overlays.add(
                        TilesOverlay(tileProvider(map, lightningSource()), map.context).apply {
                            loadingBackgroundColor = AndroidColor.TRANSPARENT
                        }
                    )
                }
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(ui.place.latitude, ui.place.longitude)
                    title = ui.place.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                map.invalidate()
            }
        )
        DisposableEffect(Unit) { onDispose { } }
        if (frame != null) {
            val fmt = remember { SimpleDateFormat("HH:mm", Locale("de", "CH")) }
            Text(
                "${if (frame.kind == "nowcast") "Nowcast" else "Radar"} · ${fmt.format(Date(frame.time * 1000))}"
            )
            Slider(
                value = frameIndex.toFloat(),
                onValueChange = { frameIndex = it.toInt(); playing = false },
                valueRange = 0f..maxOf(frames.lastIndex, 1).toFloat(),
                steps = maxOf(frames.size - 2, 0)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { playing = !playing }) { Text(if (playing) "Pause" else "Abspielen") }
            FilterChip(selected = showIr, onClick = { showIr = !showIr }, label = { Text("Infrarot") })
            FilterChip(selected = showLightning, onClick = { showLightning = !showLightning }, label = { Text("Blitz") })
        }
    }
}

private fun esriSource() = object : OnlineTileSourceBase("EsriTopo", 1, 16, 256, "", arrayOf("")) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return ESRI_BASEMAP.replace("{z}", "$z").replace("{x}", "$x").replace("{y}", "$y")
    }
}

private fun rainSource(host: String, path: String) = object : OnlineTileSourceBase("RV", 1, 7, 256, ".png", arrayOf(host)) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return radarTileUrl(host, path).replace("{z}", "$z").replace("{x}", "$x").replace("{y}", "$y")
    }
}

private fun irSource(host: String, path: String) = object : OnlineTileSourceBase("IR", 1, 7, 256, ".png", arrayOf(host)) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return infraredTileUrl(host, path).replace("{z}", "$z").replace("{x}", "$x").replace("{y}", "$y")
    }
}

private fun lightningSource() = object : OnlineTileSourceBase("LI", 1, 8, 256, ".png", arrayOf("https://view.eumetsat.int")) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val n = 1 shl z
        val lonMin = x.toDouble() / n * 360.0 - 180.0
        val lonMax = (x + 1).toDouble() / n * 360.0 - 180.0
        fun lat(t: Int): Double {
            val merc = Math.PI * (1 - 2.0 * t / n)
            return Math.toDegrees(Math.atan(Math.sinh(merc)))
        }
        val latMax = lat(y)
        val latMin = lat(y + 1)
        return "https://view.eumetsat.int/geoserver/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&LAYERS=mtg_fd:li_afa&STYLES=mtg_li_afa&FORMAT=image/png&TRANSPARENT=true&SRS=EPSG:4326&WIDTH=256&HEIGHT=256&BBOX=$lonMin,$latMin,$lonMax,$latMax"
    }
}

private fun tileProvider(map: MapView, source: OnlineTileSourceBase) =
    org.osmdroid.tileprovider.MapTileProviderBasic(map.context).apply { tileSource = source }
