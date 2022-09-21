# RF2 Package Info
This is the start of a java utility to inspect SNOMED CT RF2 packages.

So far it can only be run on the command line.

- Counts the number of rows in each module
- Reads the Module Dependency Refset 
  - Prints each dependency version required
  - States whether each required module is contained in the package (without version check)

## Build and Run
```
mvn clean package
java -jar target/rf2-package-info-*.jar <path-to-your-rf2-package-zip> 
```
