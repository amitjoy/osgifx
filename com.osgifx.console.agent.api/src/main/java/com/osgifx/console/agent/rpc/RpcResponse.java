/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.dto.DTO;

/**
 * Envelope for RPC responses.
 * <p>
 * This wrapper ensures that exceptions on the Agent side are captured
 * and transmitted back to the UI with their full stack trace.
 */
public class RpcResponse extends DTO {

    /** The result of the method call (if successful) */
    public Object data;

    /** The error message (if failed) */
    public String error;

    /** The full stack trace of the exception (for debugging in UI) */
    public String stackTrace;

    /**
     * Factory method for a successful response.
     */
    public static RpcResponse success(final Object data) {
        final RpcResponse r = new RpcResponse();
        r.data = data;
        return r;
    }

    /**
     * Factory method for a failed response.
     */
    public static RpcResponse error(final Throwable t) {
        final RpcResponse r = new RpcResponse();
        r.error = t.getClass().getName() + ": " + t.getMessage();

        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        r.stackTrace = sw.toString();

        return r;
    }
}