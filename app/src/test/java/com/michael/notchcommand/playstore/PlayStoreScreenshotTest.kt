package com.michael.notchcommand.playstore

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.test.core.app.ApplicationProvider
import com.michael.notchcommand.presentation.settings.SettingsScreen
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreScreenshotTest {

  private val app: Application
    get() = ApplicationProvider.getApplicationContext()

  @Test
  @Config(qualifiers = "w360dp-h640dp-xxhdpi")
  fun phone_01_dashboard() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/01_dashboard.png") {
      SettingsScreen(viewModel = viewModel)
    }
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xxhdpi")
  fun phone_02_single_tap() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/02_single_tap.png") {
      SettingsScreen(
        viewModel = viewModel,
        initialSingleExpanded = true,
        scrollState = ScrollState(initial = 2400)
      )
    }
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xxhdpi")
  fun phone_03_double_tap() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/03_double_tap.png") {
      SettingsScreen(
        viewModel = viewModel,
        initialDoubleExpanded = true,
        scrollState = ScrollState(initial = 2400)
      )
    }
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xxhdpi")
  fun phone_04_long_press() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/04_long_press.png") {
      SettingsScreen(
        viewModel = viewModel,
        initialLongExpanded = true,
        scrollState = ScrollState(initial = 2400)
      )
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-xhdpi")
  fun tablet_01_dashboard() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/01_dashboard.png") {
      SettingsScreen(viewModel = viewModel)
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-xhdpi")
  fun tablet_02_single_tap() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/02_single_tap.png") {
      SettingsScreen(
        viewModel = viewModel,
        initialSingleExpanded = true,
        rightScrollState = ScrollState(initial = 600)
      )
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-xhdpi")
  fun tablet_03_double_tap() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/03_double_tap.png") {
      SettingsScreen(
        viewModel = viewModel,
        initialDoubleExpanded = true,
        rightScrollState = ScrollState(initial = 600)
      )
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-xhdpi")
  fun tablet_04_long_press() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/04_long_press.png") {
      SettingsScreen(
        viewModel = viewModel,
        initialLongExpanded = true,
        rightScrollState = ScrollState(initial = 600)
      )
    }
  }
}
