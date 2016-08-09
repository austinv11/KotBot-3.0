package com.austinv11.kotbot

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Administrators: Table() {
    val key = integer("id").autoIncrement().primaryKey()
    val id = text("user_id")
}

object Tags: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val guild_id = text("guild_id")
    val tag_id = text("tag_id")
    val value = text("value")
}

object Bots: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val bot_id = text("bot_id")
    val prefix = text("prefix")
}

val db = Database.connect("jdbc:h2:./database", driver = "org.h2.Driver")

fun initializeDatabase() {
    transaction {
        create(Administrators, Tags)
    }
}
