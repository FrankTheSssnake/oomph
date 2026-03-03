package com.example.oomph

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.oomph.ui.theme.OomphTheme
import kotlin.math.absoluteValue

/**
 * Main Activity for Oof.
 * Implements a modern, gesture-driven UI using Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var impactDetector: ImpactDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge support for a modern immersive look
        enableEdgeToEdge()
        
        prefs = getSharedPreferences("oof_prefs", Context.MODE_PRIVATE)
        
        // Setup a local detector for UI amplitude feedback
        impactDetector = ImpactDetector(
            context = this,
            threshold = prefs.getFloat("threshold", 5.0f),
            onImpact = {},
            onAmplitudeUpdate = {}
        )
        
        // Ensure background service is running
        startOofService()

        setContent {
            OomphTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OofAppUI(
                        prefs = prefs,
                        impactDetector = impactDetector
                    )
                }
            }
        }
    }

    private fun startOofService() {
        val intent = Intent(this, OofService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        impactDetector.start()
    }

    override fun onPause() {
        super.onPause()
        impactDetector.stop()
    }
}

@Composable
fun OofAppUI(prefs: SharedPreferences, impactDetector: ImpactDetector) {
    var threshold by remember { mutableFloatStateOf(prefs.getFloat("threshold", 5.0f)) }
    var volume by remember { mutableFloatStateOf(prefs.getFloat("volume", 1.0f)) }
    val initialMode = prefs.getString("mode", "pain") ?: "pain"
    
    // Mode data with names, IDs, and placeholder icons
    // To finish the UI, replace imageRes with actual high-quality assets in res/drawable
    val modes = listOf(
        ModeItem("PAIN", "pain", android.R.drawable.ic_dialog_alert),
        ModeItem("SEXY", "sexy", android.R.drawable.ic_menu_gallery),
        ModeItem("HALO", "halo", android.R.drawable.btn_star_big_on)
    )
    
    val initialPageIndex = modes.indexOfFirst { it.id == initialMode }.coerceAtLeast(0)
    val pagerState = rememberPagerState(pageCount = { modes.size }, initialPage = initialPageIndex)

    // Save mode selection to SharedPreferences when the carousel changes
    LaunchedEffect(pagerState.currentPage) {
        prefs.edit().putString("mode", modes[pagerState.currentPage].id).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OOF",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Mode Carousel using HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 16.dp
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            
            ModeCard(
                mode = modes[page],
                modifier = Modifier
                    .graphicsLayer {
                        // Smooth scaling and alpha animation for the carousel effect
                        val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.4f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                    }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Controls Area with Custom Vertical Sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            VerticalSlider(
                label = "SENSITIVITY",
                value = threshold,
                onValueChange = { 
                    threshold = it
                    prefs.edit().putFloat("threshold", it).apply()
                    impactDetector.setThreshold(it)
                },
                valueRange = 1f..25f,
                displayValue = "%.1fg".format(threshold)
            )

            VerticalSlider(
                label = "VOLUME",
                value = volume,
                onValueChange = { 
                    volume = it
                    prefs.edit().putFloat("volume", it).apply()
                },
                valueRange = 0f..1f,
                displayValue = "${(volume * 100).toInt()}%"
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

data class ModeItem(val name: String, val id: String, val imageRes: Int)

@Composable
fun ModeCard(mode: ModeItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = mode.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Subtle dark gradient overlay to ensure text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 400f
                        )
                    )
            )
            
            Text(
                text = mode.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun VerticalSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom interactive vertical bar component
        Box(
            modifier = Modifier
                .width(80.dp)
                .weight(1f)
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val percentage = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        val newValue = valueRange.start + (valueRange.endInclusive - valueRange.start) * percentage
                        onValueChange(newValue)
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val dragY = change.position.y
                        val percentage = (1f - (dragY / size.height)).coerceIn(0f, 1f)
                        val newValue = valueRange.start + (valueRange.endInclusive - valueRange.start) * percentage
                        onValueChange(newValue)
                    }
                }
        ) {
            val fillRatio = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val animatedFill by animateFloatAsState(targetValue = fillRatio, label = "fill")
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = displayValue,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
