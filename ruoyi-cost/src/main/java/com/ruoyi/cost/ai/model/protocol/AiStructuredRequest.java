package com.ruoyi.cost.ai.model.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** JSON Schema Structured Output 请求。 */
public class AiStructuredRequest extends AiChatRequest
{
    private String schemaName;
    private JsonNode jsonSchema;

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public JsonNode getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(JsonNode jsonSchema) { this.jsonSchema = jsonSchema; }
}
