package com.example.signal.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat

object CalendarHelper {

    private const val TAG = "CalendarHelper"

    /**
     * Inserts a meeting event into the device's primary calendar.
     * Returns the event ID on success, or null on failure/no permission.
     */
    fun insertMeetingEvent(
        context: Context,
        title: String,
        description: String,
        startMs: Long,
        durationMs: Long = 60 * 60 * 1000L   // default 1 hour
    ): Long? {
        if (!hasCalendarPermission(context)) {
            Log.w(TAG, "Calendar write permission not granted — skipping event insertion")
            return null
        }

        val calendarId = getPrimaryCalendarId(context) ?: run {
            Log.w(TAG, "No primary calendar found on device")
            return null
        }

        val endMs = startMs + durationMs

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return try {
            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()

            if (eventId != null) {
                // Add a 15-minute reminder
                insertReminder(context, eventId, minutesBefore = 15)
                Log.d(TAG, "Inserted calendar event id=$eventId for '$title'")
            }
            eventId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert calendar event: ${e.message}", e)
            null
        }
    }

    private fun insertReminder(context: Context, eventId: Long, minutesBefore: Int) {
        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
        }
        try {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        } catch (e: Exception) {
            Log.w(TAG, "Could not add reminder: ${e.message}")
        }
    }

    /**
     * Returns the ID of the first writable calendar (primary account preferred).
     */
    private fun getPrimaryCalendarId(context: Context): Long? {
        if (!hasCalendarPermission(context)) return null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        // Select only writable calendars
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, selection, selectionArgs,
                null
            )?.use { cursor ->
                var bestId: Long? = null
                var bestScore = -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val type = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)) ?: ""
                    val isPrimary = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)) == 1
                    
                    var currentScore = 0
                    if (type.lowercase() == "com.google") currentScore += 10
                    if (isPrimary) currentScore += 5
                    
                    if (currentScore > bestScore) {
                        bestScore = currentScore
                        bestId = id
                    }
                }
                
                if (bestId == null) {
                    Log.w(TAG, "No suitable calendar found (Total calendars checked: ${cursor.count})")
                } else {
                    Log.d(TAG, "Selected calendar ID $bestId with score $bestScore")
                }
                
                bestId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query calendars: ${e.message}", e)
            null
        }
    }

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
}
