#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

base_dir="$( cd "$(dirname "$0")/../.." >/dev/null 2>&1 ; pwd -P )"
readonly base_dir
pom_file="$base_dir/pom.xml"
readonly pom_file

if [[ ! -f "$pom_file" ]]; then
    echo "Could not find pom.xml at $pom_file" >&2
    exit 1
fi

# Extract the first <version>...</version> occurrence from the root pom.xml.
project_version=$(grep -m1 -oP '(?<=<version>).*?(?=</version>)' "$pom_file" | tr -d '\r')
readonly project_version
echo "Read project version '$project_version' from $pom_file"

if [[ -z "$project_version" ]]; then
    echo "Could not extract version from $pom_file" >&2
    exit 1
fi

if [[ "$project_version" == *-SNAPSHOT ]]; then
    echo "Release version must not end with -SNAPSHOT: $project_version" >&2
    exit 1
fi

readonly changes_file="$base_dir/doc/changes/changes_${project_version}.md"
if [[ ! -f "$changes_file" ]]; then
    echo "Could not find release notes file $changes_file" >&2
    exit 1
fi

find_single_artifact() {
    local description="$1" directory="$2" pattern="$3"
    local matches
    matches=$(find "$directory" -maxdepth 1 -type f -name "$pattern" -printf '%T@ %p\n' 2>/dev/null \
        | sort -rn | cut -d' ' -f2-)
    if [[ -z "$matches" ]]; then
        echo "Could not find $description in $directory" >&2
        return 1
    fi
    if [[ $(echo "$matches" | wc -l) -gt 1 ]]; then
        echo "Found several candidates for $description, using the newest:" >&2
        echo "$matches" >&2
    fi
    echo "$matches" | head -n 1
}

artifact_path=$(find_single_artifact "plugin distribution archive" \
    "$base_dir/intellij-plugin/build/distributions" "openfasttrace-lsp-intellij-plugin-${project_version}.zip")
readonly artifact_path

server_jar=$(find_single_artifact "standalone server JAR" \
    "$base_dir/target" "openfasttrace-language-server-${project_version}-standalone.jar")
readonly server_jar

# One package per platform, each bundling a Java runtime for that platform.
mapfile -t vscode_extensions < <(find "$base_dir/vscode-extension" -maxdepth 1 -type f -name '*.vsix' | sort)
readonly vscode_extensions
if [[ ${#vscode_extensions[@]} -eq 0 ]]; then
    echo "Could not find any VS Code extension package in $base_dir/vscode-extension" >&2
    exit 1
fi

echo "Calculate sha256sum for plugin archive, server JAR and VS Code extension"

# checksum for plugin
file_dir="$(dirname "$artifact_path")"
file_name="$(basename "$artifact_path")"
cd "$file_dir"
checksum_plugin_name="${file_name}.sha256"
sha256sum "$file_name" > "$checksum_plugin_name"
checksum_plugin_path="$file_dir/$checksum_plugin_name"
cd "$base_dir"

# checksum for server JAR
server_dir="$(dirname "$server_jar")"
server_name="$(basename "$server_jar")"
cd "$server_dir"
checksum_server_name="${server_name}.sha256"
sha256sum "$server_name" > "$checksum_server_name"
checksum_server_path="$server_dir/$checksum_server_name"
cd "$base_dir"

# checksums for every VS Code extension package
vscode_upload_files=()
for extension in "${vscode_extensions[@]}"; do
    vscode_dir="$(dirname "$extension")"
    vscode_name="$(basename "$extension")"
    cd "$vscode_dir"
    sha256sum "$vscode_name" > "${vscode_name}.sha256"
    cd "$base_dir"
    vscode_upload_files+=("$extension" "$vscode_dir/${vscode_name}.sha256")
done
readonly vscode_upload_files

readonly title="Release $project_version"
readonly tag="$project_version"
echo "Creating release:"
echo "Git tag      : $tag"
echo "Title        : $title"
echo "Changes file : $changes_file"
echo "Plugin file  : $artifact_path"
echo "Plugin sha256: $checksum_plugin_path"
echo "Server JAR   : $server_jar"
echo "Server sha256: $checksum_server_path"
echo "VS Code ext. : ${#vscode_extensions[@]} package(s)"
printf '  %s\n' "${vscode_extensions[@]}"

release_url=$(gh release create --latest --title "$title" --notes-file "$changes_file" --target main "$tag" \
    "$artifact_path" "$checksum_plugin_path" "$server_jar" "$checksum_server_path" \
    "${vscode_upload_files[@]}")
readonly release_url
echo "Release URL: $release_url"


