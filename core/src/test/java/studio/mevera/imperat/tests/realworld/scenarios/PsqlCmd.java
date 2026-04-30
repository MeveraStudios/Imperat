package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code psql -h <host> -p <port> -U <user> -d <db> [-c <sql>]}.
 *
 * <p>Real-world reference:
 * {@code psql -h db.local -p 5432 -U admin -d analytics -c "SELECT 1"}.</p>
 */
@RootCommand("psql")
public final class PsqlCmd {

    public static volatile String LAST_HOST;
    public static volatile String LAST_PORT;
    public static volatile String LAST_USER;
    public static volatile String LAST_DB;
    public static volatile String LAST_SQL;

    @Execute
    public void run(
            TestCommandSource s,
            @Flag({"host", "h"}) @Default("localhost") String host,
            @Flag({"port", "p"}) @Default("5432") String port,
            @Flag({"username", "U"}) @Default("postgres") String user,
            @Flag({"dbname", "d"}) @Default("postgres") String db,
            @Flag({"command", "c"}) String sql
    ) {
        LAST_HOST = host;
        LAST_PORT = port;
        LAST_USER = user;
        LAST_DB = db;
        LAST_SQL = sql;
    }
}
