package sailpoint.rule;

import sailpoint.object.Application;
import sailpoint.object.Identity;
import sailpoint.object.Link;

public class Account implements sailpoint.server.IIdnRuleUtil.Account {
    /**
     * Simple Account implementation that wraps an IIQ Link.
     */
    private final Link link;

    public Account(Link link) {
        this.link = link;
    }

    public String getNativeIdentity() {
        return link.getNativeIdentity();
    }

    public String getDisplayableName() {
        return link.getDisplayableName();
    }

    public Object getAttribute(String name) {
        return link.getAttribute(name);
    }

    public Object getRawAttribute(String name) {
        return link.getAttribute(name);
    }

    public String getApplicationName() {
        Application app = link.getApplication();
        return app != null ? app.getName() : null;
    }

    public String getIdentityName() {
        Identity id = link.getIdentity();
        return id != null ? id.getName() : null;
    }
}
