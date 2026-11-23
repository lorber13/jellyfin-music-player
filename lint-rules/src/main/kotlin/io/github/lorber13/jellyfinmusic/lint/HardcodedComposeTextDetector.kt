package io.github.lorber13.jellyfinmusic.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.tryResolve

/**
 * Reports text written directly in Compose code, which translators cannot reach (UI-05).
 *
 * It looks at string arguments of `@Composable` functions, such as `Text("Play")`, and at
 * `contentDescription = "Play"` inside `Modifier.semantics`. Previews are exempt.
 */
class HardcodedComposeTextDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java, UBinaryExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        // Tests may show fixed text on purpose.
        if (context.isTestSource) return null

        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                checkComposableCall(context, node)
            }

            override fun visitBinaryExpression(node: UBinaryExpression) {
                checkSemanticsAssignment(context, node)
            }
        }
    }

    private fun checkComposableCall(context: JavaContext, call: UCallExpression) {
        val function = call.resolve() ?: return
        if (!function.hasAnnotation(COMPOSABLE_ANNOTATION)) return

        for ((argument, parameter) in context.evaluator.computeArgumentMapping(call, function)) {
            if (parameter.type.canonicalText in TEXT_TYPES) {
                reportIfHardcodedText(context, argument)
            }
        }
    }

    private fun checkSemanticsAssignment(context: JavaContext, assignment: UBinaryExpression) {
        if (assignment.operator != UastBinaryOperator.ASSIGN) return

        val target = assignment.leftOperand.skipParenthesizedExprDown() as? USimpleNameReferenceExpression ?: return
        if (target.identifier !in SEMANTICS_TEXT_PROPERTIES) return

        val property = target.tryResolve() as? PsiMember ?: return
        val propertyPackage = property.containingClass?.qualifiedName?.substringBeforeLast('.')
        if (propertyPackage != SEMANTICS_PACKAGE) return

        reportIfHardcodedText(context, assignment.rightOperand)
    }

    private fun reportIfHardcodedText(context: JavaContext, expression: UExpression) {
        if (!isHardcodedText(expression) || isInPreview(expression)) return

        context.report(
            ISSUE,
            expression,
            context.getLocation(expression),
            "Hardcoded text can't be translated: move it to `strings.xml` and use `stringResource`"
        )
    }

    /**
     * True for a string literal, a string template or a concatenation whose fixed parts contain a
     * letter. Text without letters, such as `"·"` or `"42"`, needs no translation.
     */
    private fun isHardcodedText(expression: UExpression): Boolean =
        fixedTextParts(expression).any { part -> part.any(Char::isLetter) }

    /** The string literals that make up [expression], however deeply templates and `+` nest them. */
    private fun fixedTextParts(expression: UExpression): List<String> =
        when (val unwrapped = expression.skipParenthesizedExprDown()) {
            is ULiteralExpression -> listOfNotNull(unwrapped.value as? String)

            // Kotlin wraps even a plain "text" in a template node, so `"a" + b` nests two levels.
            is UPolyadicExpression -> unwrapped.operands.flatMap(::fixedTextParts)

            else -> emptyList()
        }

    /** True inside a function annotated with `@Preview` or with a multipreview annotation. */
    private fun isInPreview(element: UElement): Boolean {
        val function = element.getParentOfType<UMethod>() ?: return false
        return function.uAnnotations.any { annotation ->
            annotation.qualifiedName in PREVIEW_ANNOTATIONS ||
                annotation.resolve()?.let { isMultipreview(it, depth = 1) } == true
        }
    }

    /**
     * A multipreview is an annotation class that carries `@Preview`, either itself or through
     * another multipreview. Compiled ones, such as `@PreviewLightDark`, carry their `@Preview`s
     * wrapped in the `Preview.Container` that Kotlin generates for repeated annotations. The depth
     * limit, as in Slack's compose-lints, stops annotation classes that annotate each other.
     */
    private fun isMultipreview(annotationClass: PsiClass, depth: Int): Boolean {
        if (depth > MAX_MULTIPREVIEW_DEPTH) return false
        // Through UAST, as in Slack's compose-lints: reading the PSI annotations instead missed a
        // multipreview declared in Kotlin source that carries another one.
        val annotations = annotationClass.toUElementOfType<UClass>()?.uAnnotations ?: return false
        return annotations.any { annotation ->
            annotation.qualifiedName in PREVIEW_ANNOTATIONS ||
                annotation.resolve()?.let { isMultipreview(it, depth + 1) } == true
        }
    }

    companion object {
        private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"
        private const val PREVIEW_ANNOTATION = "androidx.compose.ui.tooling.preview.Preview"
        private val PREVIEW_ANNOTATIONS = setOf(PREVIEW_ANNOTATION, "$PREVIEW_ANNOTATION.Container")
        private const val MAX_MULTIPREVIEW_DEPTH = 4
        private const val SEMANTICS_PACKAGE = "androidx.compose.ui.semantics"

        private val TEXT_TYPES = setOf(
            "java.lang.String",
            "java.lang.CharSequence",
            "androidx.compose.ui.text.AnnotatedString"
        )

        // Listed by name because other semantics properties, such as testTag, are not shown to users.
        private val SEMANTICS_TEXT_PROPERTIES = setOf("contentDescription", "stateDescription")

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "HardcodedComposeText",
            briefDescription = "Hardcoded text in Compose code",
            explanation = """
                Text shown to users must come from string resources so the app can be \
                translated. Move the text to `strings.xml` and read it with `stringResource` \
                (or `pluralStringResource` for counts).

                Code inside `@Preview` functions is not checked. For other text that users \
                never see, add `@Suppress("HardcodedComposeText")` with a comment saying why.
            """,
            category = Category.I18N,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(HardcodedComposeTextDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
