import * as fs from "fs";
import * as path from "path";
import * as vscode from "vscode";
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind,
} from "vscode-languageclient/node";

const SUPPORTED_EXTENSIONS = [
  "md",
  "java", "kt", "kts",
  "c", "cpp", "cc", "cxx", "h", "hpp",
  "py",
  "js", "ts",
  "rb", "go", "rs", "cs",
];

const BUNDLED_JAR_NAME = "openfasttrace-language-server.jar";

let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const jarPath = resolveServerJar(context);
  if (jarPath === undefined) {
    vscode.window.showErrorMessage(
      "OpenFastTrace: no server JAR found. Run 'mvn package' in the project root " +
        "and recompile this extension."
    );
    return;
  }

  const javaPath = vscode.workspace.getConfiguration("oft").get<string>("java.path", "java");

  const serverOptions: ServerOptions = {
    command: javaPath,
    args: ["-jar", jarPath],
    transport: TransportKind.stdio,
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [
      { scheme: "file", pattern: `**/*.{${SUPPORTED_EXTENSIONS.join(",")}}` },
    ],
    outputChannelName: "OpenFastTrace Language Server",
  };

  client = new LanguageClient(
    "openfasttraceLsp",
    "OpenFastTrace Language Server",
    serverOptions,
    clientOptions
  );

  try {
    await client.start();
  } catch (error) {
    vscode.window.showErrorMessage(`OpenFastTrace: failed to start language server: ${error}`);
  }
}

export function deactivate(): Thenable<void> | undefined {
  return client?.stop();
}

function resolveServerJar(context: vscode.ExtensionContext): string | undefined {
  const bundledJar = path.join(context.extensionPath, "server", BUNDLED_JAR_NAME);
  return fs.existsSync(bundledJar) ? bundledJar : undefined;
}
