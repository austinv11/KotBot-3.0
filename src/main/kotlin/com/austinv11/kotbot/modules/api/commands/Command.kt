package com.austinv11.kotbot.modules.api.commands

import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.Permissions
import java.lang.reflect.Method
import java.util.*

/**
 * Use this to declare a command.
 */
open class Command(val aliases: Array<String> = emptyArray(),
                   val description: String,
                   val allowInPrivateChannels: Boolean = true,
                   val allowInPublicChannels: Boolean = true,
                   val expensive: Boolean = false,
                   val requiredPermissions: EnumSet<Permissions> = EnumSet.of(Permissions.SEND_MESSAGES)) {

    val name: String

    private val executors: List<Method>
    private val params: MutableMap<Method, MutableList<Pair<String, String>>> = mutableMapOf()

    init {
        name = this.javaClass.simpleName.removeSuffix("Kt").removeSuffix("Command").toLowerCase()

        executors = this.javaClass.declaredMethods
                .filter { it.isAnnotationPresent(Executor::class.java) }
                .sortedWith(Comparator<Method> { o1, o2 -> //We want to sort it so help messages look nicer
                    if (o1.parameterCount == o2.parameterCount) {
                        val result = o1.name.compareTo(o2.name)
                        if (result == 0) {
                            o1.parameters.forEachIndexed { i, parameter ->
                                val compareResult = parameter.name.compareTo(o2.parameters[i].name)
                                if (compareResult != 0)
                                    return@Comparator compareResult
                            }
                            return@Comparator 0
                        } else {
                            return@Comparator result
                        }
                    } else {
                        return@Comparator o1.parameterCount.compareTo(o2.parameterCount)
                    }
                })

        executors.forEach {
            it.isAccessible = true //Make sure we can access the executors

            val paramList = mutableListOf<Pair<String, String>>() //Parsing params for the help command
            it.parameters.forEach {
                var description: String = ""
                if (it.isAnnotationPresent(Description::class.java)) {
                    description = it.getDeclaredAnnotation(Description::class.java).data
                }

                paramList + Pair(it.name, description)
            }

            params[it] = paramList
        }
    }

    /**
     * This is called internally to coerce a string into proper args and execute the command.
     */
    fun _execute(args: String): String? {
        //First attempt to use executors with the same number of args as the split args string
        val split = args.split(" ")
        var eligibleExecutors = executors.filter { it.parameterCount == split.size }

        if (eligibleExecutors.size > 0) {
            //Potential executors found
            eligibleExecutors.forEach {
                var completedLoop = true
                val objects: MutableList<Any> = mutableListOf()

                it.parameters.forEachIndexed { i, parameter ->
                    try {
                        objects + parameter.type.cast(split[i])
                    } catch (e: ClassCastException) {
                        completedLoop = false
                        return@forEachIndexed
                    }
                }

                if (completedLoop) {
                    return@_execute it.invoke(this, objects.toTypedArray())?.toString()
                }
            }
        }

        //If a suitable executor was found, this would not be reached
        eligibleExecutors = executors.filter { it.parameterCount < split.size && (it.parameterCount != 0 || split.size == 0) }

        eligibleExecutors.forEach {
            var completedLoop = true
            val objects: MutableList<Any> = mutableListOf()
            val lastIndex = it.parameterCount-1
            val paramCount = it.parameterCount

            it.parameters.forEachIndexed { i, parameter ->
                if (i != lastIndex) {
                    try {
                        objects + parameter.type.cast(split[i])
                    } catch (e: ClassCastException) {
                        completedLoop = false
                        return@forEachIndexed
                    }
                } else {
                    objects + buildString {
                        for (index in i..paramCount)
                            append(split[index] + " ")
                    }.trimEnd()
                }
            }

            if (completedLoop) {
                return@_execute it.invoke(this, objects.toTypedArray())?.toString()
            }
        }

        if (split.size != 0) {
            return@_execute executors.filter { it.parameterCount == 0 }.firstOrNull()?.invoke(this)?.toString()
        }

        throw CommandException("Unable to find a suitable executor for the provided command!")
    }
}

class CommandException(override val message: String) : Exception(message)
