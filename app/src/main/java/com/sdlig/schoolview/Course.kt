package com.sdlig.schoolview

import com.google.gson.annotations.SerializedName

data class Course(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("start_at") val startAt: String?, // Nullable if start_at can be null
    @SerializedName("grading_standard_id") val gradingStandardId: Int?, // Nullable if grading_standard_id can be null
    @SerializedName("is_public") val isPublic: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("course_code") val courseCode: String,
    @SerializedName("default_view") val defaultView: String,
    @SerializedName("root_account_id") val rootAccountId: Int,
    @SerializedName("enrollment_term_id") val enrollmentTermId: Int,
    @SerializedName("license") val license: String,
    @SerializedName("grade_passback_setting") val gradePassbackSetting: String?, // Nullable if grade_passback_setting can be null
    @SerializedName("end_at") val endAt: String?, // Nullable if end_at can be null
    @SerializedName("public_syllabus") val publicSyllabus: Boolean,
    @SerializedName("public_syllabus_to_auth") val publicSyllabusToAuth: Boolean,
    @SerializedName("storage_quota_mb") val storageQuotaMb: Int,
    @SerializedName("is_public_to_auth_users") val isPublicToAuthUsers: Boolean,
    @SerializedName("homeroom_course") val homeroomCourse: Boolean,
    @SerializedName("course_color") val courseColor: String?, // Nullable if course_color can be null
    @SerializedName("friendly_name") val friendlyName: String?, // Nullable if friendly_name can be null
    @SerializedName("apply_assignment_group_weights") val applyAssignmentGroupWeights: Boolean,
    @SerializedName("calendar") val calendar: Calendar,
    @SerializedName("time_zone") val timeZone: String,
    @SerializedName("blueprint") val blueprint: Boolean,
    @SerializedName("template") val template: Boolean,
    @SerializedName("enrollments") val enrollments: List<Enrollment>,
    @SerializedName("hide_final_grades") val hideFinalGrades: Boolean,
    @SerializedName("workflow_state") val workflowState: String,
    @SerializedName("restrict_enrollments_to_course_dates") val restrictEnrollmentsToCourseDates: Boolean,
    @SerializedName("overridden_course_visibility") val overriddenCourseVisibility: String // Assuming overridden_course_visibility is always present
)

data class Calendar(
    @SerializedName("ics") val icsUrl: String
)

data class Enrollment(
    @SerializedName("type") val type: String,
    @SerializedName("role") val role: String,
    @SerializedName("role_id") val roleId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("enrollment_state") val enrollmentState: String,
    @SerializedName("limit_privileges_to_course_section") val limitPrivilegesToCourseSection: Boolean
)

