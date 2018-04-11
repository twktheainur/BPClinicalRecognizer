package org.sifrproject.server;

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
    private final ConceptRecognizer conceptRecognizer;
//    private final Timeout poolTimeout = new Timeout(1, TimeUnit.SECONDS);

    RecognizerClientHandler(final Socket clientSocket, final ConceptRecognizer conceptRecognizer) {
        this.clientSocket = clientSocket;
        this.conceptRecognizer = conceptRecognizer;
    }


    @Override
    public void run() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()))) {
            try (PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF8"))) {
                //Initializing protocol
                outputWriter.println("mgrep");
                outputWriter.println("4");
                outputWriter.flush();

                String command = "init";
                while ((command != null) && !command.isEmpty()) {
                    if(!command.equals("init")) {
                        final String text = command.substring(3);
                        if (!text.isEmpty()) {
                            annotateAndWrite(text, outputWriter);
                        }
                    }
                    command = userInput.readLine();
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
//        try {
//            final ConceptRecognizer conceptRecognizer = recognizerPool.claim(poolTimeout);
//            try {

                final List<AnnotationToken> annotations = conceptRecognizer.recognize(text,false);
                for (final AnnotationToken token : annotations) {
                    outputWriter.println(token);
                }
                outputWriter.println();
                outputWriter.flush();
//            } catch (final RuntimeException e) {
//                logger.error("Cannot obtain recognizer from pool: {}", e.getLocalizedMessage());
//            } finally {
//                if (conceptRecognizer != null) {
//                    conceptRecognizer.release();
//                }
//            }
//        } catch (final InterruptedException e) {
//            logger.error("Cannot get concept recognizer instance: {}", e.getLocalizedMessage());
//        }
    }

}
