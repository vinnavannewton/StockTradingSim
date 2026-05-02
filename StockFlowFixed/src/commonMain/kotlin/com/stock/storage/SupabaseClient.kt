package com.stock.storage

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth

val supabase = createSupabaseClient(
    supabaseUrl = "https://lrwnpjulgtkcvkrknmsg.supabase.co",
    supabaseKey = "sb_publishable_LEQvPGnrusAM--OYJntoxA_CR4wESMJ"
) {
    install(Postgrest)
    install(Auth)
}
