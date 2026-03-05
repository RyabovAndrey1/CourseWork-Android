package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность пользователя в локальной БД
 */
@Entity(tableName = "users")
@Parcelize
data class UserEntity(
    @PrimaryKey
    val userId: Long,
    val login: String,
    val email: String,
    val lastName: String,
    val firstName: String,
    val middleName: String?,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) : Parcelable {

    val fullName: String
        get() = "$lastName $firstName${if (middleName != null) " $middleName" else ""}"

    val roleEnum: Role
        get() = Role.valueOf(role)
}

enum class Role {
    ADMIN, TEACHER, STUDENT, DEANERY
}