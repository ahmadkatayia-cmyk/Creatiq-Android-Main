package com.laminated.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laminated.app.canvas.CanvasViewModel
import com.laminated.app.canvas.TimeLapseSettings
import com.laminated.app.gallery.CanvasPreset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ScreenFill get() = if (Pc.light) Color(0xFFF2F2F2) else Color(0xFF1F1F1F)
private val PanelFill get() = if (Pc.light) Color(0xFFDFDFDF) else Color(0xFF282828)
private val Ink get() = if (Pc.light) Color(0xFF3E3E3E) else Color(0xFFAAAAAA)
private val IdleInk get() = if (Pc.light) Color(0xFF3E3E3E) else Color(0xFF797979)
private val Editing = Color(0xFF436BF0)
private val KeypadFill get() = if (Pc.light) Color(0xFFD1D3D8) else Color(0xFF1A1A1A)
private val DigitFill get() = if (Pc.light) Color(0xFFFFFFFF) else Color(0xFF525252)
private val KeyFill get() = if (Pc.light) Color(0xFFABB2BB) else Color(0xFF2B2B2B)
private val UnitChosen get() = if (Pc.light) Color(0xFF818C96) else Color(0xFFA1A1A1)
private val SwitchTrack get() = if (Pc.light) Color(0xFFD5D5D5) else Color(0xFF353535)
private val SwitchIdle get() = if (Pc.light) Color(0xFF3B3B3B) else Color(0xFFC1C1C1)
private val CapsuleFill get() = if (Pc.light) Color(0xFFFBFBFB) else Color(0xFF1A1A1A)
private val CapsuleEdge get() = if (Pc.light) Color(0xFFE0E0E0) else Color(0xFF373737)
private val DoneBlue = Color(0xFF0190FF)

/** The units sizes are typed in: the key's name, the unit's (as presets keep it) and its mark. */
private enum class SizeUnit(val label: String, val key: String, val mark: String) {
    MILLIMETERS("Millimeters", "mm", "mm"), CENTIMETERS("Centimeters", "cm", "cm"), INCHES("Inches", "in", "″"), PIXELS("Pixels", "px", "px")
}

/** The fields the keypad types into. */
private enum class Field { WIDTH, HEIGHT, DPI }

/** A section of the rail: its name, and its icon shown [size] across. */
private class Section(val label: String, val icon: ImageVector, val size: Dp)

private val Sections = listOf(
    Section("Dimensions", PcIcons.InfoDimensions, 50.dp),
    Section("Color profile", PcIcons.InfoProfile, 35.dp),
    Section("Time-lapse settings", PcIcons.InfoVideo, 60.dp),
    Section("Canvas properties", PcIcons.TabCanvas, 40.dp),
)

/**
 * Custom Canvas, full screen: a panel of sections (dimensions, colour profile, time-lapse
 * settings, canvas properties) on the left, the chosen one on the right with the canvas's name,
 * Cancel and Done; sizes are typed on a keypad of its own, in pixels, inches, centimetres or
 * millimetres. It edits [CanvasViewModel.editingPreset], or makes a new canvas ([onMade] then).
 */
