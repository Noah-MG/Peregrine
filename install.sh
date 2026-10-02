#!/usr/bin/env bash
#
# Project installer.
#
#   REPO=yourname/yourrepo; curl -fsSL \
#     https://raw.githubusercontent.com/$REPO/main/install.sh | bash -s -- "$REPO"
#
# Clones your fork, clears docs/, overlays the FTC SDK on top of it, and
# patches build.dependencies.gradle for FTC Dashboard / Road Runner.
#
# Everything lives inside main(), called on the final line, so a truncated
# download cannot execute a partial script.

set -euo pipefail

# ---- configuration ---------------------------------------------------------
TEMPLATE_REPO="YOURUSER/YOURREPO"                      # the upstream you expect forks of
SDK_REPO="FIRST-Tech-Challenge/FtcRobotController"
SDK_VERSION="${SDK_VERSION:-latest}"                   # "latest" or a tag like v11.1.0
GRADLE_FILE="build.dependencies.gradle"
MARKER="// ---- added by installer (safe to edit; remove this block to undo) ----"

EXTRA_REPOSITORY="maven { url = 'https://maven.brott.dev/' }"
EXTRA_DEPENDENCIES=(
  "com.acmerobotics.dashboard:dashboard:0.4.16"
  "com.fasterxml.jackson.core:jackson-databind:2.13.4.2"
  "org.ejml:ejml-all:0.38"
)
# ---------------------------------------------------------------------------

REPO=""
REPO_REF="main"
TARGET_DIR=""
CLOBBER=1          # 1 = SDK files win on conflict (robocopy-like), 0 = yours win
TMPDIR_PATH=""
SDK_ROOT=""
STEP=0
TOTAL_STEPS=7

# ---- output helpers --------------------------------------------------------

if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  C_BLUE=$'\033[1;34m'; C_GREEN=$'\033[1;32m'; C_YELLOW=$'\033[1;33m'
  C_RED=$'\033[1;31m';  C_DIM=$'\033[2m';      C_OFF=$'\033[0m'
else
  C_BLUE=""; C_GREEN=""; C_YELLOW=""; C_RED=""; C_DIM=""; C_OFF=""
fi

step()  { STEP=$((STEP + 1)); printf '\n%s[%d/%d]%s %s\n' "$C_BLUE" "$STEP" "$TOTAL_STEPS" "$C_OFF" "$*"; }
ok()    { printf '  %s/%s %s\n' "$C_GREEN" "$C_OFF" "$*"; }
note()  { printf '  %s%s%s\n' "$C_DIM" "$*" "$C_OFF"; }
warn()  { printf '  %s!%s %s\n' "$C_YELLOW" "$C_OFF" "$*" >&2; }
abort() { printf '\n%sError:%s %s\n' "$C_RED" "$C_OFF" "$*" >&2; exit 1; }

# Redraw a bar in place. Falls back to periodic lines when stdout is not a tty,
# so piping the installer into a log file does not produce megabytes of \r.
bar() {
  local cur=$1 total=$2 label=$3 width=28
  local pct=0 filled i line=""
  [ "$total" -gt 0 ] && pct=$(( cur * 100 / total ))
  [ "$pct" -gt 100 ] && pct=100
  filled=$(( pct * width / 100 ))
  for ((i = 0; i < width; i++)); do
    if [ "$i" -lt "$filled" ]; then line+="#"; else line+="."; fi
  done
  if [ -t 1 ]; then
    printf '\r  [%s] %3d%%  %s' "$line" "$pct" "$label"
  elif [ $(( cur % 200 )) -eq 0 ]; then
    printf '  ... %d/%d %s\n' "$cur" "$total" "$label"
  fi
}

bar_done() { [ -t 1 ] && printf '\r\033[K'; return 0; }

# ---- setup -----------------------------------------------------------------

usage() {
  cat <<EOF
Usage: install.sh [OWNER/REPO] [options]

  OWNER/REPO        your fork, e.g. alice/myproject
                    (also settable via the REPO environment variable)

Options:
  --ref REF         branch, tag, or commit to check out      (default: main)
  --dir PATH        where to clone                           (default: ./<repo name>)
  --sdk-version V   FTC SDK tag, e.g. v11.1.0                (default: latest release)
  --keep-mine       on a file collision, keep your repo's copy instead of the SDK's
  -h, --help        show this message
EOF
}

