package ru.aasmc.githubpaging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.api.IN_QUALIFIER
import ru.aasmc.githubpaging.db.RemoteKeys
import ru.aasmc.githubpaging.db.RepoDatabase
import ru.aasmc.githubpaging.model.Repo
import java.io.IOException

private const val GITHUB_STARTING_PAGE_INDEX = 1

@OptIn(ExperimentalPagingApi::class)
class GithubRemoteMediator(
    private val query: String,
    private val service: GithubService,
    private val repoDatabase: RepoDatabase
) : RemoteMediator<Int, Repo>() {

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
        // 1. Find out what page we need to load from the network based on the LoadType
        val page: Int = when (loadType) {
            // we load data for the first time or PagingAdapter.refresh() is called
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }
            // need to load data at the beginning of the currently loaded data set
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                // if remoteKeys is null, that means the refresh result is not in the db yet.
                val prevKey = remoteKeys?.prevKey
                if (prevKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                prevKey
            }
            // need to load data at the end of the currently loaded dataset
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                // if the remoteKeys is null, the refresh result is not in the db yet.
                // we can return Success with endOfPaginationReached = false, because
                // Paging till call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its nextKey is null, that means we
                // have reached the end of pagination for append.
                val nextKey = remoteKeys?.nextKey
                if (nextKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                nextKey
            }
        }
        // 2. Trigger network request
        val apiQuery = query + IN_QUALIFIER
        try {
            val apiResponse = service.searchRepos(apiQuery, page, state.config.pageSize)

            val repos = apiResponse.items
            // 3. Once the network request completes, if the received list is not empty
            val endOfPaginationReached = repos.isEmpty()
            repoDatabase.withTransaction {
                // 4. If this is a new query (loadType == REFRESH) then we clear the database
                if (loadType == LoadType.REFRESH) {
                    repoDatabase.remoteKeysDao().clearRemoteKeys()
                    repoDatabase.reposDao().clearRepos()
                }
                // 5. Compute the the RemoteKeys for every Repo
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = repos.map {
                    RemoteKeys(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
                }
                // 6. Save the RemoteKeys and Repos in the database
                repoDatabase.remoteKeysDao().insertAll(keys)
                repoDatabase.reposDao().insertAll(repos)
            }
            // 7. Return MediatorResult.Success(endOfPaginationReached = false)
            // If the list of repos was empty then we return MediatorResult.Success(endOfPaginationReached = true)
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            //    if we get an error requesting data we return MediatorResult.Error
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            return MediatorResult.Error(e)
        }
    }

    /**
     * Returns the [RemoteKeys] of the last page that was retrieved from the network.
     * If the [RemoteKeys] is null, that means the refresh result is not in the
     * database yet.
     */
    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repo>): RemoteKeys? {
        // get the last page that was retrieved that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull() {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.let { repo ->
            repoDatabase.remoteKeysDao().remoteKeysRepoId(repo.id)
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Repo>): RemoteKeys? {
        // get the first page that was retrieved, that contained items.
        // From that first page, get the first item.
        return state.pages.firstOrNull {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.let { repo ->
            repoDatabase.remoteKeysDao().remoteKeysRepoId(repo.id)
        }
    }

    /**
     * The point of reference for loading our data is the state.anchorPosition.
     * If this is the first time load, then the anchorPosition is null.
     * When PagingDataAdapter.refresh() is called, the anchorPosition is the first
     * visible position in the displayed list, so we will need to load the page that contains
     * that specific item.
     *
     *  1. Based on the anchorPosition from the state we can get the closest Repo item to that
     *  position by calling state.closestItemToPosition().
     *
     *  2. Based on the Repo item, we can get the RemoteKeys from the db.
     */
    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, Repo>
    ): RemoteKeys? {
        // the paging library is trying to load data after the anchor position
        // Get the item closes to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                repoDatabase.remoteKeysDao().remoteKeysRepoId(repoId)
            }
        }
    }
}




























