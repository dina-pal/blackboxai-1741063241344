package com.dinapal.busdakho.util

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.minutes

object DateTimeUtil {
    private val defaultTimeZone: ZoneId = ZoneId.systemDefault()
    private const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd"
    private const val DEFAULT_TIME_FORMAT = "HH:mm"
    private const val DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    // Date Formatters
    private val dateFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)
    private val timeFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)

    // Custom Formatters
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    fun getCurrentDateTime(): LocalDateTime = LocalDateTime.now()

    fun getCurrentDate(): LocalDate = LocalDate.now()

    fun getCurrentTime(): LocalTime = LocalTime.now()

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatTime(time: LocalTime): String = time.format(timeFormatter)

    fun formatDateTime(dateTime: LocalDateTime): String = dateTime.format(dateTimeFormatter)

    fun parseDate(dateString: String): LocalDate = LocalDate.parse(dateString, dateFormatter)

    fun parseTime(timeString: String): LocalTime = LocalTime.parse(timeString, timeFormatter)

    fun parseDateTime(dateTimeString: String): LocalDateTime =
        LocalDateTime.parse(dateTimeString, dateTimeFormatter)

    fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

    fun formatShortTime(time: LocalTime): String = time.format(shortTimeFormatter)

    fun formatDayOfWeek(date: LocalDate): String = date.format(dayFormatter)

    fun formatMonthYear(date: LocalDate): String = date.format(monthYearFormatter)

    fun getTimeAgo(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> formatDate(dateTime.toLocalDate())
        }
    }

    fun getEstimatedArrivalTime(
        currentTime: LocalDateTime,
        estimatedMinutes: Int
    ): LocalDateTime = currentTime.plusMinutes(estimatedMinutes.toLong())

    fun formatEstimatedTime(estimatedMinutes: Int): String {
        return when {
            estimatedMinutes < 1 -> "Arriving now"
            estimatedMinutes == 1 -> "1 minute"
            estimatedMinutes < 60 -> "$estimatedMinutes minutes"
            else -> {
                val hours = estimatedMinutes / 60
                val minutes = estimatedMinutes % 60
                when {
                    minutes == 0 -> "$hours hour${if (hours > 1) "s" else ""}"
                    hours == 1 -> "1 hour $minutes min"
                    else -> "$hours hours $minutes min"
                }
            }
        }
    }

    fun isWithinOperatingHours(
        currentTime: LocalTime,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean {
        return if (endTime.isBefore(startTime)) {
            // Service runs overnight
            currentTime.isAfter(startTime) || currentTime.isBefore(endTime)
        } else {
            currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
        }
    }

    fun getNextDepartureTime(
        currentTime: LocalTime,
        frequency: Int // in minutes
    ): LocalTime {
        val minutesUntilNextDeparture = frequency - (currentTime.minute % frequency)
        return currentTime.plusMinutes(minutesUntilNextDeparture.toLong())
    }

    fun getDaySchedule(
        operatingDays: List<DayOfWeek>,
        currentDate: LocalDate = LocalDate.now()
    ): Boolean {
        return operatingDays.contains(currentDate.dayOfWeek)
    }

    fun formatScheduleTime(scheduleTime: LocalTime): String {
        val now = LocalTime.now()
        return when {
            scheduleTime.isBefore(now) -> "Departed"
            scheduleTime == now -> "Departing now"
            else -> {
                val minutes = ChronoUnit.MINUTES.between(now, scheduleTime)
                when {
                    minutes < 60 -> "In $minutes min"
                    else -> scheduleTime.format(shortTimeFormatter)
                }
            }
        }
    }

    fun getWeekDates(date: LocalDate = LocalDate.now()): List<LocalDate> {
        val monday = date.with(DayOfWeek.MONDAY)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun isToday(date: LocalDate): Boolean {
        return date == LocalDate.now()
    }

    fun isTomorrow(date: LocalDate): Boolean {
        return date == LocalDate.now().plusDays(1)
    }

    fun formatRelativeDate(date: LocalDate): String {
        return when {
            isToday(date) -> "Today"
            isTomorrow(date) -> "Tomorrow"
            else -> formatShortDate(date)
        }
    }

    fun calculateDuration(startTime: LocalTime, endTime: LocalTime): Duration {
        val minutes = if (endTime.isBefore(startTime)) {
            ChronoUnit.MINUTES.between(startTime, endTime.plusDays(1))
        } else {
            ChronoUnit.MINUTES.between(startTime, endTime)
        }
        return minutes.minutes
    }

    fun formatDuration(duration: Duration): String {
        val totalMinutes = duration.inWholeMinutes
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours == 0L -> "$minutes min"
            minutes == 0L -> "$hours hr"
            else -> "$hours hr $minutes min"
        }
    }

    enum class TimeOfDay {
        MORNING,
        AFTERNOON,
        EVENING,
        NIGHT
    }

    fun getTimeOfDay(time: LocalTime = LocalTime.now()): TimeOfDay {
        return when (time.hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    fun getGreeting(timeOfDay: TimeOfDay = getTimeOfDay()): String {
        return when (timeOfDay) {
            TimeOfDay.MORNING -> "Good morning"
            TimeOfDay.AFTERNOON -> "Good afternoon"
            TimeOfDay.EVENING -> "Good evening"
            TimeOfDay.NIGHT -> "Good night"
        }
    }
}
