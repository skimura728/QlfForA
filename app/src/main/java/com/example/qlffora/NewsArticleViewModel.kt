package com.example.qlffora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewsArticleViewModel : ViewModel() {
    private val newsArticleModel = NewsArticleModel()
    private val newsArticles: MutableStateFlow<CategoryNewsMap?> =
        MutableStateFlow<CategoryNewsMap?>(emptyMap())
    val selectedArticle = MutableStateFlow<NewsArticle?>(null)
    val summaryUiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)

    init {
        viewModelScope.launch {
            newsArticles.value = newsArticleModel.loadAllNews()
        }
    }

    val uiState: StateFlow<NewsUiState> = newsArticles
        .map { news ->
        if (news == null) NewsUiState.Loading
        else NewsUiState.Success(newsArticles = news)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NewsUiState.Loading
        )

    fun selectArticleWithSummary(article: NewsArticle) {
        selectedArticle.value = article
        summaryUiState.value = SummaryUiState.Loading

        viewModelScope.launch {
            try {
                val summary = newsArticleModel.getSummary(article.link)
                summaryUiState.value = SummaryUiState.Success(summary)
            } catch (e: Exception) {
                summaryUiState.value = SummaryUiState.Error("Failed to load summary.")
            }
        }
    }
}

sealed interface NewsUiState {
    data object Loading: NewsUiState
    data class Success(
        val newsArticles: CategoryNewsMap
    ): NewsUiState
}

sealed interface SummaryUiState {
    data object Idle : SummaryUiState
    data object Loading : SummaryUiState
    data class Success(val summary: String) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}