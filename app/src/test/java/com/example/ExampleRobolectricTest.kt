package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.archvm.model.ArchPackageRegistry
import com.example.archvm.model.VFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ArchVM", appName)
  }

  @Test
  fun `vfs initializes default arch system directories and files`() {
    val vfs = VFS()
    assertNotNull(vfs.getDir("/etc"))
    assertNotNull(vfs.getDir("/home/user"))
    assertNotNull(vfs.getDir("/usr/bin"))
    assertNotNull(vfs.getFile("/etc/os-release"))
    assertNotNull(vfs.getFile("/home/user/welcome.txt"))
    assertTrue(vfs.getFile("/etc/os-release")?.content?.contains("Arch Linux") == true)
  }

  @Test
  fun `package registry contains core developer distributions`() {
    val catalog = ArchPackageRegistry.getInitialCatalog()
    val names = catalog.map { it.name }
    assertTrue(names.contains("pacman"))
    assertTrue(names.contains("python"))
    assertTrue(names.contains("gcc"))
    assertTrue(names.contains("nodejs"))
    assertTrue(names.contains("rust"))
    assertTrue(names.contains("htop"))
    assertTrue(names.contains("neofetch"))
  }
}
