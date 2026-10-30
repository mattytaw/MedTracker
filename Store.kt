package com.example.medtracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.util.UUID

data class Medicine(val id: String, val name: String, val strength: String, val dose: String, val times: List<String>, val gapHours: Int, val maxPer24h: Int)
data class Dose(val id: String, val medicineId: String, val at: Long)

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("medtracker_local", Context.MODE_PRIVATE)
    fun medicines(): List<Medicine> = parse(prefs.getString("medicines", "[]") ?: "[]") { o ->
        Medicine(o.getString("id"), o.getString("name"), o.optString("strength"), o.getString("dose"),
            o.optJSONArray("times")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            o.getInt("gapHours"), o.getInt("maxPer24h"))
    }
    fun doses(): List<Dose> = parse(prefs.getString("doses", "[]") ?: "[]") { o -> Dose(o.getString("id"), o.getString("medicineId"), o.getLong("at")) }
    private fun <T> parse(raw: String, f: (JSONObject)->T): List<T> = try { val a = JSONArray(raw); (0 until a.length()).map { f(a.getJSONObject(it)) } } catch (_: Exception) { emptyList() }
    fun saveMedicine(m: Medicine) { val a=JSONArray(); (medicines().filterNot { it.id==m.id }+m).forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("strength",it.strength).put("dose",it.dose).put("times",JSONArray(it.times)).put("gapHours",it.gapHours).put("maxPer24h",it.maxPer24h)) }; prefs.edit().putString("medicines",a.toString()).apply() }
    fun deleteMedicine(id: String) { val a=JSONArray(); medicines().filterNot { it.id==id }.forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("strength",it.strength).put("dose",it.dose).put("times",JSONArray(it.times)).put("gapHours",it.gapHours).put("maxPer24h",it.maxPer24h)) }; prefs.edit().putString("medicines",a.toString()).apply(); val d=JSONArray(); doses().filterNot { it.medicineId==id }.forEach { d.put(JSONObject().put("id",it.id).put("medicineId",it.medicineId).put("at",it.at)) }; prefs.edit().putString("doses",d.toString()).apply() }
    fun addDose(medicineId: String, at: Long = System.currentTimeMillis()) { val a=JSONArray(prefs.getString("doses","[]")); a.put(JSONObject().put("id",UUID.randomUUID().toString()).put("medicineId",medicineId).put("at",at)); prefs.edit().putString("doses",a.toString()).apply() }
    fun removeDose(id: String) { val a=JSONArray(); doses().filterNot { it.id==id }.forEach { a.put(JSONObject().put("id",it.id).put("medicineId",it.medicineId).put("at",it.at)) }; prefs.edit().putString("doses",a.toString()).apply() }
    fun last(m: Medicine): Dose? = doses().filter { it.medicineId==m.id }.maxByOrNull { it.at }
    fun safety(m: Medicine, now: Long = System.currentTimeMillis()): String? {
        val recent=doses().filter { it.medicineId==m.id && it.at<=now && it.at>now-86_400_000L }
        if(recent.size>=m.maxPer24h) return "Maximum ${m.maxPer24h} doses in a rolling 24 hours reached."
        val last=recent.maxByOrNull { it.at } ?: return null
        val next=last.at + m.gapHours*3_600_000L
        if(now<next) return "Minimum ${m.gapHours}-hour gap. Next eligible at ${format(next)}."
        return null
    }
    companion object { fun format(time: Long): String = java.time.format.DateTimeFormatter.ofPattern("d MMM, h:mm a").format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())) }
}
