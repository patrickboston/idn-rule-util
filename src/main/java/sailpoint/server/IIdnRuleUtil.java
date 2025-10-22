package sailpoint.server;

import java.util.List;

import sailpoint.rule.ManagedAttributeDetails;
import sailpoint.tools.GeneralException;

/**
 * Interface that mirrors SailPoint IdentityNow's sailpoint.server.IdnRuleUtil API.
 * NOTE: We map the IDN "Account" concept to a local Account interface so this can run in IIQ.
 */
public interface IIdnRuleUtil {

    // Account existence
    boolean accountExistsByDisplayName(String applicationName, String displayName) throws GeneralException;
    boolean accountExistsByNativeIdentity(String applicationName, String nativeIdentity) throws GeneralException;

    // Attribute-based searches across accounts
    int attrSearchCountAccounts(List<String> sourceIds, String attributeName, String operation, List<String> values) throws GeneralException;
    String attrSearchGetIdentityName(List<String> sourceIds, String attributeName, String operation, List<String> values) throws GeneralException;

    // Counts
    int countAccounts(String applicationName) throws GeneralException;
    int countIdentitiesBySearchableIdentityAttribute(String attributeName, String operation, String value) throws GeneralException;

    // Finds
    List<Account> findAccountsByAttribute(String applicationName, String identityName, String attributeName, Object attributeValue) throws GeneralException;
    List<sailpoint.object.Identity> findIdentitiesBySearchableIdentityAttribute(String attributeName, String operation, String value, String sortAttribute) throws GeneralException;

    // Account attribute accessors
    String getAccountAttribute(Account account, String attribute) throws GeneralException;
    String getAccountAttribute(String applicationName, String nativeIdentity, String attribute) throws GeneralException;

    boolean getAccountAttributeBool(Account account, String attribute) throws GeneralException;
    boolean getAccountAttributeBool(String applicationName, String nativeIdentity, String attribute) throws GeneralException;

    int getAccountAttributeInt(Account account, String attribute) throws GeneralException;
    int getAccountAttributeInt(String applicationName, String nativeIdentity, String attribute) throws GeneralException;

    // Account lookup
    Account getAccountByDisplayName(String applicationName, String displayName) throws GeneralException; // kept for parity but will return our Account or null
    Account getAccountByNativeIdentity(String applicationName, String nativeIdentity) throws GeneralException;

    List<Account> getAllAccounts(String applicationName, String identityName) throws GeneralException;
    Account getFirstAccount(String applicationName, String identityName) throws GeneralException;
    String getFirstAccountNativeIdentity(String applicationName, String identityName) throws GeneralException;
    String getFirstAccountNativeIdentityByAttribute(String applicationName, String identityName, String attributeName, Object attributeValue) throws GeneralException;

    // Identity and ManagedAttribute helpers
    sailpoint.object.Identity getIdentityById(String id) throws GeneralException;
    String getManagedAttributeDescription(String sourceId, String name, String value, sailpoint.object.ManagedAttribute.Type type) throws GeneralException;
    ManagedAttributeDetails getManagedAttributeDetails(String sourceId, String name, String value, sailpoint.object.ManagedAttribute.Type type) throws GeneralException;

    // Raw attribute accessors (object form)
    Object getRawAccountAttribute(Account account, String attribute) throws GeneralException;
    Object getRawAccountAttribute(String applicationName, String nativeIdentity, String attribute) throws GeneralException;

    // Source attribute accessors
    String getSourceAttributeBySourceId(String sourceId, String attributeName) throws GeneralException;
    String getSourceAttributeBySourceName(String sourceName, String attributeName) throws GeneralException;

    // Uniqueness checks
    boolean isUniqueAccountId(String identityNameOrId, String applicationNameOrId, String accountId) throws GeneralException;
    boolean isUniqueLDAPValue(String identityNameOrId, String applicationNameOrId, String attributeName, String attributeValue) throws GeneralException;

    /**
     * Minimal Account contract used by this shim. Implementations wrap IIQ Link objects.
     */
    interface Account {
        String getNativeIdentity();
        String getDisplayableName();
        Object getAttribute(String name);
        Object getRawAttribute(String name);
        String getApplicationName();
        String getIdentityName(); // identity owner name (if known)
    }
}
