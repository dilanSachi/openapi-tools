/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.openapi.service.mapper.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.openapi.service.mapper.Constants;
import io.ballerina.openapi.service.mapper.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.service.mapper.diagnostic.ExceptionDiagnostic;
import io.ballerina.openapi.service.mapper.diagnostic.OpenAPIMapperDiagnostic;
import io.ballerina.openapi.service.mapper.model.ModuleMemberVisitor;
import io.ballerina.openapi.service.mapper.model.OASResult;
import io.ballerina.openapi.service.mapper.type.TypeMapper;
import io.ballerina.runtime.api.utils.IdentifierUtils;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LIST_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAPPING_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NUMERIC_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.openapi.service.mapper.Constants.BALLERINA;
import static io.ballerina.openapi.service.mapper.Constants.HTTP;
import static io.ballerina.openapi.service.mapper.Constants.HYPHEN;
import static io.ballerina.openapi.service.mapper.Constants.JSON_EXTENSION;
import static io.ballerina.openapi.service.mapper.Constants.SLASH;
import static io.ballerina.openapi.service.mapper.Constants.SPECIAL_CHAR_REGEX;
import static io.ballerina.openapi.service.mapper.Constants.UNDERSCORE;
import static io.ballerina.openapi.service.mapper.Constants.YAML_EXTENSION;

/**
 * Utilities used in Ballerina  to OpenAPI converter.
 */
public class MapperCommonUtils {

    private static final SyntaxKind[] validExpressionKind = {STRING_LITERAL, NUMERIC_LITERAL, BOOLEAN_LITERAL,
            LIST_CONSTRUCTOR, MAPPING_CONSTRUCTOR};

    /**
     * Retrieves a matching OpenApi {@link Schema} for a provided ballerina type.
     *
     * @param type ballerina type name as a String
     * @return OpenApi {@link Schema} for type defined by {@code type}
     */
    public static Schema<?> getOpenApiSchema(String type) {
        Schema<?> schema;
        switch (type) {
            case Constants.STRING:
            case Constants.PLAIN:
                schema = new StringSchema();
                break;
            case Constants.BOOLEAN:
                schema = new BooleanSchema();
                break;
            case Constants.ARRAY:
            case Constants.TUPLE:
                schema = new ArraySchema();
                break;
            case Constants.INT:
            case Constants.INTEGER:
                schema = new IntegerSchema();
                schema.setFormat("int64");
                break;
            case Constants.BYTE_ARRAY:
            case Constants.OCTET_STREAM:
                schema = new StringSchema();
                schema.setFormat("byte");
                break;
            case Constants.NUMBER:
            case Constants.DECIMAL:
                schema = new NumberSchema();
                schema.setFormat(Constants.DOUBLE);
                break;
            case Constants.FLOAT:
                schema = new NumberSchema();
                schema.setFormat(Constants.FLOAT);
                break;
            case Constants.MAP_JSON:
            case Constants.MAP:
                schema = new ObjectSchema();
                schema.additionalProperties(true);
                break;
            case Constants.X_WWW_FORM_URLENCODED:
                schema = new ObjectSchema();
                schema.setAdditionalProperties(new StringSchema());
                break;
            case Constants.TYPE_REFERENCE:
            case Constants.TYPEREFERENCE:
            case Constants.XML:
            case Constants.JSON:
            default:
                schema = new Schema<>();
                break;
        }
        return schema;
    }

    /**
     * Retrieves a matching OpenApi {@link Schema} for a provided ballerina type.
     *
     * @param type ballerina type with SYNTAX KIND
     * @return OpenApi {@link Schema} for type defined by {@code type}
     */
    public static Schema<?> getOpenApiSchema(SyntaxKind type) {
        Schema<?> schema;
        switch (type) {
            case STRING_TYPE_DESC:
                schema = new StringSchema();
                break;
            case BOOLEAN_TYPE_DESC:
                schema = new BooleanSchema();
                break;
            case ARRAY_TYPE_DESC:
                schema = new ArraySchema();
                break;
            case INT_TYPE_DESC:
                schema = new IntegerSchema();
                schema.setFormat("int64");
                break;
            case BYTE_TYPE_DESC:
                schema = new StringSchema();
                schema.setFormat("byte");
                break;
            case DECIMAL_TYPE_DESC:
                schema = new NumberSchema();
                schema.setFormat(Constants.DOUBLE);
                break;
            case FLOAT_TYPE_DESC:
                schema = new NumberSchema();
                schema.setFormat(Constants.FLOAT);
                break;
            case MAP_TYPE_DESC:
                schema = new ObjectSchema();
                break;
            default:
                schema = new Schema<>();
                break;
        }
        return schema;
    }

