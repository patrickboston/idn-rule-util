package sailpoint.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of the IdentityNow ManagedAttributeDetails class.
 * Used for testing rules in IdentityIQ that reference IdnRuleUtil methods.
 */
public class ManagedAttributeDetails {

    private String name;
    private String value;
    private String description;
    private String type;
    private Map<String, Object> attributes = new HashMap<>();

    public ManagedAttributeDetails() {}

    // ---- Getters ----

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // ---- Fluent Setters ----

    public ManagedAttributeDetails setName(String name) {
        this.name = name;
        return this;
    }

    public ManagedAttributeDetails setValue(String value) {
        this.value = value;
        return this;
    }

    public ManagedAttributeDetails setDescription(String description) {
        this.description = description;
        return this;
    }

    public ManagedAttributeDetails setType(String type) {
        this.type = type;
        return this;
    }

    public ManagedAttributeDetails setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
        return this;
    }

    // ---- Convenience ----

    @Override
    public String toString() {
        return "ManagedAttributeDetails{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
