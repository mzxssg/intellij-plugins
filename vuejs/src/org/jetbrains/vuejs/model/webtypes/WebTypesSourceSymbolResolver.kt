// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model.webtypes

import com.intellij.lang.ecmascript6.psi.JSExportAssignment
import com.intellij.lang.ecmascript6.resolve.ES6PsiUtil
import com.intellij.lang.ecmascript6.resolve.JSFileReferencesUtil
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSNamedElement
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.resolve.JSResolveResult
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.vuejs.model.webtypes.json.Source

class WebTypesSourceSymbolResolver(private val context: PsiFile, private val pluginName: String) {

  fun resolve(source: Source): PsiElement? {
    val properties: Map<String, *> = source.getAdditionalProperties()
    val symbolName = properties["symbol"]
    val moduleName = properties["module"]
    val file = properties["file"]
    val offset = properties["offset"]
    if (symbolName is String && moduleName is String?) {
      val modules = if (moduleName != null && moduleName.startsWith("./")) {
        context.parent
          ?.virtualFile
          ?.findFileByRelativePath(moduleName)
          ?.let { context.manager.findFile(it) }
          ?.let { listOf(it) }
        ?: emptyList()
      }
      else
        JSFileReferencesUtil.resolveModuleReference(context, moduleName ?: pluginName)
      for (module in modules) {
        JSResolveResult.resolve(ES6PsiUtil.resolveSymbolInModule(symbolName, context, module as JSElement))
          ?.let { return it }
      }
    }
    else if (file is String && offset is Int) {
      return context.parent
        ?.virtualFile
        ?.findFileByRelativePath(file)
        ?.let { context.manager.findFile(it) }
        ?.findElementAt(offset)
        ?.let {
          PsiTreeUtil.getParentOfType(it, JSClass::class.java, JSObjectLiteralExpression::class.java,
                                      JSExportAssignment::class.java, JSNamedElement::class.java)
        }
    }
    return null
  }

}
