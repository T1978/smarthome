/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.core.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;

/**
 * This class is implementation of {@link TriggerHandlerCallback} used by the {@link Trigger}s to notify rule engine
 * about
 * appearing of new triggered data. There is one and only one {@link TriggerHandlerCallback} per RuleImpl and it is used
 * by
 * all
 * rule's {@link Trigger}s.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Kai Kreuzer - improved stability
 */
public class TriggerHandlerCallbackImpl implements TriggerHandlerCallback {

    private final String ruleUID;

    private ExecutorService executor;

    private Future<?> future;

    private final RuleEngineImpl re;

    protected TriggerHandlerCallbackImpl(RuleEngineImpl re, String ruleUID) {
        this.re = re;
        this.ruleUID = ruleUID;
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void triggered(Trigger trigger, Map<String, ?> outputs) {
        synchronized (this) {
            if (executor == null) {
                return;
            }
            future = executor.submit(new TriggerData(trigger, outputs));
        }
        re.logger.debug("The trigger '{}' of rule '{}' is triggered.", trigger.getId(), ruleUID);
    }

    public boolean isRunning() {
        Future<?> future = this.future;
        return future == null || !future.isDone();
    }

    class TriggerData implements Runnable {

        private final Trigger trigger;

        public Trigger getTrigger() {
            return trigger;
        }

        public Map<String, ?> getOutputs() {
            return outputs;
        }

        private final Map<String, ?> outputs;

        public TriggerData(Trigger t, Map<String, ?> outputs) {
            this.trigger = t;
            this.outputs = outputs;
        }

        @Override
        public void run() {
            re.runRule(ruleUID, this);
        }
    }

    public void dispose() {
        synchronized (this) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                executor.shutdownNow();
                return null;
            });
            executor = null;
        }
    }

    @Override
    public Boolean isEnabled(String ruleUID) {
        return re.isEnabled(ruleUID);
    }

    @Override
    public void setEnabled(String uid, boolean isEnabled) {
        re.setEnabled(uid, isEnabled);
    }

    @Override
    public RuleStatusInfo getStatusInfo(String ruleUID) {
        return re.getStatusInfo(ruleUID);
    }

    @Override
    public RuleStatus getStatus(String ruleUID) {
        return re.getStatus(ruleUID);
    }

    @Override
    public void runNow(String uid) {
        re.runNow(uid);
    }

    @Override
    public void runNow(String uid, boolean considerConditions, Map<String, Object> context) {
        re.runNow(uid, considerConditions, context);
    }

}
