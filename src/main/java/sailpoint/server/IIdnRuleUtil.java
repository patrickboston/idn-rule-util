package sailpoint.server;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import sailpoint.object.ManagedAttribute;
import sailpoint.rule.Account;
import sailpoint.rule.Identity;
import sailpoint.rule.ManagedAttributeDetails;
import sailpoint.tools.GeneralException;

/**
 * Mirrors the ISC rule-facing {@code sailpoint.server.IdnRuleUtil} interface
 * (https://developer.sailpoint.com/rule-java-docs/sailpoint/server/IdnRuleUtil.html).
 *
 * Signatures (parameter names, return types and declared exceptions) match the
 * ISC javadoc. The constants and nested enums ({@code ManagedAttributeProperty},
 * {@code FindOperation}) live on {@link IdnRuleUtil} so rules can reference them
 * as {@code IdnRuleUtil.FindOperation.EQUALS} exactly as they would in ISC.
 */
public interface IIdnRuleUtil {

    // Account existence / lookup (by source id)
    boolean accountExistsByNativeIdentity(String sourceId, String nativeIdentity) throws GeneralException;

    boolean accountExistsByDisplayName(String sourceId, String displayName) throws GeneralException;

    Account getAccountByNativeIdentity(String sourceId, String nativeIdentity) throws GeneralException;

    Account getAccountByDisplayName(String sourceId, String displayName) throws GeneralException;

    List<Account> getAllAccounts(String sourceId, String identityName) throws GeneralException;

    Account getFirstAccount(String sourceId, String identityName) throws GeneralException;

    String getFirstAccountNativeIdentity(String sourceId, String identityName) throws GeneralException;

    String getFirstAccountNativeIdentityByAttribute(String sourceId, String identityName, String attributeName,
            Object attributeValue) throws GeneralException;

    List<Account> findAccountsByAttribute(String sourceId, String identityName, String attributeName,
            Object attributeValue) throws GeneralException;

    int countAccounts(String sourceId) throws GeneralException;

    // Account attribute accessors
    Object getRawAccountAttribute(String sourceId, String nativeIdentity, String attribute) throws GeneralException;

    Object getRawAccountAttribute(Account account, String attribute);

    String getAccountAttribute(String sourceId, String nativeIdentity, String attribute) throws GeneralException;

    String getAccountAttribute(Account account, String attribute);

    boolean getAccountAttributeBool(String sourceId, String nativeIdentity, String attribute) throws GeneralException;

    boolean getAccountAttributeBool(Account account, String attribute);

    int getAccountAttributeInt(String sourceId, String nativeIdentity, String attribute) throws GeneralException;

    int getAccountAttributeInt(Account account, String attribute);

    // Searchable account attributes
    int attrSearchCountAccounts(List<String> sourceIds, String attributeName, String operation, List<String> values);

    String attrSearchGetIdentityName(List<String> sourceIds, String attributeName, String operation,
            List<String> values);

    // Managed attributes
    String getManagedAttributeDescription(String sourceId, String name, String value, ManagedAttribute.Type type)
            throws GeneralException;

    ManagedAttributeDetails getManagedAttributeDetails(String sourceId, String name, String value,
            ManagedAttribute.Type type) throws GeneralException;

    List<ManagedAttributeDetails> findManagedAttributes(String sourceId, String attributeName,
            IdnRuleUtil.ManagedAttributeProperty property, IdnRuleUtil.FindOperation operation, String valueToMatch,
            String entitlementType, IdnRuleUtil.ManagedAttributeProperty sortAttribute) throws GeneralException;

    // Identities
    Identity getIdentityById(String id);

    List<Identity> findIdentitiesBySearchableIdentityAttribute(String attributeName, String operation, String value,
            String sortAttribute);

    int countIdentitiesBySearchableIdentityAttribute(String attributeName, String operation, String value);

    // Uniqueness checks
    boolean isUniqueLDAPValue(String identityNameOrId, String applicationNameOrId, String attributeName,
            String attributeValue);

    boolean isUniqueAccountId(String identityNameOrId, String applicationNameOrId, String accountId);

    // Source attributes
    String getSourceAttributeBySourceId(String sourceId, String attributeName) throws GeneralException;

    String getSourceAttributeBySourceName(String sourceName, String attributeName) throws GeneralException;

    // Utility helpers
    String otoa(Object object);

    boolean otob(Object object);

    @SuppressWarnings("rawtypes")
    List asList(Object object);

    boolean nullSafeEq(Object o1, Object o2);

    boolean nullSafeEq(Object o1, Object o2, boolean nullsEq);

    boolean nullSafeCaseInsensitiveEq(String o1, String o2);

    @SuppressWarnings("rawtypes")
    <T extends Comparable> int nullSafeCompareTo(T o1, T o2);

    String dateToString(Date src, String format);

    String dateToString(Date src, String format, TimeZone tz);
}
