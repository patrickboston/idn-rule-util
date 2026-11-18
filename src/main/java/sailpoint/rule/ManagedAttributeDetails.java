package sailpoint.rule;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors the ISC rule-facing {@code sailpoint.rule.ManagedAttributeDetails}
 * class returned by {@code IdnRuleUtil.getManagedAttributeDetails} and
 * {@code IdnRuleUtil.findManagedAttributes}.
 */
public class ManagedAttributeDetails {

    private String name;
    private String displayableName;
    private String value;
    private String description;
    private String type;
    private Map<String, Object> attributes = new HashMap<>();

    public ManagedAttributeDetails() {
    }

    public String getDescription() {
        return description;
    }

    public ManagedAttributeDetails setDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public ManagedAttributeDetails setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
        return this;
    }

    public String getName() {
        return name;
    }

    public ManagedAttributeDetails setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * The display name of the entitlement, derived from its display name when
     * that is set and from its name otherwise. Only populated by
     * {@code IdnRuleUtil.findManagedAttributes}.
     */
    public String getDisplayableName() {
        return displayableName;
    }

    public ManagedAttributeDetails setDisplayableName(String displayableName) {
        this.displayableName = displayableName;
        return this;
    }

    public String getValue() {
        return value;
    }

    public ManagedAttributeDetails setValue(String value) {
        this.value = value;
        return this;
    }

    public String getType() {
        return type;
    }

    public ManagedAttributeDetails setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        return "ManagedAttributeDetails{" +
                "name='" + name + '\'' +
                ", displayableName='" + displayableName + '\'' +
                ", value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
