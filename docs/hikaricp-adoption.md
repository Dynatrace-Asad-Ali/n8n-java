# HikariCP Adoption Proposal

HikariCP is a fast, production-grade JDBC connection pooling library. We recommend migrating from SimpleConnectionPool to HikariCP for scalability, resilience, and monitoring. Example configuration:

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

HikariConfig config = new HikariConfig();
config.setJdbcUrl(DatabaseInitializer.H2_URL); // For H2 demo
config.setUsername(DatabaseInitializer.H2_USER);
config.setPassword(DatabaseInitializer.H2_PASSWORD);
config.setMaximumPoolSize(50); // Adjustable based on workload
HikariDataSource ds = new HikariDataSource(config);
```

To migrate:
1. Add HikariCP as a dependency (Maven/Gradle).
2. Replace SimpleConnectionPool usage with HikariDataSource.
3. Refactor resource acquisition to use ds.getConnection().
4. Use connection try-with-resources everywhere for reliability.

Benefits:
- Faster and lower-latency connection handling
- Advanced metrics and pool health monitoring
- Resilience during spikes and long-lived workloads

See: [HikariCP documentation](https://github.com/brettwooldridge/HikariCP)
