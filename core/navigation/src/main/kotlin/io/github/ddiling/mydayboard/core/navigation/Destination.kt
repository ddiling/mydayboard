package io.github.ddiling.mydayboard.core.navigation

/**
 * A marker for every type-safe navigation route in the app. Each route is a `@Serializable` object or
 * class implementing it, so APIs that navigate (such as the dashboard host) take a [Destination]
 * instead of an untyped `Any`, and a non-route cannot be passed by mistake.
 */
interface Destination
