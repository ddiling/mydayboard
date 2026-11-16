#!/usr/bin/env bash
# Scaffolds a new feature module that follows the project conventions.
#
#   ./scripts/new-module.sh <name>   # feature/<name>  (io.github.ddiling.mydayboard.feature.<name>)
#
# <name>: lowercase letters and digits only (it becomes part of a package name).
set -euo pipefail

cd "$(dirname "$0")/.."

name="${1:-}"

usage() {
  sed -n '2,7p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
}

[[ -n "$name" ]] || usage
[[ "$name" =~ ^[a-z][a-z0-9]*$ ]] || { echo "Invalid name '$name': use lowercase letters and digits." >&2; exit 1; }

Name="$(tr '[:lower:]' '[:upper:]' <<< "${name:0:1}")${name:1}"
dir="feature/$name"
package="io.github.ddiling.mydayboard.feature.$name"
gradle_path=":feature:$name"

[[ ! -e "$dir" ]] || { echo "$dir already exists." >&2; exit 1; }

src="$dir/src/main/kotlin/${package//.//}"
mkdir -p "$src" "$dir/src/main/res/values" "$dir/src/test/resources"
cp core/designsystem/consumer-rules.pro core/designsystem/lint-baseline.xml "$dir/"
echo "sdk=36" > "$dir/src/test/resources/robolectric.properties"

cat > "$dir/build.gradle.kts" <<EOF
plugins {
    alias(libs.plugins.mydayboard.android.feature)
}

android {
    namespace = "$package"
}
EOF

cat > "$dir/src/main/res/values/strings.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="${name}_title">$Name</string>
</resources>
EOF

cat > "$src/${Name}Screen.kt" <<EOF
package $package

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface

/** Public entry point of the feature: the only API the app module uses. */
@Composable
fun ${Name}Route(modifier: Modifier = Modifier) {
    ${Name}Screen(modifier = modifier)
}

@Composable
internal fun ${Name}Screen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.${name}_title), style = MaterialTheme.typography.headlineSmall)
    }
}

@PreviewLightDark
@Composable
private fun ${Name}ScreenPreview() {
    PreviewSurface {
        ${Name}Screen()
    }
}
EOF

# Register the module in settings.gradle.kts, after the last feature include.
python3 - "$gradle_path" <<'EOF'
import sys
path = sys.argv[1]
lines = open("settings.gradle.kts").read().splitlines()
last = max(i for i, l in enumerate(lines) if l.startswith('include(":feature:'))
lines.insert(last + 1, f'include("{path}")')
open("settings.gradle.kts", "w").write("\n".join(lines) + "\n")
EOF

echo "Created $dir ($gradle_path) and added it to settings.gradle.kts."
echo
echo "Next steps:"
echo "  1. Use it from the app module: implementation(projects.feature.$name) in app/build.gradle.kts"
echo "  2. Record its screenshot baselines: ./gradlew $gradle_path:recordRoborazziDebug"
