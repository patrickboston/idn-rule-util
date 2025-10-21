# Overview
**idn-rule-util** was created to mimic the [SailPoint IDN/ISC Cloud Rule Utility](https://developer.sailpoint.com/docs/extensibility/rules/rule-utility), which is essentially a wrapper for IdentityIQ `SailPointContext` functions such as looking up identities, accounts, and more. 

This library is meant to be used with IdentityIQ sandboxes where you can create and test your rules prior to deploying them to ISC via the SailPoint approval/deployment process, which requires their professional services hours and does not allow for any testing prior to deployment. It may require that you re-create searchable attributes and application definitions with the same names, etc. as your ISC environment.

# Usage
In order to use this library, you must have an IdentityIQ environment where the jar file can be deployed.

## Add to classpath
1. Download the latest version of the library (`idn-rule-util.jar`) from the **Releases** section of the GitHub repository
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



# Contribution
To contribute to this project, see below sections.

## Local Dependencies
This was developed using local Maven dependencies since IdentityIQ libraries are obviously not public. If you wish to work on this, you can add them by running these commands:
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