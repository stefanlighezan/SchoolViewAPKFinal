import android.icu.text.SimpleDateFormat
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
