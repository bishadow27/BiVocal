Option Explicit
Dim shell, files, folder, nodePath, action, command
Set shell = CreateObject("WScript.Shell")
Set files = CreateObject("Scripting.FileSystemObject")
folder = files.GetParentFolderName(WScript.ScriptFullName)
nodePath = shell.ExpandEnvironmentStrings("%ProgramFiles%") & "\nodejs\node.exe"
action = "--run"
If WScript.Arguments.Count > 0 Then action = WScript.Arguments(0)
If WScript.Arguments.Count > 1 Then nodePath = WScript.Arguments(1)
If action <> "--run" And action <> "--stop" Then WScript.Quit 1
If Not files.FileExists(nodePath) Then
  MsgBox "Node.js was not found. Reinstall the local Admin startup shortcut.", 16, "BIPASSION Admin"
  WScript.Quit 1
End If
command = Chr(34) & nodePath & Chr(34) & " " & Chr(34) & folder & "\local-admin.mjs" & Chr(34) & " " & action
shell.CurrentDirectory = folder
shell.Run command, 0, False
