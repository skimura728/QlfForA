package com.example.qlffora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlffora.ui.theme.QlfForATheme
import coil.compose.AsyncImage
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

@Composable
fun WrappingRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rowConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(rowConstraints) }

        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentWidth = 0
        val maxWidth = constraints.maxWidth
        val spacingPx = spacing.roundToPx()

        for (placeable in placeables) {
            if (currentWidth + placeable.width > maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentWidth = 0
            }
            currentRow.add(placeable)
            currentWidth += placeable.width + spacingPx
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val height = rows.sumOf { row -> row.maxOf { it.height } } + spacingPx * (rows.size - 1)

        layout(maxWidth, height) {
            var yOffset = 0
            for (row in rows) {
                var xOffset = 0
                val rowHeight = row.maxOf { it.height }
                for (placeable in row) {
                    placeable.placeRelative(xOffset, yOffset)
                    xOffset += placeable.width + spacingPx
                }
                yOffset += rowHeight + spacingPx
            }
        }
    }
}

@Composable
fun SelectableTextWords(
    text: String,
    onWordLongPress: (String) -> Unit,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = Int.MAX_VALUE
) {
    val words = text.split(" ")

    WrappingRow(
        modifier = Modifier.fillMaxWidth(),
        spacing = 4.dp
    ) {
        words.forEach { word ->
            Text(
                text = word,
                style = style,
                maxLines = maxLines,
                modifier = Modifier
                    .background(Color.White)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                val clean = word.trim(',', '.', '?', '!', ':', ';')
                                onWordLongPress(clean)
                            }
                        )
                    }
            )
        }
    }
}

@Composable
fun NewsArticleElement(
    category: String,
    articles: List<NewsArticle>,
    onClick: (NewsArticle) -> Unit
){
    val context = LocalContext.current
    val analytics = remember { Firebase.analytics }
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .background(Color.DarkGray)
                .padding(8.dp)
                .fillMaxWidth()
        )

        LazyRow {
            items(articles) { news ->
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .border(2.dp, Color.Transparent)
                        .background(Color.White)
                        .padding(8.dp)
                        .width(200.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val bundle = Bundle().apply {
                                        putString("title", news.title)
                                        putString("category", category)
                                    }
                                    analytics.logEvent("news_tile_tap", bundle)
                                    onClick(news)
                                        },
                                onDoubleTap = {
                                    val bundle = Bundle().apply {
                                        putString("title", news.title)
                                        putString("link", news.link)
                                    }
                                    analytics.logEvent("double_tap_open_detail", bundle)

                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                                    context.startActivity(intent)
                                }
                            )
                        }
                ) {
                    //Thumbnail
                    news.thumbnail?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    }
                    //Title
                    Text(
                        text = news.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        maxLines = 3
                    )
                    //Published Day
                    Text(
                        text = news.published,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsArticleScreen(
    uiState: NewsUiState,
    selectedArticle: NewsArticle?,
    summaryUiState: SummaryUiState,
    wordMeaning: WordMeaning?,
    isLearningMode: Boolean,
    onArticleClick: (NewsArticle) -> Unit,
    onDismissDialog: () -> Unit,
    onWordLongPress: (String) -> Unit,
    onToggleLearningMode: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("QLF BBC News",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(12.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onToggleLearningMode() })
                    }
            ) {
                AnimatedContent(
                    targetState = selectedArticle,
                    label = "summary-transition"
                ) { article ->
                    if (article == null) {
                        Text(
                            text = "Tap an article to see the summary.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    } else {
                        Column {
                            if (isLearningMode) {
                                SelectableTextWords(
                                    text = article.title,
                                    onWordLongPress = onWordLongPress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                            } else {
                                Text(
                                    text = article.title,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            when (summaryUiState) {
                                is SummaryUiState.Loading -> Text("Loading summary...")
                                is SummaryUiState.Success -> {
                                    if (isLearningMode) {
                                        SelectableTextWords(
                                            text = summaryUiState.summary,
                                            onWordLongPress = onWordLongPress,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 3
                                        )
                                    } else {
                                        Text(
                                            summaryUiState.summary,
                                            maxLines = 3,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                is SummaryUiState.Error -> Text(
                                    summaryUiState.message,
                                    color = Color.Red,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SummaryUiState.Idle -> {}
                            }
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF4F4F4))
            ) {
                when (uiState) {
                    is NewsUiState.Loading ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ){
                                CircularProgressIndicator()
                            }
                        }
                    is NewsUiState.Success -> uiState.newsArticles.forEach { (category, articles) ->
                        item {
                            NewsArticleElement(
                                category = category,
                                articles = articles,
                                onClick = onArticleClick
                            )
                        }
                    }
                }
            }
        }
        if (wordMeaning != null) {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text(text = wordMeaning.word)},
                text = { Text(text = wordMeaning.meaning)},
                confirmButton = {
                    TextButton(onClick = onDismissDialog) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun NewsArticleRoute(viewModel: NewsArticleViewModel = viewModel()){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedArticle by viewModel.selectedArticle.collectAsState()
    val summaryUiState by viewModel.summaryUiState.collectAsState()
    val wordMeaning by viewModel.selectedWordMeaning.collectAsState()
    val isLearningMode by viewModel.isLearningMode.collectAsState()

    NewsArticleScreen(
        uiState = uiState,
        selectedArticle = selectedArticle,
        summaryUiState = summaryUiState,
        wordMeaning = wordMeaning,
        isLearningMode = isLearningMode,
        onArticleClick = { viewModel.selectArticleWithSummary(it) },
        onDismissDialog = { viewModel.clearSelectedWord() },
        onWordLongPress = { viewModel.lookupWordMeaning(it) },
        onToggleLearningMode = { viewModel.toggleLearningMode()}
    )
}

class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        enableEdgeToEdge()
        setContent {
            QlfForATheme {
                NewsArticleRoute()
            }
        }
    }
}
