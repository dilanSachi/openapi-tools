/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.openapi.core.generators.service;

import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

public interface ListenerGenerator {

    int HTTP_PORT = 80;
    int HTTPS_PORT = 443;

    String getBasePath();

    ListenerDeclarationNode getListenerDeclarationNodes(List<Server> servers) throws BallerinaOpenApiException;
}
