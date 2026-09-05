package com.efajtahamid.weblab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.efajtahamid.weblab.ui.navigation.WebLabNavGraph
import com.efajtahamid.weblab.ui.theme.WebLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WebLabApp
        setContent {
            WebLabRoot(app)
        }
    }
}

@Composable
private fun WebLabRoot(app: WebLabApp) {
    WebLabTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            WebLabNavGraph(app = app)
        }
    }
}