@Composable
fun CustomCanvasScreen(vm: CanvasViewModel, onMade: () -> Unit) {
    val start = remember { vm.editingPreset?.let { vm.canvasPresets.getOrNull(it) } ?: CanvasPreset("Untitled Canvas", "px", 1080f, 1080f, 300f) }
    var section by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(start.name) }
    // the size in the unit chosen
    var width by remember { mutableStateOf(start.sizeWidth) }
    var height by remember { mutableStateOf(start.sizeHeight) }
    var dpi by remember { mutableStateOf(start.dpi) }
    var unit by remember { mutableStateOf(SizeUnit.entries.firstOrNull { it.key == start.unit } ?: SizeUnit.PIXELS) }
    var field by remember { mutableStateOf<Field?>(Field.WIDTH) }
    // what has been typed into the field, until it is left
    var typed by remember { mutableStateOf("") }
    var background by remember { mutableIntStateOf(start.background) }
    var hidden by remember { mutableStateOf(start.backgroundHidden) }
    var resolution by remember { mutableIntStateOf(start.timeLapse.resolution) }
    var quality by remember { mutableIntStateOf(start.timeLapse.quality) }
    var hevc by remember { mutableStateOf(start.timeLapse.hevc) }
    var profile by remember { mutableStateOf(start.profile) }
    // which profiles the list shows
    var cmyk by remember { mutableStateOf(com.laminated.app.core.isCmykProfile(start.profile)) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val importer = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                vm.importProfile(it)?.let { name ->
                    profile = name
                    cmyk = com.laminated.app.core.isCmykProfile(name)
                }
            }
        }
    }
    // the background's colour picker is open
    var picking by remember { mutableStateOf(false) }

    fun preset() = CanvasPreset(name.trim().ifEmpty { "Untitled Canvas" }, unit.key, width, height, dpi, background, hidden, TimeLapseSettings(resolution, quality, hevc), profile)
    // what typing has made of the field so far goes into it
    fun commit() {
        val v = typed.toFloatOrNull()
        if (v != null && v > 0f) when (field) {
            Field.WIDTH -> width = v
            Field.HEIGHT -> height = v
            Field.DPI -> dpi = v.coerceIn(1f, 9999f)
            null -> Unit
        }
        typed = ""
    }
    // the size kept, in another unit (straight from one length to another; through the pixels otherwise)
    fun convert(to: SizeUnit) {
        val p = preset()
        val from = CanvasPreset.perInch(unit.key)
        fun change(v: Float, px: Int) = CanvasPreset.perInch(to.key)?.let { if (from != null) v / from * it else px / dpi * it } ?: px.toFloat()
        width = change(width, p.width)
        height = change(height, p.height)
        unit = to
    }
    fun create() {
        commit()
        if (vm.finishCustomCanvas(preset())) onMade()
    }
    BackHandler { vm.closeCustomCanvas() }
    BoxWithConstraints(Modifier.fillMaxSize().background(ScreenFill).pointerInput(Unit) { detectTapGestures { } }) {
        val wide = maxWidth >= 600.dp && maxWidth > maxHeight
        if (wide) {
            // the sections
            Column(Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp).width(319.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(PanelFill)) {
                Text("Custom Canvas", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 17.dp))
                Sections.forEachIndexed { i, s ->
                    val chosen = i == section
                    Row(
                        Modifier.padding(start = 8.dp, bottom = 10.dp).size(304.dp, 52.dp).clip(CircleShape).background(if (chosen) Pc.Accent else Color.Transparent).clickable {
                            commit()
                            section = i
                        }.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) {
                            Icon(s.icon, contentDescription = null, tint = if (chosen) Color.White else IdleInk, modifier = Modifier.requiredSize(s.size))
                        }
                        Text(s.label, color = if (chosen) Color.White else IdleInk, fontSize = 15.sp, modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }
            // the name, Cancel and Done
            BasicTextField(
                name, { name = it.take(64) }, singleLine = true,
                textStyle = TextStyle(
                    color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                cursorBrush = SolidColor(Pc.Accent),
                modifier = Modifier.padding(start = 372.dp, end = 32.dp).offset(y = 28.dp).fillMaxWidth(),
            )
            Row(Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.height(43.dp).clip(CircleShape).background(CapsuleFill).border(1.dp, CapsuleEdge, CircleShape), verticalAlignment = Alignment.CenterVertically) {
                    CapsuleText("Cancel") { vm.closeCustomCanvas() }
                    if (section == 1) CapsuleText("Import") { importer.launch(arrayOf("*/*")) }
                }
                Spacer(Modifier.width(13.dp))
                Box(Modifier.size(44.dp, 45.dp).clip(CircleShape).background(DoneBlue).clickable { create() }, contentAlignment = Alignment.Center) {
                    Icon(PcIcons.DoneTick, contentDescription = "Create", tint = Color.White, modifier = Modifier.requiredSize(40.dp))
                }
            }
            Column(Modifier.padding(start = 372.dp, end = 32.dp, top = 120.dp)) {
                RenderSection(section, field, typed, width, height, dpi, unit, cmyk, profile, resolution, quality, hevc, background, hidden, vm, preset(), ::commit, { field = it }, { cmyk = it }, { profile = it }, { resolution = it }, { quality = it }, { hevc = it }, { picking = true }, { hidden = it })
            }
        } else {
            // Compact phone layout
            Column(Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        name, { name = it.take(64) }, singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(Pc.Accent),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Row(Modifier.height(38.dp).clip(CircleShape).background(CapsuleFill).border(1.dp, CapsuleEdge, CircleShape), verticalAlignment = Alignment.CenterVertically) {
                        CapsuleText("Cancel") { vm.closeCustomCanvas() }
                        if (section == 1) CapsuleText("Import") { importer.launch(arrayOf("*/*")) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(38.dp).clip(CircleShape).background(DoneBlue).clickable { create() }, contentAlignment = Alignment.Center) {
                        Icon(PcIcons.DoneTick, contentDescription = "Create", tint = Color.White, modifier = Modifier.requiredSize(28.dp))
                    }
                }
                // Horizontal Section Tabs
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Sections.forEachIndexed { i, s ->
                        val chosen = i == section
                        Row(
                            Modifier.clip(CircleShape).background(if (chosen) Pc.Accent else PanelFill).clickable {
                                commit()
                                section = i
                            }.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(s.icon, contentDescription = null, tint = if (chosen) Color.White else IdleInk, modifier = Modifier.size(18.dp))
                            Text(s.label, color = if (chosen) Color.White else IdleInk, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                // Content Area
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (section == 0 && field != null) 200.dp else 16.dp).verticalScroll(rememberScrollState())
                ) {
                    RenderSection(section, field, typed, width, height, dpi, unit, cmyk, profile, resolution, quality, hevc, background, hidden, vm, preset(), ::commit, { field = it }, { cmyk = it }, { profile = it }, { resolution = it }, { quality = it }, { hevc = it }, { picking = true }, { hidden = it })
                }
            }
        }
        // the keypad, while a size is typed
        if (section == 0 && field != null) {
            Keypad(
                Modifier.align(Alignment.BottomCenter),
                wide,
                unit,
                onUnit = {
                    commit()
                    convert(it)
                },
                onKey = { k ->
                    when (k) {
                        "⌫" -> typed = typed.dropLast(1)
                        "." -> if ('.' !in typed && field != Field.DPI && unit != SizeUnit.PIXELS) typed += "."
                        else -> if (typed.length < 7) typed += k
                    }
                },
                onNext = {
                    commit()
                    field = when (field) { Field.WIDTH -> Field.HEIGHT; Field.HEIGHT -> Field.DPI; else -> Field.WIDTH }
                },
                onDone = {
                    commit()
                    field = null
                },
            )
        }
        // the background's colour, in a compact picker hanging from its swatch
        if (picking && section == 3) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { picking = false } })
            val pick = remember { HeldColor(background) { background = it } }
            CompactColors(vm, pick, arrowX = 200.dp, modifier = Modifier.offset(x = (maxWidth - 260.dp).coerceAtLeast(10.dp), y = 168.dp))
        }
    }
}

