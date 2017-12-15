package org.sifrproject.server;

import org.sifrproject.recognizer.AnnotationToken;
import org.sifrproject.recognizer.ConceptRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Pool;
import stormpot.Timeout;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecognizerClientHandler implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(RecognizerClientHandler.class);
    private final Socket clientSocket;
    private final Pool<ConceptRecognizer> recognizerPool;
    private final Timeout poolTimeout = new Timeout(1, TimeUnit.SECONDS);

    public RecognizerClientHandler(final Socket clientSocket, final Pool<ConceptRecognizer> recognizerPool) {
        this.clientSocket = clientSocket;
        this.recognizerPool = recognizerPool;
    }


    @Override
    public void run() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()))) {
            try (PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF8"))) {
                outputWriter.println("mgrep");
                outputWriter.println("4");
                outputWriter.flush();
                final String command = userInput.readLine();
                final String[] commandParts = command.split("\t");
                if (commandParts.length > 1) {
                    annotateAndWrite(commandParts[1],outputWriter);
                    outputWriter.println();
                }
            } catch (final UnsupportedEncodingException e) {
                logger.error("Encoding error: {}", e.getLocalizedMessage());
            } catch (final IOException e) {
                logger.error("Cannot send reply to client: {}", e.getLocalizedMessage());
            }
        } catch (final IOException e1) {
            logger.error("Cannot read client intput: {}", e1.getLocalizedMessage());
        }
        try {
            clientSocket.close();
        } catch (final IOException e) {
            logger.error("Cannot close client socket connection: {}", e.getLocalizedMessage());
        }
    }

    private void annotateAndWrite(final String text, final PrintWriter outputWriter){
        try {
            final ConceptRecognizer conceptRecognizer = recognizerPool.claim(poolTimeout);
            try {

                final List<AnnotationToken> annotations = conceptRecognizer.recognize(text);
                for (final AnnotationToken token : annotations) {
                    outputWriter.println(token);
                }
                outputWriter.flush();
            } catch (final RuntimeException e) {
                logger.error("Cannot obtain recognizer from pool: {}", e.getLocalizedMessage());
            } finally {
                if (conceptRecognizer != null) {
                    conceptRecognizer.release();
                }
            }
        } catch (final InterruptedException e) {
            logger.error("Cannot get concept recognizer instance: {}", e.getLocalizedMessage());
        }
    }

}
