package studio.mevera.imperat.tests.realworld.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

/**
 * End-to-end tests modelling 10 real CLI tools through Imperat
 * annotations only — git, docker, npm, curl, grep, kubectl, ssh, aws,
 * ffmpeg, tar. Each scenario exercises one or more of: subcommands,
 * short/long flag prefixes, value flags, switches, alias combining,
 * inline {@code =value}, greedy positional, tail-optional positional.
 */
@DisplayName("Real-world CLI scenarios")
final class RealWorldCliScenariosTest {

    private static TestImperat imperat;
    private static TestCommandSource source;

    @BeforeAll
    static void registerAll() {
        imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(GitCommitCmd.class);
        imperat.registerCommand(DockerRunCmd.class);
        imperat.registerCommand(NpmInstallCmd.class);
        imperat.registerCommand(CurlCmd.class);
        imperat.registerCommand(GrepCmd.class);
        imperat.registerCommand(KubectlGetCmd.class);
        imperat.registerCommand(SshCmd.class);
        imperat.registerCommand(AwsS3CpCmd.class);
        imperat.registerCommand(FfmpegCmd.class);
        imperat.registerCommand(TarCmd.class);
        source = imperat.createDummySender();
    }

    private static ExecutionResult<TestCommandSource> exec(String line) {
        return imperat.execute(source, line);
    }

    // ----- 1. git commit ---------------------------------------------

    @Test
    @DisplayName("git commit -m \"fix: bug\" --amend --no-verify")
    void gitCommitAmendNoVerify() {
        ExecutionResult<TestCommandSource> r = exec("git commit -m fix-bug --amend --no-verify");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("fix-bug", GitCommitCmd.LAST_MESSAGE);
        assertTrue(GitCommitCmd.LAST_AMEND);
        assertTrue(GitCommitCmd.LAST_NO_VERIFY);
        assertFalse(GitCommitCmd.LAST_ALL);
    }

    @Test
    @DisplayName("git commit --message=initial -a (inline = + alias short form)")
    void gitCommitInlineEqualsAndAlias() {
        ExecutionResult<TestCommandSource> r = exec("git commit --message=initial -a");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("initial", GitCommitCmd.LAST_MESSAGE);
        assertTrue(GitCommitCmd.LAST_ALL);
        assertFalse(GitCommitCmd.LAST_AMEND);
    }

    // ----- 2. docker run ---------------------------------------------

    @Test
    @DisplayName("docker run -d --rm --name web -p 8080:80 nginx:latest")
    void dockerRunDetachedNamedPort() {
        ExecutionResult<TestCommandSource> r =
                exec("docker run -d --rm --name web -p 8080:80 nginx:latest");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("nginx:latest", DockerRunCmd.LAST_IMAGE);
        assertTrue(DockerRunCmd.LAST_DETACH);
        assertTrue(DockerRunCmd.LAST_RM);
        assertEquals("web", DockerRunCmd.LAST_NAME);
        assertEquals("8080:80", DockerRunCmd.LAST_PORT);
    }

    // ----- 3. npm install --------------------------------------------

    @Test
    @DisplayName("npm install lodash --save-dev (long-form switch)")
    void npmInstallSaveDev() {
        ExecutionResult<TestCommandSource> r = exec("npm install lodash --save-dev");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("lodash", NpmInstallCmd.LAST_PACKAGE);
        assertTrue(NpmInstallCmd.LAST_SAVE_DEV);
        assertFalse(NpmInstallCmd.LAST_GLOBAL);
    }

    @Test
    @DisplayName("npm install typescript -g (single-char alias)")
    void npmInstallGlobalAlias() {
        ExecutionResult<TestCommandSource> r = exec("npm install typescript -g");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("typescript", NpmInstallCmd.LAST_PACKAGE);
        assertTrue(NpmInstallCmd.LAST_GLOBAL);
        assertFalse(NpmInstallCmd.LAST_SAVE_DEV);
    }

    // ----- 4. curl ---------------------------------------------------

    @Test
    @DisplayName("curl -X POST -H \"...\" -d \"...\" -o out.json https://api.example.com")
    void curlPostWithHeaderAndOutput() {
        ExecutionResult<TestCommandSource> r =
                exec("curl -X POST -H ContentType -d payload -o out.json https://api.example.com");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("https://api.example.com", CurlCmd.LAST_URL);
        assertEquals("POST", CurlCmd.LAST_METHOD);
        assertEquals("ContentType", CurlCmd.LAST_HEADER);
        assertEquals("payload", CurlCmd.LAST_DATA);
        assertEquals("out.json", CurlCmd.LAST_OUTPUT);
        assertFalse(CurlCmd.LAST_FOLLOW);
    }

    @Test
    @DisplayName("curl -L https://example.com (switch + bare positional)")
    void curlFollowRedirects() {
        ExecutionResult<TestCommandSource> r = exec("curl -L https://example.com");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("https://example.com", CurlCmd.LAST_URL);
        assertTrue(CurlCmd.LAST_FOLLOW);
        assertEquals("GET", CurlCmd.LAST_METHOD);
    }

    // ----- 5. grep ---------------------------------------------------

    @Test
    @DisplayName("grep -r -n -i --include=*.java TODO src (alias short forms + inline =)")
    void grepRecursiveCaseInsensitiveInclude() {
        ExecutionResult<TestCommandSource> r =
                exec("grep -r -n -i --include=*.java TODO src");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("TODO", GrepCmd.LAST_PATTERN);
        assertEquals("src", GrepCmd.LAST_PATH);
        assertTrue(GrepCmd.LAST_RECURSIVE);
        assertTrue(GrepCmd.LAST_LINE_NUMBER);
        assertTrue(GrepCmd.LAST_IGNORE_CASE);
        assertEquals("*.java", GrepCmd.LAST_INCLUDE);
    }

