package io.github.ddiling.mydayboard.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * Forbids `Any` and `Any?` as a type, like ESLint's `no-explicit-any`: a value typed `Any` hides what it is,
 * so mistakes surface at runtime instead of compile time. Use the exact type, a generic, or a sealed type.
 *
 * The one unavoidable use is the parameter of `override fun equals(other: Any?)`, which the language fixes.
 * Supertype lists (`: Any()`) are not type usages in this sense and are left alone.
 * Generic bounds (`<T : Any>`, `where T : Any`) are allowed: they only say "T is non-null", they do not
 * loosen a type.
 */
class NoAnyType(
    config: Config,
) : Rule(config, "`Any` is not an exact type. Use the precise type or a generic instead.") {
    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)
        if (typeReference.isAny() && !typeReference.isAllowedUse()) {
            report(
                Finding(
                    Entity.from(typeReference),
                    "`Any` hides the real type. Use the exact type (or a generic / sealed type); " +
                        "only `override fun equals(other: Any?)` and generic bounds like `<T : Any>` may use `Any`.",
                ),
            )
        }
    }

    private fun KtTypeReference.isAny(): Boolean {
        val type = typeElement as? KtUserType ?: nullableInner()
        return type != null && type.typeArguments.isEmpty() && type.referencedName == "Any"
    }

    /** `Any?` is a nullable wrapper around the user type `Any`. */
    private fun KtTypeReference.nullableInner(): KtUserType? =
        (typeElement as? KtNullableType)?.innerType as? KtUserType

    private fun KtTypeReference.isEqualsParameter(): Boolean {
        val function = (parent as? KtParameter)?.parent?.parent as? KtNamedFunction
        return function != null &&
            function.name == "equals" &&
            function.hasModifier(KtTokens.OVERRIDE_KEYWORD) &&
            function.valueParameters.size == 1
    }

    private fun KtTypeReference.isSupertype(): Boolean =
        parent is KtSuperTypeListEntry || parent?.parent is KtSuperTypeListEntry

    private fun KtTypeReference.isGenericBound(): Boolean = parent is KtTypeParameter || parent is KtTypeConstraint

    private fun KtTypeReference.isAllowedUse(): Boolean = isEqualsParameter() || isSupertype() || isGenericBound()
}
