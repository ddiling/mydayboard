package io.github.ddiling.mydayboard.feature.trackers.reason

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject
import kotlin.random.Random

/**
 * The logic behind the reason dialog, shared by the dashboard card and the detail screen so both behave the
 * same. It only decides and remembers; the ViewModel that owns it logs the event, and only after [confirm], so
 * the event is stamped when the user chooses to log, not when they tapped. The flag and the reasons are read
 * once per tap, so changes made on the reasons screen apply at the next tap with no stale copy in screen state.
 * Each ViewModel gets its own instance.
 */
internal class ReasonPrompter
    @Inject
    constructor(
        private val trackers: TrackerRepository,
        private val reasons: TrackerReasonRepository,
        private val random: Random,
    ) {
        private val _prompt = MutableStateFlow<ReasonPrompt?>(null)

        /** The dialog to show, or null when none is open. */
        val prompt: StateFlow<ReasonPrompt?> = _prompt.asStateFlow()

        private var choices: List<TrackerReason> = emptyList()

        /**
         * Opens the dialog and returns true when the tracker asks for a reason; returns false (the caller logs
         * at once) when it does not, or when it is gone, because logging for a missing tracker is a no-op.
         */
        suspend fun request(
            trackerId: String,
            kind: ReasonPromptKind,
        ): Boolean {
            val tracker = trackers.observeTracker(trackerId).first()
            if (tracker == null || !tracker.askForReason) return false
            choices = reasons.observeReasons(trackerId).first()
            val reason = pickReason(choices, currentId = null, random)
            _prompt.value =
                if (reason == null) {
                    ReasonPrompt.NoReasons(trackerId, tracker.name, kind)
                } else {
                    ReasonPrompt.WithReason(trackerId, tracker.name, kind, reason, canSwap = choices.size > 1)
                }
            return true
        }

        /** Swaps the shown reason for a different one; does nothing without a second reason. */
        fun another() {
            val current = _prompt.value as? ReasonPrompt.WithReason ?: return
            val next = pickReason(choices, current.reason.id, random) ?: return
            _prompt.value = current.copy(reason = next)
        }

        /** Closes the dialog and saves nothing: the "wait" button, a tap outside and Back all end here. */
        fun dismiss() {
            _prompt.value = null
        }

        /** Closes the dialog and returns what was open, for the caller to log; null when nothing was open. */
        fun confirm(): ReasonPrompt? = _prompt.getAndUpdate { null }
    }
