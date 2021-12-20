# Github Paging
This is an educational app based on the official codelab from Android Developers. 
https://developer.android.com/codelabs/android-paging

The app allows you to search GitHub for repositories whose name or description contains a specific 
word. The list of repositories is displayed in descending order based on the number of stars, 
then alphabetically by name.

The app follows the architecture recommended in the " Guide to app architecture". Here's what you
will find in each package:
- api - Github API calls, using Retrofit.
- data - the repository class, responsible for triggering API requests and caching the responses 
  in memory.
- model - the Repo data model, which is also a table in the Room database; and RepoSearchResult, 
  a class that is used by the UI to observe both search results data and network errors.
- ui - classes related to displaying an Activity with a RecyclerView.

The aim of the project is to study Paging 3 API. 