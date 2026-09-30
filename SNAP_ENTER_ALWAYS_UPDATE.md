# Snap / Enter Always Update

## 2.40.7 UX stability patch

- Snap Search now detects the user's vertical finger gesture directly instead of waiting for a large `scrollY` change.
- A small upward swipe (10dp) hides Search; a small downward swipe (10dp) restores it without requiring the user to reach the top of the list.
- Search height and bottom margin collapse/expand with the animation, so the ScrollView receives the freed space and touch coordinates remain aligned with what is drawn.
- Search no longer moves the main content using `content.translationY` during Snap.
- Snap animation starts from the current fraction, so reversing direction does not restart from 0/1.
- Favorites now expose the same Search field and can filter favorite tools.
- Rapid repeated taps on the same tool are debounced for 350ms to prevent duplicate tool launches.

## Validation

The XML layout parses successfully and static source checks pass. A full Gradle build was not performed in this offline environment because the project does not have its Gradle/Android dependencies cached locally.
