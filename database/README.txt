GREEN CART DATABASE

For a fresh MySQL setup:
1. Open MySQL Workbench.
2. Connect to MySQL.
3. Open and run 01_mysql_workbench_setup.sql.
4. Start the Spring Boot application.

The application uses database: grocery_ordering
Default local credentials expected by application.properties:
  username: root
  password: GreenCart@123

Existing databases do not need the setup script again. Hibernate ddl-auto=update will add missing runtime tables/columns when the application starts.
