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
package com.netease.mebius.client.executor.parser;

import com.netease.mebius.client.constant.ConstantVar;
import com.netease.mebius.client.enums.Annotation;
import com.netease.mebius.client.enums.ClassType;
import com.netease.mebius.client.model.CallMethodNode;
import com.netease.mebius.client.model.MethodCallResult;
import com.netease.mebius.client.model.UrlMappingInfo;
import com.netease.mebius.client.utils.ParseUtils;
import jdk.internal.org.objectweb.asm.tree.AnnotationNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dubbo注解解析器
 */
@Slf4j
public class DubboParser implements AnnotationParser {

    @Override
    public void parse(MethodCallResult resultCallMethod, CallMethodNode callMethodNode) {
        // 处理DubboService注解，类似于Service注解
        if (StringUtils.equals(resultCallMethod.getAnnotation(), Annotation.DubboService.name())) {
            resultCallMethod.setClassType(ClassType.Dubbo);
            resultCallMethod.setTopEntry(callMethodNode.getClassName().replace("/", ".") + "." +
                    callMethodNode.getMethodSig().split("#")[0]);
            return;
        }
        
        // 处理InnerAPI注解，类似于Controller注解，可以与RequestMapping等共存
        if (StringUtils.equals(resultCallMethod.getAnnotation(), Annotation.InnerAPI.name()) ||
                StringUtils.equals(resultCallMethod.getAnnotation(), Annotation.Controller.name()) ||
                StringUtils.equals(resultCallMethod.getAnnotation(), Annotation.RestController.name())) {
            // 先从class注解中获取根mapping路径
            ArrayList<String> baseRequestMappingList = Lists.newArrayList();
            
            // 查找类上的@RequestMapping或@InnerAPI注解
            for (AnnotationNode node : callMethodNode.getClassNode().visibleAnnotations) {
                if ((StringUtils.equals(ParseUtils.getSimpleName(node.desc), Annotation.RequestMapping.name()) ||
                        StringUtils.equals(ParseUtils.getSimpleName(node.desc), Annotation.InnerAPI.name())) && node.values != null) {
                    for (int i = 0; i < node.values.size(); i++) {
                        if (node.values.get(i).equals("value") || node.values.get(i).equals("path")) {
                            if (node.values.get(i + 1) instanceof String) {
                                String baseRequestMapping = node.values.get(i + 1).toString();
                                if (baseRequestMapping.endsWith("/")) {
                                    baseRequestMapping = baseRequestMapping.substring(0, baseRequestMapping.length() - 1);
                                    baseRequestMappingList.add(baseRequestMapping);
                                } else
                                    baseRequestMappingList.add(baseRequestMapping);
                            } else if (node.values.get(i + 1) instanceof ArrayList) {
                                ArrayList<String> oldBaseRequestMappingList = (ArrayList) node.values.get(i + 1);
                                if (oldBaseRequestMappingList != null && oldBaseRequestMappingList.size() != 0) {
                                    for (String baseRequestMapping : oldBaseRequestMappingList) {
                                        if (baseRequestMapping.endsWith("/")) {
                                            baseRequestMapping = baseRequestMapping.substring(0, baseRequestMapping.length() - 1);
                                            baseRequestMappingList.add(baseRequestMapping);
                                        } else
                                            baseRequestMappingList.add(baseRequestMapping);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 获取方法上的所有注解
            List<AnnotationNode> annotationNodes = callMethodNode.getMethodNode().visibleAnnotations;

            // 处理方法注解
            if (annotationNodes != null) {
                for (AnnotationNode annotationNode : annotationNodes) {
                    // 先查看方法上是否有InnerAPI注解
                    boolean isInnerAPI = StringUtils.equals(ParseUtils.getSimpleName(annotationNode.desc), Annotation.InnerAPI.name());
                    
                    // 如果是InnerAPI注解或Web接口注解（RequestMapping, PostMapping等）
                    if (isInnerAPI || Arrays.asList(ConstantVar.INTERFACE_ANNOTATIONS).contains(ParseUtils.getSimpleName(annotationNode.desc))) {
                        ArrayList<String> mappingUrlList = Lists.newArrayList();

                        if (annotationNode.values != null) {
                            for (int i = 0; i < ((ArrayList) annotationNode.values).size(); i++) {
                                if (((ArrayList) annotationNode.values).get(i).equals("value") || ((ArrayList) annotationNode.values).get(i).equals("path")) {
                                    // 可能是单个值或数组
                                    Object pathValue = annotationNode.values.get(i + 1);
                                    ArrayList<String> values = new ArrayList<>();
                                    
                                    if (pathValue instanceof String) {
                                        values.add((String) pathValue);
                                    } else if (pathValue instanceof ArrayList) {
                                        values.addAll((ArrayList<String>) pathValue);
                                    }
                                    
                                    if (values != null && !values.isEmpty()) {
                                        for (String subMapping : values) {
                                            if (baseRequestMappingList != null && baseRequestMappingList.size() != 0) {
                                                for (String baseRequestMapping : baseRequestMappingList) {
                                                    String mappingUrl = ParseUtils.standardMappingUrl(baseRequestMapping) + ParseUtils.standardMappingUrl(subMapping);
                                                    mappingUrlList.add(mappingUrl);
                                                }
                                            } else {
                                                String mappingUrl = ParseUtils.standardMappingUrl(subMapping);
                                                mappingUrlList.add(mappingUrl);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 如果没有找到URL路径，使用类级别的路径
                        if (mappingUrlList.isEmpty() && !baseRequestMappingList.isEmpty()) {
                            for (String baseRequestMapping : baseRequestMappingList) {
                                String mappingUrl = ParseUtils.standardMappingUrl(baseRequestMapping);
                                mappingUrlList.add(mappingUrl);
                            }
                        }
                        
                        // 设置路径
                        if (!mappingUrlList.isEmpty()) {
                            resultCallMethod.setTopEntry(mappingUrlList.stream().collect(Collectors.joining(",")));
                        } else {
                            // 如果没有找到任何路径，使用类名和方法名
                            resultCallMethod.setTopEntry(callMethodNode.getClassName().replace("/", ".") + "." +
                                    callMethodNode.getMethodSig().split("#")[0]);
                        }
                        
                        // 设置HTTP方法
                        if (StringUtils.equals(ParseUtils.getSimpleName(annotationNode.desc), Annotation.PostMapping.name())) {
                            resultCallMethod.setHttpMethod("POST");
                        } else if (StringUtils.equals(ParseUtils.getSimpleName(annotationNode.desc), Annotation.GetMapping.name())) {
                            resultCallMethod.setHttpMethod("GET");
                        } else if (StringUtils.equals(ParseUtils.getSimpleName(annotationNode.desc), Annotation.RequestMapping.name())) {
                            resultCallMethod.setHttpMethod("GET");
                            
                            // 检查是否有ResponseBody注解
                            for (AnnotationNode an : annotationNodes) {
                                if (StringUtils.equals(Annotation.ResponseBody.name(), ParseUtils.getSimpleName(an.desc))) {
                                    resultCallMethod.setHttpMethod("POST");
                                    break;
                                }
                            }
                        }
                        
                        // 如果是InnerAPI，设置类型为Dubbo，否则为Controller
                        resultCallMethod.setClassType(isInnerAPI ? ClassType.Dubbo : ClassType.Controller);
                    }
                }
            }
        }
    }

    @Override
    public void parseUrl(UrlMappingInfo urlMappingInfo, CallMethodNode callMethodNode) {
        // 仅处理DubboService和InnerAPI注解
        if (!StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.DubboService.name()) &&
                !StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.InnerAPI.name()) &&
                !StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.Controller.name()) &&
                !StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.RestController.name())) {
            return;
        }
        
        // 处理InnerAPI和Controller/RestController注解的逻辑类似
        if (StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.InnerAPI.name()) ||
                StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.Controller.name()) ||
                StringUtils.equals(urlMappingInfo.getAnnotation(), Annotation.RestController.name())) {
            // 先从class注解中获取根路径
            String baseRequestMapping = "";
            for (AnnotationNode node : callMethodNode.getClassNode().visibleAnnotations) {
                if ((StringUtils.equals(ParseUtils.getSimpleName(node.desc), Annotation.RequestMapping.name()) ||
                        StringUtils.equals(ParseUtils.getSimpleName(node.desc), Annotation.InnerAPI.name())) && node.values != null) {
                    for (int i = 0; i < node.values.size(); i++) {
                        if (node.values.get(i).equals("value") || node.values.get(i).equals("path")) {
                            if (node.values.get(i + 1) instanceof String) {
                                baseRequestMapping = node.values.get(i + 1).toString();
                                if (baseRequestMapping.endsWith("/")) {
                                    baseRequestMapping = baseRequestMapping.substring(0, baseRequestMapping.length() - 1);
                                }
                            } else if (node.values.get(i + 1) instanceof ArrayList) {
                                ArrayList<String> values = (ArrayList) node.values.get(i + 1);
                                if (values != null && !values.isEmpty()) {
                                    baseRequestMapping = values.get(0);
                                    if (baseRequestMapping.endsWith("/")) {
                                        baseRequestMapping = baseRequestMapping.substring(0, baseRequestMapping.length() - 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 获取方法上的所有注解
            List<AnnotationNode> annotationNodes = callMethodNode.getMethodNode().visibleAnnotations;

            // 处理方法注解
            if (annotationNodes != null) {
                for (AnnotationNode annotationNode : annotationNodes) {
                    // 处理InnerAPI或Web接口注解
                    if (StringUtils.equals(ParseUtils.getSimpleName(annotationNode.desc), Annotation.InnerAPI.name()) || 
                            Arrays.asList(ConstantVar.INTERFACE_ANNOTATIONS).contains(ParseUtils.getSimpleName(annotationNode.desc))) {
                        String subMapping = "";
                        if (annotationNode.values != null) {
                            for (int i = 0; i < ((ArrayList) annotationNode.values).size(); i++) {
                                if (((ArrayList) annotationNode.values).get(i).equals("value") || ((ArrayList) annotationNode.values).get(i).equals("path")) {
                                    Object pathValue = annotationNode.values.get(i + 1);
                                    if (pathValue instanceof String) {
                                        subMapping = ParseUtils.standardMappingUrl((String) pathValue);
                                    } else if (pathValue instanceof ArrayList) {
                                        ArrayList<String> values = (ArrayList) pathValue;
                                        if (values != null && !values.isEmpty()) {
                                            subMapping = ParseUtils.standardMappingUrl(values.get(0));
                                        }
                                    }
                                }
                            }
                        }
                        urlMappingInfo.setMappingUrl(baseRequestMapping + subMapping);
                    }
                }
            }
        }
    }
}