parse_args() {
  REPO="${REPO:-${GITHUB_REPO:-}}"
  while [ $# -gt 0 ]; do
    case "$1" in
      --ref)         REPO_REF="${2:?--ref needs a value}"; shift 2 ;;
      --dir)         TARGET_DIR="${2:?--dir needs a value}"; shift 2 ;;
      --sdk-version) SDK_VERSION="${2:?--sdk-version needs a value}"; shift 2 ;;
      --keep-mine)   CLOBBER=0; shift ;;
      -h|--help)     usage; exit 0 ;;
      -*)            abort "Unknown option: $1" ;;
      *)             REPO="$1"; shift ;;
    esac
  done
}

check_deps() {
  local missing=() c
  for c in git curl unzip; do
    command -v "$c" >/dev/null 2>&1 || missing+=("$c")
  done
  [ ${#missing[@]} -eq 0 ] || abort "Missing required commands: ${missing[*]}"

  case "$(uname -s)" in
    Linux|Darwin|MINGW*|MSYS*|CYGWIN*) ;;
    *) abort "Unsupported shell environment: $(uname -s)" ;;
  esac
  ok "git, curl, unzip present"
}

# Resolve which fork to clone: argument, then env var, then gh CLI, then ask.
resolve_repo() {
  if [ -z "$REPO" ] && command -v gh >/dev/null 2>&1; then
    local login
    login="$(gh api user --jq .login 2>/dev/null || true)"
    if [ -n "$login" ]; then
      REPO="$login/${TEMPLATE_REPO#*/}"
      note "Guessed $REPO from your gh login."
    fi
  fi

  if [ -z "$REPO" ]; then
    [ -e /dev/tty ] || abort "No repo given. Pass it: ... | bash -s -- owner/repo"
    printf '  Your GitHub username: '
    local user; read -r user < /dev/tty
    [ -n "$user" ] || abort "No username entered."
    REPO="$user/${TEMPLATE_REPO#*/}"
  fi

  case "$REPO" in
    */*) ;;
    *) abort "Expected OWNER/REPO, got '$REPO'" ;;
  esac

  # Confirm it exists before git produces a cryptic auth prompt.
  if ! curl -fsSL -o /dev/null "https://api.github.com/repos/$REPO"; then
    abort "Cannot reach github.com/$REPO - is it public, and have you forked yet?"
  fi
  ok "Installing from $REPO"
}

clone_repo() {
  [ -n "$TARGET_DIR" ] || TARGET_DIR="./${REPO#*/}"
  if [ -e "$TARGET_DIR" ]; then
    abort "$TARGET_DIR already exists. Move it, or pass --dir somewhere-else."
  fi
  git clone --progress --branch "$REPO_REF" \
      "https://github.com/$REPO.git" "$TARGET_DIR" 2>&1 | sed 's/^/  /'
  cd "$TARGET_DIR"
  ok "Cloned into $(pwd)"
}

clear_docs() {
  if [ -d docs ]; then
    local n; n="$(find docs -mindepth 1 | wc -l | tr -d ' ')"
    find docs -mindepth 1 -delete
    ok "Emptied docs/ ($n entries removed)"
  else
    mkdir -p docs
    note "No docs/ directory; created an empty one."
  fi
}

# Ask GitHub for the newest SDK release tag. Plain grep so jq is not required.
resolve_sdk_version() {
  if [ "$SDK_VERSION" != "latest" ]; then
    ok "Using pinned SDK $SDK_VERSION"
    return
  fi
  local json tag
  json="$(curl -fsSL "https://api.github.com/repos/$SDK_REPO/releases/latest")" \
    || abort "Could not query the SDK release list. Pass --sdk-version to skip this."
  tag="$(printf '%s' "$json" \
        | grep -o '"tag_name"[[:space:]]*:[[:space:]]*"[^"]*"' \
        | head -1 | sed 's/.*"\([^"]*\)"$/\1/')"
  [ -n "$tag" ] || abort "Could not parse a release tag from the GitHub API response."
  SDK_VERSION="$tag"
  ok "Latest SDK release is $SDK_VERSION"
}

download_sdk() {
  TMPDIR_PATH="$(mktemp -d)"
  local url="https://github.com/$SDK_REPO/archive/refs/tags/$SDK_VERSION.zip"
  note "$url"
  curl -fL --progress-bar -o "$TMPDIR_PATH/sdk.zip" "$url" \
    || abort "Download failed. Check that tag $SDK_VERSION exists."
  ok "Downloaded $(du -h "$TMPDIR_PATH/sdk.zip" | cut -f1 | tr -d ' ')"
}

