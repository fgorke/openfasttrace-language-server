import * as vscode from "vscode";

export function activate(context: vscode.ExtensionContext): void {
  console.log("OpenFastTrace Language Server extension activated");
}

export function deactivate(): Thenable<void> | undefined {
  return undefined;
}
