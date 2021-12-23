# Github Paging
This is an educational app based on the official codelab from Android Developers. 
https://developer.android.com/codelabs/android-paging

The app allows you to search GitHub for repositories whose name or description contains a specific 
word. The list of repositories is displayed in descending order based on the number of stars, 
then alphabetically by name.

The app follows the architecture recommended in the " Guide to app architecture". Here's what you
will find in each package:

## api
- Github API calls, using Retrofit.
 
## data
- Repository class, responsible for triggering API requests and caching the responses in memory.
- RemoteMediator - an intermediary between local database and the network, that fetches data if
there's no more data to show in the local DB. 
- RemotePresentationState - summarizes the presentation states of data sets as fetched by the Pager.
- PagingSource - this class is here to show how to use Paging library without RemoteMediator. Currently
the it is not used. 

## db
Room database and DAOs that handle repositories and RemoteKeys. RemoteKeys is needed to ensure RemoteMediator
properly loads data from the network. 

## model
The Repo data model, which is also a table in the Room database.

## ui
Classes related to displaying an Activity with a RecyclerView.

The aim of the project is to study Paging 3 API.

### Copyright
```text
/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```