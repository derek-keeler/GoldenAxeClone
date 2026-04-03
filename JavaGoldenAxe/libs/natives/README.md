# Native files for future Linux/macOS controller support

This folder currently contains only the Windows native libraries required by the
Windows launcher:

- `jinput-dx8_64.dll`
- `jinput-raw_64.dll`
- `jinput-wintab.dll`

When Linux/macOS controller support is implemented, add the platform natives
back into this same folder.

## Files to restore

- Linux (64-bit): `libjinput-linux64.so`
- macOS: `libjinput-osx.jnilib`

## Where to get them

Use the same JInput release already used by this project:

- `JavaGoldenAxe\libs\jinput-2.0.10.jar`
- `JavaGoldenAxe\libs\jutils-1.0.0.jar`

Download the official JInput 2.0.10 distribution and copy the matching native
files from its `natives` package into this directory.
