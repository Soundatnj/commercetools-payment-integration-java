Build and publish workflow
===========================

The main goal of the build process is to publish the artifacts to public repositories, 
like [JCenter](https://jcenter.bintray.com/) and [Maven Central](https://search.maven.org/).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [Build and deploy a new version](#build-and-deploy-a-new-version)
- [Integration tests](#integration-tests)
- [Publish workflow](#publish-workflow)
  - [Full build with tests, documentation publishing and Bintray upload](#full-build-with-tests-documentation-publishing-and-bintray-upload)
  - [Publish to local maven repo](#publish-to-local-maven-repo)
  - [Publish snapshots to oss.sonatype.org](#publish-snapshots-to-osssonatypeorg)
  - [Publish to Bintray](#publish-to-bintray)
  - [Publish to Maven](#publish-to-maven)
- [All in one publish script](#all-in-one-publish-script)
- [Known issues](#known-issues)
    - [`PayonePrepaidTest.testPaymentFlow`](#payoneprepaidtesttestpaymentflow)
    - [Any test which makes requests to commercetools platform environment may fail with:](#any-test-which-makes-requests-to-commercetools-platform-environment-may-fail-with)
    - [Aggregated Javadoc may fail without visible reason](#aggregated-javadoc-may-fail-without-visible-reason)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Build and deploy a new version

If the project and CI tools (like Travis) are completely configured, to perform a new version deploy push a new git tag:
```
git tag X.X.X
git push --tags
```

This will initiate [respective Travis build](https://travis-ci.org/commercetools/commercetools-payment-integration-java)
and upload the artifacts to Bintray. Then one have to promote/publish them 
from Bintray to _JCenter_ and/or _Maven Central_. See [Publish workflow](#publish-workflow) below for more details.

# Integration tests
 
For successful integration test on remote machine (e.g. Travis) following settings are required:
 - **Mandatory CTP environment variables** (see [`ItConfig.java`](/blob/master/src/it/com/commercetools/config/ItConfig.java)):
 
    |  Key name                 | commercetools test environment                                                                    |
    |---------------------------|---------------------------------------------------------------------------------------------------|
    | CT_PROJECT_KEY            | project-payment-21                                                                                |
    | CT_PROJECT_KEY            | project-payment-21                                                                                |
    | CT_CLIENT_ID              | see [CTP API Settings for project-payment-21](https://admin.commercetools.com/project-payment-21) |
    | CT_CLIENT_SECRET          | see [CTP API Settings for project-payment-21](https://admin.commercetools.com/project-payment-21) |
    | CT_PAYONE_INTEGRATION_URL | `https://ct-payment-integration-java.herokuapp.com/commercetools/handle/payments/`                |
    
  - `CT_PAYONE_INTEGRATION_URL` resource with deployed 
    [commercetools payone integration service](https://github.com/commercetools/commercetools-payone-integration).
    This service must be connected to the same project (*CT_PROJECT_KEY*).
    - For current workflow the service might be deployed to [Heroku](https://dashboard.heroku.com/apps/ct-payment-integration-java/settings)
    using [*heroku cli*](https://devcenter.heroku.com/articles/heroku-cli) or direct push to heroku repository from 
    [payone-integration-service](https://github.com/commercetools/commercetools-payone-integration):
    ```
    git clone git@github.com:commercetools/commercetools-payone-integration.git
    cd commercetools-payone-integration
    git push https://git.heroku.com/ct-payment-integration-java.git master
    ```
      For more information about how to deploy the service see [Heroku Deployment](https://devcenter.heroku.com/categories/deployment) and 
      [Payone integration service build](https://github.com/commercetools/commercetools-payone-integration#build)
    
    - To avoid any side-effect of previous tests it is recommended to re-start the service 
    before the build (this will re-initialize all custom types, see [IntegrationService#start()](https://github.com/commercetools/commercetools-payone-integration/blob/927adfa637918c20feb03242242f9d57f5561669/service/src/main/java/com/commercetools/pspadapter/payone/IntegrationService.java#L52)):
      ```
      heroku restart --app ct-payment-integration-java
      ```
      This step requires local [installed and logged in heroku CLI](https://devcenter.heroku.com/articles/heroku-command-line).
      Alternatively for remote (travis) build `$HEROKU_API_KEY` environment variable may be used instead of `heroku login`.
       
  - `GRGIT_USER` environment variable (github token) to publish aggregated Javadoc
  - `BINTRAY_USER` and `BINTRAY_KEY` environment variables to publish the artifacts to Bintray 

If you have all above - build and run the tests (both unit and integration):
```
heroku restart --app ct-payment-integration-java # may be skipped on secondary run
./gradlew clean build
```

To run all these tests locally (including IDE) with mandatory properties above - use `it.properties` file 
(in `it/resources` directory) with all secret values. See [`ItConfig.java`](/blob/master/src/it/com/commercetools/config/ItConfig.java) for more details. 
Also you could copy and edit [`it/resources/it.properties.skeleton`](/blob/master/src/it/resources/it.properties.skeleton).

The gradle _build_ task is configured to be depended on all the tests, including Unit tests on all sub-projects and 
common integration tests from [`src/`](/src/) directory.

Because of the [known issues](#known-issues) some tests may fail.

# Publish workflow

## Full build with tests, documentation publishing and Bintray upload

Replace `X.X.X` in the snippet:

```
./gradlew clean build aggregateJavaDoc gitPublishPush bintrayUpload -Dbuild.version=X.X.X
```

## Publish to local maven repo
 
This step may be used for local test versions:
```
./gradlew clean install -Dbuild.version=X.X.X
```

If you want to review full generated `pom.xml` (with license, scm, developers) like it will be published, then use:
```
./gradlew clean publishToMavenLocal -Dbuild.version=X.X.X
```

where `publishToMavenLocal` is a task from 
[`maven-publish`](https://docs.gradle.org/3.3/userguide/publishing_maven.html#publishing_maven:install)
plugin.

Use [publish-to-maven-local.sh](./publish-to-maven-local.sh) script for easier publishing process.

## Publish snapshots to oss.sonatype.org

**Note:** this expected to be changed to https://oss.jfrog.org in the future.

To publish to [OSS Sonatype snapshots](https://oss.sonatype.org/content/repositories/snapshots/com/commercetools/)
repo the following command is used:

```bash
./gradlew clean build uploadArchives -Dbuild.version=X.X.X-SNAPSHOT
```

The `-SNAPSHOT` suffix is mandatory. 

**Note**: for publishing to OSS Sonatype you need specify **API** User/Key (not login credentials) for  
`OSS_USER`/`OSS_KEY` environment variables or `ossUser`/`ossKey` gradle build properties 
(the properties have precedence over environment variables). 

See more configuration details of the oss uploading in [oss-upload.gradle](./build-scripts/oss-upload.gradle) file.

Use [publish-to-oss.sh](./publish-to-oss.sh) script for easier publishing process.

## Publish to Bintray

[Bintray documentation about publish process](https://blog.bintray.com/2014/02/11/bintray-as-pain-free-gateway-to-maven-central/)

Bintray publish is performed by [`gradle-bintray-plugin`](https://github.com/bintray/gradle-bintray-plugin). 
The artifacts are published to [bintray commercetools maven repo](https://bintray.com/commercetools/maven/payment).

If you are a new developer in the project - update contributors list in 
[`build.gradle`](/build.gradle)`-> subprojects -> pomConfig ->developers`.

To initiate publish call:
```
./gradlew clean build bintrayUpload -Dbuild.version=X.X.X
```

**NOTE**: Bintray does not allow to publish snapshots thus `X.X.X` should not contain _SNAPSHOT_.
If you wish to use snapshots, https://oss.jfrog.com account should be configured.
See https://blog.bintray.com/2014/02/11/bintray-as-pain-free-gateway-to-maven-central/ for more info.

Snapshots could be published to oss using [Publish snapshots](#publish-snapshots-to-osssonatypeorg) guides. 
If you need to publish test versions to bintray - use _alpha_, _beta_ etc suffixes.

Use [publish-to-bintray.sh](./publish-to-bintray.sh) script for easier publishing process.

After publishing to Bintray artifacts are available in [Bintray Download](http://dl.bintray.com/commercetools/maven/com/commercetools/payment/)
but still not available in [JCenter](https://jcenter.bintray.com/com/commercetools/payment/). 

To publish the artifacts to JCenter do the next:
  1. Go to https://bintray.com/commercetools/maven/payment
  1. You will see something like _Notice: You have 24 unpublished item(s) for this package (expiring in 6 days and 22 hours)_
  1. Click _Publish_
  1. Check [JCenter Payment](https://jcenter.bintray.com/com/commercetools/payment/) reference.

## Publish to Maven

Publishing to Maven Central requires the following steps:

 1. Build the app and upload to Bintray (see the steps above for integration tests)
 1. [Signing up the app with PGP key](https://blog.bintray.com/2013/08/06/fight-crime-with-gpg/): for now we use Bintray's 
    "a stock built-in key-pair so that it can auto-sign every file you upload"
 1. [Manually release from Bintray web page to Maven Central](https://blog.bintray.com/2015/09/17/publishing-your-maven-project-to-bintray/)
 
**Note**: Maven Central has much stricter requirements to published artifacts, e.g. the should have mandatory POM fields 
(like developers list, SCM references; this is configured in the [build script](/build.gradle)) 
and mandatory signed by GPG key (could be performed by Bintray settings). For more info about Maven Central 
requirements see [Requirements](http://central.sonatype.org/pages/requirements.html) page.

As soon as artifacts are synced you will be able to find them in the Maven Central repo and mirrors:

https://repo1.maven.org/maven2/com/commercetools/payment/
http://repo2.maven.org/maven2/com/commercetools/payment/

# All in one publish script

As a summary of all the steps above you may use _all-in-one_ publish script from the root of the repo:

```
./publish.sh X.X.X
```
where `X.X.X` is a new deploy version.

This scripts restarts Heroku integration test environment, builds the app, runs unit and integration tests, 
aggregates and deploys to Github all javadoc, uploads all artifacts to Bintray. 
To execute the script successfully you should have all the settings above, namely:

 - installed and logged in heroku cli (or `$HEROKU_API_KEY` environment variable set instead of log in)
 - exported `GRGIT_USER` github token to publish the documentation
 - exported `BINTRAY_USER` and `BINTRAY_KEY` environment variables.

# Known issues
  ### [`PayonePrepaidTest.testPaymentFlow`](https://github.com/commercetools/commercetools-payment-integration-java/blob/master/src/it/com/commercetools/payment/PayonePrepaidTest.java)
  and [`PayonePaypalTest.testPaymentFlow`](https://github.com/commercetools/commercetools-payment-integration-java/blob/master/src/it/com/commercetools/payment/PayonePaypalTest.java)
 sometimes fail with error:
 > _expected:<[SUCCESS]> but was:<[FAILED]>_. 
 
 in asserts of `assertPaymentTransactionObjectCreation():`
 > assertThat(ptcr.getOperationResult()).isEqualTo(OperationResult.SUCCESS);
 
  
 Still not clear why, but should be investigated.
 It might be connected to parallel execution, but likely not.
 
 ### Any test which makes requests to commercetools platform environment may fail with:
 > java.util.concurrent.ExecutionException: io.sphere.sdk.http.HttpException: 
 > The underlying HTTP client detected a problem.
 
 ### Aggregated Javadoc may fail without visible reason
  
  The used `gradle-aggregate-javadocs-plugin` has know issue of bad errors reporting: 
  it fails without explicitly pointing the reason, something like this:
  
  ```
  gradle Javadoc generation failed. Generated Javadoc options file (useful for troubleshooting)... 
  See generated javadoc.options
  ```
  
  To find the real issue run default gradle `javadoc` task like this:
  
  ```
    ./gradlew clean javadoc
  ```
  and this will point you the problematic files/docs. 
