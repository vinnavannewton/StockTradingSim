import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

fun main() {
    val client = createSupabaseClient("https://lrwnpjulgtkcvkrknmsg.supabase.co", "sb_publishable_LEQvPGnrusAM--OYJntoxA_CR4wESMJ") {
        install(Auth)
    }
    println("Supabase Auth installed.")
}
