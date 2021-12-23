package ru.aasmc.githubpaging.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.aasmc.githubpaging.data.GithubRepository
import ru.aasmc.githubpaging.model.Repo

class SearchRepositoriesViewModel(
    private val repository: GithubRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * Stream of immutable states representative of the UI.
     */
    val state: StateFlow<UiState>

    val pagingDataFlow: Flow<PagingData<UiModel>>

    val accept: (UiAction) -> Unit

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction>()
        val searches = actionStateFlow
            .filterIsInstance<UiAction.Search>()
            .distinctUntilChanged()
            .onStart { emit(UiAction.Search(query = initialQuery)) }

        val queriesScrolled = actionStateFlow
            .filterIsInstance<UiAction.Scroll>()
            .distinctUntilChanged()
            // this is shared to keep the flow "hot" while caching the last query scrolled,
            // otherwise each flatMapLatest invocation would lose the last query scrolled
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            .onStart { emit(UiAction.Scroll(currentQuery = lastQueryScrolled)) }

        pagingDataFlow = searches
            // each new search query requires a new Pager to be created.
            .flatMapLatest { searchRepo(queryString = it.query) }
            // cache content of the Flow in the coroutine scope and keep it active
            // within the viewModelScope
            .cachedIn(viewModelScope)

        state = combine(
            searches,
            queriesScrolled,
            ::Pair
        ).map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState()
        )

        accept = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }
    }

    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state.value.lastQueryScrolled
        super.onCleared()
    }

    private fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {
        val newResult: Flow<PagingData<UiModel>> = repository.getSearchResultStream(queryString)
            .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
            .map {
                it.insertSeparators{ before, after ->
                    if (after == null) {
                        // we are at the end of the list
                        return@insertSeparators null
                    }
                    if (before == null) {
                        // we are at the beginning of the list
                        return@insertSeparators UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                    }
                    // check between two items
                    if (before.roundedStarCount > after.roundedStarCount) {
                        if (after.roundedStarCount >= 1) {
                            UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                        } else {
                            UiModel.SeparatorItem("< 10.000+ stars")
                        }
                    } else {
                        null
                    }
                }
            }
        return newResult
    }
}

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(val currentQuery: String) : UiAction()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    // tracks if user scrolled after making a current query
    // if so, then we don't need to scroll to the top of the list as soon
    // as the PagingData flow emits another value
    // if the user hasn't scrolled for the current query, we do scroll to the
    // top of the list.
    val hasNotScrolledForCurrentSearch: Boolean = false
)

private const val VISIBLE_THRESHOLD = 5
private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val DEFAULT_QUERY = "Android"
private const val LAST_QUERY_SCROLLED = "last_query_scrolled"

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

/**
 * Rounds up the number of stars that enables us to separate repositories
 * based on 10k stars.
 */
private val UiModel.RepoItem.roundedStarCount: Int
    get() = this.repo.stars / 10_000



















