/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
package io.ballerina.openapi.service.mapper.example.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.openapi.service.mapper.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.service.mapper.diagnostic.ExceptionDiagnostic;
import io.ballerina.openapi.service.mapper.diagnostic.OpenAPIMapperDiagnostic;
import io.ballerina.openapi.service.mapper.example.ExamplesMapper;
import io.ballerina.tools.diagnostics.Location;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.openapi.service.mapper.Constants.EXAMPLE;
import static io.ballerina.openapi.service.mapper.Constants.EXAMPLES;
import static io.ballerina.openapi.service.mapper.Constants.REQUEST;
import static io.ballerina.openapi.service.mapper.example.CommonUtils.hasBothOpenAPIExampleAnnotations;

/**
 * This {@link RequestExampleMapper} class represents the example mapper for resource function parameter.
 *
 * @since 2.1.0
 */
public class RequestExampleMapper extends ExamplesMapper {

    RequestBody requestBody;
    List<OpenAPIMapperDiagnostic> diagnostics;
    List<AnnotationAttachmentSymbol> annotations;
    boolean disabled = false;
    String paramName;
    Location location;

    public RequestExampleMapper(ParameterSymbol parameterSymbol, RequestBody requestBody, SemanticModel semanticModel,
                                List<OpenAPIMapperDiagnostic> diagnostics) {
        super(semanticModel);
        this.annotations = parameterSymbol.annotAttachments();
        this.requestBody = requestBody;
        this.diagnostics = diagnostics;
        this.paramName = parameterSymbol.getName().orElse("");
        this.location = parameterSymbol.getLocation().orElse(null);

        if (hasBothOpenAPIExampleAnnotations(annotations, semanticModel)) {
            diagnostics.add(new ExceptionDiagnostic(DiagnosticMessages.OAS_CONVERTOR_136, location));
            disabled = true;
        }
    }

    @Override
    public void setExample() {
        if (disabled) {
            return;
        }
        try {
            Optional<Object> exampleValue = extractExample(annotations);
            if (exampleValue.isEmpty()) {
                return;
            }

            Content content = getValidatedContent();
            if (Objects.isNull(content)) {
                return;
            }
            content.forEach((mediaType, mediaTypeObject) -> mediaTypeObject.setExample(exampleValue.get()));
        } catch (JsonProcessingException exception) {
            diagnostics.add(new ExceptionDiagnostic(DiagnosticMessages.OAS_CONVERTOR_134, location, EXAMPLE,
                    REQUEST, paramName));
        }
    }

    private Content getValidatedContent() {
        Content content = requestBody.getContent();
        if (Objects.isNull(content)) {
            return null;
        }
        int mediaTypesSize = content.size();
        if (mediaTypesSize != 1) {
            diagnostics.add(new ExceptionDiagnostic(DiagnosticMessages.OAS_CONVERTOR_135, location, paramName));
            return null;
        }
        return content;
    }

    @Override
    public void setExamples() {
        if (disabled) {
            return;
        }
        try {
            Optional<Map<String, Example>> exampleValues = extractExamples(annotations);
            if (exampleValues.isEmpty()) {
                return;
            }
            Content content = getValidatedContent();
            if (Objects.isNull(content)) {
                return;
            }
            content.forEach((mediaType, mediaTypeObject) -> mediaTypeObject.setExamples(exampleValues.get()));
        }  catch (JsonProcessingException exception) {
            diagnostics.add(new ExceptionDiagnostic(DiagnosticMessages.OAS_CONVERTOR_134, location, EXAMPLES,
                    REQUEST, paramName));
        }
    }
}
