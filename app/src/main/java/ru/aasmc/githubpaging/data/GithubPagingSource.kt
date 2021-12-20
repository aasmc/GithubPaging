package ru.aasmc.githubpaging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.api.IN_QUALIFIER
import ru.aasmc.githubpaging.data.GithubRepository.Companion.NETWORK_PAGE_SIZE
import ru.aasmc.githubpaging.model.Repo
import java.io.IOException

private const val GITHUB_STARTING_PAGE_INDEX = 1

class GithubPagingSource(
    private val service: GithubService,
    private val query: String
) : PagingSource<Int, Repo>() {

    /**
     * This function is called by the Paging library to asynchronously fetch more data
     * to be displayed as the user scrolls around.
     *
     * @param holds information related to the load operation, including
     *        - key of the page to be loaded. null when the load is called for the first time.
     *        In this case we need to define the initial page key.
     *        - load size - the requested number of items to load.
     * @return [LoadResult] that represents a
     *        - [LoadResult.Page] if successful
     *        - [LoadResult.Error] on error.
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val position = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + IN_QUALIFIER
        return try {
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val repos = response.items
            val nextKey = if (repos.isEmpty()) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we are not requesting duplicating items at the 2nd request
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = repos,
                prevKey = if (position == GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (e: IOException) {
            return LoadResult.Error(e)
        } catch (e: HttpException) {
            return LoadResult.Error(e)
        }
    }

    /**
     * This function is used to get key for subsequent refresh calls
     * to PagingSource.load()
     */
    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        // we need to get the previous key (or the next key if previous is null)
        // of the page that was closest to the most recently accessed index.
        // anchor position is the most recently accessed index.
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}












