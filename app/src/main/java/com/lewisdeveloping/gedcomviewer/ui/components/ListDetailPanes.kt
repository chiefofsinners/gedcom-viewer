package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme
import kotlin.math.roundToInt

/** How the index (list) and family (detail) screens share the window. */
enum class PaneMode {
    /** One screen at a time, switched with the bottom tabs. */
    SINGLE,

    /** Index on the left, family on the right: an unfolded book-style foldable or a wide window. */
    SIDE_BY_SIDE,

    /** Family above the fold, index below it: a foldable half-opened in tabletop posture. */
    STACKED
}

@Composable
fun currentPaneMode(): PaneMode {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val sizeClass = adaptiveInfo.windowSizeClass
    val hinges = adaptiveInfo.windowPosture.hingeList
    return when {
        // A vertical fold means an unfolded book-style device, which reports a medium width
        // (around 670dp on a Galaxy Z Fold) - too narrow for the width rule below alone.
        hinges.any { it.isVertical } &&
            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> PaneMode.SIDE_BY_SIDE
        hinges.any { !it.isVertical && it.isSeparating } -> PaneMode.STACKED
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> PaneMode.SIDE_BY_SIDE
        else -> PaneMode.SINGLE
    }
}

/**
 * Lays out [listPane] and [detailPane] for a two-pane [mode], splitting along the device's
 * fold when there is one so that neither pane's content runs across the hinge.
 */
@Composable
fun ListDetailPanes(
    mode: PaneMode,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    require(mode != PaneMode.SINGLE) { "ListDetailPanes needs a two-pane mode" }
    val sideBySide = mode == PaneMode.SIDE_BY_SIDE
    val hinge = currentWindowAdaptiveInfo().windowPosture.hingeList
        .firstOrNull { it.isVertical == sideBySide }
        ?.bounds
    // Hinge bounds are in window coordinates; this keeps them right if the panes are inset.
    var origin by remember { mutableStateOf(Offset.Zero) }
    val dividerColor = AppTheme.colors.border.copy(alpha = 0.5f)

    // Each pane gives up the system-bar insets on its edge that faces the other pane.
    val listInsets = WindowInsets.safeDrawing.only(if (sideBySide) WindowInsetsSides.Right else WindowInsetsSides.Top)
    val detailInsets = WindowInsets.safeDrawing.only(if (sideBySide) WindowInsetsSides.Left else WindowInsetsSides.Bottom)

    Layout(
        modifier = modifier.onPlaced { origin = it.positionInWindow() },
        content = {
            Box(Modifier.consumeWindowInsets(listInsets)) { listPane() }
            Box(Modifier.consumeWindowInsets(detailInsets)) { detailPane() }
            Box(Modifier.background(dividerColor))
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val (gapStart, gapEnd) = paneGap(
            hinge = hinge?.translate(-origin.x, -origin.y),
            sideBySide = sideBySide,
            extent = if (sideBySide) width else height,
            minListPx = 320.dp.roundToPx()
        )
        val (list, detail, divider) = measurables
        // A flat fold has no physical gap, so draw a hairline where the panes meet.
        val dividerThickness = if (gapEnd > gapStart) 0 else 1.dp.roundToPx()

        if (sideBySide) {
            val listPlaceable = list.measure(Constraints.fixed(gapStart, height))
            val detailPlaceable = detail.measure(Constraints.fixed(width - gapEnd, height))
            val dividerPlaceable = divider.measure(Constraints.fixed(dividerThickness, height))
            layout(width, height) {
                // Physical placement: the hinge does not move for right-to-left locales.
                listPlaceable.place(0, 0)
                detailPlaceable.place(gapEnd, 0)
                dividerPlaceable.place(gapStart, 0, zIndex = 1f)
            }
        } else {
            // Tabletop: the family view faces the user on the upper half and the index, which
            // is tapped, sits on the lower half resting on the table.
            val detailPlaceable = detail.measure(Constraints.fixed(width, gapStart))
            val listPlaceable = list.measure(Constraints.fixed(width, height - gapEnd))
            val dividerPlaceable = divider.measure(Constraints.fixed(width, dividerThickness))
            layout(width, height) {
                detailPlaceable.place(0, 0)
                listPlaceable.place(0, gapEnd)
                dividerPlaceable.place(0, gapStart, zIndex = 1f)
            }
        }
    }
}

/** Returns where the first pane ends and the second begins, along the split axis. */
private fun paneGap(hinge: Rect?, sideBySide: Boolean, extent: Int, minListPx: Int): Pair<Int, Int> {
    if (hinge != null) {
        val start = (if (sideBySide) hinge.left else hinge.top).roundToInt().coerceIn(0, extent)
        val end = (if (sideBySide) hinge.right else hinge.bottom).roundToInt().coerceIn(start, extent)
        return start to end
    }
    if (!sideBySide) return extent / 2 to extent / 2
    val split = maxOf((extent * 0.4f).roundToInt(), minOf(minListPx, extent / 2))
    return split to split
}
