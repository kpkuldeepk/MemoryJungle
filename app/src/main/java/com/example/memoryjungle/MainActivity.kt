package com.example.memoryjungle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private val Purple = Color(0xFF6545E8)
private val PurpleDark = Color(0xFF4F2DD2)
private val Lavender = Color(0xFFF4F1FF)
private val Ink = Color(0xFF252238)
private val Muted = Color(0xFF777187)
private val Green = Color(0xFF38B878)
private val Yellow = Color(0xFFFFC857)
private val Pink = Color(0xFFFF6B8A)
private val White = Color.White

data class MemoryCard(
    val id: Int,
    val emoji: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

enum class Difficulty(
    val title: String,
    val pairs: Int,
    val columns: Int,
    val subtitle: String
) {
    EASY("Easy", 6, 3, "Little Explorer"),
    MEDIUM("Medium", 8, 4, "Jungle Ranger"),
    HARD("Hard", 10, 4, "Memory Master")
}

private val animals = listOf(
    "🦁", "🐼", "🐸", "🐯", "🐵",
    "🦊", "🐨", "🐰", "🦄", "🐙",
    "🐳", "🦋", "🐥", "🐢", "🦖"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemoryJungleApp()
                }
            }
        }
    }
}

@Composable
fun MemoryJungleApp() {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var cards by remember { mutableStateOf(createDeck(difficulty)) }
    var moves by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun restart(newDifficulty: Difficulty = difficulty) {
        difficulty = newDifficulty
        cards = createDeck(newDifficulty)
        moves = 0
        elapsedSeconds = 0
        isBusy = false
        gameStarted = false
        showWin = false
    }

    LaunchedEffect(gameStarted, showWin) {
        while (gameStarted && !showWin) {
            delay(1000)
            elapsedSeconds++
        }
    }

    LaunchedEffect(cards) {
        if (cards.isNotEmpty() && cards.all { it.isMatched }) {
            showWin = true
        }
    }

    val matchedPairs = cards.count { it.isMatched } / 2
    val totalPairs = difficulty.pairs
    val stars = calculateStars(moves, totalPairs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF5A3ADD),
                        Color(0xFF8066F4),
                        Lavender,
                        Lavender
                    )
                )
            )
    ) {
        FloatingDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Header(
                difficulty = difficulty,
                moves = moves,
                elapsedSeconds = elapsedSeconds,
                onRestart = { restart() }
            )

            GameBody(
                modifier = Modifier.weight(1f),
                difficulty = difficulty,
                cards = cards,
                matchedPairs = matchedPairs,
                totalPairs = totalPairs,
                isBusy = isBusy,
                onCardTap = { card ->
                    if (isBusy || card.isMatched || card.isFaceUp) return@GameBody

                    if (!gameStarted) gameStarted = true

                    val flipped = cards.map {
                        if (it.id == card.id) it.copy(isFaceUp = true) else it
                    }
                    cards = flipped

                    val faceUp = flipped.filter { it.isFaceUp && !it.isMatched }
                    if (faceUp.size == 2) {
                        moves++
                        isBusy = true

                        scope.launch {
                            val first = faceUp[0]
                            val second = faceUp[1]

                            if (first.emoji == second.emoji) {
                                delay(420)
                                cards = cards.map {
                                    if (it.id == first.id || it.id == second.id) {
                                        it.copy(isMatched = true)
                                    } else it
                                }
                            } else {
                                delay(820)
                                cards = cards.map {
                                    if (it.id == first.id || it.id == second.id) {
                                        it.copy(isFaceUp = false)
                                    } else it
                                }
                            }
                            isBusy = false
                        }
                    }
                }
            )

            BottomControls(
                selected = difficulty,
                onDifficultyChange = { restart(it) }
            )
        }

        if (showWin) {
            WinDialog(
                moves = moves,
                seconds = elapsedSeconds,
                stars = stars,
                difficulty = difficulty,
                onPlayAgain = { restart() },
                onNextLevel = {
                    val next = when (difficulty) {
                        Difficulty.EASY -> Difficulty.MEDIUM
                        Difficulty.MEDIUM -> Difficulty.HARD
                        Difficulty.HARD -> Difficulty.EASY
                    }
                    restart(next)
                }
            )
        }
    }
}

