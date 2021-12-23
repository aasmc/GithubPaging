package ru.aasmc.githubpaging

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.data.GithubRepository
import ru.aasmc.githubpaging.db.RepoDatabase
import ru.aasmc.githubpaging.ui.ViewModelFactory

object Injection {

    private fun provideGithubRepository(context: Context): GithubRepository {
        return GithubRepository(GithubService.create(), RepoDatabase.getInstance(context))
    }

    fun provideViewModelFactory(context: Context, owner: SavedStateRegistryOwner): ViewModelProvider.Factory {
        return ViewModelFactory(owner, provideGithubRepository(context))
    }

}