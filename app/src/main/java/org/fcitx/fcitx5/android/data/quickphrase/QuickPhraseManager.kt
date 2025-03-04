package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.errorArg
import java.io.File
import java.io.InputStream

object QuickPhraseManager {

    private val builtinQuickPhraseDir = File(
        DataManager.dataDir, "usr/share/fcitx5/data/quickphrase.d"
    )

    private val customQuickPhraseDir = File(
        appContext.getExternalFilesDir(null)!!, "data/data/quickphrase.d"
    ).also { it.mkdirs() }

    fun listQuickPhrase(): List<QuickPhrase> {
        val builtin = listDir(builtinQuickPhraseDir) { file ->
            BuiltinQuickPhrase(file, File(customQuickPhraseDir, file.name))
        }
        val custom = listDir(customQuickPhraseDir) { file ->
            CustomQuickPhrase(file).takeUnless { cq -> builtin.any { cq.name == it.name } }
        }
        return builtin + custom
    }

    fun newEmpty(name: String): CustomQuickPhrase {
        val file = File(customQuickPhraseDir, "$name.${QuickPhrase.EXT}")
        file.createNewFile()
        return CustomQuickPhrase(file)
    }

    fun importFromFile(file: File): Result<CustomQuickPhrase> {
        if (file.extension != QuickPhrase.EXT)
            errorArg(R.string.exception_quickphrase_filename, file.path)
        // throw away data, only ensuring the format is correct
        return QuickPhraseData.fromLines(file.readLines()).map {
            val dest = File(customQuickPhraseDir, file.name)
            file.copyTo(dest)
            CustomQuickPhrase(dest)
        }
    }

    fun importFromInputStream(stream: InputStream, name: String): Result<CustomQuickPhrase> {
        val tempFile = File(appContext.cacheDir, name)
        tempFile.outputStream().use {
            stream.copyTo(it)
        }
        val new = importFromFile(tempFile)
        tempFile.delete()
        return new
    }


    private fun <T : QuickPhrase> listDir(
        dir: File,
        block: (File) -> T?
    ): List<T> =
        dir.listFiles()
            ?.mapNotNull { file ->
                file.name.takeIf { name ->
                    name.endsWith(".${QuickPhrase.EXT}") || name.endsWith(".${QuickPhrase.EXT}.${QuickPhrase.DISABLE}")
                }
                    ?.let { block(file) }
            } ?: listOf()


}