    public static String generateRelativePath(FunctionDefinitionNode resourceFunction) {
        StringBuilder relativePath = new StringBuilder();
        relativePath.append("/");
        if (!resourceFunction.relativeResourcePath().isEmpty()) {
            for (Node node: resourceFunction.relativeResourcePath()) {
                if (node instanceof ResourcePathParameterNode pathNode) {
                    relativePath.append("{");
                    relativePath.append(pathNode.paramName().get());
                    relativePath.append("}");
                } else if ((resourceFunction.relativeResourcePath().size() == 1)
                        && (node.toString().trim().equals("."))) {
                    return relativePath.toString();
                } else {
                    relativePath.append(node.toString().trim());
                }
            }
        }
        return relativePath.toString();
    }

    /**
     * Generate operationId by removing special characters.
     *
     * @param operationID input function name, record name or operation Id
     * @return string with new generated name
     */
    public static String getOperationId(String operationID) {
        //For the flatten enable we need to remove first Part of valid name check
        // this - > !operationID.matches("\\b[a-zA-Z][a-zA-Z0-9]*\\b") &&
        if (operationID.matches("\\b[0-9]*\\b")) {
            return operationID;
        }
        String[] split = operationID.split(Constants.SPECIAL_CHAR_REGEX);
        StringBuilder validName = new StringBuilder();
        for (String part : split) {
            if (!part.isBlank()) {
                if (split.length > 1) {
                    part = part.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                            part.substring(1).toLowerCase(Locale.ENGLISH);
                }
                validName.append(part);
            }
        }
        operationID = validName.toString();
        return operationID.substring(0, 1).toLowerCase(Locale.ENGLISH) + operationID.substring(1);
    }

    /**
     * This util function uses to take the field value from annotation field.
     *
     * @param annotations         - Annotation node list
     * @param annotationReference - Annotation reference that needs to extract
     * @param annotationField     - Annotation field name that uses to take value
     * @return - string value of field
     */
    public static Optional<String> extractServiceAnnotationDetails(NodeList<AnnotationNode> annotations,
                                                                   String annotationReference, String annotationField) {
        for (AnnotationNode annotation : annotations) {
            List<String> expressionNode = extractAnnotationFieldDetails(annotationReference, annotationField,
                    annotation, null);
            if (!expressionNode.isEmpty()) {
                return Optional.of(expressionNode.get(0));
            }
        }
        return Optional.empty();
    }

    /**
     * This util functions is used to extract the details of annotation field.
     *
     * @param annotationReference Annotation reference name that need to extract
     * @param annotationField     Annotation field name that need to extract details.
     * @param annotation          Annotation node
     * @return List of string
     */

