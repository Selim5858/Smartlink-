package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allLinkButtons: Flow<List<LinkButton>> = appDao.getAllLinkButtons()
    val usersCount: Flow<Int> = appDao.getUsersCount()

    suspend fun insertLinkButton(linkButton: LinkButton) {
        appDao.insertLinkButton(linkButton)
    }

    suspend fun updateLinkButton(linkButton: LinkButton) {
        appDao.updateLinkButton(linkButton)
    }

    suspend fun deleteLinkButton(linkButton: LinkButton) {
        appDao.deleteLinkButton(linkButton)
    }

    suspend fun deleteLinkButtonById(id: Int) {
        appDao.deleteLinkButtonById(id)
    }

    suspend fun registerUser(user: User): Boolean {
        return try {
            appDao.registerUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUser(username: String): User? {
        return appDao.getUser(username)
    }
}
