package ru.aasmc.githubpaging.data

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan

enum class RemotePresentationState {
    INITIAL, REMOTE_LOADING, SOURCE_LOADING, PRESENTED
}

/**
 * Currently we trigger a scroll to the top of the list on new queries when the source
 * LoadState is NotLoading. We also have to make sure that our newly-added RemoteMediator
 * has a LoadState NotLoading as well. To do this we use the [RemotePresentationState] enum,
 * that summarizes the presentation states of our list as fetched by the Pager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<CombinedLoadStates>.asRemotePresentationState(): Flow<RemotePresentationState> =
    // scan -> Folds the given flow with operation, emitting every intermediate result, including initial value.
    scan(RemotePresentationState.INITIAL) { state, loadState ->
        when (state) {
            RemotePresentationState.INITIAL -> {
                when (loadState.mediator?.refresh) {
                    is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                    else -> state
                }
            }
            RemotePresentationState.REMOTE_LOADING -> {
                when (loadState.source.refresh) {
                    is LoadState.Loading -> RemotePresentationState.SOURCE_LOADING
                    else -> state
                }
            }
            RemotePresentationState.SOURCE_LOADING -> {
                when (loadState.source.refresh) {
                    is LoadState.NotLoading -> RemotePresentationState.PRESENTED
                    else -> state
                }
            }
            RemotePresentationState.PRESENTED -> {
                when (loadState.mediator?.refresh) {
                    is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                    else -> state
                }
            }
        }
    }
        .distinctUntilChanged()