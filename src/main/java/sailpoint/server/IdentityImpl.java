package sailpoint.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sailpoint.rule.Identity;

/**
 * IIQ-backed implementation of {@link sailpoint.rule.Identity}.
 *
 * Values are copied from the IIQ Identity at construction time so the object
 * behaves like the detached DTO that ISC hands to rules. Fields that are not
 * standard IIQ Identity properties are read from identity attributes using the
 * ISC attribute names (e.g. {@code cloudLifecycleState}, {@code phone}).
 *
 * This class is an implementation detail of the shim; rules should only use the
 * {@link Identity} interface.
 */
public class IdentityImpl implements Identity {

    public static final String ATTR_LIFECYCLE_STATE = "cloudLifecycleState";
    public static final String ATTR_UID = "uid";
    public static final String ATTR_PHONE = "phone";
    public static final String ATTR_WORK_PHONE = "workPhone";
    public static final String ATTR_PERSONAL_EMAIL = "personalEmail";
    public static final String ATTR_COUNTRY = "country";
    public static final String ATTR_EMPLOYEE_NUMBER = "identificationNumber";

    private final String id;
    private final String name;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final boolean manager;
    private final String managerId;
    private final String managerName;
    private final Map<String, Object> attributes;

    public IdentityImpl(sailpoint.object.Identity identity) {
        this.id = identity.getId();
        this.name = identity.getName();
        this.displayName = identity.getDisplayName();
        this.firstName = identity.getFirstname();
        this.lastName = identity.getLastname();
        this.email = identity.getEmail();
        this.manager = identity.getManagerStatus();
        sailpoint.object.Identity mgr = identity.getManager();
        this.managerId = mgr != null ? mgr.getId() : null;
        this.managerName = mgr != null ? mgr.getName() : null;
        Map<String, Object> attrs = identity.getAttributes();
        this.attributes = attrs != null ? new HashMap<>(attrs) : new HashMap<>();
    }

    private String attr(String key) {
        Object val = attributes.get(key);
        return val != null ? val.toString() : null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPhone() {
        return attr(ATTR_PHONE);
    }

    @Override
    public String getWorkPhone() {
        return attr(ATTR_WORK_PHONE);
    }

    @Override
    public String getPersonalEmail() {
        return attr(ATTR_PERSONAL_EMAIL);
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getLifecycleState() {
        return attr(ATTR_LIFECYCLE_STATE);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * ISC's uid is the identity's username. IIQ has no separate uid, so an
     * explicit {@code uid} attribute is used when present, otherwise the name.
     */
    @Override
    public String getUid() {
        String uid = attr(ATTR_UID);
        return uid != null ? uid : name;
    }

    @Override
    public boolean isManager() {
        return manager;
    }

    @Override
    public String getManagerId() {
        return managerId;
    }

    @Override
    public String getManagerName() {
        return managerName;
    }

    @Override
    public String getCountry() {
        return attr(ATTR_COUNTRY);
    }

    @Override
    public String getEmployeeNumber() {
        return attr(ATTR_EMPLOYEE_NUMBER);
    }

    @Override
    public String toString() {
        return "Identity{id='" + id + "', name='" + name + "', displayName='" + displayName + "'}";
    }
}
