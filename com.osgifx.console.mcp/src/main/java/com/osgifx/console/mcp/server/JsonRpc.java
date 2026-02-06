/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.mcp.server;

import java.util.Map;

/**
 * Standard JSON-RPC 2.0 Protocol DTOs.
 * <p>
 * This class defines the data structures used by GSON to map incoming and
 * outgoing JSON-RPC messages. It supports the Request, Response, and Error
 * objects defined in the JSON-RPC 2.0 specification.
 */
public class JsonRpc {

    public static class Request {
        public String              jsonrpc = "2.0";
        public String              method;
        public Object              id;             // Can be Integer or String
        public Map<String, Object> params;
    }

    public static class Response {
        public String jsonrpc = "2.0";
        public Object id;
        public Object result;
        public Error  error;
    }

    public static class Error {
        public int    code;
        public String message;

        public Error(final int code, final String message) {
            this.code    = code;
            this.message = message;
        }
    }
}