## What & why

<!-- One or two sentences. Link the issue. -->

## Screenshots

<!-- Before / after for UI changes. If screenshot tests failed on purpose,
     run the "Record screenshots" workflow on this branch. -->

## Checklist

- [ ] `./gradlew spotlessApply :affectedStaticAnalysis :affectedTest -PaffectedBase=origin/main` passes locally
- [ ] No new dependency between features (shared code goes to `core/`)
- [ ] New UI has `@Preview`s (they become screenshot tests automatically)
- [ ] User-facing strings are in `strings.xml`, prefixed with the feature name
- [ ] Add the `e2e` label if this touches a critical user flow
