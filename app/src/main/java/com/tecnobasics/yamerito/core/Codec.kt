package com.tecnobasics.yamerito.core

import org.json.JSONArray
import org.json.JSONObject

/** Convierte los datos a JSON y de regreso. Tolera campos faltantes para no perder nada al actualizar. */
object Codec {
    private const val VERSION = 1

    fun encode(d: AppData): String = JSONObject()
        .put("v", VERSION)
        .put("subjects", JSONArray().apply {
            d.subjects.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("teacher", it.teacher).put("color", it.color))
            }
        })
        .put("tasks", JSONArray().apply {
            d.tasks.forEach { t ->
                put(
                    JSONObject()
                        .put("id", t.id)
                        .put("subjectId", t.subjectId)
                        .put("title", t.title)
                        .put("notes", t.notes)
                        .put("dueMs", t.dueMs)
                        .put("reminders", t.reminders?.let { ints(it) } ?: JSONObject.NULL)
                        .put("createdMs", t.createdMs)
                        .put("armedMs", t.armedMs)
                        .put("deliveredMs", t.deliveredMs ?: JSONObject.NULL),
                )
            }
        })
        .put("defaultReminders", ints(d.defaultReminders))
        .put("lastCheckMs", d.lastCheckMs)
        .toString()

    fun decode(text: String): AppData {
        val o = JSONObject(text)
        val subjects = o.optJSONArray("subjects").objects().map {
            Subject(
                id = it.getString("id"),
                name = it.optString("name"),
                teacher = it.optString("teacher"),
                color = it.optInt("color", 0),
            )
        }
        val tasks = o.optJSONArray("tasks").objects().map {
            val created = it.optLong("createdMs", 0L)
            Task(
                id = it.getString("id"),
                subjectId = it.optString("subjectId"),
                title = it.optString("title"),
                notes = it.optString("notes"),
                dueMs = it.getLong("dueMs"),
                reminders = if (it.isNull("reminders")) null else it.optJSONArray("reminders").ints(),
                createdMs = created,
                armedMs = it.optLong("armedMs", created),
                deliveredMs = if (it.isNull("deliveredMs")) null else it.optLong("deliveredMs"),
            )
        }
        return AppData(
            subjects = subjects,
            tasks = tasks,
            defaultReminders = if (o.has("defaultReminders")) o.optJSONArray("defaultReminders").ints() else Reminders.DEFAULT,
            lastCheckMs = o.optLong("lastCheckMs", 0L),
        )
    }

    private fun ints(list: List<Int>) = JSONArray().apply { list.forEach { put(it) } }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.ints(): List<Int> =
        if (this == null) emptyList() else (0 until length()).map { optInt(it) }.filter { it > 0 }
}
