# C Interop Example

## Generate static library

on Windows, you need to generate a static lib manually from `libaffcompose.dll` for compilation

```powershell
# Run in Visual Studio Developer Shell
lib.exe /def:libaffcompose.def /machine:x64 /out:libaffcompose.lib
```