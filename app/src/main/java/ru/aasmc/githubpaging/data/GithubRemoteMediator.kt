package ru.aasmc.githubpaging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.db.RepoDatabase
import ru.aasmc.githubpaging.model.Repo

@OptIn(ExperimentalPagingApi::class)
class GithubRemoteMediator(
    private val query: String,
    private val service: GithubService,
    private val repoDatabase: RepoDatabase
): RemoteMediator<Int, Repo>() {

    /**
     * Loads data from the network if we don't have any more data in the database.
     *
     * @param state - [PagingState] that gives us information about the pages that were loaded
     *        before, the most recently accessed index in the list and the [PagingConfig]
     *        we defined when initializing the paging system.
     *
     * @param loadType [LoadType] tells us whether we need to load data at the end (LoadType.APPEND)
     *        or at the beginning of the data (LoadType.PREPEND) that we previously loaded,
     *        or if this is the first time we're loading data (LoadType.REFRESH).
     *
     * @return [MediatorResult] object that can either be:
     *        - Error - if we gon an error while requesting data from the network
     *        - Success - if all went well. Here we also need to pass in a signal that
     *        tells whether more data can be loaded or not. E.g. if the network response was successful
     *        but we got an empty list of repos, it means there's no more data to be loaded.
     */
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
         TODO()
    }
}




























