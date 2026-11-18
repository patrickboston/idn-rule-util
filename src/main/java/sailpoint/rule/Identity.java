package sailpoint.rule;

import java.util.Map;

/**
 * Mirrors the ISC rule-facing {@code sailpoint.rule.Identity} interface.
 *
 * ISC returns this lightweight DTO (not {@code sailpoint.object.Identity}) from
 * {@code IdnRuleUtil.getIdentityById} and
 * {@code IdnRuleUtil.findIdentitiesBySearchableIdentityAttribute}, so rules
 * should only call the methods declared here.
 */
public interface Identity {

    /** @return the internal name of the identity */
    String getName();

    /** @return the display name of the identity */
    String getDisplayName();

    /** @return the internal unique identifier */
    String getId();

    String getFirstName();

    String getLastName();

    String getPhone();

    String getWorkPhone();

    String getPersonalEmail();

    /** @return the work email */
    String getEmail();

    /** @return the current lifecycle state */
    String getLifecycleState();

    /** @return the raw attributes derived from attribute promotion */
    Map<String, Object> getAttributes();

    String getUid();

    /** @return true if the identity has reports */
    boolean isManager();

    /** @return the internal id of the identity's manager */
    String getManagerId();

    /** @return the name of the identity's manager */
    String getManagerName();

    String getCountry();

    String getEmployeeNumber();
}
