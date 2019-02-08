package org.sifrproject.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GetProperty {
    ;
    private static final Logger logger = LoggerFactory.getLogger(GetProperty.class);

    /**
     * Prints out system property values based on command line arguments.
     *
     * @param args Names of system properties to print values for.
     */
    public static void main(final String[] args) {
        if (args.length < 1) {
            logger.error("ERROR: Please pass a system property name an an argument " +
                    "to the program.");
            System.exit(1);
        }

        for (final String property : args) {
            if (!System.getProperties().containsKey(property)) {
                logger.error(String.format("ERROR: Unable to find property '%s'.", property));
                System.exit(1);
            }
        }

        for (final String property : args) {
            System.out.println(System.getProperty(property));
        }
    }

}