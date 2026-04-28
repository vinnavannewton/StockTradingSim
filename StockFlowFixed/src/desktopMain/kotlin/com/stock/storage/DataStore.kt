package com.stock.storage

import com.stock.api.SupabaseManager
import com.stock.model.User

actual object DataStore {
    actual suspend fun save(user: User)             = SupabaseManager.saveUser(user)
    actual suspend fun load(defaultBalance: Double) = SupabaseManager.loadUser(defaultBalance)
    actual suspend fun reset()                      = SupabaseManager.resetUser()
}
