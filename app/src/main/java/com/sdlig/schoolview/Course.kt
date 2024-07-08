import java.text.SimpleDateFormat
import java.util.*

data class Course(
    val id: Int?,
    val name: String?,
    val created_at: String?
) {
    fun isNull(): Boolean {
        return id == null || name == null || created_at == null
    }

    fun isOutdated(): Boolean {
        if (created_at == null) {
            return true  // If created_at is null, consider it outdated
        }

        // Current date
        val currentDate = Calendar.getInstance().time

        // Parse created_at string to Date object
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val createdAtDate = sdf.parse(created_at)

        // Calculate one year from createdAtDate, setting the month to June
        val oneYearFromCreatedAt = Calendar.getInstance()
        oneYearFromCreatedAt.time = createdAtDate
        oneYearFromCreatedAt.add(Calendar.YEAR, 1)
        oneYearFromCreatedAt.set(Calendar.MONTH, Calendar.JUNE)

        // Compare currentDate with oneYearFromCreatedAt
        return currentDate.after(oneYearFromCreatedAt.time)
    }
}
