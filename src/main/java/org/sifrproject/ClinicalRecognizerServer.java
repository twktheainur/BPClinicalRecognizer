package org.sifrproject;

import org.sifrproject.recognizer.ConceptRecognizer;
import org.sifrproject.recognizer.pool.FaironRecognizerAllocator;
import org.sifrproject.server.RecognizerClientHandler;
import org.sifrproject.server.StartStopJoinRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Allocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.yield;

/**
 * Hello world!
 */
public final class ClinicalRecognizerServer implements StartStopJoinRunnable {


    //private final Properties configuration;
    private static final Logger logger = LoggerFactory.getLogger(ClinicalRecognizerServer.class);
    private static final int TCP_PORT_MAX = 65536;

    private final int port;
    private final Thread thread = new Thread(this);
    private final Pool<ConceptRecognizer> recognizerPool;

    /**
     * Pool of worker threads of unbounded size. A new thread will be created
     * for each concurrent connection, and old threads will be shut down if they
     * remain unused for about 1 minute.
     */
    private final ExecutorService workers;

    /**
     * Server socket on which to accept incoming client connections.
     */
    private ServerSocket listenSocket;

    /**
     * Flag to keep this server running.
     */
    private volatile boolean keepRunning = true;


    private ClinicalRecognizerServer(final int port, final Path dictionaryPath) {
        this.port = port;

        final Allocator<ConceptRecognizer> allocator = new FaironRecognizerAllocator(dictionaryPath);
        final Config<ConceptRecognizer> config = new Config<ConceptRecognizer>().setAllocator(allocator);
        config.setBackgroundExpirationEnabled(false);
        config.setSize(2);
        recognizerPool = new BlazePool<>(config);

//        //Forcing the creation of at least one object at startup
//        try {
//            recognizerPool.returnObject(recognizerPool.borrowObject());
//        } catch (final Exception e) {
//            logger.error("Cannot instantiate initial recognizer instance {}", e.getLocalizedMessage());
//        }

        workers = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                600L, TimeUnit.SECONDS,
                new SynchronousQueue<>());

        try {
            listenSocket = new ServerSocket(port);
        } catch (final IOException e) {
            logger.error("An exception occurred while creating the listen socket: {}", e.getMessage());
            System.exit(1);
        }
    }

    private static void usage() {
        logger.error("Syntax: ClinicalRecognitionServer host port path_to_dictionary");
        System.exit(1);
    }

    @SuppressWarnings("CallToThreadYield")
    @Override
    public void run() {
        logger.debug("Accepting incoming connections on port {}", port);

        // Accept an incoming connection, handle it, then close and repeat.
        try {
            while (keepRunning) {
                try {
                    // Accept the next incoming connection
                    final Socket clientSocket = listenSocket.accept();

                    final Runnable handler = new RecognizerClientHandler(clientSocket, recognizerPool);
                    workers.execute(handler);

                } catch (final SocketTimeoutException te) {
                    // Ignored, timeouts will happen every 1 second
                } catch (final IOException ioe) {
                    logger.error("Exception occurred while handling client request: {}", ioe.getMessage());
                    // Yield to other threads if an exception occurs (prevent CPU
                    // spin)
                    yield();
                }
            }

            // Make sure to release the port, otherwise it may remain bound for several minutes
            listenSocket.close();
        } catch (final IOException ioe) {
            // Ignored
        }
        logger.debug("Stopped accepting incoming connections.");

    }

    /**
     * Shuts down this server.  Since the main server thread will time out every 1 second,
     * the shutdown process should complete in at most 1 second from the time this method is invoked.
     */
    @Override
    public void shutdown() {
        logger.info("Shutting down the server.");
        keepRunning = false;
        workers.shutdownNow();
        try {
            thread.join();
        } catch (final InterruptedException e) {
            // Ignored, we're exiting anyway
        }
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public void join() {
        try {
            thread.join();
        } catch (final InterruptedException e) {
            logger.debug("Join interrupted");
        }
    }

    @SuppressWarnings("LocalVariableOfConcreteClass")
    public static void main(final String[] args) {

        if (args.length < 2) {
            usage();
        }
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (final NumberFormatException nfe) {
            logger.error("Invalid listen port value: \"{}\".", args[0]);
            usage();
            System.exit(1);
        }

        // Make sure the port number is valid for TCP.
        if ((port <= 0) || (port > TCP_PORT_MAX)) {
            logger.error("Port value must be in (0, 65535].");
            System.exit(1);
        }
        final Path dictionaryPath = Paths.get(args[1]);


        final StartStopJoinRunnable server = new ClinicalRecognizerServer(port, dictionaryPath);
        server.start();
    }
}
