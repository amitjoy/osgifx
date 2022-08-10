package org.slf4j.simple;

import java.io.PrintStream;

/**
 * This class encapsulates the user's choice of output target.
 */
class OutputChoice {

    enum OutputChoiceType {
        SYS_OUT, CACHED_SYS_OUT, SYS_ERR, CACHED_SYS_ERR, FILE;
    }

    final OutputChoiceType outputChoiceType;
    final PrintStream      targetPrintStream;

    OutputChoice(final OutputChoiceType outputChoiceType) {
        if (outputChoiceType == OutputChoiceType.FILE) {
            throw new IllegalArgumentException();
        }
        this.outputChoiceType = outputChoiceType;
        if (outputChoiceType == OutputChoiceType.CACHED_SYS_OUT) {
            targetPrintStream = System.out;
        } else if (outputChoiceType == OutputChoiceType.CACHED_SYS_ERR) {
            targetPrintStream = System.err;
        } else {
            targetPrintStream = null;
        }
    }

    OutputChoice(final PrintStream printStream) {
        outputChoiceType  = OutputChoiceType.FILE;
        targetPrintStream = printStream;
    }

    PrintStream getTargetPrintStream() {
        switch (outputChoiceType) {
        case SYS_OUT:
            return System.out;
        case SYS_ERR:
            return System.err;
        case CACHED_SYS_ERR, CACHED_SYS_OUT, FILE:
            return targetPrintStream;
        default:
            throw new IllegalArgumentException();
        }

    }

}