/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.supervisor.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.osgifx.console.supervisor.Supervisor.RpcType.MQTT_RPC;
import static com.osgifx.console.supervisor.Supervisor.RpcType.SOCKET_RPC;
import static com.osgifx.console.supervisor.rpc.RpcSupervisor.CONDITION_ID_VALUE;
import static com.osgifx.console.supervisor.rpc.RpcSupervisor.MQTT_CONNECTION_LISTENER_FILTER_PROP;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;
import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.aries.component.dsl.OSGiResult;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.messaging.MessageSubscription;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.mu.util.Substring;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.rpc.mqtt.MqttRPC;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.MqttConnection;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.supervisor.Supervisor;

@Component(property = MQTT_CONNECTION_LISTENER_FILTER_PROP)
@SatisfyingConditionTarget("(" + CONDITION_ID + "=" + CONDITION_ID_VALUE + ")")
public final class RpcSupervisor extends AbstractRpcSupervisor<Supervisor, Agent>
        implements Supervisor, MqttClientConnectedListener, MqttClientDisconnectedListener {

    public static final String CONDITION_ID_VALUE                    = "rpc-agent";
    public static final String MQTT_CONDITION_ID                     = "mqtt-messaging";
    public static final String MQTT_CONNECTION_LISTENER_FILTER_KEY   = "mqtt_connection_filter";
    public static final String MQTT_CONNECTION_LISTENER_FILTER_VALUE = "true";
    public static final String MQTT_CONNECTION_LISTENER_FILTER_PROP  = MQTT_CONNECTION_LISTENER_FILTER_KEY + "="
            + MQTT_CONNECTION_LISTENER_FILTER_VALUE;
    public static final String MQTT_CONNECTION_LISTENER_FILTER       = "(" + MQTT_CONNECTION_LISTENER_FILTER_PROP + ")";

    private Appendable                 stdout;
    private Appendable                 stderr;
    private int                        shell = -100;
    private CompletableFuture<Boolean> mqttConnectionPromise;

    private final List<EventListener>    eventListeners    = Lists.newCopyOnWriteArrayList();
    private final List<LogEntryListener> logEntryListeners = Lists.newCopyOnWriteArrayList();

    @Activate
    private BundleContext bundleContext;

    @Reference
    private LoggerFactory factory;

    @Reference
    private EventAdmin eventAdmin;

    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile MessageSubscription subscriber;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    private FluentLogger logger;
    private OSGiResult   mqttMessagingCondition;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Deactivate
    void deactivate() {
        // this will be called when the agent is disconnected to deregister the service
        Optional.ofNullable(mqttMessagingCondition).ifPresent(OSGiResult::close);
        mqttMessagingCondition = null;
    }

    @Override
    public RpcType getType() {
        return remoteRPC instanceof MqttRPC<Supervisor, Agent> ? MQTT_RPC : SOCKET_RPC;
    }

    @Override
    public void connect(final SocketConnection socketConnection) throws Exception {
        checkNotNull(socketConnection, "'socketConnection' cannot be null");
        connectToSocket(Agent.class, this, socketConnection);
    }

    @Override
    public void connect(final MqttConnection mqttConnection) throws Exception {
        checkNotNull(mqttConnection, "'mqttConnection' cannot be null");

        mqttConnectionPromise = new CompletableFuture<>();
        try {
            // @formatter:off
            mqttMessagingCondition = connectToMQTT(
                                              bundleContext,
                                              configurationAdmin,
                                              Agent.class,
                                              this,
                                              mqttConnection,
                                              MQTT_CONDITION_ID);
            // @formatter:on
            mqttConnectionPromise.get(mqttConnection.timeout(), MILLISECONDS);

            final var lwtTopic = mqttConnection.lwtTopic();
            if (subscriber != null && !Strings.isNullOrEmpty(lwtTopic)) {
                subscriber.subscribe(lwtTopic).forEach(t -> {
                    logger.atInfo().log("Server notified about the disconnection of the remote agent");
                    sendEvent(AGENT_DISCONNECTED_EVENT_TOPIC);
                });
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (final Exception e) {
            throw e;
        }
    }

    @Override
    public boolean stdout(final String out) throws Exception {
        if (stdout != null) {
            stdout.append(out);
            return true;
        }
        return false;
    }

    @Override
    public boolean stderr(final String out) throws Exception {
        if (stderr != null) {
            stderr.append(out);
            return true;
        }
        return false;
    }

    @Override
    public void onOSGiEvent(final XEventDTO event) {
        checkNotNull(event, "'event' cannot be null");
        eventListeners.stream().filter(l -> matchTopic(event.topic, l.topics()))
                .forEach(listener -> listener.onEvent(event));
    }

    @Override
    public void logged(final XLogEntryDTO logEvent) {
        checkNotNull(logEvent, "'logEvent' cannot be null");
        logEntryListeners.forEach(listener -> listener.logged(logEvent));
    }

    @Override
    public void addOSGiEventListener(final EventListener eventListener) {
        checkNotNull(eventListener, "'logEntryListener' cannot be null");
        if (eventListeners.contains(eventListener)) {
            return;
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeOSGiEventListener(final EventListener eventListener) {
        checkNotNull(eventListener, "'eventListener' cannot be null");
        eventListeners.remove(eventListener);
    }

    @Override
    public void addOSGiLogListener(final LogEntryListener logEntryListener) {
        checkNotNull(logEntryListener, "'logEntryListener' cannot be null");
        if (logEntryListeners.contains(logEntryListener)) {
            return;
        }
        logEntryListeners.add(logEntryListener);
    }

    @Override
    public void removeOSGiLogListener(final LogEntryListener logEntryListener) {
        checkNotNull(logEntryListener, "'logEntryListener' cannot be null");
        logEntryListeners.remove(logEntryListener);
    }

    @Override
    public synchronized void onConnected(final MqttClientConnectedContext context) {
        logger.atInfo().log("Successfully connected to '%s'", context.getClientConfig().getServerHost());
        mqttConnectionPromise.complete(true);
    }

    @Override
    public synchronized void onDisconnected(final MqttClientDisconnectedContext context) {
        logger.atInfo().log("Successfully disconnected from '%s'", context.getClientConfig().getServerHost());
        final Throwable cause = context.getCause();
        if (mqttConnectionPromise != null && cause != null) {
            mqttConnectionPromise.completeExceptionally(cause);
        }
    }

    public void setStdout(final Appendable out) throws Exception {
        stdout = out;
    }

    public void setStderr(final Appendable err) throws Exception {
        stderr = err;
    }

    public void setStdin(final InputStream in) throws Exception {
        final var isr = new InputStreamReader(in);

        final Thread stdin = new Thread("stdin") {
            @Override
            public void run() {
                final var sb = new StringBuilder();
                while (!isInterrupted()) {
                    try {
                        if (isr.ready()) {
                            final var read = isr.read();
                            if (read < 0) {
                                return;
                            }
                            sb.append((char) read);
                        } else if (sb.length() == 0) {
                            sleep(100);
                        } else {
                            getAgent().stdin(sb.toString());
                            sb.setLength(0);
                        }
                    } catch (final Exception e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }
        };
        stdin.start();
    }

    public void setStreams(final Appendable out, final Appendable err) throws Exception {
        setStdout(out);
        setStderr(err);
        getAgent().redirect(shell);
    }

    /**
     * The shell port to use.
     *
     * <ul>
     * <li>&lt;0 – Attach to a local Gogo CommandSession
     * <li>0 – Use the standard console
     * <li>else – Open a stream to that port
     * </ul>
     */
    public void setShell(final int shellPort) {
        shell = shellPort;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void disconnect() throws Exception {
        if (isOpen()) {
            getAgent().disconnect();
            remoteRPC.close();
        }
        mqttConnectionPromise = null;
        // this will be called when the agent is disconnected to deregister the service
        Optional.ofNullable(mqttMessagingCondition).ifPresent(OSGiResult::close);
        mqttMessagingCondition = null;
    }

    public void redirect(final int shell) throws Exception {
        if (this.shell != shell && isOpen()) {
            getAgent().redirect(shell);
            this.shell = shell;
        }
    }

    private static boolean matchTopic(final String receivedEventTopic, final Collection<String> listenerTopics) {
        if (listenerTopics.contains("*")) {
            return true;
        }
        for (final String topic : listenerTopics) {
            // positive match if only *
            if ("*".equals(topic)) {
                return true;
            }
            // positive match if it does contain * at the end and is a substring of the
            // received event topic
            if (topic.contains("*")) {
                final var prefix = Substring.upToIncluding(Substring.last("/")).from(topic).orElse(null);
                if (receivedEventTopic.startsWith(prefix)) {
                    return true;
                }
            }
            // positive match if the it matches exactly the received event topic
            if (receivedEventTopic.equalsIgnoreCase(topic)) {
                return true;
            }
        }
        return false;
    }

    private void sendEvent(final String topic) {
        final var event = new Event(topic, Map.of());
        eventAdmin.postEvent(event);
    }

}
