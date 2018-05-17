package org.sifrproject.server;

import org.apache.commons.daemon.DaemonContext;
import org.sifrproject.recognizer.ConceptRecognizer;
import org.sifrproject.recognizer.FaironConceptRecognizer;
import org.sifrproject.recognizer.SynchronizedConceptRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
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
    private static final String AN_EXCEPTION_OCCURRED_WHILE_CREATING_THE_LISTEN_SOCKET = "An exception occurred while creating the listen socket: {}";
    public static final String DICTIONARY_DEFAULT_NAME = "dictionary.txt";
    private static final String DEFAULT_CONFIG_PATH = Paths.get(File.separator+"etc","bpclinrec","config.xml").toAbsolutePath().toString();
    private static final long KEEP_ALIVE_TIME = 600L;

    private final int port;
    private final Thread thread = new Thread(this);
    private final ConceptRecognizer conceptRecognizer;

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


    private ClinicalRecognizerServer(final int port, final InputStream dictionaryStream) {
        this.port = port;
        conceptRecognizer = new SynchronizedConceptRecognizer(new FaironConceptRecognizer(dictionaryStream));

        workers = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new SynchronousQueue<>());

        try {
            listenSocket = new ServerSocket(port);
        } catch (final IOException e) {
            logger.error(AN_EXCEPTION_OCCURRED_WHILE_CREATING_THE_LISTEN_SOCKET, e.getMessage());
            System.exit(1);
        }
    }


    @SuppressWarnings("unused")
    public ClinicalRecognizerServer() throws IOException {

        final Properties properties = new Properties();
        properties.loadFromXML(new FileInputStream(DEFAULT_CONFIG_PATH));

        final int port = Integer.valueOf(properties.getProperty("port","55555"));
        final String dictionaryPath = properties.getProperty("dictionary.path", DICTIONARY_DEFAULT_NAME);

        this.port = port;
        conceptRecognizer = new SynchronizedConceptRecognizer(new FaironConceptRecognizer(new FileInputStream(dictionaryPath)));

        workers = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new SynchronousQueue<>());

        try {
            listenSocket = new ServerSocket(port);
        } catch (final IOException e) {
            logger.error(AN_EXCEPTION_OCCURRED_WHILE_CREATING_THE_LISTEN_SOCKET, e.getMessage());
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

                    final Runnable handler = new RecognizerClientHandler(clientSocket, conceptRecognizer);
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
     * the stop process should complete in at most 1 second from the time this method is invoked.
     */
    @Override
    public void stop() {
        logger.info("Shutting down the server.");
        keepRunning = false;
        workers.shutdownNow();

    }

    @Override
    public void init(final DaemonContext daemonContext) {
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public void destroy() {

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
    public static void main(final String[] args) throws FileNotFoundException {

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


        final StartStopJoinRunnable server = new ClinicalRecognizerServer(port, new FileInputStream(dictionaryPath.toFile()));
        server.start();
    }
}