@Composable
private fun RenderSection(
    section: Int,
    field: Field?,
    typed: String,
    width: Float,
    height: Float,
    dpi: Float,
    unit: SizeUnit,
    cmyk: Boolean,
    profile: String,
    resolution: Int,
    quality: Int,
    hevc: Boolean,
    background: Int,
    hidden: Boolean,
    vm: CanvasViewModel,
    preset: CanvasPreset,
    commit: () -> Unit,
    onField: (Field) -> Unit,
    onCmyk: (Boolean) -> Unit,
    onProfile: (String) -> Unit,
    onResolution: (Int) -> Unit,
    onQuality: (Int) -> Unit,
    onHevc: (Boolean) -> Unit,
    onPicking: () -> Unit,
    onHidden: (Boolean) -> Unit,
) {
    when (section) {
        0 -> {
            Group {
                SizeRow("Width", if (field == Field.WIDTH && typed.isNotEmpty()) typed else CanvasPreset.number(width), unit.mark, field == Field.WIDTH) { commit(); onField(Field.WIDTH) }
                Line()
                SizeRow("Height", if (field == Field.HEIGHT && typed.isNotEmpty()) typed else CanvasPreset.number(height), unit.mark, field == Field.HEIGHT) { commit(); onField(Field.HEIGHT) }
            }
            Spacer(Modifier.height(24.dp))
            Group {
                SizeRow("DPI", if (field == Field.DPI && typed.isNotEmpty()) typed else "${dpi.roundToInt()}", "", field == Field.DPI) { commit(); onField(Field.DPI) }
                Line()
                SizeRow("Maximum Layers", "${vm.layersFor(preset.width, preset.height)}", "", false) {}
            }
        }
        1 -> {
            KindSwitch(cmyk, onCmyk)
            Spacer(Modifier.height(24.dp))
            Group {
                val names = if (cmyk) com.laminated.app.core.cmykProfiles() else com.laminated.app.core.colorProfiles()
                for ((i, name) in names.withIndex()) {
                    if (i > 0) Line()
                    ChoiceRow(name, name == profile) { onProfile(name) }
                }
            }
        }
        2 -> {
            Group {
                for ((i, pair) in listOf("1080p" to "1920 × 1080px", "2K" to "2732 × 2048px", "4K" to "3840 × 2160px").withIndex()) {
                    if (i > 0) Line()
                    ChoiceRow(pair.first, i == resolution, pair.second) { onResolution(i) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Group {
                for ((i, label) in listOf("Low quality", "Good quality", "Studio quality", "Lossless").withIndex()) {
                    if (i > 0) Line()
                    ChoiceRow(label, i == quality, tick = true) { onQuality(i) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Group { SwitchLine("HEVC", hevc, onHevc) }
        }
        else -> {
            Group {
                Row(
                    Modifier.fillMaxWidth().height(47.dp).clickable(onClick = onPicking).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Background color", color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF000000 or background.toLong())).border(1.dp, Color(0xFF555555), CircleShape))
                }
            }
            Spacer(Modifier.height(24.dp))
            Group { SwitchLine("Background hidden", hidden, onHidden) }
        }
    }
}

/** One of the header capsule's words, a button. */
@Composable
private fun CapsuleText(label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxHeight().clickable(onClick = onClick).padding(horizontal = 13.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Color(0xFFA5A5A5), fontSize = 17.sp)
    }
}

/** RGB or CMYK: which profiles the colour profile page lists, as wide as the list, the chosen half blue. */
@Composable
private fun KindSwitch(cmyk: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(34.dp).clip(CircleShape).background(SwitchTrack).padding(2.dp)) {
        for ((i, label) in listOf("RGB", "CMYK").withIndex()) {
            val on = cmyk == (i == 1)
            val ink = if (on) Color.White else SwitchIdle
            Row(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (on) Pc.Accent else Color.Transparent).clickable { onChange(i == 1) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (i == 0) PcIcons.ProfileRgb else PcIcons.ProfileCmyk, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                Text(label, color = ink, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PanelFill), content = content)
}

@Composable
private fun Line() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(ScreenFill))
}

@Composable
private fun SizeRow(label: String, value: String, unit: String, editing: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(47.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (editing) Editing else Ink, fontSize = 15.sp)
        if (unit.isNotEmpty()) Text(" $unit", color = Ink, fontSize = 15.sp)
    }
}

@Composable
private fun ChoiceRow(label: String, chosen: Boolean, detail: String? = null, tick: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(47.dp).background(if (chosen) Pc.Accent else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (chosen) Color.White else Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (detail != null) Text(detail, color = if (chosen) Color.White else Ink, fontSize = 15.sp)
        if (tick && chosen) Icon(PcIcons.MenuTick, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SwitchLine(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(47.dp).clickable { onChange(!on) }.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(on, enabled = true)
    }
}

@Composable
private fun Keypad(modifier: Modifier, wide: Boolean, unit: SizeUnit, onUnit: (SizeUnit) -> Unit, onKey: (String) -> Unit, onNext: () -> Unit, onDone: () -> Unit) {
    if (wide) {
        val w = 150.dp
        val h = 87.5.dp
        val down = 9.dp
        val across = 9.dp
        Box(
            modifier.fillMaxWidth().height(457.dp).background(KeypadFill).padding(top = 47.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(down)) {
                    for (u in listOf(SizeUnit.MILLIMETERS, SizeUnit.CENTIMETERS, SizeUnit.INCHES, SizeUnit.PIXELS)) {
                        Key(u.label, if (u == unit) UnitChosen else KeyFill, if (u != unit) Ink else if (Pc.light) Color.White else Color(0xFF1F1F1F), 15.sp, w, h) { onUnit(u) }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(down)) {
                    for (row in listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"))) {
                        Row(horizontalArrangement = Arrangement.spacedBy(across)) { for (k in row) Key(k, DigitFill, Ink, 26.sp, w, h) { onKey(k) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(across)) {
                        Key("0", DigitFill, Ink, 26.sp, w * 2 + across, h) { onKey("0") }
                        Key(".", DigitFill, Ink, 26.sp, w, h) { onKey(".") }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(down)) {
                    Key("Backspace", KeyFill, Ink, 15.sp, w, h) { onKey("⌫") }
                    Key("Next", KeyFill, Ink, 15.sp, w, h) { onNext() }
                    Key("Done", KeyFill, Ink, 15.sp, w, h * 2 + down) { onDone() }
                }
            }
        }
    } else {
        Column(
            modifier.fillMaxWidth().background(KeypadFill).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (u in listOf(SizeUnit.MILLIMETERS, SizeUnit.CENTIMETERS, SizeUnit.INCHES, SizeUnit.PIXELS)) {
                    val chosen = u == unit
                    Box(
                        Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (chosen) UnitChosen else KeyFill)
                            .clickable { onUnit(u) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(u.mark.ifEmpty { u.label }, color = if (chosen) (if (Pc.light) Color.White else Color(0xFF1F1F1F)) else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            val keyHeight = 44.dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Key("7", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("7") }
                Key("8", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("8") }
                Key("9", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("9") }
                Key("⌫", KeyFill, Ink, 18.sp, Modifier.weight(1f).height(keyHeight)) { onKey("⌫") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Key("4", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("4") }
                Key("5", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("5") }
                Key("6", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("6") }
                Key("Next", KeyFill, Ink, 14.sp, Modifier.weight(1f).height(keyHeight)) { onNext() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Key("1", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("1") }
                Key("2", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("2") }
                Key("3", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey("3") }
                Key(".", DigitFill, Ink, 20.sp, Modifier.weight(1f).height(keyHeight)) { onKey(".") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Key("0", DigitFill, Ink, 20.sp, Modifier.weight(2f).height(keyHeight)) { onKey("0") }
                Key("Done", DoneBlue, Color.White, 14.sp, Modifier.weight(2f).height(keyHeight)) { onDone() }
            }
        }
    }
}

@Composable
private fun Key(label: String, fill: Color, ink: Color, size: androidx.compose.ui.unit.TextUnit, width: Dp, height: Dp, onClick: () -> Unit) {
    Box(Modifier.size(width, height).clip(RoundedCornerShape(8.dp)).background(fill).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = ink, fontSize = size)
    }
}

@Composable
private fun Key(label: String, fill: Color, ink: Color, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(fill).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = ink, fontSize = size, fontWeight = FontWeight.SemiBold)
    }
}
