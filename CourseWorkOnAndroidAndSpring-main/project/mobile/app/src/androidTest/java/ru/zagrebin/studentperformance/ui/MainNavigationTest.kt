package ru.ryabov.studentperformance.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.util.clearTestSession
import ru.ryabov.studentperformance.util.setTestSession

/**
 * UI-тесты навигации по главному экрану и выхода.
 * Перед запуском устанавливается тестовая сессия, чтобы открыть MainActivity.
 */
@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @After
    fun tearDown() {
        clearTestSession()
    }

    @Test
    fun mainScreen_displaysBottomNav_andGroupsFragment() {
        setTestSession()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))
            onView(withId(R.id.contentFrame)).check(matches(isDisplayed()))
            onView(withId(R.id.rvGroups)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun navigation_subjectsTab_showsSubjectsList() {
        setTestSession()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_subjects)).perform(click())
            onView(withId(R.id.rvSubjects)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun navigation_gradesTab_showsGradesFragment() {
        setTestSession()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_grades)).perform(click())
            onView(withId(R.id.rvGrades)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun navigation_profileTab_showsLogoutButton() {
        setTestSession()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_profile)).perform(click())
            onView(withId(R.id.btnLogout)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun logout_opensAuthScreen() {
        setTestSession()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_profile)).perform(click())
            onView(withId(R.id.btnLogout)).perform(click())
            onView(withId(R.id.etLogin)).check(matches(isDisplayed()))
        }
    }
}
