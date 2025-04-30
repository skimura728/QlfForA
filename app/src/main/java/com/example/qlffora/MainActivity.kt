package com.example.qlffora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    articles: List<NewsArticle>
){
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
                        .focusable()
                        .background(Color.White)
                        .padding(8.dp)
                        .width(200.dp)
                ) {
                    Text(
                        news.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsArticleScreen(
    uiState: NewsUiState
) {
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(title = {Text("QLF BBC News")})
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F4F4))
                .padding(innerPadding)
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
                        NewsArticleElement(category, articles)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleRoute(viewModel: NewsArticleViewModel = viewModel()){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NewsArticleScreen(
        uiState = uiState
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
