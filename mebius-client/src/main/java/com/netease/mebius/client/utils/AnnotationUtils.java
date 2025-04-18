 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.netease.mebius.client.utils;

import com.netease.mebius.client.enums.Annotation;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 * 注解工具类
 */
public class AnnotationUtils {

    /**
     * 初始化需要的筛选的类注解
     *
     * @param annotations
     * @return
     */
    public static List<String> init(List<String> annotations) {
            List<String> NewAnnotations = Lists.newArrayList();
        if (CollectionUtils.isEmpty(annotations)) {
            //暂时只加了接口
            NewAnnotations.add(Annotation.Controller.name());
            NewAnnotations.add(Annotation.RestController.name());
            NewAnnotations.add(Annotation.FeignClient.name());
            NewAnnotations.add(Annotation.Service.name());
            NewAnnotations.add(Annotation.Component.name());
            NewAnnotations.add(Annotation.RpcService.name());
            NewAnnotations.add(Annotation.RemoteMethod.name());
            NewAnnotations.add(Annotation.DubboService.name());
            NewAnnotations.add(Annotation.InnerAPI.name());
        }else {
            NewAnnotations.addAll(annotations);
        }
        return NewAnnotations;
    }
}