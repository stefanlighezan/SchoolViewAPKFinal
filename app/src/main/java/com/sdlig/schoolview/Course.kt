import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Course(
    val id: String,
    val name: String,
    val created_at: String // Adjust the type as per your API response
) {
    fun isNull(): Boolean {
        return id.isEmpty() || name.isEmpty() || created_at.isEmpty()
    }
}
