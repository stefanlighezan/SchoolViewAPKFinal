import com.sdlig.schoolview.Course
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/v1/courses")
    fun fetchDataFromUrl(
        @Query("access_token") accessToken: String,
        @Query("enrollment_type") enrollmentType: String,
        @Query("page") page: Int
    ): Call<List<Course>>
}
