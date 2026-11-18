package sailpoint.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import sailpoint.api.SailPointContext;
import sailpoint.connector.Connector;
import sailpoint.connector.ConnectorFactory;
import sailpoint.connector.ObjectNotFoundException;
import sailpoint.object.Application;
import sailpoint.object.Filter;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.QueryOptions;
import sailpoint.rule.Account;
import sailpoint.rule.Identity;
import sailpoint.rule.ManagedAttributeDetails;
import sailpoint.tools.GeneralException;

/**
 * IIQ-backed implementation of the ISC {@code sailpoint.server.IdnRuleUtil}
 * API, for testing ISC cloud rules in an IdentityIQ sandbox.
 *
 * Source arguments: ISC takes source <b>ids</b>. IIQ application ids will
 * never match ISC source ids, so every {@code sourceId} argument here is
 * resolved as an id first and then, if no application has that id, as an
 * application name. Rules can therefore be tested with IIQ ids or names, but
 * must use ISC source ids once deployed.
 */
public class IdnRuleUtil implements IIdnRuleUtil {

    /** Operation that matches values beginning with the supplied value. */
    public static final String STARTS_WITH_FILTER = "StartsWith";

    /** Operation that matches values equal to the supplied value. */
    public static final String EQUALS_FILTER = "Equals";

    /** The Permission managed attribute type, for findManagedAttributes. */
    public static final String TYPE_PERMISSION = "Permission";

    /** The Entitlement managed attribute type, for findManagedAttributes. */
    public static final String TYPE_ENTITLEMENT = "Entitlement";

    /** ISC caps findIdentitiesBySearchableIdentityAttribute at 50 results. */
    static final int MAX_IDENTITY_RESULTS = 50;

    /** ISC caps findManagedAttributes at 250 results. */
    static final int MAX_MANAGED_ATTRIBUTE_RESULTS = 250;

    /**
     * The properties of the managed attribute model that findManagedAttributes
     * can query and sort on.
     */
    public enum ManagedAttributeProperty {
        /** The display name of the entitlement (display name, or name if unset). */
        displayableName,
        /** The raw value of the entitlement. */
        value
    }

    /** The match operations supported by findManagedAttributes. */
    public enum FindOperation {
        /** Matches property values equal to the supplied value. */
        EQUALS,
        /** Matches property values beginning with the supplied value. */
        STARTS_WITH
    }

    private final SailPointContext context;

    public IdnRuleUtil(SailPointContext context) {
        this.context = Objects.requireNonNull(context, "SailPointContext is required");
    }

    // -------------------------
    // Account existence / lookup
    // -------------------------
    @Override
    public boolean accountExistsByNativeIdentity(String sourceId, String nativeIdentity) throws GeneralException {
        return getFirstLink(sourceId, "nativeIdentity", nativeIdentity) != null;
    }

    @Override
    public boolean accountExistsByDisplayName(String sourceId, String displayName) throws GeneralException {
        return getFirstLink(sourceId, "displayName", displayName) != null;
    }

    @Override
    public Account getAccountByNativeIdentity(String sourceId, String nativeIdentity) throws GeneralException {
        Link link = getFirstLink(sourceId, "nativeIdentity", nativeIdentity);
        return link != null ? new Account(link) : null;
    }

    @Override
    public Account getAccountByDisplayName(String sourceId, String displayName) throws GeneralException {
        Link link = getFirstLink(sourceId, "displayName", displayName);
        return link != null ? new Account(link) : null;
    }

    /**
     * @return the accounts, or null if the identity has no accounts on the
     *         source or either parameter is null (matches ISC)
     */
    @Override
    public List<Account> getAllAccounts(String sourceId, String identityName) throws GeneralException {
        if (sourceId == null || identityName == null) {
            return null;
        }
        List<Account> accounts = getIdentityAccounts(sourceId, identityName);
        return accounts.isEmpty() ? null : accounts;
    }

