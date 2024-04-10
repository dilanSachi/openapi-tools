/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.openapi.core.generators.client.mime;

import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.openapi.core.generators.common.GeneratorConstants;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

import static io.ballerina.openapi.core.generators.common.GeneratorConstants.BYTE;

/**
 * Defines the payload structure of "application/octet-stream" mime type.
 *
 * @since 1.3.0
 */
public class OctetStreamType extends MimeType {

    @Override
    public void setPayload(List<StatementNode> statementsList, Map.Entry<String, MediaType> mediaTypeEntry) {

        String payloadName = GeneratorConstants.PAYLOAD;
        Schema<?> mediaTypeSchema = mediaTypeEntry.getValue().getSchema();
        if (mediaTypeSchema.getFormat() != null && mediaTypeSchema.getFormat().equals(BYTE)) {
            payloadName = "encodedRequestBody";
            VariableDeclarationNode encodedVariable = GeneratorUtils.getSimpleStatement("string",
                    payloadName, "payload.toBase64()");
            statementsList.add(encodedVariable);
        }
        setPayload(statementsList, payloadName, javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM);
    }
}
