package com.stock.storage

import com.stock.model.User

expect object DataStore {
    suspend fun save(user: User)
    suspend fun load(defaultBalance: Double): User
    suspend fun reset()
}
