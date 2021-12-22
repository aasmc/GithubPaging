package ru.aasmc.githubpaging.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.aasmc.githubpaging.Injection
import ru.aasmc.githubpaging.databinding.ActivitySearchRepositoriesBinding
import ru.aasmc.githubpaging.model.Repo

class SearchRepositoriesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySearchRepositoriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val viewModel = ViewModelProvider(this, Injection.provideViewModelFactory(this))
            .get(SearchRepositoriesViewModel::class.java)

        // add dividers between RecyclerView's row items
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.list.addItemDecoration(decoration)
        binding.bindState(
            uiState = viewModel.state,
            pagingData = viewModel.pagingDataFlow,
            uiActions = viewModel.accept
        )
    }

    /**
     * Binds the UiState provided by the [SearchRepositoriesViewModel] to the UI,
     * and allows the UI to feed back user actions to it.
     */
    private fun ActivitySearchRepositoriesBinding.bindState(
        uiState: StateFlow<UiState>,
        pagingData: Flow<PagingData<Repo>>,
        uiActions: (UiAction) -> Unit
    ) {
        val repoAdapter = ReposAdapter()
        list.adapter = repoAdapter
            .withLoadStateHeaderAndFooter(
                header = ReposLoadStateAdapter { repoAdapter.retry() },
                footer = ReposLoadStateAdapter { repoAdapter.retry() }
            )
        bindSearch(
            uiState = uiState,
            onQueryChanged = uiActions
        )

        bindList(
            reposAdapter = repoAdapter,
            uiState = uiState,
            pagingData = pagingData,
            onScrollChanged = uiActions
        )
    }

    private fun ActivitySearchRepositoriesBinding.bindSearch(
        uiState: StateFlow<UiState>,
        onQueryChanged: (UiAction.Search) -> Unit
    ) {
        searchRepo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }

        searchRepo.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiState
                    .map { it.query }
                    .distinctUntilChanged()
                    .collect(searchRepo::setText)
            }
        }
    }

    private fun ActivitySearchRepositoriesBinding.updateRepoListFromInput(
        onQueryChanged: (UiAction.Search) -> Unit
    ) {
        searchRepo.text.trim().let {
            if (it.isNotEmpty()) {
                onQueryChanged(
                    UiAction.Search(
                        query = it.toString()
                    )
                )
            }
        }
    }

    private fun ActivitySearchRepositoriesBinding.bindList(
        reposAdapter: ReposAdapter,
        uiState: StateFlow<UiState>,
        pagingData: Flow<PagingData<Repo>>,
        onScrollChanged: (UiAction.Scroll) -> Unit
    ) {
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            // need the listener to know if the user has scrolled for the current query
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) {
                    onScrollChanged(
                        UiAction.Scroll(
                            currentQuery = uiState.value.query
                        )
                    )
                }
            }
        })
        // the type of LoadStateFlow is a CombinedLoadState that allows us to get the load
        // state for the three different types of load operations:
        // - refresh - loading PagingData for the first time
        // - prepend - loading data at the start of the list
        // - append - loading data at the end of the list
        val notLoading = reposAdapter.loadStateFlow
            // only emit when REFRESH LoadState for the paging source changes
            .distinctUntilChangedBy { it.source.refresh }
            // only react to cases where REFRESH completes i.e. NotLoading
            .map { it.source.refresh is LoadState.NotLoading }

        val hasNotScrolledForCurrentSearch = uiState
            .map { it.hasNotScrolledForCurrentSearch }
            .distinctUntilChanged()

        val shouldScrollToTop = combine(
            notLoading,
            hasNotScrolledForCurrentSearch,
            Boolean::and
        )
            .distinctUntilChanged()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Terminal flow operator that collects the given flow with a provided action.
                // The crucial difference from collect is that when the original flow emits a
                // new value then the action block for the previous value is cancelled.
                // so here we cancel collection on previous pagingData when a new one is emited
                pagingData.collectLatest(reposAdapter::submitData)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                shouldScrollToTop.collect { shouldScroll ->
                    if (shouldScroll) list.scrollToPosition(0)
                }
            }
        }
    }
}



















