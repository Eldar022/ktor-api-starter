package com.example

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class Task(
    val id: Int,
    val title: String,
    val completed: Boolean = false
)

@Serializable
data class CreateTaskRequest(val title: String)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null
)

object TaskStore {
    private val nextId = AtomicInteger(1)
    private val tasks = ConcurrentHashMap<Int, Task>()

    fun all(): List<Task> = tasks.values.sortedBy { it.id }

    fun find(id: Int): Task? = tasks[id]

    fun create(title: String): Task {
        val task = Task(id = nextId.getAndIncrement(), title = title)
        tasks[task.id] = task
        return task
    }

    fun delete(id: Int): Boolean = tasks.remove(id) != null

    fun clear() {
        tasks.clear()
        nextId.set(1)
    }
}
