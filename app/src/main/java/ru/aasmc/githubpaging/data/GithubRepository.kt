package ru.aasmc.githubpaging.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.model.Repo

private const val GITHUB_STARTING_PAGE_INDEX = 1
private const val TAG = "GithubRepository"

/**
 * Repository class that works with local and remote dataSources.
 */
class GithubRepository(
    private val service: GithubService
) {
    fun getSearchResultStream(query: String): Flow<PagingData<Repo>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { GithubPagingSource(service, query) }
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}



























