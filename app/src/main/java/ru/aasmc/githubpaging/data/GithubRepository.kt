package ru.aasmc.githubpaging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.db.RepoDatabase
import ru.aasmc.githubpaging.model.Repo

/**
 * Repository class that works with local and remote dataSources.
 */
class GithubRepository(
    private val service: GithubService,
    private val database: RepoDatabase
) {
    @OptIn(ExperimentalPagingApi::class)
    fun getSearchResultStream(query: String): Flow<PagingData<Repo>> {
        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { database.reposDao().reposByName(dbQuery) }

        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = GithubRemoteMediator(
                query,
                service,
                database
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}



























