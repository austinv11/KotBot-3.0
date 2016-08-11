package com.austinv11.kotbot.modules.api.commands

import sx.blah.discord.handle.obj.Permissions
import java.lang.reflect.Method
import java.util.*

/**
 * Use this to declare a command.
 */
open class Command(val description: String,
                   val aliases: Array<String> = emptyArray(),
                   val allowInPrivateChannels: Boolean = true,
                   val allowInPublicChannels: Boolean = true,
                   val expensive: Boolean = false,
                   val requiredPermissions: EnumSet<Permissions> = EnumSet.of(Permissions.SEND_MESSAGES),
                   val approvedUsers: ApprovedUsers = ApprovedUsers.ANYONE) {

    val name: String

    internal val executors: List<Method>
    internal val params: MutableMap<Method, MutableList<Pair<String, String>>> = mutableMapOf()

    init {
        name = this.javaClass.simpleName.removeSuffix("Kt").removeSuffix("Command").toLowerCase()

        executors = this.javaClass.declaredMethods
                .filter { it.isAnnotationPresent(Executor::class.java) }
                .sortedWith(Comparator<Method> { o1, o2 -> //We want to sort it so help messages look nicer
                    if (o1.parameterCount == o2.parameterCount) {
                        val result = o1.name.compareTo(o2.name)
                        if (result == 0) {
                            o1.parameters.forEachIndexed { i, parameter ->
                                if ((parameter.type == String::class.java)
                                        .xor(o2.parameters[i].type == String::class.java)) {
                                    return@Comparator if (parameter.type == String::class.java) 1 else -1 //Sorts string params last to prevent the wrong executor from being used
                                } 
                                
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
                var name: String = it.name
                if (it.isAnnotationPresent(Description::class.java)) {
                    val annotation = it.getDeclaredAnnotation(Description::class.java)
                    description = annotation.description
                    name = annotation.name
                }

                if (it.type.isEnum) {
                    var builder = StringJoiner(", ")
                    it.type.enumConstants.forEach { builder.add(it.toString()) }
                    name += " (${builder.toString()})"
                } else {
                    name += " (${it.type.simpleName})"
                }

                paramList.add(Pair(name, description))
            }

            params[it] = paramList
        }
    }

    /**
     * This is called internally to coerce a string into proper args and execute the command.
     */
    fun _execute(args: String): String? {
        //First attempt to use executors with the same number of args as the split args string
        var split = args.split(" ")

        if (split[0] == "")
            split = emptyList()

        var eligibleExecutors = executors.filter { it.parameterCount == split.size }

        if (eligibleExecutors.size > 0) {
            //Potential executors found
            eligibleExecutors.forEach {
                var completedLoop = true
                val objects: MutableList<Any> = mutableListOf()

                it.parameters.forEachIndexed { i, parameter ->
                    try {
                        if (parameter.type.isEnum) {
                            objects.add(parameter.type.getMethod("valueOf", String::class.java).invoke(null, split[i].toUpperCase()))
                        } else if (parameter.type == String::class.java) {
                            objects.add(split[i])
                        } else {
                            objects.add(parameter.type.kotlin.javaObjectType.getDeclaredMethod("valueOf", String::class.java).invoke(null, split[i]))
                        }
                    } catch (e: Exception) {
                        completedLoop = false
                        return@forEachIndexed
                    }
                }

                if (completedLoop) {
                    val params = objects.toTypedArray()
                    if (params.size == 0)
                        return@_execute it.invoke(this)?.toString()
                    else
                        return@_execute it.invoke(this, *params)?.toString()
                }
            }
        }

        //If a suitable executor was found, this would not be reached
        eligibleExecutors = executors.filter { it.parameterCount < split.size && (it.parameterCount != 0 || split.size == 0) }

        eligibleExecutors.sortedWith(Comparator { o1, o2 ->
            val initialResult = Math.negateExact(o1.parameterCount.compareTo(o2.parameterCount))
            if (initialResult == 0) {
                return@Comparator Math.negateExact(o1.name.compareTo(o2.name))
            } else
                return@Comparator initialResult
        }).forEach {
            var completedLoop = true
            val objects: MutableList<Any> = mutableListOf()
            val lastIndex = it.parameterCount-1
            val paramCount = it.parameterCount

            it.parameters.forEachIndexed { i, parameter ->
                if (i != lastIndex) {
                    try {
                        if (parameter.type.isEnum) {
                            objects.add(parameter.type.getMethod("valueOf", String::class.java).invoke(null, split[i].toUpperCase()))
                        } else if (parameter.type == String::class.java) {
                            objects.add(split[i])
                        } else {
                            objects.add(parameter.type.kotlin.javaObjectType.getDeclaredMethod("valueOf", String::class.java).invoke(null, split[i]))
                        }
                    } catch (e: Exception) {
                        completedLoop = false
                        return@forEachIndexed
                    }
                } else {
                    objects.add(buildString {
                        for (index in i..split.size-1)
                            append(split[index] + " ")
                    }.trimEnd())
                }
            }

            if (completedLoop) {
//                try {
                    val params = objects.toTypedArray()
                    if (params.size == 0)
                        return@_execute it.invoke(this)?.toString()
                    else
                        return@_execute it.invoke(this, *params)?.toString()
//                } catch (e: Exception) {} //FIXME: Remove this hack (Ignore until this is uncommented)
            }
        }

        if (split.size != 0) {
            return@_execute executors.filter { it.parameterCount == 0 }.firstOrNull()?.invoke(this)?.toString()
        }

        throw CommandException("Unable to find a suitable executor for the provided command!")
    }
}

class CommandException(override val message: String) : Exception(message)

enum class ApprovedUsers {
    ANYONE, ADMINISTRATORS, OWNER
}
