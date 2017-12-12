package org.sifrproject.server;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.sifrproject.recognizer.AnnotationToken;
import org.sifrproject.recognizer.ConceptRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class RecognizerClientHandler implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(RecognizerClientHandler.class);
    private final Socket clientSocket;
    private final GenericObjectPool<ConceptRecognizer> recognizerPool;

    public RecognizerClientHandler(final Socket clientSocket, final GenericObjectPool<ConceptRecognizer> recognizerPool) {
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
                    try {
                        final ConceptRecognizer conceptRecognizer = recognizerPool.borrowObject();
                        final List<AnnotationToken> annotations = conceptRecognizer.recognize(commandParts[1]);
                        recognizerPool.returnObject(conceptRecognizer);

                        for (final AnnotationToken token : annotations) {
                            outputWriter.println(token);
                        }
                        outputWriter.flush();
                    } catch (final Exception e) {
                        logger.error("Cannot obtain recognizer from pool: {}", e.getLocalizedMessage());
                    }
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

}
