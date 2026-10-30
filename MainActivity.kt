package com.example.medtracker

import android.Manifest
import android.app.*
import android.os.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.view.*
import android.widget.*
import android.text.InputType
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity: Activity() {
    private lateinit var store: Store
    private lateinit var root: LinearLayout
    private val pad=18
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); store=Store(this); window.statusBarColor=0xff123f4a.toInt(); showToday(); if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),100) }
    private fun text(s:String, size:Float=16f, bold:Boolean=false): TextView = TextView(this).apply { text=s; textSize=size; setTextColor(0xff17313a.toInt()); if(bold) setTypeface(null,Typeface.BOLD); setPadding(0,8,0,8) }
    private fun button(s:String, action:()->Unit): Button = Button(this).apply { text=s; setOnClickListener { action() } }
    private fun screen(title:String) { root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad); setBackgroundColor(0xfff6f9f8.toInt()) }; val scroll=ScrollView(this); scroll.addView(root); setContentView(scroll); root.addView(text(title,27f,true)); val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }; listOf("Today" to {showToday()},"Calendar" to {showCalendar()},"Add med" to {editMedicine(null)}).forEach { (name,fn)->nav.addView(button(name,fn),LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)) }; root.addView(nav) }
    private fun section(): LinearLayout { val l=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,12,16,12); setBackgroundColor(0xffffffff.toInt()) }; val params=LinearLayout.LayoutParams(-1,-2).apply { topMargin=14 }; root.addView(l,params); return l }
    private fun showToday() { screen("Medication tracker"); val meds=store.medicines(); if(meds.isEmpty()) root.addView(text("No medications yet. Tap Add med to get started.")); for(m in meds) { val l=section(); l.addView(text(m.name,21f,true)); l.addView(text("${m.strength} • ${m.dose} • Min gap ${m.gapHours}h • Max ${m.maxPer24h} doses/24h",14f)); l.addView(text("Reminder times: ${m.times.joinToString().ifBlank { "None (as needed)" }}",14f)); l.addView(text("Last taken: ${store.last(m)?.let { Store.format(it.at) } ?: "Not recorded"}",14f)); val warning=store.safety(m); l.addView(text(warning ?: "Eligible to log a dose",14f,true)); l.addView(button("MARK TAKEN NOW") { if(warning!=null) AlertDialog.Builder(this).setTitle("Check dose timing").setMessage("$warning\n\nLog anyway? Only do this if instructed by your clinician or correcting the record.").setNegativeButton("Cancel",null).setPositiveButton("Log anyway") { _,_-> log(m) }.show() else log(m) }); l.addView(button("Edit / Delete") { editMedicine(m) }) } }
    private fun log(m:Medicine) { store.addDose(m.id); Reminders.scheduleAll(this); showToday(); Toast.makeText(this,"Dose recorded",Toast.LENGTH_SHORT).show() }
    private fun field(label:String, initial:String="", numeric:Boolean=false): EditText { root.addView(text(label,14f,true)); return EditText(this).apply { setSingleLine(true); setText(initial); if(numeric) inputType=InputType.TYPE_CLASS_NUMBER; root.addView(this) } }
    private fun editMedicine(existing:Medicine?) { screen(if(existing==null) "Add medication" else "Edit medication"); val name=field("Medication name",existing?.name?:""); val strength=field("Strength (e.g. 500 mg per tablet)",existing?.strength?:""); val dose=field("Dose to take (e.g. 2 tablets)",existing?.dose?:""); val times=field("Daily reminder times, 24-hour format (e.g. 08:00, 20:00). Blank for as-needed.",existing?.times?.joinToString(", ")?:""); val gap=field("Minimum hours between doses",existing?.gapHours?.toString()?:"6",true); val max=field("Maximum doses in any 24 hours",existing?.maxPer24h?.toString()?:"4",true)
        root.addView(button("SAVE MEDICATION") { val n=name.text.toString().trim(); val d=dose.text.toString().trim(); val g=gap.text.toString().toIntOrNull(); val mx=max.text.toString().toIntOrNull(); val slots=times.text.toString().split(",").map { it.trim() }.filter { it.isNotBlank() }; val valid=slots.all { try { LocalTime.parse(it,DateTimeFormatter.ofPattern("HH:mm")); true } catch (_:Exception) { false } }; if(n.isBlank()||d.isBlank()||g==null||g<1||g>168||mx==null||mx<1||mx>48||!valid) { Toast.makeText(this,"Check name, dose, hours, daily maximum and HH:mm times",Toast.LENGTH_LONG).show(); return@button }; val med=Medicine(existing?.id?:UUID.randomUUID().toString(),n,strength.text.toString().trim(),d,slots,g,mx); store.saveMedicine(med); Reminders.scheduleAll(this); showToday() })
        if(existing!=null) root.addView(button("DELETE MEDICATION") { AlertDialog.Builder(this).setTitle("Delete ${existing.name}?").setMessage("This also deletes its recorded history.").setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_->store.deleteMedicine(existing.id); Reminders.scheduleAll(this); showToday() }.show() })
    }
    private fun showCalendar() { screen("Dose history"); val today=LocalDate.now(); showDay(today); root.addView(button("CHOOSE DATE") { DatePickerDialog(this,{ _,year,month,day->showCalendarDate(LocalDate.of(year,month+1,day)) },today.year,today.monthValue-1,today.dayOfMonth).show() }) }
    private fun showCalendarDate(date:LocalDate) { screen("Dose history"); showDay(date); root.addView(button("CHOOSE DATE") { DatePickerDialog(this,{ _,y,m,d->showCalendarDate(LocalDate.of(y,m+1,d)) },date.year,date.monthValue-1,date.dayOfMonth).show() }) }
    private fun showDay(date:LocalDate) { root.addView(text(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")),19f,true)); val meds=store.medicines().associateBy { it.id }; val doses=store.doses().filter { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate()==date }.sortedBy { it.at }; if(doses.isEmpty()) root.addView(text("No doses recorded for this day.")); for(d in doses) { val l=section(); l.addView(text("${meds[d.medicineId]?.name ?: "Deleted medication"} • ${Store.format(d.at)}",16f,true)); l.addView(button("Remove incorrect entry") { AlertDialog.Builder(this).setTitle("Remove dose record?").setNegativeButton("Cancel",null).setPositiveButton("Remove") { _,_->store.removeDose(d.id); showCalendarDate(date) }.show() }) } }
}
