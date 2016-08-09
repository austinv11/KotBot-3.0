package com.austinv11.kotbot

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Administrators: Table() {
    val id = varchar("id", 17).primaryKey()
    
}

val db = Database.connect("jdbc:h2:./database", driver = "org.h2.Driver")

fun initializeDatabase() {
    transaction {
        create(Administrators)
    }
}
