import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Course(
    val id: Int?,
    val name: String?,
    val account_id: Int,
    val uuid: String,
    val start_at: String?,  // Nullable because it can be null in JSON
    val grading_standard_id: Int?,  // Nullable because it can be null in JSON
    val is_public: Boolean,
    val created_at: String?,
    val course_code: String,
    val default_view: String,
    val root_account_id: Int,
    val enrollment_term_id: Int,
    val license: String,
    val grade_passback_setting: String?,  // Nullable because it can be null in JSON
    val end_at: String?,  // Nullable because it can be null in JSON
    val public_syllabus: Boolean,
    val public_syllabus_to_auth: Boolean,
    val storage_quota_mb: Int,
    val is_public_to_auth_users: Boolean,
    val homeroom_course: Boolean,
    val course_color: String?,  // Nullable because it can be null in JSON
    val friendly_name: String?,  // Nullable because it can be null in JSON
    val apply_assignment_group_weights: Boolean,
    val calendar: CalendarData,
    val time_zone: String,
    val blueprint: Boolean,
    val template: Boolean,
    val enrollments: List<Enrollment>,
    val hide_final_grades: Boolean,
    val workflow_state: String,
    val restrict_enrollments_to_course_dates: Boolean,
    val overridden_course_visibility: String
) {
    fun isNull() = name == null || created_at == null

    fun isOutdated(): Boolean {
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()).toInt()
        val courseStartYear = created_at?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(it)?.let { date ->
                SimpleDateFormat("yyyy", Locale.getDefault()).format(date).toInt()
            }
        }
        return courseStartYear?.let { it != currentYear } ?: false
    }
}

data class CalendarData(
    val ics: String
)

data class Enrollment(
    val type: String,
    val role: String,
    val role_id: Int,
    val user_id: Int,
    val enrollment_state: String,
    val limit_privileges_to_course_section: Boolean
)
