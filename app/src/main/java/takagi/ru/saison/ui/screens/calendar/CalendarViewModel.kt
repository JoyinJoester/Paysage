package takagi.ru.saison.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.repository.EventRepository
import takagi.ru.saison.domain.model.Event
import takagi.ru.saison.domain.model.WeekPattern
import takagi.ru.saison.domain.repository.CourseSettingsRepository
import takagi.ru.saison.util.WeekCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository,
    private val courseSettingsRepository: CourseSettingsRepository,
    private val weekCalculator: WeekCalculator
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val courses: StateFlow<List<Course>> = combine(
        _selectedDate,
        courseSettingsRepository.getSettings()
    ) { date, settings ->
        Pair(date, settings)
    }.flatMapLatest { (date, settings) ->
        courseRepository.getCoursesByDay(date.dayOfWeek)
            .map { rawCourses ->
                // Calculate week number (use current date as fallback if semesterStartDate is null)
                val weekNumber = weekCalculator.calculateCurrentWeek(
                    settings.semesterStartDate ?: LocalDate.now(), 
                    date
                )
                
                // Filter by date range AND week pattern
                val activeCourses = rawCourses.filter { course ->
                    val isDateInRange = !date.isBefore(course.startDate) && !date.isAfter(course.endDate)
                    
                    val isWeekMatch = when (course.weekPattern) {
                        WeekPattern.ALL -> true
                        WeekPattern.ODD -> weekNumber % 2 != 0
                        WeekPattern.EVEN -> weekNumber % 2 == 0
                        WeekPattern.A -> weekNumber % 2 != 0 // Treat A as Odd
                        WeekPattern.B -> weekNumber % 2 == 0 // Treat B as Even
                        WeekPattern.CUSTOM -> course.customWeeks?.contains(weekNumber) == true
                    }
                    
                    isDateInRange && isWeekMatch
                }
                mergeAdjacentCourses(activeCourses)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun mergeAdjacentCourses(courses: List<Course>): List<Course> {
        if (courses.isEmpty()) return emptyList()
        
        // Sort by start time
        val sorted = courses.sortedWith(compareBy({ it.startTime }, { it.name }))
        val merged = mutableListOf<Course>()
        
        var current = sorted[0]
        
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            
            // Check if same course (name, location, instructor)
            val isSameCourse = current.name == next.name && 
                               current.location == next.location && 
                               current.instructor == next.instructor
            
            if (isSameCourse) {
                // Check adjacency
                // Case 1: Using periods (if available)
                val isPeriodAdjacent = if (current.periodEnd != null && next.periodStart != null) {
                    current.periodEnd!! + 1 == next.periodStart!!
                } else false
                
                // Case 2: Using time (gap less than 30 mins)
                // Calculate minutes between current.endTime and next.startTime
                val currentEndMinutes = current.endTime.hour * 60 + current.endTime.minute
                val nextStartMinutes = next.startTime.hour * 60 + next.startTime.minute
                val gapMinutes = nextStartMinutes - currentEndMinutes
                
                val isTimeAdjacent = gapMinutes in 0..30 // Allow up to 30 min break
                
                if (isPeriodAdjacent || isTimeAdjacent) {
                    // Merge: extend current course to end of next course
                    current = current.copy(
                        endTime = next.endTime,
                        periodEnd = next.periodEnd
                    )
                    continue
                }
            }
            
            merged.add(current)
            current = next
        }
        merged.add(current)
        
        return merged
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        taskRepository.getTasksByDateRange(startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<Event>> = _selectedDate.flatMapLatest { date ->
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        eventRepository.getEventsByDateRange(startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val daysWithCourses: StateFlow<Set<java.time.DayOfWeek>> = courseRepository.getAllCourses()
        .map { courses -> courses.map { it.dayOfWeek }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val datesWithTasksOrEvents: StateFlow<Set<LocalDate>> = combine(
        taskRepository.getAllTasks(),
        eventRepository.getAllEvents()
    ) { tasks, events ->
        val taskDates = tasks.mapNotNull { it.dueDate?.toLocalDate() }.toSet()
        val eventDates = events.map { it.eventDate.toLocalDate() }.toSet()
        taskDates + eventDates
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }
}

enum class CalendarViewMode {
    MONTH, WEEK, DAY
}
