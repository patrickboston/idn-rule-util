package sailpoint.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import sailpoint.api.SailPointContext;
import sailpoint.connector.Connector;
import sailpoint.connector.ConnectorException;
import sailpoint.connector.ConnectorFactory;
import sailpoint.object.ResourceObject;
import sailpoint.object.Application;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;
import sailpoint.rule.ManagedAttributeDetails;

/**
 * IIQ-backed implementation of IIdnRuleUtil.
 *
 * NOTE / Design choices:
 * - We map IDN Account -> AccountImpl which wraps an IIQ Link.
 * - Some IDN-specific behaviors (e.g. searchable account attributes across
 * sources, connector getObject)
 * are approximated with IIQ Link queries. See inline TODOs if you want to
 * refine behavior.
 */
public class IdnRuleUtil implements IIdnRuleUtil {

    private final SailPointContext context;

    public IdnRuleUtil(SailPointContext context) {
        this.context = Objects.requireNonNull(context, "SailPointContext is required");
    }

    // -------------------------
    // Existence / lookup helpers
    // -------------------------
    @Override
    public boolean accountExistsByDisplayName(String applicationName, String displayName) throws GeneralException {
        return getFirstLinkByAppAndAttribute(applicationName, "displayName", displayName) != null;
    }

    @Override
    public boolean accountExistsByNativeIdentity(String applicationName, String nativeIdentity)
            throws GeneralException {
        return getFirstLinkByAppAndAttribute(applicationName, "nativeIdentity", nativeIdentity) != null;
    }

    // -------------------------
    // attribute search across sources
    // -------------------------
    @Override
    public int attrSearchCountAccounts(List<String> sourceIds, String attributeName, String operation,
            List<String> values) throws GeneralException {
        // Approximation: count Links where application.id in sourceIds and
        // attributeName equals any of the supplied values.
        if (sourceIds == null || sourceIds.isEmpty() || values == null || values.isEmpty()) {
            return 0;
        }

        QueryOptions qo = new QueryOptions();
        List<Filter> sourceFilters = new ArrayList<>();
        for (String sid : sourceIds) {
            sourceFilters.add(Filter.eq("application.id", sid));
        }
        qo.add(Filter.and(Filter.or(sourceFilters.toArray(new Filter[0])),
                Filter.in(attributeName, values)));
        return context.countObjects(Link.class, qo);
    }

    @Override
    public String attrSearchGetIdentityName(List<String> sourceIds, String attributeName, String operation,
            List<String> values) throws GeneralException {
        // Return the identity name of the first matching account.
        if (sourceIds == null || sourceIds.isEmpty() || values == null || values.isEmpty()) {
            return null;
        }

        QueryOptions qo = new QueryOptions();
        List<Filter> sourceFilters = new ArrayList<>();
        for (String sid : sourceIds) {
            sourceFilters.add(Filter.eq("application.id", sid));
        }
        qo.add(Filter.and(Filter.or(sourceFilters.toArray(new Filter[0])),
                Filter.in(attributeName, values)));
        List<Link> links = context.getObjects(Link.class, qo);
        if (links == null || links.isEmpty()) {
            return null;
        }
        Link first = links.get(0);
        Identity id = first.getIdentity();
        return id != null ? id.getName() : null;
    }

