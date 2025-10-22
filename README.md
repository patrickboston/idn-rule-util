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
idn.countAccounts("HR [source]");
```

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
To deploy a compiled package (jar) to the repository, follow [this guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token) for setting up your local Maven setting with a personal access token to authenticate to GitHub.