    // ----- 6. kubectl get --------------------------------------------

    @Test
    @DisplayName("kubectl get pods my-pod -n production -o yaml -l app=web")
    void kubectlGetPodWithSelector() {
        ExecutionResult<TestCommandSource> r =
                exec("kubectl get pods my-pod -n production -o yaml -l app=web");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("pods", KubectlGetCmd.LAST_RESOURCE);
        assertEquals("my-pod", KubectlGetCmd.LAST_NAME);
        assertEquals("production", KubectlGetCmd.LAST_NAMESPACE);
        assertEquals("yaml", KubectlGetCmd.LAST_OUTPUT);
        assertEquals("app=web", KubectlGetCmd.LAST_SELECTOR);
        assertFalse(KubectlGetCmd.LAST_ALL_NAMESPACES);
    }

    @Test
    @DisplayName("kubectl get nodes --all-namespaces (long-form switch + omitted optional name)")
    void kubectlGetNodesAllNs() {
        ExecutionResult<TestCommandSource> r = exec("kubectl get nodes --all-namespaces");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("nodes", KubectlGetCmd.LAST_RESOURCE);
        org.junit.jupiter.api.Assertions.assertNull(KubectlGetCmd.LAST_NAME);
        assertTrue(KubectlGetCmd.LAST_ALL_NAMESPACES);
    }

    // ----- 7. ssh ----------------------------------------------------

    @Test
    @DisplayName("ssh -i id_rsa -p 2222 -v admin@example.com uptime")
    void sshWithKeyPortAndCommand() {
        ExecutionResult<TestCommandSource> r =
                exec("ssh -i id_rsa -p 2222 -v admin@example.com uptime");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("admin@example.com", SshCmd.LAST_DESTINATION);
        assertEquals("id_rsa", SshCmd.LAST_KEY);
        assertEquals("2222", SshCmd.LAST_PORT);
        assertTrue(SshCmd.LAST_VERBOSE);
        assertEquals("uptime", SshCmd.LAST_REMOTE_COMMAND);
    }

    @Test
    @DisplayName("ssh user@host (defaults: port=22, no key, no remote command)")
    void sshDefaultsOnly() {
        ExecutionResult<TestCommandSource> r = exec("ssh user@host");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("user@host", SshCmd.LAST_DESTINATION);
        assertEquals("22", SshCmd.LAST_PORT);
        org.junit.jupiter.api.Assertions.assertNull(SshCmd.LAST_KEY);
        org.junit.jupiter.api.Assertions.assertNull(SshCmd.LAST_REMOTE_COMMAND);
        assertFalse(SshCmd.LAST_VERBOSE);
    }

    // ----- 8. aws s3 cp ---------------------------------------------

    @Test
    @DisplayName("aws s3 cp ./build s3://bucket/dist --recursive --acl public-read")
    void awsS3CpRecursive() {
        ExecutionResult<TestCommandSource> r =
                exec("aws s3 cp ./build s3://bucket/dist --recursive --acl public-read");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("./build", AwsS3CpCmd.LAST_SRC);
        assertEquals("s3://bucket/dist", AwsS3CpCmd.LAST_DST);
        assertTrue(AwsS3CpCmd.LAST_RECURSIVE);
        assertEquals("public-read", AwsS3CpCmd.LAST_ACL);
        assertEquals("STANDARD", AwsS3CpCmd.LAST_STORAGE_CLASS);
    }

    // ----- 9. ffmpeg -------------------------------------------------

    @Test
    @DisplayName("ffmpeg -i input.mp4 --vcodec libx264 --vbitrate 1M -y output.mp4")
    void ffmpegEncode() {
        ExecutionResult<TestCommandSource> r =
                exec("ffmpeg -i input.mp4 --vcodec libx264 --vbitrate 1M -y output.mp4");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertEquals("input.mp4", FfmpegCmd.LAST_INPUT);
        assertEquals("output.mp4", FfmpegCmd.LAST_OUTPUT);
        assertEquals("libx264", FfmpegCmd.LAST_VIDEO_CODEC);
        assertEquals("1M", FfmpegCmd.LAST_VIDEO_BITRATE);
        assertTrue(FfmpegCmd.LAST_OVERWRITE);
    }

    // ----- 10. tar ---------------------------------------------------

    @Test
    @DisplayName("tar --create --gzip --verbose --file backup.tar.gz src docs (multiple long switches + greedy)")
    void tarCreateArchive() {
        ExecutionResult<TestCommandSource> r =
                exec("tar --create --gzip --verbose --file backup.tar.gz src docs");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertTrue(TarCmd.LAST_CREATE);
        assertTrue(TarCmd.LAST_GZIP);
        assertTrue(TarCmd.LAST_VERBOSE);
        assertFalse(TarCmd.LAST_EXTRACT);
        assertEquals("backup.tar.gz", TarCmd.LAST_ARCHIVE);
        assertEquals("src docs", TarCmd.LAST_FILES);
    }

    @Test
    @DisplayName("tar -czv -f archive.tgz src (combined alias short switches)")
    void tarCombinedAliases() {
        ExecutionResult<TestCommandSource> r = exec("tar -czv -f archive.tgz src");
        assertFalse(r.hasFailed(), "exec failed: " + r.getError());
        assertTrue(TarCmd.LAST_CREATE);
        assertTrue(TarCmd.LAST_GZIP);
        assertTrue(TarCmd.LAST_VERBOSE);
        assertEquals("archive.tgz", TarCmd.LAST_ARCHIVE);
        assertEquals("src", TarCmd.LAST_FILES);
    }
}
