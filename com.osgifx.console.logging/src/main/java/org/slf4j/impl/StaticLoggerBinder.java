package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

import com.osgifx.console.logging.forwarder.Slf4jLoggerFactory;

/**
 * This class bridges between SLF4j by implementing a getSingleton() method on
 * the class with this name.
 */
class StaticLoggerBinder implements LoggerFactoryBinder {

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is usually modified with each release.
     */
    // To avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION; // !final

    private static final StaticLoggerBinder SINGLETON;

    static {
        SINGLETON             = new StaticLoggerBinder();
        REQUESTED_API_VERSION = "1.7.36";
    }

    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    private StaticLoggerBinder() {
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return Slf4jLoggerFactory.getSlf4jLoggerFactory();
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return Slf4jLoggerFactory.getSlf4jLoggerFactory().getClass().getName();
    }

}