    @Override
    public Account getFirstAccount(String sourceId, String identityName) throws GeneralException {
        Application app = resolveSource(sourceId);
        if (app == null || identityName == null) {
            return null;
        }
        QueryOptions qo = new QueryOptions();
        qo.setResultLimit(1);
        qo.add(Filter.eq("application.id", app.getId()));
        qo.add(Filter.eq("identity.name", identityName));
        List<Link> links = context.getObjects(Link.class, qo);
        return (links == null || links.isEmpty()) ? null : new Account(links.get(0));
    }

    @Override
    public String getFirstAccountNativeIdentity(String sourceId, String identityName) throws GeneralException {
        Account a = getFirstAccount(sourceId, identityName);
        return a != null ? a.getNativeIdentity() : null;
    }

    /**
     * @return the first non-null and non-empty nativeIdentity whose attribute
     *         matches. Two nulls are equal; an empty string is treated as null.
     */
    @Override
    public String getFirstAccountNativeIdentityByAttribute(String sourceId, String identityName,
            String attributeName, Object attributeValue) throws GeneralException {
        for (Account account : findAccountsByAttribute(sourceId, identityName, attributeName, attributeValue)) {
            String ni = account.getNativeIdentity();
            if (ni != null && !ni.isEmpty()) {
                return ni;
            }
        }
        return null;
    }

    /**
     * Iterates the identity's accounts on the source and matches the attribute
     * in memory (like ISC), so the attribute does not need to be searchable.
     *
     * @return the matching accounts, or an empty list if none match
     */
    @Override
    public List<Account> findAccountsByAttribute(String sourceId, String identityName, String attributeName,
            Object attributeValue) throws GeneralException {
        List<Account> result = new ArrayList<>();
        if (sourceId == null || identityName == null) {
            return result;
        }
        Object expected = emptyToNull(attributeValue);
        for (Account account : getIdentityAccounts(sourceId, identityName)) {
            Object actual = emptyToNull(account.getAttributes().get(attributeName));
            if (nullSafeEq(expected, actual, true)) {
                result.add(account);
            }
        }
        return result;
    }

