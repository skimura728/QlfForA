package com.example.qlffora

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class NewsArticleViewModel : ViewModel() {
    private val newsArticleModel = NewsArticleModel()
    private val newsArticles: MutableStateFlow<CategoryNewsMap?> =
        MutableStateFlow<CategoryNewsMap?>(emptyMap())
    val selectedArticle = MutableStateFlow<NewsArticle?>(null)
    val summaryUiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    private val _selectedWordMeaning = MutableStateFlow<WordMeaning?>(null)
    val selectedWordMeaning: StateFlow<WordMeaning?> = _selectedWordMeaning
    private val _isLearningMode = MutableStateFlow(false)
    val isLearningMode: StateFlow<Boolean> = _isLearningMode

    private val analytics = Firebase.analytics

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
        val bundle = Bundle().apply {
            putString("title", article.title)
            putString("category", article.category)
        }
        analytics.logEvent("summary_expand", bundle)

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

    fun lookupWordMeaning(word: String) {
        val bundle = Bundle().apply {
            putString("word", word)
        }
        analytics.logEvent("dictionary_open", bundle)

        _selectedWordMeaning.value = WordMeaning(word, "Loading...")

        viewModelScope.launch {
            try {
                val meaning = newsArticleModel.getMeaning(word)
                _selectedWordMeaning.value = WordMeaning(word, meaning)
            } catch (e: Exception) {
                _selectedWordMeaning.value = WordMeaning(word, "Failed to fetch meaning.")
            }
        }
    }

    fun clearSelectedWord() {
        _selectedWordMeaning.value = null
    }

    fun toggleLearningMode() {
        _isLearningMode.value = !_isLearningMode.value
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