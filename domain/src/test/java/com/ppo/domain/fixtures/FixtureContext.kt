package com.ppo.domain.fixtures

import com.ppo.domain.builders.CategoryBuilder
import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.builders.UserSettingsBuilder
import com.ppo.domain.model.Category
import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.User
import com.ppo.domain.model.UserSettings
import org.junit.runners.model.FrameworkMethod
import java.time.Instant

object FixtureContext {
    private val currentUser = ThreadLocal<User?>()
    private val currentSettings = ThreadLocal<UserSettings?>()
    private val currentCategory = ThreadLocal<Category?>()
    private val currentTimeRecord = ThreadLocal<TimeRecord?>()

    val user: User
        get() = currentUser.get() ?: TestUserMother.regularUser()

    val settings: UserSettings
        get() = currentSettings.get() ?: TestUserSettingsMother.defaultSettings(user.id)

    val category: Category
        get() = currentCategory.get() ?: TestCategoryMother.userCustomCategory(user.id)

    val timeRecord: TimeRecord
        get() = currentTimeRecord.get() ?: TestTimeRecordMother.completedRecord(user.id, category.id)

    fun setup(method: FrameworkMethod) {
        if (method.getAnnotation(WithAdminUser::class.java) != null) {
            currentUser.set(TestUserMother.adminUser())
        } else if (method.getAnnotation(WithRegularUser::class.java) != null) {
            currentUser.set(TestUserMother.regularUser())
        }

        val strictAnn = method.getAnnotation(WithStrictSettings::class.java)
        if (strictAnn != null) {
            val u = currentUser.get() ?: TestUserMother.regularUser()
            currentUser.set(u)
            val settings = UserSettingsBuilder()
                .withUserId(u.id)
                .withMinDuration(true, strictAnn.minDurationValue, strictAnn.minDurationUnit)
                .withAutoStop(true, strictAnn.autoStopMinutes, DurationUnit.minutes)
                .build()
            currentSettings.set(settings)
        } else if (method.getAnnotation(WithDefaultSettings::class.java) != null) {
            val u = currentUser.get() ?: TestUserMother.regularUser()
            currentUser.set(u)
            currentSettings.set(TestUserSettingsMother.defaultSettings(u.id))
        }

        val baseCatAnn = method.getAnnotation(WithBaseCategory::class.java)
        if (baseCatAnn != null) {
            currentCategory.set(TestCategoryMother.baseWorkCategory())
        } else if (method.getAnnotation(WithUserCategory::class.java) != null) {
            val u = currentUser.get() ?: TestUserMother.regularUser()
            currentUser.set(u)
            currentCategory.set(TestCategoryMother.userCustomCategory(u.id))
        } else {
            val catAnn = method.getAnnotation(WithCustomCategory::class.java)
            if (catAnn != null) {
                val u = currentUser.get() ?: TestUserMother.regularUser()
                val cat = CategoryBuilder()
                    .withUserId(if (catAnn.isDefault) null else u.id)
                    .withName(catAnn.name)
                    .withColorHex(catAnn.colorHex)
                    .apply { if (catAnn.isDefault) asDefault() }
                    .build()
                currentCategory.set(cat)
            }
        }

        val activeRecAnn = method.getAnnotation(WithActiveTimeRecord::class.java)
        if (activeRecAnn != null) {
            val u = currentUser.get() ?: TestUserMother.regularUser()
            val c = currentCategory.get() ?: TestCategoryMother.userCustomCategory(u.id)
            val rec = TimeRecordBuilder()
                .withUserId(u.id)
                .withCategoryId(c.id)
                .withDescription(activeRecAnn.description)
                .asActive()
                .build()
            currentTimeRecord.set(rec)
        }

        val completedRecAnn = method.getAnnotation(WithCompletedTimeRecord::class.java)
        if (completedRecAnn != null) {
            val u = currentUser.get() ?: TestUserMother.regularUser()
            val c = currentCategory.get() ?: TestCategoryMother.userCustomCategory(u.id)
            val start = Instant.now().minusSeconds(completedRecAnn.durationMinutes * 60)
            val end = Instant.now()
            val rec = TimeRecordBuilder()
                .withUserId(u.id)
                .withCategoryId(c.id)
                .withStartTime(start)
                .withEndTime(end)
                .withDescription(completedRecAnn.description)
                .build()
            currentTimeRecord.set(rec)
        }
    }

    fun clear() {
        currentUser.remove()
        currentSettings.remove()
        currentCategory.remove()
        currentTimeRecord.remove()
    }
}
