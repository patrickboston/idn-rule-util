# Overview
**idn-rule-util** was created to mimic the [SailPoint IDN/ISC Cloud Rule Utility](https://developer.sailpoint.com/docs/extensibility/rules/rule-utility), which is essentially a wrapper for IdentityIQ `SailPointContext` functions such as looking up identities, accounts, and more. 

This library is meant to be used with IdentityIQ sandboxes where you can create and test your rules prior to deploying them to ISC via the SailPoint approval/deployment process, which requires their professional services hours and does not allow for any testing prior to deployment. It may require that you re-create searchable attributes and application definitions with the same names, etc. as your ISC environment.

# Usage
In order to use this library, you must have an IdentityIQ environment where the jar file can be deployed.

## Add to classpath
1. Download the latest version of the library (`idn-rule-util.jar`) from the **Packages** section of the GitHub repository
2. Place the jar file in `<identityiq-home>/WEB-INF/lib` (or build place in SSB directory and re-deploy fresh WAR)
3. Restart your application server

## Rule Setup
1. Import the class to your rule
```java
import sailpoint.server.IdnRuleUtil;
```
2. Declare an instance of `IdnRuleUtil` giving it the same variable name `idn` as is passed into ISC rules
```java
IdnRuleUtil idn = new IdnRuleUtil(context);
```
3. Use the functions as you would in an ISC rule
```java
idn.countAccounts("2c9180...sourceId");
```

## Matching the ISC API
The public API mirrors the [ISC rule Java docs](https://developer.sailpoint.com/docs/extensibility/rules/java-docs): `sailpoint.server.IdnRuleUtil`, `sailpoint.rule.Account`, `sailpoint.rule.Identity` and `sailpoint.rule.ManagedAttributeDetails`. Only call methods that appear in those docs; anything else compiles in IIQ but fails in ISC.

- **Source IDs:** ISC methods take a `sourceId`. IIQ application IDs never match ISC source IDs, so this library resolves each `sourceId` argument as an IIQ application ID first, then as an application name. Rules can be tested with IIQ IDs or names, but **must be switched to ISC source IDs before deployment**.
- **Identities:** `getIdentityById` and `findIdentitiesBySearchableIdentityAttribute` return `sailpoint.rule.Identity`, not `sailpoint.object.Identity`. Fields that are not standard IIQ identity properties are read from identity attributes: `cloudLifecycleState`, `uid` (falls back to name), `phone`, `workPhone`, `personalEmail`, `country` and `employeeNumber`.
- **Accounts:** `sailpoint.rule.Account` exposes only the ISC getters (`getId`, `getNativeIdentity`, `getSourceId`, `getIdentityId`, `getAttributes`, `isDisabled`, ...). Read attributes with `account.getAttributes().get("x")` or `idn.getAccountAttribute(account, "x")`.
- **Enums and constants:** `IdnRuleUtil.FindOperation`, `IdnRuleUtil.ManagedAttributeProperty`, `IdnRuleUtil.EQUALS_FILTER`, `STARTS_WITH_FILTER`, `TYPE_ENTITLEMENT` and `TYPE_PERMISSION` are available as in ISC.

## Cleanup before ISC deployment
After you have developed and tested your rule in IdentityIQ and are ready to submit it for SailPoint ISC review, you will need to make sure you remove the `sailpoint.server.IdnRuleUtil` class import and `IdnRuleUtil` object declaration. **SailPoint's Cloud Rule validator does not appear to pick this up an un-allowed class, but your rule will either be declined during review or will throw errors when deployed, so make sure you perform this clean up. Additionally, make sure you convert any IIQ rule types to ISC rule types per the next section.**

## Rule Types
ISC contains a couple of rule types which do not exist in IdentityIQ. Depending on how you create the IIQ rules, you may need to convert them to the equivalent ISC rule type before deployment. You could however just set the rule types in IIQ to the ISC rule type, it will not affect how they run. Here are the equivalent rule types (IIQ to ISC):
- FieldValue - AttributeGenerator
- FieldValue - AttributeGeneratorFromTemplate

There is no equivalent of a `Transform` rule type in IIQ.




# Contribution
To contribute to this project, see below sections.

## Local Dependencies
This was developed using local Maven dependencies since IdentityIQ libraries are obviously not public. If you wish to work on this, you can add them by running these commands in your local project/workspace:
```
mvn deploy:deploy-file -DgroupId=sailpoint -DartifactId=connector-bundle-identityiq -Dversion=8.5 -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=<your-iiq-path>/WEB-INF/lib/identityiq.jar

mvn deploy:deploy-file -DgroupId=sailpoint -DartifactId=connector-bundle-identityiq -Dversion=8.5 -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=<your-iiq-path>/WEB-INF/lib/connector-bundle-identityiq.jar
```

## Compiling and Packaging
To compile this project and build a JAR file for IdentityIQ, run the following Maven commands:
```
mvn clean
mvn compile
mvn package
```

## Deploy to GitHub Package
To deploy a compiled package (jar) to the repository, follow [this guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token) for setting up your local Maven setting with a personal access token to authenticate to GitHub, the run:
```
mvn deploy
```