    @Override
    public int countAccounts(String sourceId) throws GeneralException {
        Application app = resolveSource(sourceId);
        if (app == null) {
            return 0;
        }
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq("application.id", app.getId()));
        return context.countObjects(Link.class, qo);
    }

    // -------------------------
    // Account attribute accessors
    // -------------------------
    @Override
    public Object getRawAccountAttribute(String sourceId, String nativeIdentity, String attribute)
            throws GeneralException {
        Link link = getFirstLink(sourceId, "nativeIdentity", nativeIdentity);
        return link != null ? link.getAttribute(attribute) : null;
    }

    @Override
    public Object getRawAccountAttribute(Account account, String attribute) {
        return account != null ? account.getAttributes().get(attribute) : null;
    }

    @Override
    public String getAccountAttribute(String sourceId, String nativeIdentity, String attribute)
            throws GeneralException {
        return otoa(getRawAccountAttribute(sourceId, nativeIdentity, attribute));
    }

    @Override
    public String getAccountAttribute(Account account, String attribute) {
        return otoa(getRawAccountAttribute(account, attribute));
    }

    @Override
    public boolean getAccountAttributeBool(String sourceId, String nativeIdentity, String attribute)
            throws GeneralException {
        return otob(getRawAccountAttribute(sourceId, nativeIdentity, attribute));
    }

    @Override
    public boolean getAccountAttributeBool(Account account, String attribute) {
        return otob(getRawAccountAttribute(account, attribute));
    }

    @Override
    public int getAccountAttributeInt(String sourceId, String nativeIdentity, String attribute)
            throws GeneralException {
        return toInt(getRawAccountAttribute(sourceId, nativeIdentity, attribute));
    }

    @Override
    public int getAccountAttributeInt(Account account, String attribute) {
        return toInt(getRawAccountAttribute(account, attribute));
    }

    // -------------------------
    // Searchable account attributes
    // -------------------------
    @Override
    public int attrSearchCountAccounts(List<String> sourceIds, String attributeName, String operation,
            List<String> values) {
        try {
            Filter filter = buildAttrSearchFilter(sourceIds, attributeName, operation, values);
            if (filter == null) {
                return 0;
            }
            QueryOptions qo = new QueryOptions();
            qo.add(filter);
            return context.countObjects(Link.class, qo);
        } catch (GeneralException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String attrSearchGetIdentityName(List<String> sourceIds, String attributeName, String operation,
            List<String> values) {
        try {
            Filter filter = buildAttrSearchFilter(sourceIds, attributeName, operation, values);
            if (filter == null) {
                return null;
            }
            QueryOptions qo = new QueryOptions();
            qo.add(filter);
            qo.add(Filter.notnull("identity"));
            qo.setResultLimit(1);
            List<Link> links = context.getObjects(Link.class, qo);
            if (links == null || links.isEmpty() || links.get(0).getIdentity() == null) {
                return null;
            }
            return links.get(0).getIdentity().getName();
        } catch (GeneralException e) {
            throw new IllegalStateException(e);
        }
    }

    // -------------------------
    // Managed attributes
    // -------------------------
    @Override
    public String getManagedAttributeDescription(String sourceId, String name, String value,
            ManagedAttribute.Type type) throws GeneralException {
        ManagedAttribute ma = findManagedAttribute(sourceId, name, value, type);
        return ma != null ? describe(ma) : null;
    }

    @Override
    public ManagedAttributeDetails getManagedAttributeDetails(String sourceId, String name, String value,
            ManagedAttribute.Type type) throws GeneralException {
        ManagedAttribute ma = findManagedAttribute(sourceId, name, value, type);
        return ma != null ? toDetails(ma, false) : null;
    }

    @Override
    public List<ManagedAttributeDetails> findManagedAttributes(String sourceId, String attributeName,
            ManagedAttributeProperty property, FindOperation operation, String valueToMatch,
            String entitlementType, ManagedAttributeProperty sortAttribute) throws GeneralException {
        Objects.requireNonNull(sourceId, "sourceId is required");
        List<ManagedAttributeDetails> result = new ArrayList<>();
        Application app = resolveSource(sourceId);
        if (app == null) {
            return result;
        }

        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.eq("application.id", app.getId()));
        if (attributeName != null) {
            filters.add(Filter.eq("attribute", attributeName));
        }
        if (property != null) {
            if (operation == FindOperation.STARTS_WITH) {
                filters.add(Filter.like(property.name(), valueToMatch, Filter.MatchMode.START));
            } else {
                filters.add(Filter.eq(property.name(), valueToMatch));
            }
        }
        if (entitlementType != null) {
            filters.add(Filter.eq("type", entitlementType));
        } else {
            // ISC: when null, every type other than Permission is matched
            filters.add(Filter.or(Filter.isnull("type"), Filter.ne("type", TYPE_PERMISSION)));
        }

        QueryOptions qo = new QueryOptions();
        qo.add(Filter.and(filters));
        qo.setResultLimit(MAX_MANAGED_ATTRIBUTE_RESULTS);
        if (sortAttribute != null) {
            qo.addOrdering(sortAttribute.name(), true);
        }

        Iterator<ManagedAttribute> it = context.search(ManagedAttribute.class, qo);
        while (it.hasNext()) {
            result.add(toDetails(it.next(), true));
        }
        return result;
    }

    // -------------------------
    // Identities
    // -------------------------
    /**
     * @throws IllegalStateException if the identity is not found (matches ISC)
     */
    @Override
    public Identity getIdentityById(String id) {
        try {
            sailpoint.object.Identity identity = id != null
                    ? context.getObjectById(sailpoint.object.Identity.class, id)
                    : null;
            if (identity == null) {
                throw new IllegalStateException("Identity not found: " + id);
            }
            return new IdentityImpl(identity);
        } catch (GeneralException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Identity> findIdentitiesBySearchableIdentityAttribute(String attributeName, String operation,
            String value, String sortAttribute) {
        QueryOptions qo = new QueryOptions();
        qo.add(buildIdentitySearchFilter(attributeName, operation, value));
        // ISC sorts ascending, by uid when no sort attribute is given (IIQ: name)
        qo.addOrdering(sortAttribute != null ? sortAttribute : "name", true);
        qo.setResultLimit(MAX_IDENTITY_RESULTS);
        try {
            List<Identity> result = new ArrayList<>();
            Iterator<sailpoint.object.Identity> it = context.search(sailpoint.object.Identity.class, qo);
            while (it.hasNext()) {
                result.add(new IdentityImpl(it.next()));
            }
            return result;
        } catch (GeneralException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int countIdentitiesBySearchableIdentityAttribute(String attributeName, String operation, String value) {
        QueryOptions qo = new QueryOptions();
        qo.add(buildIdentitySearchFilter(attributeName, operation, value));
        try {
            return context.countObjects(sailpoint.object.Identity.class, qo);
        } catch (GeneralException e) {
            throw new IllegalStateException(e);
        }
    }

    // -------------------------
    // Uniqueness checks
    // -------------------------
    /**
     * Calls getObject() on the source's connector.
     *
     * @return true if no account with the given id exists
     * @throws IllegalStateException if the source can't be found or the
     *                               connector call fails
     */
    @Override
    public boolean isUniqueAccountId(String identityNameOrId, String applicationNameOrId, String accountId) {
        Application app = requireApplication(applicationNameOrId);
        try {
            Connector connector = ConnectorFactory.getConnector(app, null);
            return connector.getObject(Connector.TYPE_ACCOUNT, accountId, null) == null;
        } catch (ObjectNotFoundException e) {
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Connector getObject failed for " + app.getName() + ": "
                    + e.getMessage(), e);
        }
    }

    /**
     * Searches the source's LDAP directory for (attributeName=attributeValue).
     * Assumes a single-domain AD/LDAP application using {@code domainSettings}.
     *
     * @return true if no entry has the value
     * @throws IllegalStateException if the source can't be found or the search
     *                               fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean isUniqueLDAPValue(String identityNameOrId, String applicationNameOrId, String attributeName,
            String attributeValue) {
        Application app = requireApplication(applicationNameOrId);

        List<Map<String, Object>> domainList = (List<Map<String, Object>>) app.getAttributeValue("domainSettings");
        if (domainList == null || domainList.isEmpty()) {
            throw new IllegalStateException("No domainSettings on application: " + app.getName());
        }
        Map<String, Object> domainSettings = domainList.get(0);

        List<?> servers = (List<?>) domainSettings.get("servers");
        String host = (servers != null && !servers.isEmpty()) ? otoa(servers.get(0)) : null;
        String port = otoa(domainSettings.get("port"));
        String baseDN = otoa(domainSettings.get("domainDN"));
        String user = otoa(domainSettings.get("user"));
        String password = otoa(domainSettings.get("password"));
        boolean useSSL = otob(domainSettings.get("useSSL"));

        if (host == null || baseDN == null || user == null || password == null) {
            throw new IllegalStateException("Missing LDAP connection info on application: " + app.getName());
        }

        Hashtable<String, String> env = new Hashtable<>();
        String ldapUrl = "ldap" + (useSSL ? "s" : "") + "://" + host + (port != null ? ":" + port : "");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, user);

        DirContext ctx = null;
        try {
            env.put(Context.SECURITY_CREDENTIALS, context.decrypt(password));
            ctx = new InitialDirContext(env);

            String filter = "(" + attributeName + "=" + escapeLdapValue(attributeValue) + ")";
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[] { attributeName });
            controls.setCountLimit(1);

            NamingEnumeration<SearchResult> results = ctx.search(baseDN, filter, controls);
            boolean hasEntries = results.hasMore();
            results.close();
            return !hasEntries;
        } catch (NamingException | GeneralException e) {
            throw new IllegalStateException("LDAP lookup failed for " + app.getName() + ": " + e.getMessage(), e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // -------------------------
    // Source attributes
    // -------------------------
    @Override
    public String getSourceAttributeBySourceId(String sourceId, String attributeName) throws GeneralException {
        if (attributeName == null) {
            return null;
        }
        Application app = resolveSource(sourceId);
        return app != null ? otoa(app.getAttributeValue(attributeName)) : null;
    }

    @Override
    public String getSourceAttributeBySourceName(String sourceName, String attributeName) throws GeneralException {
        if (sourceName == null || attributeName == null) {
            return null;
        }
        Application app = context.getObjectByName(Application.class, sourceName);
        return app != null ? otoa(app.getAttributeValue(attributeName)) : null;
    }

    // -------------------------
    // Utility helpers (ISC IdnRuleUtil)
    // -------------------------
    /** @return the String value of a non-null object, null otherwise */
    @Override
    public String otoa(Object object) {
        return object != null ? object.toString() : null;
    }

    /** @return true if the object is Boolean.TRUE or the String "true" ignoring case */
    @Override
    public boolean otob(Object object) {
        if (object instanceof Boolean) {
            return (Boolean) object;
        }
        return object instanceof String && "true".equalsIgnoreCase((String) object);
    }

    /**
     * @return a List as is, any other collection copied into a new List, a
     *         non-collection wrapped in a single element List, or null for null
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List asList(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof List) {
            return (List) object;
        }
        if (object instanceof Collection) {
            return new ArrayList((Collection) object);
        }
        List list = new ArrayList();
        list.add(object);
        return list;
    }

    /** @return true if both objects are non-null and equal (two nulls are not equal) */
    @Override
    public boolean nullSafeEq(Object o1, Object o2) {
        return nullSafeEq(o1, o2, false);
    }

    @Override
    public boolean nullSafeEq(Object o1, Object o2, boolean nullsEq) {
        if (o1 == null && o2 == null) {
            return nullsEq;
        }
        return o1 != null && o1.equals(o2);
    }

    /** @return true if the Strings are equal ignoring case (two nulls are equal) */
    @Override
    public boolean nullSafeCaseInsensitiveEq(String o1, String o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        return o1 != null && o1.equalsIgnoreCase(o2);
    }

    /** Two nulls are equal, and a null sorts after a non-null. */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T extends Comparable> int nullSafeCompareTo(T o1, T o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        return o1.compareTo(o2);
    }

    /** Formats the date using the JVM's default time zone. */
    @Override
    public String dateToString(Date src, String format) {
        return dateToString(src, format, TimeZone.getDefault());
    }

    @Override
    public String dateToString(Date src, String format, TimeZone tz) {
        if (src == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(tz != null ? tz : TimeZone.getDefault());
        return sdf.format(src);
    }

    // -------------------------
    // Internal helpers
    // -------------------------
    /**
     * Resolves an ISC source id argument to an IIQ Application: by id first,
     * then by name (IIQ ids never match ISC source ids).
     */
    private Application resolveSource(String sourceIdOrName) throws GeneralException {
        if (sourceIdOrName == null) {
            return null;
        }
        Application app = context.getObjectById(Application.class, sourceIdOrName);
        if (app == null) {
            app = context.getObjectByName(Application.class, sourceIdOrName);
        }
        return app;
    }

    private Application requireApplication(String applicationNameOrId) {
        Application app;
        try {
            app = resolveSource(applicationNameOrId);
        } catch (GeneralException e) {
            throw new IllegalStateException(e);
        }
        if (app == null) {
            throw new IllegalStateException("Could not find source by id or name: " + applicationNameOrId);
        }
        return app;
    }

    private Link getFirstLink(String sourceId, String property, Object value) throws GeneralException {
        Application app = resolveSource(sourceId);
        if (app == null || value == null) {
            return null;
        }
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq("application.id", app.getId()));
        qo.add(Filter.eq(property, value));
        qo.setResultLimit(1);
        List<Link> list = context.getObjects(Link.class, qo);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private List<Account> getIdentityAccounts(String sourceId, String identityName) throws GeneralException {
        List<Account> out = new ArrayList<>();
        Application app = resolveSource(sourceId);
        if (app == null || identityName == null) {
            return out;
        }
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq("application.id", app.getId()));
        qo.add(Filter.eq("identity.name", identityName));
        Iterator<Link> it = context.search(Link.class, qo);
        while (it.hasNext()) {
            out.add(new Account(it.next()));
        }
        return out;
    }

    private Filter buildAttrSearchFilter(List<String> sourceIds, String attributeName, String operation,
            List<String> values) throws GeneralException {
        if (attributeName == null) {
            throw new IllegalStateException("attributeName is required");
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("values are required");
        }
        boolean startsWith = isStartsWith(operation);

        List<String> appIds = new ArrayList<>();
        if (sourceIds != null) {
            for (String sid : sourceIds) {
                Application app = resolveSource(sid);
                if (app != null) {
                    appIds.add(app.getId());
                }
            }
        }
        if (appIds.isEmpty()) {
            return null;
        }

        List<Filter> valueFilters = new ArrayList<>();
        for (String v : values) {
            valueFilters.add(startsWith
                    ? Filter.like(attributeName, v, Filter.MatchMode.START)
                    : Filter.eq(attributeName, v));
        }
        Filter valueFilter = valueFilters.size() == 1 ? valueFilters.get(0) : Filter.or(valueFilters);
        return Filter.and(Filter.in("application.id", appIds), valueFilter);
    }

    private Filter buildIdentitySearchFilter(String attributeName, String operation, String value) {
        if (attributeName == null) {
            throw new IllegalStateException("attributeName is required");
        }
        if (value == null) {
            throw new IllegalStateException("value is required");
        }
        return isStartsWith(operation)
                ? Filter.like(attributeName, value, Filter.MatchMode.START)
                : Filter.eq(attributeName, value);
    }

    /** Only Equals and StartsWith are supported, as in ISC. */
    private boolean isStartsWith(String operation) {
        if (STARTS_WITH_FILTER.equalsIgnoreCase(operation)) {
            return true;
        }
        if (EQUALS_FILTER.equalsIgnoreCase(operation)) {
            return false;
        }
        throw new IllegalStateException("Unsupported operation '" + operation + "'; use "
                + EQUALS_FILTER + " or " + STARTS_WITH_FILTER);
    }

    private ManagedAttribute findManagedAttribute(String sourceId, String name, String value,
            ManagedAttribute.Type type) throws GeneralException {
        Application app = resolveSource(sourceId);
        if (app == null) {
            return null;
        }
        // ISC: type defaults to Entitlement when null
        String typeName = (type != null ? type : ManagedAttribute.Type.Entitlement).name();
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.eq("application.id", app.getId()));
        qo.add(Filter.eq("attribute", name));
        qo.add(Filter.eq("value", value));
        qo.add(Filter.eq("type", typeName));
        qo.setResultLimit(1);
        List<ManagedAttribute> mas = context.getObjects(ManagedAttribute.class, qo);
        return (mas == null || mas.isEmpty()) ? null : mas.get(0);
    }

    private String describe(ManagedAttribute ma) {
        String desc = ma.getDescription(Locale.getDefault());
        if (desc == null) {
            desc = ma.getDescription(Locale.ENGLISH);
        }
        if (desc == null && ma.getDescriptions() != null && !ma.getDescriptions().isEmpty()) {
            desc = ma.getDescriptions().values().iterator().next();
        }
        return desc;
    }

    private ManagedAttributeDetails toDetails(ManagedAttribute ma, boolean includeDisplayableName) {
        ManagedAttributeDetails details = new ManagedAttributeDetails()
                .setName(ma.getName())
                .setValue(ma.getValue())
                .setDescription(describe(ma))
                .setType(ma.getType());
        if (ma.getAttributes() != null) {
            details.setAttributes(new HashMap<>(ma.getAttributes()));
        }
        if (includeDisplayableName) {
            details.setDisplayableName(ma.getDisplayableName());
        }
        return details;
    }

    private static Object emptyToNull(Object value) {
        return (value instanceof String && ((String) value).isEmpty()) ? null : value;
    }

    private static int toInt(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        return Integer.parseInt(raw.toString().trim());
    }

    private static String escapeLdapValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\5c")
                .replace("*", "\\2a")
                .replace("(", "\\28")
                .replace(")", "\\29")
                .replace("\u0000", "\\00");
    }
}
