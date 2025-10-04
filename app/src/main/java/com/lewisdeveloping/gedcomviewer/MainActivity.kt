package com.lewisdeveloping.gedcomviewer

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lewisdeveloping.gedcomviewer.ui.GedcomViewerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val transparent = android.graphics.Color.TRANSPARENT
        val autoStyle = SystemBarStyle.auto(transparent, transparent)
        enableEdgeToEdge(
            statusBarStyle = autoStyle,
            navigationBarStyle = autoStyle
        )
        setContent { GedcomViewerApp() }
    }
}
