/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timerservice;

import java.lang.reflect.Method;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerConfig;

/**
 * Holds data about an automatic timer
 * @author Stuart Douglas
 */
public final class AutoTimer {
    private final ScheduleExpression scheduleExpression;
    private final TimerConfig timerConfig;
    private Method method;

    public AutoTimer() {
        scheduleExpression = new ScheduleExpression();
        timerConfig = new TimerConfig();
    }

    public AutoTimer(final ScheduleExpression scheduleExpression, final TimerConfig timerConfig, final Method method) {
        this.scheduleExpression = scheduleExpression;
        this.timerConfig = timerConfig;
        this.method = method;
    }

    public ScheduleExpression getScheduleExpression() {
        return scheduleExpression;
    }

    public TimerConfig getTimerConfig() {
        return timerConfig;
    }

    public Method getMethod() {
        return method;
    }
}
