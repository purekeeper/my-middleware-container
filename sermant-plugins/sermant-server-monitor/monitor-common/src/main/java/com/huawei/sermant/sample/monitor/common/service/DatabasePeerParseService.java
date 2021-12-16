/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.sermant.sample.monitor.common.service;

import com.huawei.sermant.core.plugin.service.PluginService;

/**
 * Database peer 解析服务
 */
public interface DatabasePeerParseService extends PluginService {

    /**
     * 把URL解析成database peer
     *
     * @param url database url
     * @return database peer
     */
    String parse(String url);
}