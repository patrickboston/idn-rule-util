package sailpoint.rule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sailpoint.object.Application;
import sailpoint.object.Identity;
import sailpoint.object.Link;

/**
 * Mirrors the ISC rule-facing {@code sailpoint.rule.Account} class.
 *
 * Only the public getters below exist in ISC. Values are copied from the IIQ
 * Link at construction time so the object behaves like the detached DTO that
 * ISC hands to rules.
 *
 * NOTE: the {@link #Account(Link)} constructor is for the shim only and does not
 * exist in ISC; do not call it from rules.
 */
public class Account {

    /** Name/type of the ISC system source that holds "system" accounts. */
    private static final String SYSTEM_SOURCE = "IdentityNow";

    private String id;
    private String nativeIdentity;
    private String uuid;
    private String sourceId;
    private String identityId;
    private Map<String, Object> attributes = new HashMap<>();
    private boolean authoritative;
    private String description;
    private boolean disabled;
    private boolean locked;
    private boolean systemAccount;
    private boolean uncorrelated;
    private boolean manuallyCorrelated;
    private boolean entitlements;

    public Account() {
    }

    public Account(Link link) {
        this.id = link.getId();
        this.nativeIdentity = link.getNativeIdentity();
        this.uuid = link.getUuid();
        this.description = link.getDescription();
        this.disabled = link.isDisabled();
        this.locked = link.isLocked();
        this.manuallyCorrelated = link.isManuallyCorrelated();
        this.entitlements = link.hasEntitlements();
        if (link.getAttributes() != null) {
            this.attributes = new HashMap<>(link.getAttributes());
        }

        Application app = link.getApplication();
        if (app != null) {
            this.sourceId = app.getId();
            this.authoritative = app.isAuthoritative();
            this.systemAccount = SYSTEM_SOURCE.equals(app.getName()) || SYSTEM_SOURCE.equals(app.getType());
        }

        Identity identity = link.getIdentity();
        if (identity != null) {
            this.identityId = identity.getId();
            this.uncorrelated = !identity.isCorrelated();
        } else {
            this.uncorrelated = true;
        }
    }

    /** @return the unique identifier for the account within the context of the source */
    public String getId() {
        return id;
    }

    public String getNativeIdentity() {
        return nativeIdentity;
    }

    /** @return the UUID or null if it does not exist */
    public String getUuid() {
        return uuid;
    }

    /** @return the id of the source the account is on */
    public String getSourceId() {
        return sourceId;
    }

    /** @return the id of the identity that owns the account */
    public String getIdentityId() {
        return identityId;
    }

    /** @return the raw attributes of the account */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public boolean isAuthoritative() {
        return authoritative;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isLocked() {
        return locked;
    }

    /** @return true when the account is on the IdentityNow system source */
    public boolean isSystemAccount() {
        return systemAccount;
    }

    public boolean isUncorrelated() {
        return uncorrelated;
    }

    public boolean isManuallyCorrelated() {
        return manuallyCorrelated;
    }

    public boolean hasEntitlements() {
        return entitlements;
    }

    @Override
    public String toString() {
        return "Account{id='" + id + "', nativeIdentity='" + nativeIdentity + "', sourceId='" + sourceId
                + "', identityId='" + identityId + "'}";
    }
}
