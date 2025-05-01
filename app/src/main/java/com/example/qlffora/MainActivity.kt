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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlffora.ui.theme.QlfForATheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun NewsArticleElement(
    category: String,
    articles: List<NewsArticle>,
    onClick: (NewsArticle) -> Unit
){
    val context = LocalContext.current
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
                                onTap = { onClick(news) },
                                onDoubleTap = {
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
    onArticleClick: (NewsArticle) -> Unit
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
                            Text(
                                text = article.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            when (summaryUiState) {
                                is SummaryUiState.Loading -> Text("Loading summary...")
                                is SummaryUiState.Success -> Text(
                                    summaryUiState.summary,
                                    maxLines = 3,
                                    style = MaterialTheme.typography.bodySmall
                                )

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
                                onClick = onArticleClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleRoute(viewModel: NewsArticleViewModel = viewModel()){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedArticle by viewModel.selectedArticle.collectAsState()
    val summaryUiState by viewModel.summaryUiState.collectAsState()

    NewsArticleScreen(
        uiState = uiState,
        selectedArticle = selectedArticle,
        summaryUiState = summaryUiState,
        onArticleClick = {viewModel.selectArticleWithSummary(it)}
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QlfForATheme {
                NewsArticleRoute()
            }
        }
    }
}
