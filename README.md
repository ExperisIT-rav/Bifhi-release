# Bifhi-release



To import the projet in eclipse : 
- import the project like a maven project
- go to C:\Users\derouze\.m2\repository\org\projectlombok\lombok\1.12.2 et double click to install lombok into eclipse
- restart eclipse


cd C:\HewlettPackardEnterprise\git\Bifhi-release
mvn clean install -DskipTests=true
cd find-idol
mvn spring-boot:run -Dhp.find.home=C:\HewlettPackardEnterprise\git\Bifhi-release\find-idol\home