    // -------------------------
    // Counts and identity search
    // -------------------------
    @Override
    public int countAccounts(String applicationName) throws GeneralException {
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq("application.name", applicationName));
        return context.countObjects(Link.class, qo);
    }

    @Override
    public int countIdentitiesBySearchableIdentityAttribute(String attributeName, String operation, String value)
            throws GeneralException {
        // Approximation: count Identities where attributeName matches value.
        if (attributeName == null) {
            return 0;
        }
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq(attributeName, value));
        return context.countObjects(Identity.class, qo);
    }

    @Override
    public List<Account> findAccountsByAttribute(String applicationName, String identityName, String attributeName,
            Object attributeValue) throws GeneralException {

        QueryOptions qo = new QueryOptions();
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.eq("application.name", applicationName));
        filters.add(Filter.eq("identity.name", identityName));
        if (attributeName != null && attributeValue != null) {
            filters.add(Filter.eq(attributeName, attributeValue));
        }
        qo.add(Filter.and(filters.toArray(new Filter[0])));
        List<Link> links = context.getObjects(Link.class, qo);

        List<Account> result = new ArrayList<>();
        if (links != null) {
            for (Link l : links) {
                result.add(convertLinkToAccount(l));
            }
        }
        return result;
    }

    @Override
    public List<sailpoint.object.Identity> findIdentitiesBySearchableIdentityAttribute(String attributeName,
            String operation, String value, String sortAttribute) throws GeneralException {
        if (attributeName == null) {
            return Collections.emptyList();
        }
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq(attributeName, value));
        if (sortAttribute != null) {
            qo.setOrderBy(sortAttribute);
        }
        return context.getObjects(Identity.class, qo);
    }

    // -------------------------
    // Account attribute accessors
    // -------------------------
    @Override
    public String getAccountAttribute(Account account, String attribute) throws GeneralException {
        Object raw = getRawAccountAttribute(account, attribute);
        return raw != null ? raw.toString() : null;
    }

    @Override
    public String getAccountAttribute(String applicationName, String nativeIdentity, String attribute)
            throws GeneralException {
        Object raw = getRawAccountAttribute(applicationName, nativeIdentity, attribute);
        return raw != null ? raw.toString() : null;
    }

    @Override
    public boolean getAccountAttributeBool(Account account, String attribute) throws GeneralException {
        Object raw = getRawAccountAttribute(account, attribute);
        if (raw == null)
            return false;
        if (raw instanceof Boolean)
            return (Boolean) raw;
        return Boolean.parseBoolean(raw.toString());
    }

    @Override
    public boolean getAccountAttributeBool(String applicationName, String nativeIdentity, String attribute)
            throws GeneralException {
        Object raw = getRawAccountAttribute(applicationName, nativeIdentity, attribute);
        if (raw == null)
            return false;
        if (raw instanceof Boolean)
            return (Boolean) raw;
        return Boolean.parseBoolean(raw.toString());
    }

    @Override
    public int getAccountAttributeInt(Account account, String attribute) throws GeneralException {
        Object raw = getRawAccountAttribute(account, attribute);
        if (raw == null)
            return 0;
        if (raw instanceof Number)
            return ((Number) raw).intValue();
        try {
            return Integer.parseInt(raw.toString());
        } catch (Exception e) {
            throw new GeneralException("Attribute cannot be parsed as int: " + raw, e);
        }
    }

    @Override
    public int getAccountAttributeInt(String applicationName, String nativeIdentity, String attribute)
            throws GeneralException {
        Object raw = getRawAccountAttribute(applicationName, nativeIdentity, attribute);
        if (raw == null)
            return 0;
        if (raw instanceof Number)
            return ((Number) raw).intValue();
        try {
            return Integer.parseInt(raw.toString());
        } catch (Exception e) {
            throw new GeneralException("Attribute cannot be parsed as int: " + raw, e);
        }
    }

    // -------------------------
    // Account lookup & lists
    // -------------------------
    @Override
    public Account getAccountByDisplayName(String applicationName, String displayName) throws GeneralException {
        Link link = getFirstLinkByAppAndAttribute(applicationName, "displayName", displayName);
        return link != null ? convertLinkToAccount(link) : null;
    }

    @Override
    public Account getAccountByNativeIdentity(String applicationName, String nativeIdentity) throws GeneralException {
        Link link = getFirstLinkByAppAndAttribute(applicationName, "nativeIdentity", nativeIdentity);
        return link != null ? convertLinkToAccount(link) : null;
    }

    @Override
    public List<Account> getAllAccounts(String applicationName, String identityName) throws GeneralException {
        QueryOptions qo = new QueryOptions();
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.eq("application.name", applicationName));
        filters.add(Filter.eq("identity.name", identityName));
        qo.add(Filter.and(filters.toArray(new Filter[0])));
        Iterator<Link> linkIterator = context.search(Link.class, qo);
        List<Account> out = new ArrayList<>();
        while (linkIterator.hasNext()) {
            Link link = linkIterator.next();
            out.add(convertLinkToAccount(link));
        }
        return out;
    }

    @Override
    public Account getFirstAccount(String applicationName, String identityName) throws GeneralException {
        QueryOptions qo = new QueryOptions();
        qo.setResultLimit(1);
        qo.add(Filter.eq("application.name", applicationName));
        qo.add(Filter.eq("identity.name", identityName));
        List<Link> links = context.getObjects(Link.class, qo);
        return (links == null || links.isEmpty()) ? null : convertLinkToAccount(links.get(0));
    }

    @Override
    public String getFirstAccountNativeIdentity(String applicationName, String identityName) throws GeneralException {
        Account a = getFirstAccount(applicationName, identityName);
        return a != null ? a.getNativeIdentity() : null;
    }

    @Override
    public String getFirstAccountNativeIdentityByAttribute(String applicationName, String identityName,
            String attributeName, Object attributeValue) throws GeneralException {
        List<Account> accounts = findAccountsByAttribute(applicationName, identityName, attributeName, attributeValue);
        return (accounts == null || accounts.isEmpty()) ? null : accounts.get(0).getNativeIdentity();
    }

    // -------------------------
    // Identity & ManagedAttribute helpers
    // -------------------------
    @Override
    public sailpoint.object.Identity getIdentityById(String id) throws GeneralException {
        if (id == null)
            return null;
        return context.getObjectById(sailpoint.object.Identity.class, id);
    }

    @Override
    public String getManagedAttributeDescription(String sourceId, String name, String value, ManagedAttribute.Type type)
            throws GeneralException {
        QueryOptions qo = new QueryOptions();
        qo.setResultLimit(1);
        List<Filter> filters = new ArrayList<>();
        if (sourceId != null)
            filters.add(Filter.eq("application.id", sourceId));
        filters.add(Filter.eq("attribute", name));
        filters.add(Filter.eq("value", value));
        filters.add(Filter.eq("type", type));
        if (!filters.isEmpty())
            qo.add(Filter.and(filters.toArray(new Filter[0])));
        List<ManagedAttribute> mas = context.getObjects(ManagedAttribute.class, qo);
        if (mas == null || mas.isEmpty())
            return null;
        return mas.get(0).getDescription(Locale.ENGLISH);
    }

    @Override
    public ManagedAttributeDetails getManagedAttributeDetails(String sourceId, String name, String value,
            ManagedAttribute.Type type) throws GeneralException {
        // Build a minimal ManagedAttributeDetails-like object.
        // sailpoint.rule.ManagedAttributeDetails exists in IDN rule API; create and
        // populate a compatible object if available in your runtime.
        List<ManagedAttribute> mas = context.getObjects(ManagedAttribute.class,
                new QueryOptions().add(Filter.and(
                        Filter.eq("application.id", sourceId),
                        Filter.eq("attribute", name),
                        Filter.eq("value", value),
                        Filter.eq("type", type))));
        if (mas == null || mas.isEmpty()) {
            return null;
        }

        ManagedAttribute ma = mas.get(0);
        ManagedAttributeDetails details = new ManagedAttributeDetails();
        details.setDescription(ma.getDescription((Locale.ENGLISH)));
        details.setValue(ma.getValue());
        details.setName(ma.getName());
        // If additional fields are needed, populate them here.

        return details;
    }

    // -------------------------
    // Raw attribute accessors
    // -------------------------
    @Override
    public Object getRawAccountAttribute(Account account, String attribute) throws GeneralException {
        if (account == null)
            return null;
        return account.getRawAttribute(attribute);
    }

    @Override
    public Object getRawAccountAttribute(String applicationName, String nativeIdentity, String attribute)
            throws GeneralException {
        Link link = getFirstLinkByAppAndAttribute(applicationName, "nativeIdentity", nativeIdentity);
        if (link == null)
            return null;
        return link.getAttribute(attribute);
    }

    // -------------------------
    // Source attribute by id/name
    // -------------------------
    @Override
    public String getSourceAttributeBySourceId(String sourceId, String attributeName) throws GeneralException {
        if (sourceId == null || attributeName == null)
            return null;
        Application app = context.getObjectById(sailpoint.object.Application.class, sourceId);
        if (app == null)
            return null;
        Object val = app.getAttributeValue(attributeName);
        return val != null ? val.toString() : null;
    }

    @Override
    public String getSourceAttributeBySourceName(String sourceName, String attributeName) throws GeneralException {
        if (sourceName == null || attributeName == null)
            return null;
        Application app = context.getObjectByName(Application.class, sourceName);
        if (app == null)
            return null;
        Object val = app.getAttributeValue(attributeName);
        return val != null ? val.toString() : null;
    }

    // -------------------------
    // Uniqueness checks
    // -------------------------
    @Override
    public boolean isUniqueAccountId(String identityNameOrId, String applicationNameOrId, String accountId)
            throws GeneralException {
        Application app = fetchApplicationByIdOrName(applicationNameOrId);
        Connector appConn = ConnectorFactory.getConnector(app, null);
        try {
            return (ResourceObject) appConn.getObject(Connector.TYPE_ACCOUNT, accountId, null) != null ? false : true;
        } catch (ConnectorException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isUniqueLDAPValue(String identityNameOrId,
            String applicationNameOrId,
            String attributeName,
            String attributeValue) throws GeneralException {

        Application app = fetchApplicationByIdOrName(applicationNameOrId);
        if (app == null) {
            throw new GeneralException("Application not found: " + applicationNameOrId);
        }

        // Assumes 1 domain setup
        List<Map<String, Object>> domainList = (List<Map<String, Object>>) app.getAttributeValue("domainSettings");
        Map<String, Object> domainSettings = domainList.get(0);

        // Read connection details from the Application attributes
        List servers = (List) domainSettings.get("servers");
        String host = (String) servers.get(0);
        String port = (String) domainSettings.get("port");
        String baseDN = (String) domainSettings.get("domainDN");
        String user = (String) domainSettings.get("user");
        String password = (String) domainSettings.get("password");
        String useSSL = (String) domainSettings.get("useSSL");

        if (host == null || baseDN == null || port == null || user == null || password == null) {
            throw new GeneralException("Missing LDAP connection info on application: " + app.getName());
        }

        // Build LDAP connection environment
        Hashtable<String, String> env = new Hashtable<>();
        String ldapUrl = "ldap" + ("true".equalsIgnoreCase(useSSL) ? "s" : "") +
                "://" + host + (port != null ? ":" + port : "");

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, baseDN);
        env.put(Context.SECURITY_CREDENTIALS, context.decrypt(password));

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);

            // Create LDAP filter: e.g. (attributeName=attributeValue)
            String filter = "(" + attributeName + "=" + escapeLdapValue(attributeValue) + ")";

            // Search controls
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[] { attributeName });

            NamingEnumeration<SearchResult> results = ctx.search(baseDN, filter, controls);

            // If any result found, it means value is NOT unique
            boolean hasEntries = results.hasMore();
            results.close();

            return !hasEntries; // true = unique, false = already exists
        } catch (NamingException e) {
            throw new GeneralException("LDAP lookup failed for " + app.getName() + ": " + e.getMessage(), e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String escapeLdapValue(String value) {
        return value.replace("\\", "\\5c")
                .replace("*", "\\2a")
                .replace("(", "\\28")
                .replace(")", "\\29")
                .replace("\u0000", "\\00");
    }

    // -------------------------
    // Helper utilities
    // -------------------------
    private Link getFirstLinkByAppAndAttribute(String applicationName, String attributeName, Object attributeValue)
            throws GeneralException {
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.and(Filter.eq("application.name", applicationName), Filter.eq(attributeName, attributeValue)));
        List<Link> list = context.getObjects(Link.class, qo);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private Application fetchApplicationByIdOrName(String idOrName) throws GeneralException {
        Application app = null;
        try {
            app = context.getObject(Application.class, idOrName);
            if (app == null)
                throw new GeneralException("Could not find application by id or name: " + idOrName);
        } catch (Exception e) {
            throw new GeneralException("Could not find application by id or name: " + idOrName, e);
        }
        return app;
    }

    private Account convertLinkToAccount(Link link) {
        return new sailpoint.rule.Account(link);
    }
}
