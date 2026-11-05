package com.laminated.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laminated.app.canvas.CanvasViewModel

private val PanelFill get() = if (Pc.light) Color(0xFFE2E2E2) else Color(0xFF484848)
private val PanelTitleInk get() = if (Pc.light) Color(0xFF393939) else Color(0xFFC5C5C5)
private val ButtonInk get() = if (Pc.light) Color(0xFF4A4A4A) else Color(0xFFA8A8A8)
private val CloseFill get() = if (Pc.light) Color(0xFFCFCFCF) else Color(0xFF2A2A2A)

/**
 * The Copy & Paste panel (three fingers swiped down): Cut, Copy, Copy All, Duplicate, Cut & Paste
 * and Paste, a little below the middle of the screen; a tap off it or its ✕ closes it.
 */
@Composable
fun BoxScope.CopyPastePanel(vm: CanvasViewModel) {
    if (!vm.copyPasteOpen) return
    fun act(f: () -> Unit) {
        vm.copyPasteOpen = false
        f()
    }
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { vm.copyPasteOpen = false } }) {
        val isCompact = maxWidth < 600.dp
        val panelWidth = if (isCompact) minOf(360.dp, maxWidth - 16.dp) else 496.dp
        val buttonWidth = if (isCompact) 72.dp else 82.7.dp
        val scrollState = rememberScrollState()

        Box(
            Modifier.offset(x = (maxWidth - panelWidth) / 2, y = maxHeight * (617f / 1133f))
                .size(panelWidth, 106.dp)
                .clip(RoundedCornerShape(if (isCompact) 22.dp else 30.dp))
                .background(PanelFill)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 5.dp).size(40.dp, 4.dp).clip(CircleShape).background(Color(0xFF7C7C7C)))
            Text("Copy & Paste", color = PanelTitleInk, fontSize = if (isCompact) 17.sp else 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = 18.dp, y = if (isCompact) 10.dp else 12.dp))
            Box(Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 6.dp).size(36.dp).clip(CircleShape).background(CloseFill).clickable { vm.copyPasteOpen = false }, contentAlignment = Alignment.Center) {
                Icon(PcIcons.Cross, contentDescription = "Close", tint = ButtonInk, modifier = Modifier.size(16.dp))
            }
            Row(
                Modifier.fillMaxWidth().offset(y = 48.dp).horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 0.dp),
            ) {
                PanelButton(PcIcons.CopyCut, "Cut", width = buttonWidth) { act(vm::cutLayer) }
                PanelButton(PcIcons.CopyOne, "Copy", width = buttonWidth) { act(vm::copyLayer) }
                PanelButton(PcIcons.CopyAll, "Copy All", width = buttonWidth) { act(vm::copyCanvas) }
                PanelButton(PcIcons.CopyDuplicate, "Duplicate", width = buttonWidth) { act(vm::duplicateCurrent) }
                PanelButton(PcIcons.CutPaste, "Cut & Paste", width = buttonWidth) { act(vm::cutAndPaste) }
                PanelButton(PcIcons.PasteDown, "Paste", enabled = vm.canPaste, width = buttonWidth) { act(vm::paste) }
            }
        }
    }
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, enabled: Boolean = true, width: Dp = 82.7.dp, onClick: () -> Unit) {
    val ink = if (enabled) ButtonInk else ButtonInk.copy(alpha = 0.4f)
    // the icon (a 50 frame) 28 across, centred 17 down; the name from 34
    Box(Modifier.width(width).height(50.dp).clickable(enabled = enabled, onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp).requiredSize(28.dp))
        Text(label, color = ink, fontSize = 12.sp, maxLines = 1, modifier = Modifier.align(Alignment.TopCenter).padding(top = 33.dp))
    }
}