# unzip -o prints one line per member; count those against the archive listing
# to drive a real progress bar instead of a spinner.
extract_sdk() {
  local total done=0 line
  total="$(unzip -Z1 "$TMPDIR_PATH/sdk.zip" | wc -l | tr -d ' ')"
  while IFS= read -r line; do
    case "$line" in
      *inflating:*|*extracting:*|*creating:*)
        done=$((done + 1))
        bar "$done" "$total" "extracting"
        ;;
    esac
  done < <(unzip -o "$TMPDIR_PATH/sdk.zip" -d "$TMPDIR_PATH/extracted" 2>&1)
  bar_done
  ok "Extracted $done files"

  # The archive holds exactly one top-level directory whose name embeds the
  # version. Discover it rather than guessing at the naming convention.
  SDK_ROOT="$(find "$TMPDIR_PATH/extracted" -mindepth 1 -maxdepth 1 -type d | head -1)"
  [ -n "$SDK_ROOT" ] || abort "Archive did not contain a top-level directory."
}

# Merge the SDK tree into the project so same-named folders combine rather than
# replace. `cp -R src/. dest/` is the POSIX equivalent of `robocopy src dest /E`.
merge_sdk() {
  local total done=0 flags="-Rv"
  total="$(find "$SDK_ROOT" | wc -l | tr -d ' ')"
  [ "$CLOBBER" -eq 1 ] || flags="-Rvn"

  while IFS= read -r _; do
    done=$((done + 1))
    bar "$done" "$total" "merging"
  done < <(cp $flags "$SDK_ROOT/." . 2>/dev/null)
  bar_done

  if [ "$CLOBBER" -eq 1 ]; then
    ok "Merged SDK into project (SDK files took precedence on collisions)"
  else
    ok "Merged SDK into project (your existing files were kept)"
  fi
}

cleanup_tmp() {
  if [ -n "$TMPDIR_PATH" ] && [ -d "$TMPDIR_PATH" ]; then
    rm -rf "$TMPDIR_PATH"
    TMPDIR_PATH=""
  fi
}

# Gradle merges repeated repositories{} and dependencies{} blocks within one
# script, so appending a block adds to the existing ones. That means we never
# parse or rewrite upstream's lines, and upstream stays free to bump its own
# versions without breaking this patch.
patch_gradle() {
  [ -f "$GRADLE_FILE" ] || abort "$GRADLE_FILE not found after the merge."

  if grep -qF "$MARKER" "$GRADLE_FILE"; then
    ok "$GRADLE_FILE already patched; leaving it alone"
    return
  fi

  cp "$GRADLE_FILE" "$GRADLE_FILE.orig"

  local needed=() dep
  for dep in "${EXTRA_DEPENDENCIES[@]}"; do
    if grep -qF "$dep" "$GRADLE_FILE"; then
      note "already present: $dep"
    else
      needed+=("$dep")
    fi
  done

  {
    printf '\n%s\n' "$MARKER"
    if ! grep -qF "maven.brott.dev" "$GRADLE_FILE"; then
      printf 'repositories {\n    %s\n}\n\n' "$EXTRA_REPOSITORY"
    fi
    if [ ${#needed[@]} -gt 0 ]; then
      printf 'dependencies {\n'
      for dep in "${needed[@]}"; do
        printf "    implementation '%s'\n" "$dep"
      done
      printf '}\n'
    fi
  } >> "$GRADLE_FILE"

  ok "Patched $GRADLE_FILE (${#needed[@]} added; backup at $GRADLE_FILE.orig)"
}

main() {
  parse_args "$@"
  trap cleanup_tmp EXIT INT TERM

  printf '%sFTC project installer%s\n' "$C_BLUE" "$C_OFF"

  step "Checking prerequisites";       check_deps
  step "Resolving your repository";    resolve_repo; clone_repo
  step "Clearing docs/";               clear_docs
  step "Finding the FTC SDK release";  resolve_sdk_version
  step "Downloading the SDK";          download_sdk
  step "Extracting and merging";       extract_sdk; merge_sdk; cleanup_tmp
  step "Patching Gradle";              patch_gradle

  printf '\n%sDone.%s Project is at %s\n' "$C_GREEN" "$C_OFF" "$(pwd)"
  printf '  Open it in Android Studio, then let Gradle sync.\n\n'
}

main "$@"