@Composable
private fun Header(
    difficulty: Difficulty,
    moves: Int,
    elapsedSeconds: Int,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "MEMORY JUNGLE",
                    color = White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Match the Animals!",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "${difficulty.subtitle} • ${difficulty.title}",
                    color = White.copy(alpha = 0.80f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.18f))
                    .clickable { onRestart() }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("↻", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = "👣",
                label = "MOVES",
                value = moves.toString()
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = "⏱",
                label = "TIME",
                value = formatTime(elapsedSeconds)
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = "🏆",
                label = "BEST",
                value = when (difficulty) {
                    Difficulty.EASY -> "12"
                    Difficulty.MEDIUM -> "18"
                    Difficulty.HARD -> "24"
                }
            )
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(White.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(7.dp))
        Column {
            Text(
                label,
                color = White.copy(alpha = 0.68f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                value,
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun GameBody(
    modifier: Modifier = Modifier,
    difficulty: Difficulty,
    cards: List<MemoryCard>,
    matchedPairs: Int,
    totalPairs: Int,
    isBusy: Boolean,
    onCardTap: (MemoryCard) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 34.dp,
                    topEnd = 34.dp
                )
            )
            .background(Lavender)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Find all matching pairs",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (isBusy) "Great! Look carefully…" else "Tap any card to flip it",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                "$matchedPairs / $totalPairs",
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE8E2FF))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = PurpleDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(12.dp))
        ProgressBar(progress = matchedPairs.toFloat() / totalPairs.toFloat())
        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(difficulty.columns),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cards, key = { it.id }) { card ->
                MemoryCardView(
                    card = card,
                    onClick = { onCardTap(card) }
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(CircleShape)
            .background(Color(0xFFE2DCF6))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(9.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(Pink, Yellow, Green)
                    )
                )
        )
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    val scale by animateFloatAsState(
        targetValue = if (card.isMatched) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "matchScale"
    )

    Card(
        modifier = Modifier
            .aspectRatio(0.82f)
            .graphicsLayer {
                rotationY = rotation
                scaleX = scale
                scaleY = scale
                cameraDistance = 12f * density
            }
            .clickable(enabled = !card.isMatched) { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (card.isMatched) 1.dp else 7.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (rotation <= 90f) {
                        Brush.linearGradient(
                            listOf(Color(0xFF6D4DEA), Color(0xFF4F2DD2))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(White, Color(0xFFF7F4FF))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                CardBack()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    contentAlignment = Alignment.Center
                ) {
                    CardFront(card)
                }
            }
        }
    }
}

@Composable
private fun CardBack() {
    val infinite = rememberInfiniteTransition(label = "cardPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(White.copy(alpha = 0.14f))
                .padding(13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🌿", fontSize = 28.sp)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "?",
            color = White.copy(alpha = 0.90f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun CardFront(card: MemoryCard) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = card.isMatched,
            label = "matchedContent"
        ) { matched ->
            Text(
                text = if (matched) "✨" else card.emoji,
                fontSize = if (matched) 28.sp else 42.sp
            )
        }

        if (card.isMatched) {
            Text(
                card.emoji,
                fontSize = 34.sp
            )
            Text(
                "MATCH!",
                color = Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
private fun BottomControls(
    selected: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Lavender)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { item ->
            val active = selected == item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) Purple else Color(0xFFE9E4F7))
                    .clickable { onDifficultyChange(item) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.title,
                    color = if (active) White else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun WinDialog(
    moves: Int,
    seconds: Int,
    stars: Int,
    difficulty: Difficulty,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = White,
        shape = RoundedCornerShape(30.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 54.sp)
                Text(
                    "Amazing Job!",
                    color = Ink,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "You found every animal pair!",
                    color = Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "★".repeat(stars) + "☆".repeat(3 - stars),
                    color = Yellow,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultItem("👣", moves.toString(), "Moves")
                    ResultItem("⏱", formatTime(seconds), "Time")
                    ResultItem("🌟", difficulty.title, "Level")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNextLevel,
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (difficulty == Difficulty.HARD) "Play Easy Again" else "Next Level",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDE8FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Play Again", color = PurpleDark, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ResultItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(value, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun FloatingDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            "✦",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 24.dp),
            color = White.copy(alpha = 0.25f),
            fontSize = 28.sp
        )
        Text(
            "●",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 110.dp, start = 8.dp),
            color = Yellow.copy(alpha = 0.25f),
            fontSize = 18.sp
        )
    }
}

private fun createDeck(difficulty: Difficulty): List<MemoryCard> {
    val chosen = animals.shuffled().take(difficulty.pairs)
    return (chosen + chosen)
        .shuffled()
        .mapIndexed { index, emoji ->
            MemoryCard(id = index, emoji = emoji)
        }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun calculateStars(moves: Int, pairs: Int): Int {
    if (moves == 0) return 3
    return when {
        moves <= pairs + 2 -> 3
        moves <= pairs * 2 -> 2
        else -> 1
    }
}