    public static List<String> extractAnnotationFieldDetails(String annotationReference, String annotationField,
                                                             AnnotationNode annotation, SemanticModel semanticModel) {
        List<String> mediaTypes = new ArrayList<>();
        Node annotReference = annotation.annotReference();
        if (annotReference.toString().trim().equals(annotationReference) && annotation.annotValue().isPresent()) {
            MappingConstructorExpressionNode listOfAnnotValue = annotation.annotValue().get();
            for (MappingFieldNode field : listOfAnnotValue.fields()) {
                SpecificFieldNode fieldNode = (SpecificFieldNode) field;
                if (!((fieldNode).fieldName().toString().trim().equals(annotationField)) ||
                        fieldNode.valueExpr().isEmpty()) {
                    continue;
                }
                ExpressionNode expressionNode = fieldNode.valueExpr().get();
                if (expressionNode instanceof ListConstructorExpressionNode) {
                    SeparatedNodeList<Node> mimeList = ((ListConstructorExpressionNode) expressionNode).expressions();
                    for (Object mime : mimeList) {
                        if (!(mime instanceof BasicLiteralNode)) {
                            continue;
                        }
                        mediaTypes.add(((BasicLiteralNode) mime).literalToken().text().trim().replaceAll("\"", ""));
                    }
                } else if (expressionNode instanceof QualifiedNameReferenceNode && semanticModel != null) {
                    QualifiedNameReferenceNode moduleRef = (QualifiedNameReferenceNode) expressionNode;
                    Optional<Symbol> refSymbol = semanticModel.symbol(moduleRef);
                    if (refSymbol.isPresent() && (refSymbol.get().kind() == SymbolKind.CONSTANT)
                            && ((ConstantSymbol) refSymbol.get()).resolvedValue().isPresent()) {
                        String mediaType = ((ConstantSymbol) refSymbol.get()).resolvedValue().get();
                        mediaTypes.add(mediaType.replaceAll("\"", ""));
                    }
                } else {
                    mediaTypes.add(expressionNode.toString().trim().replaceAll("\"", ""));
                }
            }
        }
        return mediaTypes;
    }

