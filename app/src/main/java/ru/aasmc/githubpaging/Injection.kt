package ru.aasmc.githubpaging

import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import ru.aasmc.githubpaging.api.GithubService
import ru.aasmc.githubpaging.data.GithubRepository
import ru.aasmc.githubpaging.ui.ViewModelFactory

object Injection {

    private fun provideGithubRepository(): GithubRepository {
        return GithubRepository(GithubService.create())
    }

    fun provideViewModelFactory(owner: SavedStateRegistryOwner): ViewModelProvider.Factory {
        return ViewModelFactory(owner, provideGithubRepository())
    }

}