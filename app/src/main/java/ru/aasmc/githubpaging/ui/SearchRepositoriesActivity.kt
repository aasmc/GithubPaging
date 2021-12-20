package ru.aasmc.githubpaging.ui

import android.app.UiAutomation
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.aasmc.githubpaging.Injection
import ru.aasmc.githubpaging.databinding.ActivitySearchRepositoriesBinding
import ru.aasmc.githubpaging.model.RepoSearchResult

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
            uiActions = viewModel.accept
        )
    }

    private fun ActivitySearchRepositoriesBinding.bindState(
        uiState: LiveData<UiState>,
        uiActions: (UiAction) -> Unit
    ) {
        val repoAdapter = ReposAdapter()
        list.adapter = repoAdapter
        bindSearch(
            uiState = uiState,
            onQueryChanged = uiActions
        )

        bindList(
            reposAdapter = repoAdapter,
            uiState = uiState,
            onScrollChanged = uiActions
        )
    }

    private fun ActivitySearchRepositoriesBinding.bindSearch(
        uiState: LiveData<UiState>,
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

        uiState
            .map(UiState::query)
            .distinctUntilChanged()
            .observe(this@SearchRepositoriesActivity, searchRepo::setText)
    }

    private fun ActivitySearchRepositoriesBinding.updateRepoListFromInput(
        onQueryChanged: (UiAction.Search) -> Unit
    ) {
        searchRepo.text.trim().let {
            if (it.isNotEmpty()) {
                list.scrollToPosition(0)
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
        uiState: LiveData<UiState>,
        onScrollChanged: (UiAction.Scroll) -> Unit
    ) {
        setupScrollListener(onScrollChanged)

        uiState
            .map(UiState::searchResult)
            .distinctUntilChanged()
            .observe(this@SearchRepositoriesActivity) { result ->
                when (result) {
                    is RepoSearchResult.Success -> {
                        showEmptyList(result.data.isEmpty())
                        reposAdapter.submitList(result.data)
                    }
                    is RepoSearchResult.Error -> {
                        Toast.makeText(
                            this@SearchRepositoriesActivity,
                            "\uD83D\uDE28 Wooops ${result.error}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun ActivitySearchRepositoriesBinding.showEmptyList(show: Boolean) {
        emptyList.isVisible = show
        list.isVisible = !show
    }

    private fun ActivitySearchRepositoriesBinding.setupScrollListener(
        onScrollChanged: (UiAction.Scroll) -> Unit
    ) {
        val layoutManager = list.layoutManager as LinearLayoutManager
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val visibleItemCount = layoutManager.childCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                onScrollChanged(
                    UiAction.Scroll(
                        visibleItemCount = visibleItemCount,
                        lastVisibleItemPosition = lastVisibleItem,
                        totalItemCount = totalItemCount
                    )
                )
            }
        })
    }
}