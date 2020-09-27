/*
Copyright 2019 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.storage.local

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.BROWSE_PANE_LOCALIZER
import biz.ganttproject.storage.DocumentUri
import biz.ganttproject.storage.StorageMode
import biz.ganttproject.storage.createPath
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import org.controlsfx.validation.ValidationResult
import java.io.File
import java.nio.file.Paths

class LocalStorageState(val currentDocument: Document,
                        val mode: StorageMode) {
  private val currentFilePath = createPath(Paths.get(currentDocument.filePath ?: "/").toFile())

  var confirmationReceived: SimpleBooleanProperty = SimpleBooleanProperty(false).also {
    it.addListener { _, _, _ -> validate() }
  }

  val currentDir: SimpleObjectProperty<File> = SimpleObjectProperty(
      DocumentUri.toFile(absolutePrefix(currentFilePath, currentFilePath.getNameCount() - 1))
  )

  val currentFile: SimpleObjectProperty<File> = SimpleObjectProperty(DocumentUri.toFile(absolutePrefix(currentFilePath)))

  val confirmationRequired: SimpleBooleanProperty = SimpleBooleanProperty(false)

  val canWrite = SimpleBooleanProperty(false)

  var validation = SimpleObjectProperty(ValidationResult())

  private fun validate() {
    LOG.debug(">>> validate: currentFile={} dir={}", currentFile.get()?.name ?: "<no file>", currentDir.get().name)
    val file = this.currentFile.get()
    if (file == null) {
      canWrite.set(false)
      validation.value = ValidationResult.fromWarning(null, i18n.formatText("validation.emptyFileName"))
      LOG.debug("<<< validate")
      return
    }
    try {
      LOG.debug("trying file with mode={}", mode.name)
      mode.tryFile(file)
      LOG.debug("try is ok. confirmation required={} received={}", confirmationRequired.value, confirmationReceived.value)
      validation.value = ValidationResult()
      canWrite.value = !confirmationRequired.value || confirmationReceived.value
    } catch (ex: StorageMode.FileException) {
      canWrite.set(false)
      LOG.debug("bad luck: error={}", ex.message ?: "")
      validation.value = ValidationResult.fromError(null, RootLocalizer.formatText(ex.message!!, *ex.args))
    }
    LOG.debug("<<< validate")
  }

  fun resolveFile(typedString: String): File =
    File(typedString).let {
      if (it.isAbsolute) { it } else File(this.currentDir.get(), typedString)
    }

  @Throws(StorageMode.FileException::class)
  fun trySetFile(typedString: String): File =
    resolveFile(typedString).also {
      mode.tryFile(it)
    }

  fun setCurrentFile(filename: String?)  {
    if (filename.isNullOrBlank()) {
      this.currentFile.set(null)
      validate()
    } else {
      resolveFile(filename).also { this.setCurrentFile(it) }
    }
  }

  fun setCurrentFile(file: File?) {
    if (mode is StorageMode.Save
        && file != null
        && file.exists()
        && currentDocument.uri != FileDocument(file).uri
        && (file != currentFile.get() || !confirmationReceived.get())) {
      confirmationReceived.set(false)
      confirmationRequired.set(true)
    } else {
      confirmationRequired.set(false)
    }
    this.currentFile.set(file)
    validate()
  }
}

private val i18n = RootLocalizer.createWithRootKey("storageService.local", BROWSE_PANE_LOCALIZER)
private val LOG = GPLogger.create("LocalStorage")
