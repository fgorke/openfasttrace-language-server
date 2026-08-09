package org.itsallcode.openfasttrace.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.tinylog.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

public class ServerLauncher {

    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        Logger.info("Starting OpenFastTrace Language Server");
        launch(System.in, System.out);
    }

    static void launch(final InputStream in, final OutputStream out)
            throws InterruptedException, ExecutionException {
        final var server = new OftLanguageServer();
        final Launcher<LanguageClient> launcher = new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(server)
                .setRemoteInterface(LanguageClient.class)
                .setInput(in)
                .setOutput(out)
                .configureGson(ServerLauncher::registerMissingInstanceCreators)
                .create();
        final LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);
        launcher.startListening().get();
    }

    // [impl->req~coverage-hierarchy~2]
    private static void registerMissingInstanceCreators(final GsonBuilder gsonBuilder) {
        final InstanceCreator<TypeHierarchyItem> typeHierarchyItem = type -> new TypeHierarchyItem(
                "", SymbolKind.Class, "", emptyRange(), emptyRange());
        gsonBuilder.registerTypeAdapter(TypeHierarchyItem.class, typeHierarchyItem);
    }

    private static Range emptyRange() {
        return new Range(new Position(0, 0), new Position(0, 0));
    }
}
