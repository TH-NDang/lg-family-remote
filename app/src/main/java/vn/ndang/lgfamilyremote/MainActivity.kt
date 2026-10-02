package vn.ndang.lgfamilyremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import vn.ndang.lgfamilyremote.ui.RemoteApp

class MainActivity : ComponentActivity() {
    private val remote: RemoteViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF315ACB), onPrimary = Color.White,
                primaryContainer = Color(0xFFE5ECFF), onPrimaryContainer = Color(0xFF183365),
                surface = Color(0xFFF5F7FB), onSurface = Color(0xFF152540),
                secondaryContainer = Color(0xFFEAF0F7), onSecondaryContainer = Color(0xFF223953)
            )) { RemoteApp(remote) }
        }
    }
    override fun onStart() { super.onStart(); remote.onStart() }
    override fun onStop() { remote.onStop(); super.onStop() }
}
