

// CanvasApiService.kt
import Course
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/api/v1/courses")
    fun getCourses(
        @Query("access_token") access_token: String,
        @Query("enrollment_type") enrollment_type: String = "student",
        @Query("page") page: Int
    ): Call<List<Course>>
}
