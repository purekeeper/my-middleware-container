/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huaweicloud.sermant.implement.log;

import com.huaweicloud.sermant.core.event.collector.LogEventCollector;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.spi.LocationAwareLogger;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Sermant日志桥接
 *
 * @author luanwenfei
 * @since 2023-03-20
 */
public class SermantBridgeHandler extends SLF4JBridgeHandler {
    @Override
    protected void callLocationAwareLogger(LocationAwareLogger lal, LogRecord record) {
        // 覆写SLF4JBridgeHandler的日志转换方法，上报日志事件
        int julLevelValue = record.getLevel().intValue();

        if (julLevelValue > Level.INFO.intValue() && julLevelValue <= Level.WARNING.intValue()) {
            // 记录警告级别日志
            LogEventCollector.getInstance().offerWarning(record);
        } else if (julLevelValue > Level.WARNING.intValue()) {
            // 记录错误级别日志
            LogEventCollector.getInstance().offerError(record);
        }
        super.callLocationAwareLogger(lal, record);
    }
}
