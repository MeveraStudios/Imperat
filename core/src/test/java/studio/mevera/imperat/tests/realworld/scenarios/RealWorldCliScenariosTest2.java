package studio.mevera.imperat.tests.realworld.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

/**
 * Second batch of real CLI scenarios — systemctl, mvn, gradle, psql,
 * rsync, find, wget, chmod, journalctl, terraform. Each modelled with
 * Imperat annotations only.
 */
@DisplayName("Real-world CLI scenarios (batch 2)")
final class RealWorldCliScenariosTest2 {

    private static TestImperat imperat;
    private static TestCommandSource source;

    @BeforeAll
    static void registerAll() {
        imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(SystemctlCmd.class);
        imperat.registerCommand(MavenCmd.class);
        imperat.registerCommand(GradleCmd.class);
        imperat.registerCommand(PsqlCmd.class);
        imperat.registerCommand(RsyncCmd.class);
        imperat.registerCommand(FindCmd.class);
        imperat.registerCommand(WgetCmd.class);
        imperat.registerCommand(ChmodCmd.class);
        imperat.registerCommand(JournalctlCmd.class);
        imperat.registerCommand(TerraformCmd.class);
        source = imperat.createDummySender();
    }

    private static ExecutionResult<TestCommandSource> exec(String line) {
        return imperat.execute(source, line);
    }

    // ----- 1. systemctl ---------------------------------------------

    @Test
    @DisplayName("systemctl restart nginx --quiet")
    void systemctlRestart() {
        ExecutionResult<TestCommandSource> r = exec("systemctl restart nginx --quiet");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("restart", SystemctlCmd.LAST_ACTION);
        assertEquals("nginx", SystemctlCmd.LAST_UNIT);
        assertTrue(SystemctlCmd.LAST_QUIET);
    }

    @Test
    @DisplayName("systemctl enable docker --now")
    void systemctlEnableNow() {
        ExecutionResult<TestCommandSource> r = exec("systemctl enable docker --now");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("enable", SystemctlCmd.LAST_ACTION);
        assertEquals("docker", SystemctlCmd.LAST_UNIT);
        assertTrue(SystemctlCmd.LAST_NOW);
        assertFalse(SystemctlCmd.LAST_QUIET);
    }

    // ----- 2. mvn ---------------------------------------------------

    @Test
    @DisplayName("mvn clean install -P prod -T 4 --offline")
    void mvnBuildWithProfile() {
        ExecutionResult<TestCommandSource> r = exec("mvn -P prod -T 4 --offline clean install");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("clean install", MavenCmd.LAST_GOALS);
        assertEquals("prod", MavenCmd.LAST_PROFILE);
        assertEquals("4", MavenCmd.LAST_THREADS);
        assertTrue(MavenCmd.LAST_OFFLINE);
    }

    // ----- 3. gradle ------------------------------------------------

    @Test
    @DisplayName("gradle build test --no-daemon --offline --parallel")
    void gradleMultiTaskBuild() {
        ExecutionResult<TestCommandSource> r =
                exec("gradle --no-daemon --offline --parallel build test");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("build test", GradleCmd.LAST_TASKS);
        assertTrue(GradleCmd.LAST_NO_DAEMON);
        assertTrue(GradleCmd.LAST_OFFLINE);
        assertTrue(GradleCmd.LAST_PARALLEL);
    }

    @Test
    @DisplayName("gradle build -b custom.gradle (alias for --build-file)")
    void gradleCustomBuildFile() {
        ExecutionResult<TestCommandSource> r = exec("gradle -b custom.gradle build");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("build", GradleCmd.LAST_TASKS);
        assertEquals("custom.gradle", GradleCmd.LAST_BUILD_FILE);
    }

    // ----- 4. psql --------------------------------------------------

    @Test
    @DisplayName("psql -h db.local -p 5432 -U admin -d analytics -c SELECT-1")
    void psqlConnectAndQuery() {
        ExecutionResult<TestCommandSource> r =
                exec("psql -h db.local -p 5432 -U admin -d analytics -c SELECT-1");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("db.local", PsqlCmd.LAST_HOST);
        assertEquals("5432", PsqlCmd.LAST_PORT);
        assertEquals("admin", PsqlCmd.LAST_USER);
        assertEquals("analytics", PsqlCmd.LAST_DB);
        assertEquals("SELECT-1", PsqlCmd.LAST_SQL);
    }

    // ----- 5. rsync -------------------------------------------------

    @Test
    @DisplayName("rsync -a -v -z --exclude=.git src/ user@host:/backup/")
    void rsyncWithExclude() {
        ExecutionResult<TestCommandSource> r =
                exec("rsync -a -v -z --exclude=.git src/ user@host:/backup/");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertTrue(RsyncCmd.LAST_ARCHIVE);
        assertTrue(RsyncCmd.LAST_VERBOSE);
        assertTrue(RsyncCmd.LAST_COMPRESS);
        assertEquals(".git", RsyncCmd.LAST_EXCLUDE);
        assertEquals("src/", RsyncCmd.LAST_SRC);
        assertEquals("user@host:/backup/", RsyncCmd.LAST_DST);
    }

