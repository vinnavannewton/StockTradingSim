package com.stock.storage

import com.stock.model.User

expect object DataStore {
    fun save(user: User)
    fun load(defaultBalance: Double): User
    fun reset()
}
