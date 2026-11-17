#!/bin/bash
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
APPDIR="$DIR/appdir"
OUT="$DIR/AetherLauncher.AppImage"

# Create compressed tarball of AppDir
echo "Compressing AppDir payload..."
TARBALL="$DIR/payload.tar.gz"
tar -czf "$TARBALL" -C "$APPDIR" .

# Create the AppImage self-extracting executable
cat << 'EOF' > "$OUT"
#!/bin/bash
set -e

# AetherLauncher Portable Linux AppImage
APP_NAME="AetherLauncher"
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/$APP_NAME"
INSTALL_DIR="$CACHE_DIR/app"
BUILD_ID="BUILD_PLACEHOLDER"

if [ "$1" = "--appimage-extract" ]; then
    EXTRACT_DIR="${2:-squashfs-root}"
    echo "Extracting $APP_NAME to $EXTRACT_DIR..."
    mkdir -p "$EXTRACT_DIR"
    PAYLOAD_LINE=$(awk '/^__PAYLOAD_BELOW__/ {print NR + 1; exit 0; }' "$0")
    tail -n +"$PAYLOAD_LINE" "$0" | tar -xz -C "$EXTRACT_DIR"
    echo "Done."
    exit 0
fi

if [ "$1" = "--appimage-help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "$APP_NAME Linux Portable AppImage"
    echo "Usage:"
    echo "  ./AetherLauncher.AppImage [options] [launcher arguments]"
    echo ""
    echo "Options:"
    echo "  --appimage-extract [DIR]   Extract contents of AppImage"
    echo "  --appimage-help            Show this help message"
    echo ""
    exit 0
fi

# Check if cached extraction is valid
NEED_EXTRACT=0
if [ ! -f "$INSTALL_DIR/AppRun" ] || [ ! -f "$CACHE_DIR/build_id" ] || [ "$(cat "$CACHE_DIR/build_id" 2>/dev/null)" != "$BUILD_ID" ]; then
    NEED_EXTRACT=1
fi

if [ "$NEED_EXTRACT" -eq 1 ]; then
    echo "Initializing $APP_NAME..."
    mkdir -p "$INSTALL_DIR"
    PAYLOAD_LINE=$(awk '/^__PAYLOAD_BELOW__/ {print NR + 1; exit 0; }' "$0")
    tail -n +"$PAYLOAD_LINE" "$0" | tar -xz -C "$INSTALL_DIR"
    echo "$BUILD_ID" > "$CACHE_DIR/build_id"
fi

exec "$INSTALL_DIR/AppRun" "$@"
__PAYLOAD_BELOW__
EOF

# Embed MD5 as build ID
BUILD_MD5=$(md5sum "$TARBALL" | cut -d' ' -f1)
sed -i "s/BUILD_PLACEHOLDER/$BUILD_MD5/g" "$OUT"

# Append tarball payload to script
cat "$TARBALL" >> "$OUT"
rm -f "$TARBALL"

chmod +x "$OUT"
echo "Successfully created $OUT"
ls -lh "$OUT"