    @Test
    @DisplayName("rsync -avz --delete src/ dst/ (combined alias short switches)")
    void rsyncCombinedAliases() {
        ExecutionResult<TestCommandSource> r = exec("rsync -avz --delete src/ dst/");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertTrue(RsyncCmd.LAST_ARCHIVE);
        assertTrue(RsyncCmd.LAST_VERBOSE);
        assertTrue(RsyncCmd.LAST_COMPRESS);
        assertTrue(RsyncCmd.LAST_DELETE);
        assertEquals("src/", RsyncCmd.LAST_SRC);
        assertEquals("dst/", RsyncCmd.LAST_DST);
    }

    // ----- 6. find --------------------------------------------------

    @Test
    @DisplayName("find /var/log --name *.log --type f --mtime +7 --delete")
    void findOldLogs() {
        ExecutionResult<TestCommandSource> r =
                exec("find /var/log --name *.log --type f --mtime +7 --delete");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("/var/log", FindCmd.LAST_PATH);
        assertEquals("*.log", FindCmd.LAST_NAME);
        assertEquals("f", FindCmd.LAST_TYPE);
        assertEquals("+7", FindCmd.LAST_MTIME);
        assertTrue(FindCmd.LAST_DELETE);
    }

    // ----- 7. wget --------------------------------------------------

    @Test
    @DisplayName("wget -O page.html --user-agent=Mozilla --no-check-certificate https://example.com")
    void wgetWithUserAgent() {
        ExecutionResult<TestCommandSource> r =
                exec("wget -O page.html --user-agent=Mozilla --no-check-certificate https://example.com");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("https://example.com", WgetCmd.LAST_URL);
        assertEquals("page.html", WgetCmd.LAST_OUTPUT);
        assertEquals("Mozilla", WgetCmd.LAST_USER_AGENT);
        assertTrue(WgetCmd.LAST_NO_CHECK_CERT);
    }

    @Test
    @DisplayName("wget -q https://example.com (quiet alias short)")
    void wgetQuietAlias() {
        ExecutionResult<TestCommandSource> r = exec("wget -q https://example.com");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("https://example.com", WgetCmd.LAST_URL);
        assertTrue(WgetCmd.LAST_QUIET);
    }

    // ----- 8. chmod -------------------------------------------------

    @Test
    @DisplayName("chmod -R 755 /var/www (recursive + two positionals)")
    void chmodRecursive() {
        ExecutionResult<TestCommandSource> r = exec("chmod -R 755 /var/www");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("755", ChmodCmd.LAST_MODE);
        assertEquals("/var/www", ChmodCmd.LAST_PATH);
        assertTrue(ChmodCmd.LAST_RECURSIVE);
        assertFalse(ChmodCmd.LAST_VERBOSE);
    }

    // ----- 9. journalctl --------------------------------------------

    @Test
    @DisplayName("journalctl -u nginx --since yesterday -n 100 -f")
    void journalctlFollowNginx() {
        ExecutionResult<TestCommandSource> r =
                exec("journalctl -u nginx --since yesterday -n 100 -f");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("nginx", JournalctlCmd.LAST_UNIT);
        assertEquals("yesterday", JournalctlCmd.LAST_SINCE);
        assertEquals("100", JournalctlCmd.LAST_LINES);
        assertTrue(JournalctlCmd.LAST_FOLLOW);
    }

    // ----- 10. terraform --------------------------------------------

    @Test
    @DisplayName("terraform apply --auto-approve --var-file=prod.tfvars --target=module.web")
    void terraformApplyTargeted() {
        ExecutionResult<TestCommandSource> r =
                exec("terraform apply --auto-approve --var-file=prod.tfvars --target=module.web");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("apply", TerraformCmd.LAST_ACTION);
        assertTrue(TerraformCmd.LAST_AUTO_APPROVE);
        assertEquals("prod.tfvars", TerraformCmd.LAST_VAR_FILE);
        assertEquals("module.web", TerraformCmd.LAST_TARGET);
    }

    @Test
    @DisplayName("terraform plan --var-file=staging.tfvars (no auto-approve, no target)")
    void terraformPlan() {
        ExecutionResult<TestCommandSource> r =
                exec("terraform plan --var-file=staging.tfvars");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("plan", TerraformCmd.LAST_ACTION);
        assertEquals("staging.tfvars", TerraformCmd.LAST_VAR_FILE);
        assertNotNull(TerraformCmd.LAST_AUTO_APPROVE);
        assertFalse(TerraformCmd.LAST_AUTO_APPROVE);
    }
}
