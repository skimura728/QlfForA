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

    init {
        viewModelScope.launch {
            newsArticles.value = newsArticleModel.loadAllNews()
        }
    }

    val uiState: StateFlow<NewsUiState> = newsArticles
        .map { news ->
        if (news == null) {
            NewsUiState.Loading
        } else {
            NewsUiState.Success(newsArticles = news)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewsUiState.Loading
    )
}

sealed interface NewsUiState {
    data object Loading: NewsUiState
    data class Success(
        val newsArticles: CategoryNewsMap
    ): NewsUiState
}