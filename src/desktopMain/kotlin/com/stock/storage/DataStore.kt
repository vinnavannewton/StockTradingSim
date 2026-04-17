package com.stock.storage

import com.stock.model.Transaction
import com.stock.model.User
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter

actual object DataStore {
    private val file: File = File(System.getProperty("user.home"), ".stockflow/userdata.txt")

    actual fun save(user: User) {
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.absolutePath + ".tmp")
            PrintWriter(BufferedWriter(FileWriter(tmp))).use { pw ->
                pw.println("BALANCE=${user.balance}")
                pw.println("INITIAL_BALANCE=${user.initialBalance}")
                pw.println("PORTFOLIO=" + user.portfolio.entries.joinToString(",") { "${it.key}:${it.value}" })
                pw.println("AVG_PRICES=" + user.avgBuyPrice.entries.joinToString(",") { "${it.key}:${it.value}" })
                pw.println("WATCHLIST=" + user.watchlist.joinToString(","))
                user.transactions.forEach { tx ->
                    pw.println("TX=${tx.type.name}|${tx.symbol}|${tx.quantity}|${tx.pricePerShare}|${tx.timestamp}")
                }
            }
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        } catch (_: Exception) {
        }
    }

    actual fun load(defaultBalance: Double): User {
        if (!file.exists()) return User(defaultBalance)

        var balance = defaultBalance
        var initialBalance = defaultBalance
        val portfolio = linkedMapOf<String, Int>()
        val avgPrices = linkedMapOf<String, Double>()
        val watchlist = mutableListOf<String>()
        val transactions = mutableListOf<Transaction>()

        try {
            FileReader(file).buffered().useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("BALANCE=") -> balance = line.substringAfter("=").toDoubleOrNull() ?: balance
                        line.startsWith("INITIAL_BALANCE=") -> initialBalance = line.substringAfter("=").toDoubleOrNull() ?: initialBalance
                        line.startsWith("PORTFOLIO=") -> parseIntMap(line.substringAfter("="), portfolio)
                        line.startsWith("AVG_PRICES=") -> parseDoubleMap(line.substringAfter("="), avgPrices)
                        line.startsWith("WATCHLIST=") -> if (line.substringAfter("=").isNotBlank()) watchlist.addAll(line.substringAfter("=").split(","))
                        line.startsWith("TX=") -> {
                            val parts = line.substringAfter("=").split("|")
                            if (parts.size == 5) {
                                val type = runCatching { Transaction.Type.valueOf(parts[0]) }.getOrNull() ?: return@forEach
                                val symbol = parts[1]
                                val qty = parts[2].toIntOrNull() ?: return@forEach
                                val price = parts[3].toDoubleOrNull() ?: return@forEach
                                val ts = parts[4].toLongOrNull() ?: return@forEach
                                transactions.add(Transaction(type, symbol, qty, price, ts))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return User(defaultBalance)
        }

        return User(balance, initialBalance, portfolio, avgPrices, transactions, watchlist)
    }

    actual fun reset() {
        runCatching { file.delete() }
    }

    private fun parseIntMap(data: String, into: MutableMap<String, Int>) {
        if (data.isBlank()) return
        data.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) into[parts[0]] = parts[1].toIntOrNull() ?: return@forEach
        }
    }

    private fun parseDoubleMap(data: String, into: MutableMap<String, Double>) {
        if (data.isBlank()) return
        data.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) into[parts[0]] = parts[1].toDoubleOrNull() ?: return@forEach
        }
    }
}
