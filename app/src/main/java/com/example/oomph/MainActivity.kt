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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oomph.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var impactDetector: ImpactDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        prefs = getSharedPreferences("oomph_prefs", Context.MODE_PRIVATE)
        
        setupLocalDetector()
        startOomphService()

        setContent {
            OomphTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Bg
                ) {
                    OomphAppUI(
                        prefs = prefs,
                        impactDetector = impactDetector
                    )
                }
            }
        }
    }

    private fun startOomphService() {
        val intent = Intent(this, OomphService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun setupLocalDetector() {
        impactDetector = ImpactDetector(
            context = this,
            threshold = prefs.getFloat("threshold", 5.0f),
            onImpact = {},
            onAmplitudeUpdate = {}
        )
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
fun OomphAppUI(prefs: SharedPreferences, impactDetector: ImpactDetector) {
    var threshold by remember { mutableFloatStateOf(prefs.getFloat("threshold", 5.0f)) }
    var volume by remember { mutableFloatStateOf(prefs.getFloat("volume", 1.0f)) }
    val initialMode = prefs.getString("mode", "pain") ?: "pain"
    
    val modes = listOf(
        ModeItem("SEXY", "sexy", "🔥"),
        ModeItem("PAIN", "pain", "⚡"),
        ModeItem("HALO", "halo", "✦")
    )
    
    val initialPageIndex = modes.indexOfFirst { it.id == initialMode }.coerceAtLeast(0)
    val pagerState = rememberPagerState(pageCount = { modes.size }, initialPage = initialPageIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        prefs.edit().putString("mode", modes[pagerState.currentPage].id).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Machined Device Container
        Column(
            modifier = Modifier
                .width(340.dp)
                .background(Panel, RoundedCornerShape(4.dp))
                .border(1.dp, EdgeLight, RoundedCornerShape(4.dp))
                .drawBehind {
                    // Subtle brushed metal effect
                    for (i in 0 until size.width.toInt() step 3) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.012f),
                            start = Offset(i.toFloat(), 0f),
                            end = Offset(i.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                }
                .padding(vertical = 32.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Header
            Text(
                text = "OOMPH",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                letterSpacing = 12.sp,
                color = TextMain,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Carousel Section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(InsetBg, RoundedCornerShape(3.dp))
                        .border(1.dp, EdgeLight.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(4.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val mode = modes[page]
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(PanelRaised, RoundedCornerShape(3.dp))
                                    .border(1.dp, EdgeLight.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = mode.icon, fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = mode.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 5.sp,
                                color = TextMain
                            )
                        }
                    }
                }

                // Nav Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            scope.launch { 
                                val prev = (pagerState.currentPage - 1 + modes.size) % modes.size
                                pagerState.animateScrollToPage(prev) 
                            } 
                        },
                        modifier = Modifier
                            .size(36.dp, 28.dp)
                            .background(PanelRaised, RoundedCornerShape(2.dp))
                            .border(0.5.dp, EdgeLight, RoundedCornerShape(2.dp))
                    ) {
                        Text("◀", color = LabelColor, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp, 6.dp)
                                    .background(
                                        if (pagerState.currentPage == index) AccentDim else InsetBg,
                                        RoundedCornerShape(1.dp)
                                    )
                                    .border(0.5.dp, EdgeLight, RoundedCornerShape(1.dp))
                            )
                        }
                    }

                    IconButton(
                        onClick = { 
                            scope.launch { 
                                val next = (pagerState.currentPage + 1) % modes.size
                                pagerState.animateScrollToPage(next) 
                            } 
                        },
                        modifier = Modifier
                            .size(36.dp, 28.dp)
                            .background(PanelRaised, RoundedCornerShape(2.dp))
                            .border(0.5.dp, EdgeLight, RoundedCornerShape(2.dp))
                    ) {
                        Text("▶", color = LabelColor, fontSize = 12.sp)
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EdgeDark)
            )

            // Sliders Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MachinedSlider(
                    modifier = Modifier.weight(1f),
                    label = "SENSITIVITY",
                    value = threshold,
                    onValueChange = {
                        threshold = it
                        prefs.edit().putFloat("threshold", it).apply()
                        impactDetector.setThreshold(it)
                    },
                    valueRange = 1f..25f,
                    displayValue = "%.1fg".format(threshold),
                    isAccent = false
                )

                MachinedSlider(
                    modifier = Modifier.weight(1f),
                    label = "VOLUME",
                    value = volume,
                    onValueChange = {
                        volume = it
                        prefs.edit().putFloat("volume", it).apply()
                    },
                    valueRange = 0f..1f,
                    displayValue = "${(volume * 100).toInt()}%",
                    isAccent = true
                )
            }
        }
    }
}

data class ModeItem(val name: String, val id: String, val icon: String)

@Composable
fun MachinedSlider(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    isAccent: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = LabelColor,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(140.dp)
                .background(InsetBg, RoundedCornerShape(2.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(2.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pct = 1f - (offset.y / size.height)
                        onValueChange(valueRange.start + pct * (valueRange.endInclusive - valueRange.start))
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val pct = 1f - (change.position.y / size.height)
                        onValueChange((valueRange.start + pct * (valueRange.endInclusive - valueRange.start)).coerceIn(valueRange))
                    }
                }
        ) {
            val fillRatio = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val animatedFill by animateFloatAsState(targetValue = fillRatio, label = "fill")

            // Ticks
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(7) { i ->
                    Box(
                        modifier = Modifier
                            .width(if (i % 3 == 0) 8.dp else 5.dp)
                            .height(1.dp)
                            .background(if (i % 3 == 0) LabelColor else EdgeLight)
                    )
                }
            }

            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill)
                    .align(Alignment.BottomCenter)
                    .background(if (isAccent) AccentDim else PanelRaised)
                    .border(0.5.dp, if (isAccent) Accent else EdgeLight, RoundedCornerShape(2.dp))
            )

            // Thumb
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(140.dp * animatedFill - 7.dp))
                    .size(30.dp, 14.dp)
                    .background(PanelRaised, RoundedCornerShape(1.dp))
                    .border(0.5.dp, EdgeLight, RoundedCornerShape(1.dp))
                    .drawBehind {
                        // Grip lines
                        for (i in 0 until size.height.toInt() step 3) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.07f),
                                start = Offset(0f, i.toFloat()),
                                end = Offset(size.width, i.toFloat()),
                                strokeWidth = 1f
                            )
                        }
                    }
            )
        }

        Text(
            text = displayValue,
            fontSize = 11.sp,
            color = TextDim,
            fontWeight = FontWeight.Medium
        )
    }
}
