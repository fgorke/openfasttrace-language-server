package org.itsallcode.openfasttrace.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.tinylog.Logger;

public class ServerLauncher {

    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        Logger.info("Starting OpenFastTrace Language Server");
        launch(System.in, System.out);
    }

    static void launch(final InputStream in, final OutputStream out)
            throws InterruptedException, ExecutionException {
        final var server = new OftLanguageServer();
        final var launcher = LSPLauncher.createServerLauncher(server, in, out);
        final LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);
        launcher.startListening().get();
    }
}
