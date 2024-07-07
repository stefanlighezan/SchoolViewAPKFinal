

// CanvasApiService.kt
import Course
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/")
    fun getCourses(
        @Query("page") page: Int
    ): Call<List<Course>>
}