    /**
     * This function uses to take the service declaration node from given required node and return all the annotation
     * nodes that attached to service node.
     */
    public static NodeList<AnnotationNode> getAnnotationNodesFromServiceNode(RequiredParameterNode headerParam) {
        NodeList<AnnotationNode> annotations = AbstractNodeFactory.createEmptyNodeList();
        NonTerminalNode parent = headerParam.parent();
        while (parent.kind() != SyntaxKind.SERVICE_DECLARATION) {
            parent = parent.parent();
        }
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) parent;
        if (serviceNode.metadata().isPresent()) {
            MetadataNode metadataNode = serviceNode.metadata().get();
            annotations = metadataNode.annotations();
        }
        return annotations;
    }

    /**
     * This function for taking the specific media-type subtype prefix from http service configuration annotation.
     * <pre>
     *     @http:ServiceConfig {
     *          mediaTypeSubtypePrefix : "vnd.exm.sales"
     *  }
     * </pre>
     */
    public static Optional<String> extractCustomMediaType(FunctionDefinitionNode functionDefNode) {
        ServiceDeclarationNode serviceDefNode = (ServiceDeclarationNode) functionDefNode.parent();
        if (serviceDefNode.metadata().isPresent()) {
            MetadataNode metadataNode = serviceDefNode.metadata().get();
            NodeList<AnnotationNode> annotations = metadataNode.annotations();
            if (!annotations.isEmpty()) {
                return MapperCommonUtils.extractServiceAnnotationDetails(annotations,
                        "http:ServiceConfig", "mediaTypeSubtypePrefix");
            }
        }
        return Optional.empty();
    }

    /**
     * This {@code NullLocation} represents the null location allocation for scenarios which has not location.
     */
    public static class NullLocation implements Location {

        @Override
        public LineRange lineRange() {
            LinePosition from = LinePosition.from(0, 0);
            return LineRange.from("", from, from);
        }

        @Override
        public TextRange textRange() {
            return TextRange.from(0, 0);
        }
    }

    /**
     * Parse and get the {@link OpenAPI} for the given OpenAPI contract.
     *
     * @param definitionURI URI for the OpenAPI contract
     * @return {@link OASResult}  OpenAPI model
     */
    public static OASResult parseOpenAPIFile(String definitionURI) {
        List<OpenAPIMapperDiagnostic> diagnostics = new ArrayList<>();
        Path contractPath = Paths.get(definitionURI);
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        if (!Files.exists(contractPath)) {
            DiagnosticMessages error = DiagnosticMessages.OAS_CONVERTOR_110;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(),
                    error.getDescription(), null);
            diagnostics.add(diagnostic);
        }
        if (!(definitionURI.endsWith(Constants.YAML_EXTENSION) || definitionURI.endsWith(Constants.JSON_EXTENSION)
                || definitionURI.endsWith(Constants.YML_EXTENSION))) {
            DiagnosticMessages error = DiagnosticMessages.OAS_CONVERTOR_110;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(),
                    error.getDescription(), null);
            diagnostics.add(diagnostic);
        }
        String openAPIFileContent = null;
        try {
            openAPIFileContent = Files.readString(contractPath);
        } catch (IOException e) {
            DiagnosticMessages error = DiagnosticMessages.OAS_CONVERTOR_108;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(), error.getDescription(), null,
                    e.toString());
            diagnostics.add(diagnostic);
        }
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openAPIFileContent, null, parseOptions);
        if (!parseResult.getMessages().isEmpty()) {
            DiagnosticMessages error = DiagnosticMessages.OAS_CONVERTOR_112;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(), error.getDescription(), null);
            diagnostics.add(diagnostic);
            return new OASResult(null, diagnostics);
        }
        OpenAPI api = parseResult.getOpenAPI();
        return new OASResult(api, diagnostics);
    }

    public static String normalizeTitle(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        String[] urlPaths = (serviceName.replaceFirst(Constants.SLASH, "")).split(SPECIAL_CHAR_REGEX);
        StringBuilder stringBuilder = new StringBuilder();
        String title = serviceName;
        if (urlPaths.length > 1) {
            for (String path : urlPaths) {
                if (path.isBlank()) {
                    continue;
                }
                stringBuilder.append(path.substring(0, 1).toUpperCase(Locale.ENGLISH));
                stringBuilder.append(path.substring(1));
                stringBuilder.append(" ");
            }
            title = stringBuilder.toString().trim();
        } else if (urlPaths.length == 1 && !urlPaths[0].isBlank()) {
            stringBuilder.append(urlPaths[0].substring(0, 1).toUpperCase(Locale.ENGLISH));
            stringBuilder.append(urlPaths[0].substring(1));
            title = stringBuilder.toString().trim();
        }
        return title;
    }

    /**
     * This util function is to check the given service is http service.
     *
     * @param serviceNode   Service node for analyse
     * @param semanticModel Semantic model
     * @return boolean output
     */
    public static boolean isHttpService(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        Optional<Symbol> serviceSymbol = semanticModel.symbol(serviceNode);
        if (serviceSymbol.isEmpty()) {
            return false;
        }

        ServiceDeclarationSymbol serviceNodeSymbol = (ServiceDeclarationSymbol) serviceSymbol.get();
        List<TypeSymbol> listenerTypes = (serviceNodeSymbol).listenerTypes();
        for (TypeSymbol listenerType : listenerTypes) {
            if (isHttpListener(listenerType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHttpListener(TypeSymbol listenerType) {
        if (listenerType.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isHttpModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isHttpModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }
        return false;
    }

    private static boolean isHttpModule(ModuleSymbol moduleSymbol) {
        if (moduleSymbol.getName().isPresent()) {
            return HTTP.equals(moduleSymbol.getName().get()) && BALLERINA.equals(moduleSymbol.id().orgName());
        } else {
            return false;
        }
    }

    /**
     * Generate file name with service basePath.
     */
    public static String getOpenApiFileName(String servicePath, String serviceName, boolean isJson) {
        String openAPIFileName;
        if (serviceName.isBlank() || serviceName.equals(SLASH) || serviceName.startsWith(SLASH + HYPHEN)) {
            String[] fileName = serviceName.split(SLASH);
            // This condition is to handle `service on ep1 {} ` multiple scenarios
            if (fileName.length > 0 && !serviceName.isBlank()) {
                openAPIFileName = FilenameUtils.removeExtension(servicePath) + fileName[1];
            } else {
                openAPIFileName = FilenameUtils.removeExtension(servicePath);
            }
        } else if (serviceName.startsWith(HYPHEN)) {
            // serviceName -> service on ep1 {} has multiple service ex: "-33456"
            openAPIFileName = FilenameUtils.removeExtension(servicePath) + serviceName;
        } else {
            // Remove starting path separate if exists
            if (serviceName.startsWith(SLASH)) {
                serviceName = serviceName.substring(1);
            }
            // Replace rest of the path separators with underscore
            openAPIFileName = serviceName.replaceAll(SLASH, "_");
        }

        return getNormalizedFileName(openAPIFileName) + Constants.OPENAPI_SUFFIX +
                (isJson ? JSON_EXTENSION : YAML_EXTENSION);
    }

    /**
     * Remove special characters from the given file name.
     */
    public static String getNormalizedFileName(String openAPIFileName) {

        String[] splitNames = openAPIFileName.split("[^a-zA-Z0-9]");
        if (splitNames.length > 0) {
            return Arrays.stream(splitNames)
                    .filter(namePart -> !namePart.isBlank())
                    .collect(Collectors.joining(UNDERSCORE));
        }
        return openAPIFileName;
    }

    public static boolean isHttpService(ModuleSymbol moduleSymbol) {
        Optional<String> moduleNameOpt = moduleSymbol.getName();
        return moduleNameOpt.isPresent() && Constants.HTTP.equals(moduleNameOpt.get())
                && Constants.BALLERINA.equals(moduleSymbol.id().orgName());
    }

    public static boolean containErrors(List<Diagnostic> diagnostics) {
        return diagnostics != null && diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR);
    }

    public static String unescapeIdentifier(String parameterName) {
        String unescapedParamName = IdentifierUtils.unescapeBallerina(parameterName);
        return unescapedParamName.replaceAll("\\\\", "").replaceAll("'", "");
    }

    public static Schema<?> handleReference(SemanticModel semanticModel, SimpleNameReferenceNode recordNode,
                                            ModuleMemberVisitor moduleMemberVisitor, TypeMapper typeMapper) {
        Schema<?> refSchema = new Schema<>();
        // Creating request body - required.
        Optional<Symbol> symbol = semanticModel.symbol(recordNode);
        if (symbol.isPresent() && symbol.get() instanceof TypeSymbol) {
            String recordName = recordNode.name().toString().trim();
            typeMapper.addMapping((TypeSymbol) symbol.get());
            refSchema.set$ref(MapperCommonUtils.unescapeIdentifier(recordName));
        }
        return refSchema;
    }

    public static String getTypeDescription(TypeReferenceTypeSymbol typeSymbol) {
        Symbol definition = typeSymbol.definition();
        if (definition instanceof Documentable) {
            Optional<Documentation> documentation = ((Documentable) definition).documentation();
            if (documentation.isPresent() && documentation.get().description().isPresent()) {
                return documentation.get().description().get().trim();
            }
        }
        return null;
    }

    public static String getRecordFieldTypeDescription(RecordFieldSymbol typeSymbol) {
        Optional<Documentation> documentation = typeSymbol.documentation();
        if (documentation.isPresent() && documentation.get().description().isPresent()) {
            return documentation.get().description().get().trim();
        }
        return null;
    }

    public static String getTypeName(TypeSymbol typeSymbol) {
        String typeName = typeSymbol.getName().orElse(typeSymbol.signature());
        return unescapeIdentifier(typeName);
    }

    public static Schema setDefaultValue(Schema schema, String defaultValue) {
        if (Objects.isNull(schema) || Objects.isNull(defaultValue)) {
            return schema;
        }
        if (!Objects.isNull(schema.get$ref())) {
            Schema<?> compoundSchema = new Schema<>();
            compoundSchema.addAllOfItem(schema);
            schema = compoundSchema;
        }
        schema.setDefault(parseBalSimpleLiteral(defaultValue));
        return schema;
    }

    public static Object parseBalSimpleLiteral(String literal) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            return mapper.readValue(literal, Object.class);
        } catch (JsonProcessingException e) {
            return literal;
        }
    }

    public static boolean isSimpleValueLiteralKind(SyntaxKind valueExpressionKind) {
        return Arrays.stream(validExpressionKind).anyMatch(syntaxKind -> syntaxKind ==
                valueExpressionKind);
    }

    public static Map<String, Schema> getComponentsSchema(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (Objects.isNull(components)) {
            return new HashMap<>();
        }
        Map<String, Schema> schemas = components.getSchemas();
        if (Objects.isNull(schemas)) {
            return new HashMap<>();
        }
        return schemas;
    }
}
